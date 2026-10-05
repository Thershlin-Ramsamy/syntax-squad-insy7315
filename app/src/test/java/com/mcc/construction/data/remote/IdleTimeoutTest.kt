package com.mcc.construction.data.remote

import org.junit.Assert.*
import org.junit.Test

class IdleTimeoutTest {
    @Test
    fun expiresAtTheConfiguredBoundary() {
        assertFalse(
            IdleTimeout.expired(
                now = 899_999L,
                lastActivity = 0L,
                minutes = 15
            )
        )

        assertTrue(
            IdleTimeout.expired(
                now = 900_000L,
                lastActivity = 0L,
                minutes = 15
            )
        )
    }

    @Test
    fun newInteractionMovesTheDeadline() {
        assertFalse(IdleTimeout.expired(900_000L, 60_000L, 15))
    }

    @Test
    fun backwardsClockChangeCannotExtendAnExistingSession() {
        assertTrue(IdleTimeout.expired(1L, 2L, 30))
    }
}