package com.aiear.capture.companion

import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.IntentSender
import android.util.Log
import androidx.core.content.getSystemService
import java.util.concurrent.Executor

/**
 * S2: one-time CompanionDeviceManager association onboarding + presence observation.
 *
 * The association is the user's explicit "these are my headphones" consent that unlocks the
 * ADR-002-legal background path: with REQUEST_COMPANION_START_FOREGROUND_SERVICES_FROM_BACKGROUND
 * the system may start our mic-FGS from [CompanionCaptureService.onDeviceAppeared] when the
 * associated device connects — no UI, no BOOT_COMPLETED, no silent-audio.
 *
 * Spike scope: associate + startObservingDevicePresence immediately. Re-observation across
 * reboots and multi-device management are F1 concerns (memory/cdm-bt).
 */
object CompanionPairing {
    const val TAG = "aiear.cdm"

    /**
     * Launch the system association chooser filtered to Bluetooth devices. [onPending] receives
     * the [IntentSender] the caller must launch from an Activity; [onAssociated] fires once the
     * user confirms (after which we begin observing presence).
     */
    fun associate(
        context: Context,
        executor: Executor,
        onPending: (IntentSender) -> Unit,
        onAssociated: () -> Unit = {},
    ) {
        val cdm = context.getSystemService<CompanionDeviceManager>()
        if (cdm == null) {
            Log.w(TAG, "CompanionDeviceManager unavailable on this device")
            return
        }
        val request =
            AssociationRequest.Builder()
                .addDeviceFilter(BluetoothDeviceFilter.Builder().build())
                .setSingleDevice(false)
                .build()
        cdm.associate(
            request,
            executor,
            object : CompanionDeviceManager.Callback() {
                override fun onAssociationPending(intentSender: IntentSender) {
                    onPending(intentSender)
                }

                override fun onAssociationCreated(associationInfo: AssociationInfo) {
                    observe(context, associationInfo)
                    onAssociated()
                }

                override fun onFailure(error: CharSequence?) {
                    Log.w(TAG, "association failed: $error")
                }
            },
        )
    }

    /** Begin presence observation so [CompanionCaptureService] gets appear/disappear callbacks. */
    fun observe(
        context: Context,
        info: AssociationInfo,
    ) {
        val cdm = context.getSystemService<CompanionDeviceManager>() ?: return
        val mac = info.deviceMacAddress
        if (mac == null) {
            Log.w(TAG, "association ${info.id} has no MAC — cannot observe presence")
            return
        }
        // Deprecated on API 35 in favour of ObservingDevicePresenceRequest, but the string form is
        // the API-34 (minSdk) surface and works here. F1 migrates when minSdk rises.
        @Suppress("DEPRECATION")
        cdm.startObservingDevicePresence(mac.toString())
        Log.i(TAG, "observing presence for association ${info.id} ($mac)")
    }
}
