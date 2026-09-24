package com.mcc.construction.data.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * A row of the ATTENDANCE table: one shift for one person.
 *
 * [clockOut] and [totalHours] stay null while the shift is open, so an open
 * shift is simply the record with no clock-out yet.
 */
data class Attendance(
    val attendanceId: Int,
    val userId: Int,
    val date: LocalDate,
    val clockIn: LocalDateTime,
    val clockOut: LocalDateTime?,
    val totalHours: Double?
) {
    val isOpen: Boolean get() = clockOut == null
}
