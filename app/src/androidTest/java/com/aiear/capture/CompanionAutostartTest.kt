package com.aiear.capture

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.aiear.capture.companion.CompanionCaptureService
import com.aiear.capture.service.MicForegroundService
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * S2 wiring test. Validates the parts of the CDM autostart path that are testable WITHOUT a real
 * paired Bluetooth headset (so it runs on a Gradle Managed Device / Firebase Test Lab / emulator):
 *
 *  - the CDM setup feature is present + our [CompanionCaptureService] is declared for the
 *    `android.companion.CompanionDeviceService` action (so the OS can bind it), and
 *  - the mic-FGS START / STOP contract that [CompanionCaptureService.onDeviceAppeared] /
 *    onDeviceDisappeared route to: starting arms a foreground mic notification; stopService
 *    tears it down and emits a STOPPED heartbeat (S2-AC3 "stop when headphones drop").
 *
 * The TRUE background trigger — the system calling onDeviceAppeared on a real BT connect while
 * the app is closed — is device-only and is the founder device_survival evidence_gap
 * (run-on-device.sh). This test can NEVER pass silently: a missing service or a service that
 * fails to stop surfaces as a concrete assertion failure.
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
class CompanionAutostartTest {
    private val device: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun grantPermissionsAndLaunch() {
        val pkg = context.packageName
        shell("pm grant $pkg android.permission.RECORD_AUDIO")
        shell("pm grant $pkg android.permission.POST_NOTIFICATIONS")
        shell("pm grant $pkg android.permission.BLUETOOTH_CONNECT")
        shell("monkey -p $pkg -c android.intent.category.LAUNCHER 1")
        device.waitForIdle()
    }

    @After
    fun cleanup() {
        runCatching { context.stopService(Intent(context, MicForegroundService::class.java)) }
    }

    @Test
    fun cdmFeature_and_service_declared() {
        assertTrue(
            "Device lacks FEATURE_COMPANION_DEVICE_SETUP — CDM autostart cannot work here.",
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP),
        )
        val intent =
            Intent("android.companion.CompanionDeviceService").setPackage(context.packageName)
        val resolved =
            context.packageManager.queryIntentServices(
                intent,
                PackageManager.ResolveInfoFlags.of(0L),
            )
        assertTrue(
            "CompanionCaptureService is not registered for the CDM action — the OS cannot bind " +
                "it, so onDeviceAppeared will never fire (S2-AC1/AC2 unreachable).",
            resolved.any { it.serviceInfo.name == CompanionCaptureService::class.java.name },
        )
    }

    @Test
    fun micFgs_startStop_contract_the_cdm_callbacks_use() {
        // The exact intent onDeviceAppeared sends.
        context.startForegroundService(MicForegroundService.startIntent(context))
        assertTrue(
            "mic-FGS notification (channel ${NotificationHelper.CHANNEL_ID}) did not appear after " +
                "start — the 'headphones connected → armed' contract is broken.",
            waitFor(START_TIMEOUT_MS) { micNotificationPresent() },
        )

        // The exact call onDeviceDisappeared makes.
        context.stopService(Intent(context, MicForegroundService::class.java))
        assertTrue(
            "No STOPPED heartbeat after stopService — capture did not tear down when the " +
                "headphones 'disappeared' (S2-AC3 violated: mic could stay open without them).",
            waitFor(STOP_TIMEOUT_MS) { heartbeatHasState("STOPPED") },
        )
        assertTrue(
            "mic-FGS notification still present after stop — the service was not foreground-stopped.",
            waitFor(STOP_TIMEOUT_MS) { !micNotificationPresent() },
        )
    }

    private fun heartbeatHasState(state: String): Boolean =
        heartbeatFiles().any { file ->
            runCatching { file.readText().contains("state=$state") }.getOrDefault(false)
        }

    private fun heartbeatFiles(): List<File> =
        (context.filesDir.listFiles { f -> f.name.startsWith("heartbeat_") } ?: emptyArray())
            .toList()

    private fun micNotificationPresent(): Boolean =
        shell("dumpsys notification --noredact").contains(NotificationHelper.CHANNEL_ID)

    private fun waitFor(
        timeoutMs: Long,
        predicate: () -> Boolean,
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (predicate()) return true
            Thread.sleep(POLL_MS)
        }
        return predicate()
    }

    private fun shell(cmd: String): String = device.executeShellCommand(cmd)

    private companion object {
        const val START_TIMEOUT_MS = 8_000L
        const val STOP_TIMEOUT_MS = 8_000L
        const val POLL_MS = 500L
    }
}
