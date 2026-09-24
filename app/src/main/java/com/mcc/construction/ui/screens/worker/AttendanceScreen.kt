package com.mcc.construction.ui.screens.worker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.construction.R
import com.mcc.construction.data.model.Attendance
import com.mcc.construction.ui.adaptive.LocalWindowLayout
import com.mcc.construction.ui.components.ActionFeedbackHost
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.LeadingIcon
import com.mcc.construction.ui.components.ListRow
import com.mcc.construction.ui.components.LoadingState
import com.mcc.construction.ui.components.ScreenScaffold
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.components.StatusChip
import com.mcc.construction.ui.components.StatusPill
import com.mcc.construction.ui.format.formatHours
import com.mcc.construction.ui.format.formatTime
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.screens.dashboard.shiftElapsedText
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Attendance wired to its ViewModel. */
@Composable
fun AttendanceRoute(viewModel: AttendanceViewModel = viewModel(factory = AttendanceViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AttendanceScreen(state, viewModel::onClockInOut, viewModel::onMessageShown)
}

/**
 * Clock in and out with one large button. The button's colour, icon and word
 * all change with the state, so it reads at a glance in gloves and sunlight.
 */
@Composable
fun AttendanceScreen(
    state: AttendanceUiState,
    onClockInOut: () -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading) {
        LoadingState(modifier)
        return
    }
    val wide = LocalWindowLayout.current.isExpanded
    ActionFeedbackHost(state.message, onMessageShown, modifier) {
        ScreenScaffold(maxContentWidth = if (wide) WIDE_CONTENT else Spacing.contentMaxWidth) {
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xxl)) {
                    Column(Modifier.weight(1f)) {
                        ClockPanel(state, onClockInOut)
                        Spacer(Modifier.height(Spacing.sectionGap))
                        WeekSummary(state)
                    }
                    Column(Modifier.weight(1f)) { History(state) }
                }
            } else {
                ClockPanel(state, onClockInOut)
                Spacer(Modifier.height(Spacing.sectionGap))
                WeekSummary(state)
                Spacer(Modifier.height(Spacing.sectionGap))
                History(state)
            }
        }
    }
}

@Composable
private fun ClockPanel(state: AttendanceUiState, onClockInOut: () -> Unit) {
    val shift = state.openShift
    if (shift != null) {
        StatusChip(
            text = stringResource(R.string.attendance_on_shift_since, shift.clockIn.formatTime()),
            tone = Tone.Success,
            icon = Icons.Filled.CheckCircle
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = shiftElapsedText(shift),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    } else {
        StatusChip(
            text = stringResource(R.string.attendance_not_clocked_in),
            tone = Tone.Neutral,
            icon = Icons.Filled.Schedule
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.attendance_clock_in_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(Modifier.height(Spacing.xl))
    BigClockButton(isClockedIn = shift != null, isUpdating = state.isUpdating, onClick = onClockInOut)
}

/** A 120dp-tall button — the single thing this screen is for. */
@Composable
private fun BigClockButton(isClockedIn: Boolean, isUpdating: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val container = if (isClockedIn) scheme.primary else scheme.secondary
    val content = if (isClockedIn) scheme.onPrimary else scheme.onSecondary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = BIG_BUTTON_HEIGHT)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(container)
            .clickable(enabled = !isUpdating, role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.xl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (isUpdating) {
            CircularProgressIndicator(color = content, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
        } else {
            Icon(
                imageVector = if (isClockedIn) Icons.AutoMirrored.Filled.Logout else Icons.AutoMirrored.Filled.Login,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.width(Spacing.lg))
        Text(
            text = stringResource(if (isClockedIn) R.string.attendance_clock_out else R.string.attendance_clock_in),
            style = MaterialTheme.typography.headlineSmall,
            color = content
        )
    }
}

@Composable
private fun WeekSummary(state: AttendanceUiState) {
    SectionHeader(title = stringResource(R.string.attendance_this_week))
    Spacer(Modifier.height(Spacing.blockGap))
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
        AppCard(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.attendance_hours_value, formatHours(state.hoursThisWeek)),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.attendance_hours_worked),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AppCard(modifier = Modifier.weight(1f)) {
            Text(
                text = state.shiftsThisWeek.toString(),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.attendance_shifts),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun History(state: AttendanceUiState) {
    SectionHeader(title = stringResource(R.string.attendance_recent))
    Spacer(Modifier.height(Spacing.blockGap))
    if (state.recentShifts.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.EventBusy,
            title = stringResource(R.string.attendance_history_empty_title),
            message = stringResource(R.string.attendance_history_empty_message)
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
        state.recentShifts.forEach { shift -> ShiftRow(shift, state.today) }
    }
}

@Composable
private fun ShiftRow(shift: Attendance, today: LocalDate) {
    ListRow(
        title = dayLabel(shift.date, today),
        subtitle = stringResource(
            R.string.attendance_shift_times,
            shift.clockIn.formatTime(),
            shift.clockOut?.formatTime().orEmpty()
        ),
        leading = { LeadingIcon(Icons.Filled.Today, Tone.Info) },
        trailing = {
            StatusPill(
                text = stringResource(R.string.attendance_hours_value, formatHours(shift.totalHours ?: 0.0)),
                tone = Tone.Success
            )
        }
    )
}

@Composable
private fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.attendance_today)
    today.minusDays(1) -> stringResource(R.string.attendance_yesterday)
    else -> date.format(DAY_FORMAT)
}

private val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM")
private val BIG_BUTTON_HEIGHT: Dp = 120.dp
private val WIDE_CONTENT: Dp = 1080.dp

private fun previewState(clockedIn: Boolean): AttendanceUiState {
    val today = LocalDate.now()
    val history = (1..5).map { days ->
        val day = today.minusDays(days.toLong())
        Attendance(days, 5, day, day.atTime(6, 50), day.atTime(16, 10), 9.33)
    }
    return AttendanceUiState(
        isLoading = false,
        openShift = if (clockedIn) Attendance(9, 5, today, LocalDateTime.now().minusMinutes(95), null, null) else null,
        recentShifts = history,
        hoursThisWeek = 27.99,
        shiftsThisWeek = 3,
        today = today
    )
}

@DevicePreviews
@Composable
private fun AttendancePreview() {
    PreviewSurface { AttendanceScreen(previewState(clockedIn = false), {}, {}) }
}

@DevicePreviews
@Composable
private fun AttendanceClockedInPreview() {
    PreviewSurface { AttendanceScreen(previewState(clockedIn = true), {}, {}) }
}
