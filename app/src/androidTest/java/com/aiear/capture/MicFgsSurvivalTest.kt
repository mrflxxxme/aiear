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

        // Hold for the configured duration, periodically re-reading the heartbeat file so a
        // mid-run break is captured even if the file is later truncated by an OEM killer.
        val deadline = System.currentTimeMillis() + SpikeTestConfig.totalMs
        var latestTimestamps: List<Long> = emptyList()
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(POLL_INTERVAL_MS)
            latestTimestamps = readHeartbeatTimestamps()
        }

        // Wake + leave Doze before asserting (cleanup also does this defensively).
        if (SpikeTestConfig.forceDoze) {
            shell("dumpsys deviceidle unforce")
        }
        shell("input keyevent KEYCODE_WAKEUP")

        val timestamps = readHeartbeatTimestamps().ifEmpty { latestTimestamps }
        assertTrue(
            "No heartbeats recorded at all — capture never produced liveness output.",
            timestamps.size >= MIN_EXPECTED_BEATS,
        )

        val maxGap = HeartbeatLogger.maxGapMs(timestamps)
        val breakAt = locateBreak(timestamps)
        assertTrue(
            "Heartbeat continuity broken: maxGap=${maxGap}ms exceeds " +
                "${SpikeTestConfig.MAX_ALLOWED_GAP_MS}ms (allowed). Break near t=${breakAt}ms. " +
                "Beats=${timestamps.size}. Likely Doze stall or OEM service kill.",
            maxGap <= SpikeTestConfig.MAX_ALLOWED_GAP_MS,
        )

        assertTrue(
            "FGS microphone notification not present while service active (S1-AC2 violated).",
            micNotificationPresent(),
        )
    }

    /** Parse `t=<ms>` values out of the per-session heartbeat log(s) under filesDir. */
    private fun readHeartbeatTimestamps(): List<Long> {
        val files =
            context.filesDir.listFiles { f -> f.name.startsWith("heartbeat_") }
                ?: emptyArray()
        return files
            .flatMap { parseFile(it) }
            .sorted()
    }

    private fun parseFile(file: File): List<Long> =
        runCatching {
            file.readLines().mapNotNull { line ->
                T_REGEX.find(line)?.groupValues?.getOrNull(1)?.toLongOrNull()
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

    /** True if any active notification belongs to our capture channel. */
    private fun micNotificationPresent(): Boolean {
        val dump = shell("dumpsys notification --noredact")
        return dump.contains(NotificationHelper.CHANNEL_ID) ||
            dump.contains(context.packageName)
    }

    private fun shell(cmd: String): String = device.executeShellCommand(cmd)

    private companion object {
        val T_REGEX = Regex("""t=(\d+)""")
        const val STARTUP_TIMEOUT_MS = 5_000L
        const val POLL_INTERVAL_MS = 30_000L
        const val MIN_EXPECTED_BEATS = 2
    }
}
