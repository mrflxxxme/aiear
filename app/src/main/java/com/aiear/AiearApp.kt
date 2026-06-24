package com.aiear

import android.app.Application
import com.aiear.capture.NotificationHelper

/**
 * Process-level entry point. Registers the FGS notification channel up front so the
 * mic service can post its ongoing notification immediately on start (S1-AC2).
 */
class AiearApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
    }
}
