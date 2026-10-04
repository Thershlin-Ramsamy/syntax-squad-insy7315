package com.mcc.construction.data.remote

import com.mcc.construction.data.model.Attendance
import com.mcc.construction.data.remote.dto.AttendanceRow
import com.mcc.construction.data.repository.AttendanceRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject

class SupabaseAttendanceRepository(
    private val client: SupabaseClient,
    private val reads: BackendReadStore,
    private val writes: BackendRepositorySupport
) : AttendanceRepository {

    private fun observeAll(): Flow<List<Attendance>> {
        return reads.observeRows<AttendanceRow>(
            table = "attendance",
            primaryKey = "attendance_id"
        ).map { rows ->
            rows.map { row ->
                mapAttendance(row)
            }.sortedWith(
                compareByDescending<Attendance> {
                    it.clockIn
                }.thenByDescending {
                    it.attendanceId
                }
            )
        }
    }

    override fun observeAttendance(
        userId: Int
    ): Flow<List<Attendance>> {
        return observeAll().map { attendance ->
            attendance.filter {
                it.userId == userId
            }
        }
    }

    override fun observeAttendanceOn(
        date: LocalDate
    ): Flow<List<Attendance>> {
        return observeAll().map { attendance ->
            attendance.filter {
                it.date == date
            }
        }
    }

    override fun observeOpenShift(
        userId: Int
    ): Flow<Attendance?> {
        return observeAttendance(userId).map { attendance ->
            attendance.firstOrNull {
                it.clockOut == null
            }
        }
    }

    override suspend fun clockIn(
        userId: Int
    ): Attendance {
        writes.requireCurrentUser(userId)

        val row = writes.executeAs<AttendanceRow>(
            action = BackendAction.CLOCK_IN,
            payload = buildJsonObject {}
        )

        return mapAttendance(row)
    }

    override suspend fun clockOut(
        userId: Int
    ): Attendance? {
        writes.requireCurrentUser(userId)

        if (!hasOpenShift(userId)) {
            return null
        }

        return try {
            val row = writes.executeAs<AttendanceRow>(
                action = BackendAction.CLOCK_OUT,
                payload = buildJsonObject {}
            )

            mapAttendance(row)
        } catch (exception: PostgrestRestException) {
            // Another device may have closed the shift
            // between our initial read and the RPC.
            if (
                exception.code == "22023" &&
                !hasOpenShift(userId)
            ) {
                null
            } else {
                throw exception
            }
        }
    }

    private suspend fun hasOpenShift(
        userId: Int
    ): Boolean {
        writes.requireCurrentUser(userId)

        val rows = client.from("attendance").select {
            filter {
                eq("user_id", userId)
                exact("clock_out", null)
            }

            limit(1)
        }.decodeList<AttendanceRow>()

        writes.requireCurrentUser(userId)

        return rows.isNotEmpty()
    }

    private fun mapAttendance(
        row: AttendanceRow
    ): Attendance {
        return Attendance(
            attendanceId = row.attendanceId,
            userId = row.userId,
            date = LocalDate.parse(row.date),
            clockIn = parseTimestamp(row.clockIn),
            clockOut = row.clockOut?.let { timestamp ->
                parseTimestamp(timestamp)
            },
            totalHours = row.totalHours?.content?.toDouble()
        )
    }

    private fun parseTimestamp(
        value: String
    ): LocalDateTime {
        return OffsetDateTime.parse(value)
            .atZoneSameInstant(SOUTH_AFRICA_ZONE)
            .toLocalDateTime()
    }

    companion object {
        private val SOUTH_AFRICA_ZONE =
            ZoneId.of("Africa/Johannesburg")
    }
}