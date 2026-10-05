package com.mcc.construction.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncScheduleTest {

    @Test
    fun convertsEverySupportedInterval() {
        assertEquals(300_000L, SyncSchedule.intervalMillis(5))
        assertEquals(900_000L, SyncSchedule.intervalMillis(15))
        assertEquals(1_800_000L, SyncSchedule.intervalMillis(30))
        assertEquals(3_600_000L, SyncSchedule.intervalMillis(60))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnsupportedInterval() {
        SyncSchedule.intervalMillis(7)
    }
}