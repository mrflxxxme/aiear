package com.aiear.capture

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Records capture liveness. Every beat goes to Logcat (tag [TAG]) and is appended to a
 * per-session file `heartbeat_<session>.log` under filesDir, so a screen-off run can be
 * pulled off-device and parsed for gaps (S1-AC1/AC3).
 *
 * The [Companion] holds PURE, Android-free functions ([format], [maxGapMs]) — this is the
 * single behavioural seam shared by the JVM unit test and the instrumented survival test.
 * An interruption is ALWAYS surfaced as a logged beat (READ_ERR/EXC); never silent.
 */
class HeartbeatLogger(
    context: Context,
    session: String,
    private val startElapsedMs: Long,
) {
    private val logFile: File = File(context.filesDir, "heartbeat_$session.log")

    /**
     * Emit one heartbeat: `state` is the capture state, `bytes` the cumulative PCM bytes
     * written so far. Elapsed time is measured from the logger's start.
     */
    fun beat(
        state: State,
        bytes: Long,
        nowElapsedMs: Long = android.os.SystemClock.elapsedRealtime(),
    ) {
        val line = format(nowElapsedMs - startElapsedMs, state.name, bytes)
        Log.i(TAG, line)
        runCatching { logFile.appendText(line + "\n") }
            .onFailure { Log.w(TAG, "heartbeat append failed: ${it.message}") }
    }

    /** Capture states a beat can report. */
    enum class State {
        /** Audio bytes were actually captured during the last interval. */
        ALIVE,

        /** Service alive but NO audio bytes captured in the interval (muted/Doze-throttled mic). */
        STALL,
        READ_ERR,
        EXC,
        STOPPED,
    }

    companion object {
        const val TAG = "AIEAR_S1"

        /**
         * Stable, greppable heartbeat line. Format is load-bearing — the survival test
         * parses `t=` timestamps back out of it, so keep it exactly:
         * `HEARTBEAT t=<elapsedMs> state=<state> bytes=<n>`.
         */
        fun format(
            elapsedMs: Long,
            state: String,
            bytes: Long,
        ): String = "HEARTBEAT t=$elapsedMs state=$state bytes=$bytes"

        /**
         * Largest gap between consecutive (assumed-sorted) heartbeat timestamps.
         * Returns 0 for empty or single-element input. This is the continuity metric:
         * a service kill/Doze stall shows up as a gap far larger than the 10s beat period.
         */
        fun maxGapMs(timestamps: List<Long>): Long {
            if (timestamps.size < 2) return 0L
            var max = 0L
            for (i in 1 until timestamps.size) {
                val delta = timestamps[i] - timestamps[i - 1]
                if (delta > max) max = delta
            }
            return max
        }

        /**
         * Worst within-session gap across ALL sessions. Each inner list is one session's
         * timestamps (one `heartbeat_<session>.log` file). Gaps are computed PER SESSION and
         * never across sessions — a kill+restart resets `t=` to ~0, and merging the two axes
         * would hide the kill behind a small cross-session delta (the exact false-PASS the S1
         * spike must avoid). Caller treats >1 session as a kill independently of this value.
         */
        fun maxGapWithinSessions(sessions: List<List<Long>>): Long {
            var worst = 0L
            for (session in sessions) {
                val gap = maxGapMs(session)
                if (gap > worst) worst = gap
            }
            return worst
        }
    }
}
