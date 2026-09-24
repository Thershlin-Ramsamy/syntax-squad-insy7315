package com.mcc.construction.data.repository

import com.mcc.construction.data.model.Attendance
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface AttendanceRepository {

    /** Every shift for [userId], newest first. */
    fun observeAttendance(userId: Int): Flow<List<Attendance>>

    /** Every shift worked on [date], by anyone — the manager's roll call. */
    fun observeAttendanceOn(date: LocalDate): Flow<List<Attendance>>

    /** The shift [userId] is currently clocked in to, or null. */
    fun observeOpenShift(userId: Int): Flow<Attendance?>

    /** Opens a shift. If one is already open, that shift is returned unchanged. */
    suspend fun clockIn(userId: Int): Attendance

    /** Closes the open shift and works out its hours. Null if none was open. */
    suspend fun clockOut(userId: Int): Attendance?
}
