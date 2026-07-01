package com.aiear.capture

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM unit tests for the PURE heartbeat seam. This is the only S1 behaviour executable in a
 * sandbox without a device: it pins the log format (so the survival parser stays valid) and
 * proves [HeartbeatLogger.maxGapMs] catches a continuity break (S1-AC1 proxy).
 */
class HeartbeatLoggerTest {
    @Test
    fun format_isExactAndGreppable() {
        assertEquals(
            "HEARTBEAT t=10000 state=ALIVE bytes=320000",
            HeartbeatLogger.format(10_000L, "ALIVE", 320_000L),
        )
    }

    @Test
    fun format_zeroValues() {
        assertEquals(
            "HEARTBEAT t=0 state=STOPPED bytes=0",
            HeartbeatLogger.format(0L, "STOPPED", 0L),
        )
    }

    @Test
    fun maxGapMs_findsLargestConsecutiveDelta() {
        // 0->10k=10k, 10k->20k=10k, 20k->60k=40k, 60k->70k=10k  => max 40k (the "break").
        assertEquals(
            40_000L,
            HeartbeatLogger.maxGapMs(listOf(0L, 10_000L, 20_000L, 60_000L, 70_000L)),
        )
    }

    @Test
    fun maxGapMs_emptyIsZero() {
        assertEquals(0L, HeartbeatLogger.maxGapMs(emptyList()))
    }

    @Test
    fun maxGapMs_singletonIsZero() {
        assertEquals(0L, HeartbeatLogger.maxGapMs(listOf(42_000L)))
    }

    @Test
    fun maxGapWithinSessions_reportsWorstInternalGap_notCrossSessionBlend() {
        // Session A has a 40 s internal break; session B is a kill+restart (t= back to ~0).
        // A naive cross-session sort would be [0,10k,10k,20k,50k] -> max delta 30k, HIDING
        // A's real 40 k break. Per-session analysis must report A's 40 000 ms.
        val sessionA = listOf(0L, 10_000L, 50_000L) // 40 s gap = the kill we hunt
        val sessionB = listOf(0L, 10_000L, 20_000L) // clean, but t= restarted
        assertEquals(
            40_000L,
            HeartbeatLogger.maxGapWithinSessions(listOf(sessionA, sessionB)),
        )
    }

    @Test
    fun maxGapWithinSessions_emptyIsZero() {
        assertEquals(0L, HeartbeatLogger.maxGapWithinSessions(emptyList()))
    }
}
