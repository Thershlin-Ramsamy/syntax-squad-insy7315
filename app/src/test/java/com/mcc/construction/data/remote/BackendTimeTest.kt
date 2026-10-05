package com.mcc.construction.data.remote

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.format.DateTimeParseException

class BackendTimeTest {

    @Test
    fun utcTimestampCanCrossIntoNextLocalDay() {
        val result = BackendTime.timestamp(
            "2026-10-04T23:30:00Z"
        )

        assertEquals(
            LocalDateTime.of(2026, 10, 5, 1, 30),
            result
        )
    }

    @Test
    fun equivalentOffsetsProduceSameApplicationTime() {
        val utc = BackendTime.timestamp(
            "2026-10-04T10:15:00Z"
        )

        val southAfrica = BackendTime.timestamp(
            "2026-10-04T12:15:00+02:00"
        )

        assertEquals(utc, southAfrica)
    }

    @Test
    fun databaseDateRemainsItsRecordedCalendarDate() {
        assertEquals(
            LocalDate.of(2026, 10, 4),
            BackendTime.date("2026-10-04")
        )
    }

    @Test(expected = DateTimeParseException::class)
    fun timestampWithoutOffsetIsRejected() {
        BackendTime.timestamp(
            "2026-10-04T10:15:00"
        )
    }
}