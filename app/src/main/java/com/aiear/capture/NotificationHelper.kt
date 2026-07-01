package com.aiear.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.aiear.R

/**
 * Owns the FGS notification channel + the ongoing mic notification required while the
 * service runs in the background (S1-AC2). LOW importance keeps it quiet but persistent.
 */
object NotificationHelper {
    const val CHANNEL_ID = "aiear_capture"
    const val NOTIF_ID = 4201

    /** Idempotent channel registration. Safe to call repeatedly (Application + service). */
    fun ensureChannel(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.notif_channel_desc)
                setShowBadge(false)
            }
        manager.createNotificationChannel(channel)
    }

    /** The ongoing notification that the OS shows for the active mic-FGS. */
    fun buildCaptureNotification(context: Context): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notif_title))
            .setContentText(context.getString(R.string.notif_text))
            // Platform icon — avoids shipping a binary asset for the spike.
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
}
