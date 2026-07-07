package com.aiear.capture.companion

import android.Manifest
import android.companion.AssociationInfo
import android.companion.CompanionDeviceService
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.aiear.capture.service.MicForegroundService

/**
 * S2: the ADR-002-legal background trigger. The system binds this service
 * (BIND_COMPANION_DEVICE_SERVICE) and invokes [onDeviceAppeared] when an associated + observed
 * Bluetooth device connects, [onDeviceDisappeared] when it drops.
 *
 * - onDeviceAppeared -> start mic-FGS from the background, legalised by
 *   REQUEST_COMPANION_START_FOREGROUND_SERVICES_FROM_BACKGROUND: "надел наушники → EARAI взвёлся"
 *   (S2-AC1/AC2).
 * - onDeviceDisappeared -> stop it (S2-AC3): never hold the mic without the headphones.
 *
 * The mic-FGS itself owns the microphone-typed startForeground (S1). This service only routes
 * the CDM presence signal to the existing START/STOP surface — no new mic path is introduced.
 */
class CompanionCaptureService : CompanionDeviceService() {
    override fun onDeviceAppeared(associationInfo: AssociationInfo) {
        // Without RECORD_AUDIO a microphone-typed startForeground throws on API 34. Skip (no audio
        // without permission is privacy-correct); the user re-grants in onboarding.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "onDeviceAppeared assoc=${associationInfo.id}: RECORD_AUDIO not granted — skip")
            return
        }
        Log.i(TAG, "onDeviceAppeared assoc=${associationInfo.id} -> start mic-FGS")
        // Legal background FGS start (REQUEST_COMPANION_START_FOREGROUND_SERVICES_FROM_BACKGROUND).
        ContextCompat.startForegroundService(this, MicForegroundService.startIntent(this))
    }

    override fun onDeviceDisappeared(associationInfo: AssociationInfo) {
        Log.i(TAG, "onDeviceDisappeared assoc=${associationInfo.id} -> stop mic-FGS")
        // stopService -> onDestroy -> stopCapture cleanup. Avoids the "start-FGS-just-to-stop"
        // crash you would get by delivering a STOP action via startForegroundService when the
        // service is not currently running.
        stopService(Intent(this, MicForegroundService::class.java))
    }

    private companion object {
        const val TAG = "aiear.cdm"
    }
}
