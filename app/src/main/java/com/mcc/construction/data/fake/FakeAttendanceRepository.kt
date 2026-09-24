package com.mcc.construction.data.fake

import com.mcc.construction.data.model.Attendance
import com.mcc.construction.data.model.PendingChangeType
import com.mcc.construction.data.repository.AttendanceRepository
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeAttendanceRepository(
    seed: FakeSeedData,
    private val sync: FakeSyncRepository,
    private val clock: Clock
) : AttendanceRepository {

    private val records = MutableStateFlow(seed.attendance)
    private val nextId = AtomicInteger(seed.attendance.maxOf { it.attendanceId } + 1)

    override fun observeAttendance(userId: Int): Flow<List<Attendance>> =
        records.map { list ->
            list.filter { it.userId == userId }.sortedByDescending { it.clockIn }
        }

    override fun observeAttendanceOn(date: LocalDate): Flow<List<Attendance>> =
        records.map { list -> list.filter { it.date == date } }

    override fun observeOpenShift(userId: Int): Flow<Attendance?> =
        records.map { list -> list.firstOrNull { it.userId == userId && it.isOpen } }

    override suspend fun clockIn(userId: Int): Attendance {
        openShift(userId)?.let { return it }

        val now = now()
        val shift = Attendance(
            attendanceId = nextId.getAndIncrement(),
            userId = userId,
            date = now.toLocalDate(),
            clockIn = now,
            clockOut = null,
            totalHours = null
        )
        records.update { it + shift }
        sync.recordChange(PendingChangeType.CLOCK_IN, now.format(TIME))
        return shift
    }

    override suspend fun clockOut(userId: Int): Attendance? {
        val open = openShift(userId) ?: return null
        val now = now()
        val closed = open.copy(clockOut = now, totalHours = hoursBetween(open.clockIn, now))
        records.update { list ->
            list.map { if (it.attendanceId == open.attendanceId) closed else it }
        }
        sync.recordChange(PendingChangeType.CLOCK_OUT, now.format(TIME))
        return closed
    }

    private fun openShift(userId: Int): Attendance? =
        records.value.firstOrNull { it.userId == userId && it.isOpen }

    private fun now(): LocalDateTime = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES)

    private companion object {
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
