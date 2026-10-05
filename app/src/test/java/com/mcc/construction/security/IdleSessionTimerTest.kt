package com.mcc.construction.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IdleSessionTimerTest {

    @Test
    fun expiresAtConfiguredBoundary() {
        val timer = IdleSessionTimer()
        timer.attach(5, 0L)

        assertFalse(timer.isExpired(899_999L, 15))
        assertTrue(timer.isExpired(900_000L, 15))
    }

    @Test
    fun interactionBeforeExpiryRestartsIdlePeriod() {
        val timer = IdleSessionTimer()
        timer.attach(5, 0L)

        assertTrue(timer.recordActivity(600_000L, 15))
        assertFalse(timer.isExpired(1_499_999L, 15))
        assertTrue(timer.isExpired(1_500_000L, 15))
    }

    @Test
    fun interactionAfterExpiryCannotReviveSession() {
        val timer = IdleSessionTimer()
        timer.attach(5, 0L)

        assertFalse(timer.recordActivity(900_000L, 15))
        assertTrue(timer.isExpired(900_001L, 120))
    }

    @Test
    fun attachingSameAccountDoesNotResetIdleTime() {
        val timer = IdleSessionTimer()
        timer.attach(5, 0L)
        timer.attach(5, 899_000L)

        assertTrue(timer.isExpired(900_000L, 15))
    }

    @Test
    fun newAccountStartsNewIdlePeriod() {
        val timer = IdleSessionTimer()
        timer.attach(5, 0L)
        timer.attach(6, 900_000L)

        assertFalse(timer.isExpired(900_001L, 15))
        assertTrue(timer.isExpired(1_800_000L, 15))
    }

    @Test
    fun shorterAdministratorTimeoutAppliesImmediately() {
        val timer = IdleSessionTimer()
        timer.attach(5, 0L)

        assertFalse(timer.isExpired(1_200_000L, 30))
        assertTrue(timer.isExpired(1_200_000L, 15))
    }

    @Test
    fun clearAllowsFreshSessionForSameAccount() {
        val timer = IdleSessionTimer()
        timer.attach(5, 0L)
        assertTrue(timer.isExpired(900_000L, 15))

        timer.clear()
        timer.attach(5, 1_000_000L)

        assertFalse(timer.isExpired(1_000_001L, 15))
    }

    @Test
    fun backgroundElapsedTimeStillCounts() {
        val timer = IdleSessionTimer()
        timer.attach(5, 100L)

        // No activity while the app is in the background.
        assertTrue(timer.isExpired(900_100L, 15))
    }
}