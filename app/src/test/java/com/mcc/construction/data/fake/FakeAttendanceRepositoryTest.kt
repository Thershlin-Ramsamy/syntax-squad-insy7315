package com.mcc.construction.data.fake

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FakeAttendanceRepositoryTest {

    private val attendance = testContainer().attendanceRepository
    private val workerId = 5

    @Test
    fun clockInOpensAShiftAndClockingInAgainReturnsTheSameOne() = runTest {
        val first = attendance.clockIn(workerId)
        val second = attendance.clockIn(workerId)

        assertEquals(first, second)
        assertEquals(first, attendance.observeOpenShift(workerId).first())
    }

    @Test
    fun clockOutClosesTheShift() = runTest {
        attendance.clockIn(workerId)

        val closed = attendance.clockOut(workerId)

        assertNotNull(closed?.clockOut)
        assertNotNull(closed?.totalHours)
        assertNull(attendance.observeOpenShift(workerId).first())
    }

    @Test
    fun clockOutWithNoOpenShiftDoesNothing() = runTest {
        assertNull(attendance.clockOut(workerId))
    }

    @Test
    fun hoursAreRoundedToTwoDecimals() {
        val start = FIXED_NOW
        assertEquals(9.42, hoursBetween(start, start.plusMinutes(565)), 0.0)
    }
}
