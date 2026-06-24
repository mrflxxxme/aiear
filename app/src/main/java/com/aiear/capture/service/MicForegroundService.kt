package com.aiear.capture.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.aiear.capture.HeartbeatLogger
import com.aiear.capture.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.coroutineContext

/**
 * Minimal mic foreground service for the S1 spike: starts mic-FGS from the foreground
 * Activity, records PCM16 mono @16 kHz to a per-session file, and emits a heartbeat every
 * [HEARTBEAT_PERIOD_MS] so a screen-off run can be checked for continuity.
 *
 * Concurrency: extends [LifecycleService] for a [lifecycleScope] that is cancelled in
 * onDestroy; the capture loop runs on [Dispatchers.IO]. START_STICKY asks the OS to restart
 * after a kill — a restart shows up as a heartbeat gap, which is the diagnostic signal.
 *
 * ADR-002: the ONLY legal start is startForegroundService() from the foreground Activity.
 * No BOOT_COMPLETED, no AccessibilityService, no silent-audio.
 */
class MicForegroundService : LifecycleService() {
    private var captureJob: Job? = null
    private var session: String = "0"

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> startCapture()
            ACTION_STOP -> {
                stopCapture()
                stopSelf()
            }
            else -> Log.w(HeartbeatLogger.TAG, "unknown action: ${intent?.action}")
        }
        // START_NOT_STICKY for the spike: an OS/OEM kill is TERMINAL and must stay visible as
        // the heartbeat log ending early — we are measuring *continuous* survival, not auto-
        // restart resilience. Sticky redelivery (null intent) would land in `else` without re-
        // promoting to foreground (FGS-did-not-start crash) and would muddy a kill with a
        // restart; resilience is an F1 concern, not S1.
        return START_NOT_STICKY
    }

    private fun startCapture() {
        if (captureJob?.isActive == true) return
        session = SystemClock.elapsedRealtime().toString()

        // Defensive: guarantee the channel exists even if Application.onCreate ordering changes.
        NotificationHelper.ensureChannel(this)

        // S1-AC2: promote to foreground with the microphone type BEFORE touching AudioRecord.
        ServiceCompat.startForeground(
            this,
            NotificationHelper.NOTIF_ID,
            NotificationHelper.buildCaptureNotification(this),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
        )

        val heartbeat = HeartbeatLogger(this, session, SystemClock.elapsedRealtime())
        captureJob =
            lifecycleScope.launch(Dispatchers.IO) {
                runCaptureLoop(heartbeat, File(filesDir, "capture_$session.pcm"))
            }
    }

    /**
     * The audio read loop. Reads from [AudioRecord] into [pcmFile] and beats ALIVE every
     * [HEARTBEAT_PERIOD_MS]. Any read failure or exception emits a READ_ERR/EXC beat so an
     * interruption is recorded, never silent (S1-AC3). Permission is gated in the Activity.
     */
    @SuppressLint("MissingPermission")
    private suspend fun runCaptureLoop(
        heartbeat: HeartbeatLogger,
        pcmFile: File,
    ) {
        val minBuffer =
            AudioRecord.getMinBufferSize(SAMPLE_RATE_HZ, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize =
            if (minBuffer > 0) minBuffer * BUFFER_MULTIPLIER else FALLBACK_BUFFER_BYTES

        var recorder: AudioRecord? = null
        var totalBytes = 0L
        var lastBeat = SystemClock.elapsedRealtime()
        try {
            recorder =
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE_HZ,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize,
                )
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                heartbeat.beat(HeartbeatLogger.State.READ_ERR, totalBytes)
                return
            }
            recorder.startRecording()
            val buffer = ByteArray(bufferSize)
            var bytesThisInterval = 0L
            pcmFile.outputStream().buffered().use { out ->
                while (coroutineContext.isActive) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        out.write(buffer, 0, read)
                        totalBytes += read
                        bytesThisInterval += read
                    } else if (read < 0) {
                        // Negative return == AudioRecord error code: record it, keep looping.
                        heartbeat.beat(HeartbeatLogger.State.READ_ERR, totalBytes)
                    }
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastBeat >= HEARTBEAT_PERIOD_MS) {
                        // ALIVE only if audio actually flowed this interval; a service that is
                        // alive but captured 0 bytes (muted/Doze-throttled mic) beats STALL, so
                        // S1-AC1 cannot be passed by mere service liveness without audio.
                        val state =
                            if (bytesThisInterval > 0L) {
                                HeartbeatLogger.State.ALIVE
                            } else {
                                HeartbeatLogger.State.STALL
                            }
                        heartbeat.beat(state, totalBytes)
                        bytesThisInterval = 0L
                        lastBeat = now
                        out.flush()
                    }
                    // Yield + respect cancellation between reads.
                    coroutineContext.ensureActive()
                    delay(LOOP_IDLE_MS)
                }
            }
        } catch (t: Throwable) {
            // Broad on purpose: the interruption MUST be visible in the log (S1-AC3).
            heartbeat.beat(HeartbeatLogger.State.EXC, totalBytes)
            Log.e(HeartbeatLogger.TAG, "capture loop exception", t)
        } finally {
            runCatching {
                recorder?.stop()
            }
            recorder?.release()
            heartbeat.beat(HeartbeatLogger.State.STOPPED, totalBytes)
        }
    }

    private fun stopCapture() {
        captureJob?.cancel()
        captureJob = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.aiear.capture.action.START"
        const val ACTION_STOP = "com.aiear.capture.action.STOP"

        private const val SAMPLE_RATE_HZ = 16_000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_MULTIPLIER = 2
        private const val FALLBACK_BUFFER_BYTES = 4096
        private const val HEARTBEAT_PERIOD_MS = 10_000L
        private const val LOOP_IDLE_MS = 20L

        /** Build a START intent targeting this service. */
        fun startIntent(context: Context): Intent =
            Intent(context, MicForegroundService::class.java).setAction(ACTION_START)

        /** Build a STOP intent targeting this service. */
        fun stopIntent(context: Context): Intent =
            Intent(context, MicForegroundService::class.java).setAction(ACTION_STOP)
    }
}
