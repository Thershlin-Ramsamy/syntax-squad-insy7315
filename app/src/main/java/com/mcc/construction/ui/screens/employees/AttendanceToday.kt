package com.mcc.construction.ui.screens.employees

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.mcc.construction.R
import com.mcc.construction.data.model.Attendance
import com.mcc.construction.ui.format.formatHours
import com.mcc.construction.ui.format.formatTime
import com.mcc.construction.ui.theme.Tone

/** Where someone is today, from their shifts. */
enum class PresenceStatus { ON_SITE, FINISHED, NOT_IN }

/** Someone's attendance today: their status and the shift behind it, if any. */
data class AttendanceToday(
    val status: PresenceStatus,
    /** The open shift when on site, otherwise the last finished one. */
    val shift: Attendance? = null,
    /** Hours of finished shifts today. */
    val hoursWorked: Double = 0.0
) {
    companion object {
        val NotIn = AttendanceToday(PresenceStatus.NOT_IN)

        /** Works out today's status from [shiftsToday], all dated today. */
        fun from(shiftsToday: List<Attendance>): AttendanceToday {
            if (shiftsToday.isEmpty()) return NotIn
            val hours = shiftsToday.sumOf { it.totalHours ?: 0.0 }
            val open = shiftsToday.firstOrNull { it.isOpen }
            return if (open != null) {
                AttendanceToday(PresenceStatus.ON_SITE, open, hours)
            } else {
                AttendanceToday(PresenceStatus.FINISHED, shiftsToday.maxBy { it.clockIn }, hours)
            }
        }
    }
}

val PresenceStatus.tone: Tone
    get() = when (this) {
        PresenceStatus.ON_SITE -> Tone.Success
        PresenceStatus.FINISHED -> Tone.Info
        PresenceStatus.NOT_IN -> Tone.Neutral
    }

val PresenceStatus.icon: ImageVector
    get() = when (this) {
        PresenceStatus.ON_SITE -> Icons.Filled.CheckCircle
        PresenceStatus.FINISHED -> Icons.Filled.Schedule
        PresenceStatus.NOT_IN -> Icons.Filled.RemoveCircleOutline
    }

@get:StringRes
val PresenceStatus.labelRes: Int
    get() = when (this) {
        PresenceStatus.ON_SITE -> R.string.presence_on_site
        PresenceStatus.FINISHED -> R.string.presence_finished
        PresenceStatus.NOT_IN -> R.string.presence_not_in
    }

/** "On site since 07:02", "Clocked out · 8.5 h" or "Not clocked in". */
@Composable
fun AttendanceToday.detailText(): String = when (status) {
    PresenceStatus.ON_SITE -> stringResource(R.string.presence_on_site_since, shift?.clockIn?.formatTime().orEmpty())
    PresenceStatus.FINISHED -> stringResource(R.string.presence_finished_hours, formatHours(hoursWorked))
    PresenceStatus.NOT_IN -> stringResource(R.string.presence_not_in_detail)
}
