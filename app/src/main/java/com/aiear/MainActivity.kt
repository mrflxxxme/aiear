package com.aiear

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.aiear.capture.companion.CompanionPairing
import com.aiear.capture.service.MicForegroundService
import com.aiear.ui.CaptureScreen
import com.aiear.ui.theme.AiearTheme

/**
 * Single screen. Requests RECORD_AUDIO (+ POST_NOTIFICATIONS on API 33+, + BLUETOOTH_CONNECT for
 * S2 CDM) up front, then hosts: a "pair headphones" action (S2 one-time association) and the
 * Start/Stop button. Starting the mic-FGS from this foreground Activity is the ADR-002-legal
 * manual start; the CDM path (CompanionCaptureService) is the legal *background* start.
 */
class MainActivity : ComponentActivity() {
    private var capturing by mutableStateOf(false)
    private var paired by mutableStateOf(false)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            // Result intentionally not blocking the UI; the spike operator grants in onboarding.
        }

    // The system association chooser runs via an IntentSender; the confirmed association is
    // delivered on CompanionDeviceManager.Callback.onAssociationCreated, not in this result.
    private val pairLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            // no-op: onAssociationCreated drives observation + the paired flag.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestRuntimePermissions()
        setContent {
            AiearTheme {
                CaptureScreen(
                    capturing = capturing,
                    paired = paired,
                    onToggle = { toggleCapture() },
                    onPair = { pairHeadphones() },
                )
            }
        }
    }

    private fun requestRuntimePermissions() {
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms += Manifest.permission.POST_NOTIFICATIONS
        }
        perms += Manifest.permission.BLUETOOTH_CONNECT
        permissionLauncher.launch(perms.toTypedArray())
    }

    /** S2 one-time onboarding: associate the headphones so the CDM background autostart applies. */
    private fun pairHeadphones() {
        CompanionPairing.associate(
            context = this,
            executor = mainExecutor,
            onPending = { sender ->
                pairLauncher.launch(IntentSenderRequest.Builder(sender).build())
            },
            onAssociated = { paired = true },
        )
    }

    private fun toggleCapture() {
        if (capturing) {
            ContextCompat.startForegroundService(this, MicForegroundService.stopIntent(this))
            capturing = false
        } else {
            // Legal manual start: from the foreground Activity (ADR-002).
            ContextCompat.startForegroundService(this, MicForegroundService.startIntent(this))
            capturing = true
        }
    }
}
