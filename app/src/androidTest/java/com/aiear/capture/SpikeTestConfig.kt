package com.aiear.capture

import androidx.test.platform.app.InstrumentationRegistry

/**
 * Reads instrumentation args so the survival run can be a quick 2-minute smoke locally or a
 * full 60-minute screen-off run on a founder device:
 *   -Pandroid.testInstrumentationRunnerArguments.durationMin=60
 *   -Pandroid.testInstrumentationRunnerArguments.forceDoze=true
 */
object SpikeTestConfig {
    private val args by lazy { InstrumentationRegistry.getArguments() }

    /** How long to hold screen-off capture. Default 2 min (sandbox/CI smoke). */
    val durationMin: Int
        get() = args.getString("durationMin", "2").toIntOrNull() ?: DEFAULT_DURATION_MIN

    /** Whether to force the device into Doze during the run (S1-AC3). */
    val forceDoze: Boolean
        get() = args.getString("forceDoze", "true").toBooleanStrictOrNull() ?: true

    val totalMs: Long
        get() = durationMin.toLong() * MILLIS_PER_MINUTE

    /**
     * Max allowed gap between heartbeats before we call the capture "broken". The beat period
     * is 10 s; 15 s tolerates one slow cycle / GC but flags any real stall or service kill.
     */
    const val MAX_ALLOWED_GAP_MS = 15_000L

    private const val DEFAULT_DURATION_MIN = 2
    private const val MILLIS_PER_MINUTE = 60_000L
}
