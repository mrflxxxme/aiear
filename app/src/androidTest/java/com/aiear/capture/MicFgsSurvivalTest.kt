package com.aiear.capture

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.aiear.capture.service.MicForegroundService
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * S1 survival test (S1-AC1/AC2/AC3). Will NOT run in this sandbox (no device/KVM) — it must
 * COMPILE here and run on a Gradle Managed Device / Firebase Test Lab / founder phone.
 *
 * Flow: grant perms via shell -> start mic-FGS -> KEYCODE_SLEEP (screen off) -> optional
 * `dumpsys deviceidle force-idle` (Doze) -> poll for SpikeTestConfig.totalMs, periodically
 * re-reading the heartbeat file -> wake + un-Doze -> parse timestamps -> assert
 * maxGapMs <= MAX_ALLOWED_GAP_MS AND the FGS notification is present.
 *
 * It can NEVER pass silently: a stall/kill surfaces as a parsed gap and a failure message
 * pointing at the break point.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MicFgsSurvivalTest {
    private val device: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun grantPermissionsAndLaunch() {
        val pkg = context.packageName
        shell("pm grant $pkg android.permission.RECORD_AUDIO")
        shell("pm grant $pkg android.permission.POST_NOTIFICATIONS")
        // Bring the app to the foreground (legal mic-FGS start surface, ADR-002).
        shell("monkey -p $pkg -c android.intent.category.LAUNCHER 1")
        device.waitForIdle()
    }

    @After
    fun cleanup() {
        runCatching { context.startService(MicForegroundService.stopIntent(context)) }
        runCatching { shell("dumpsys deviceidle unforce") }
        runCatching { shell("input keyevent KEYCODE_WAKEUP") }
    }

    @Test
    fun capture_survives_screenOff_and_doze() {
        // Start capture from the foreground app context.
        context.startForegroundService(MicForegroundService.startIntent(context))
        device.wait(Until.hasObject(By.textContains("AIEAR")), STARTUP_TIMEOUT_MS)

        // Screen off.
        shell("input keyevent KEYCODE_SLEEP")

        // Force Doze if requested (S1-AC3).
        if (SpikeTestConfig.forceDoze) {
            shell("dumpsys deviceidle force-idle")
        }

        // Hold for the configured duration. While the screen is OFF, sample the FGS
        // notification — S1-AC2 must hold DURING screen-off, not merely after wake.
        val deadline = System.currentTimeMillis() + SpikeTestConfig.totalMs
        var notifSeenDuringScreenOff = false
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(POLL_INTERVAL_MS)
            if (micNotificationPresent()) notifSeenDuringScreenOff = true
        }

        // Wake + leave Doze before final assertions (cleanup also does this defensively).
        if (SpikeTestConfig.forceDoze) {
            shell("dumpsys deviceidle unforce")
        }
        shell("input keyevent KEYCODE_WAKEUP")

        val sessions = readSessions()
        val beats = sessions.sumOf { it.size }
        assertTrue(
            "No heartbeats recorded at all — capture never produced liveness output " +
                "(permissions? MIUI autostart? run-as blocked?).",
            beats >= MIN_EXPECTED_BEATS,
        )

        // Under START_NOT_STICKY a kill is TERMINAL: more than one session file means the
        // service died and was relaunched — a survival FAILURE. Gaps are computed PER SESSION
        // (never a cross-session sort), so a kill cannot hide behind a small blended delta.
        assertTrue(
            "Service restarted: ${sessions.size} heartbeat sessions found (expected 1). " +
                "A new session = the original mic-FGS was KILLED → S1-AC1 not met.",
            sessions.size == 1,
        )

        // The single session must span ~the whole window; ending early = killed mid-run
        // (this is how a START_NOT_STICKY kill surfaces — one file that simply stops).
        val lastT = sessions.flatten().maxOrNull() ?: 0L
        assertTrue(
            "Capture ended early at t=${lastT}ms, expected ~${SpikeTestConfig.totalMs}ms " +
                "(tolerance ${EARLY_END_TOLERANCE_MS}ms) → service killed before the window ended.",
            lastT >= SpikeTestConfig.totalMs - EARLY_END_TOLERANCE_MS,
        )

        // Continuity within the session.
        val worstGap = HeartbeatLogger.maxGapWithinSessions(sessions)
        val breakAt = locateBreak(sessions.firstOrNull().orEmpty())
        assertTrue(
            "Heartbeat continuity broken: worstGap=${worstGap}ms exceeds " +
                "${SpikeTestConfig.MAX_ALLOWED_GAP_MS}ms. Break near t=${breakAt}ms. " +
                "Likely Doze stall or OEM service kill.",
            worstGap <= SpikeTestConfig.MAX_ALLOWED_GAP_MS,
        )

        // S1-AC1 proper: AUDIO actually flowed. Assert the PCM byte counter grew past a
        // conservative floor proportional to elapsed capture — a muted-but-alive mic (STALL
        // beats, 0 bytes) cannot pass this even with perfect heartbeat continuity.
        val maxBytes = readMaxBytes()
        val floorBytes = lastT / MILLIS_PER_SEC * BYTES_PER_SEC * MIN_CAPTURE_PERCENT / PERCENT
        assertTrue(
            "Audio did not flow: captured maxBytes=$maxBytes < floor=$floorBytes " +
                "(>=$MIN_CAPTURE_PERCENT% of nominal ${BYTES_PER_SEC}B/s over ${lastT}ms). " +
                "Service may be alive but the mic is muted/throttled → S1-AC1 not met.",
            maxBytes in (floorBytes + 1)..Long.MAX_VALUE,
        )

        // S1-AC2: the mic FGS notification (OUR channel) was present DURING screen-off.
        assertTrue(
            "FGS microphone notification (channel ${NotificationHelper.CHANNEL_ID}) was not " +
                "present during screen-off → S1-AC2 violated.",
            notifSeenDuringScreenOff,
        )
    }

    /** One sorted timestamp list per `heartbeat_<session>.log` — NEVER merged across sessions. */
    private fun readSessions(): List<List<Long>> =
        heartbeatFiles()
            .map { parseLongs(it, T_REGEX).sorted() }
            .filter { it.isNotEmpty() }

    /** Largest cumulative `bytes=` value seen across all session logs. */
    private fun readMaxBytes(): Long =
        heartbeatFiles()
            .flatMap { parseLongs(it, BYTES_REGEX) }
            .maxOrNull() ?: 0L

    private fun heartbeatFiles(): List<File> =
        (context.filesDir.listFiles { f -> f.name.startsWith("heartbeat_") } ?: emptyArray())
            .toList()

    private fun parseLongs(
        file: File,
        regex: Regex,
    ): List<Long> =
        runCatching {
            file.readLines().mapNotNull { line ->
                regex.find(line)?.groupValues?.getOrNull(1)?.toLongOrNull()
            }
        }.getOrDefault(emptyList())

    /** Elapsed-time location of the largest gap's start, for a human-pointable message. */
    private fun locateBreak(ts: List<Long>): Long {
        if (ts.size < 2) return 0L
        var maxDelta = -1L
        var at = 0L
        for (i in 1 until ts.size) {
            val d = ts[i] - ts[i - 1]
            if (d > maxDelta) {
                maxDelta = d
                at = ts[i - 1]
            }
        }
        return at
    }

    /** True only if a live notification belongs to OUR capture channel specifically. */
    private fun micNotificationPresent(): Boolean =
        shell("dumpsys notification --noredact").contains(NotificationHelper.CHANNEL_ID)

    private fun shell(cmd: String): String = device.executeShellCommand(cmd)

    private companion object {
        val T_REGEX = Regex("""t=(\d+)""")
        val BYTES_REGEX = Regex("""bytes=(\d+)""")
        const val STARTUP_TIMEOUT_MS = 5_000L
        const val POLL_INTERVAL_MS = 30_000L
        const val MIN_EXPECTED_BEATS = 2

        // Nominal PCM byte rate: 16 kHz * 16-bit * mono = 32 000 B/s.
        const val BYTES_PER_SEC = 32_000L
        const val MILLIS_PER_SEC = 1_000L
        const val PERCENT = 100L
        // Require >= this % of nominal bytes: conservative vs OEM throttle, but a muted mic
        // (~0 bytes) fails it.
        const val MIN_CAPTURE_PERCENT = 25L
        // The last beat may trail the deadline by up to one poll interval + a beat period.
        const val EARLY_END_TOLERANCE_MS = 45_000L
    }
}
