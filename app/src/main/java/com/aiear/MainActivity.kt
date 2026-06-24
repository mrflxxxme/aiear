package com.aiear

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.aiear.capture.service.MicForegroundService
import com.aiear.ui.CaptureScreen
import com.aiear.ui.theme.AiearTheme

/**
 * Single screen. Requests RECORD_AUDIO (+ POST_NOTIFICATIONS on API 33+) up front, then
 * hosts the Start/Stop button. Starting the mic-FGS from this foreground Activity is the
 * only ADR-002-legal start surface.
 */
class MainActivity : ComponentActivity() {
    private var capturing by mutableStateOf(false)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            // Result intentionally not blocking the UI; the spike operator grants in onboarding.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestRuntimePermissions()
        setContent {
            AiearTheme {
                CaptureScreen(
                    capturing = capturing,
                    onToggle = { toggleCapture() },
                )
            }
        }
    }

    private fun requestRuntimePermissions() {
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms += Manifest.permission.POST_NOTIFICATIONS
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    private fun toggleCapture() {
        if (capturing) {
            ContextCompat.startForegroundService(this, MicForegroundService.stopIntent(this))
            capturing = false
        } else {
            // Legal start: from the foreground Activity (ADR-002).
            ContextCompat.startForegroundService(this, MicForegroundService.startIntent(this))
            capturing = true
        }
    }
}
