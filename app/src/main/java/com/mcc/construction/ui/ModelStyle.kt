package com.mcc.construction.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.ui.graphics.vector.ImageVector
import com.mcc.construction.R
import com.mcc.construction.data.model.IncidentSeverity
import com.mcc.construction.data.model.NotificationType
import com.mcc.construction.data.model.PendingChangeType
import com.mcc.construction.data.model.ProjectStatus
import com.mcc.construction.data.model.ReportStatus
import com.mcc.construction.data.model.StockLevel
import com.mcc.construction.data.model.SyncOutcome
import com.mcc.construction.data.model.SyncState
import com.mcc.construction.data.model.TaskPriority
import com.mcc.construction.data.model.TaskStatus
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.ui.theme.Tone

/*
 * How each model value looks: its label, its colour (as a Tone) and its icon.
 *
 * Kept out of the data layer so the models stay free of Android resources, and
 * kept out of individual screens so "Blocked" is the same word and the same red
 * on the dashboard, the task list and the project page. When a screen needs to
 * show a status, it reads it from here:
 *
 *     StatusChip(stringResource(task.status.labelRes), task.status.tone)
 */

// ---------------------------------------------------------------------------
// Roles
// ---------------------------------------------------------------------------

@get:StringRes
val UserRole.labelRes: Int
    get() = when (this) {
        UserRole.WORKER -> R.string.role_worker
        UserRole.MANAGER -> R.string.role_manager
        UserRole.ADMINISTRATOR -> R.string.role_administrator
    }

@get:StringRes
val UserRole.descriptionRes: Int
    get() = when (this) {
        UserRole.WORKER -> R.string.role_worker_description
        UserRole.MANAGER -> R.string.role_manager_description
        UserRole.ADMINISTRATOR -> R.string.role_administrator_description
    }

val UserRole.icon: ImageVector
    get() = when (this) {
        UserRole.WORKER -> Icons.Filled.Engineering
        UserRole.MANAGER -> Icons.Filled.SupervisorAccount
        UserRole.ADMINISTRATOR -> Icons.Filled.AdminPanelSettings
    }

// ---------------------------------------------------------------------------
// Work
// ---------------------------------------------------------------------------

@get:StringRes
val TaskStatus.labelRes: Int
    get() = when (this) {
        TaskStatus.PENDING -> R.string.task_status_pending
        TaskStatus.IN_PROGRESS -> R.string.task_status_in_progress
        TaskStatus.COMPLETED -> R.string.task_status_completed
        TaskStatus.BLOCKED -> R.string.task_status_blocked
    }

val TaskStatus.tone: Tone
    get() = when (this) {
        TaskStatus.PENDING -> Tone.Warning
        TaskStatus.IN_PROGRESS -> Tone.Info
        TaskStatus.COMPLETED -> Tone.Success
        TaskStatus.BLOCKED -> Tone.Danger
    }

@get:StringRes
val TaskPriority.labelRes: Int
    get() = when (this) {
        TaskPriority.LOW -> R.string.task_priority_low
        TaskPriority.MEDIUM -> R.string.task_priority_medium
        TaskPriority.HIGH -> R.string.task_priority_high
    }

val TaskPriority.tone: Tone
    get() = when (this) {
        TaskPriority.LOW -> Tone.Neutral
        TaskPriority.MEDIUM -> Tone.Info
        TaskPriority.HIGH -> Tone.Danger
    }

@get:StringRes
val ProjectStatus.labelRes: Int
    get() = when (this) {
        ProjectStatus.PLANNING -> R.string.project_status_planning
        ProjectStatus.ACTIVE -> R.string.project_status_active
        ProjectStatus.ON_HOLD -> R.string.project_status_on_hold
        ProjectStatus.COMPLETED -> R.string.project_status_completed
    }

val ProjectStatus.tone: Tone
    get() = when (this) {
        ProjectStatus.PLANNING -> Tone.Neutral
        ProjectStatus.ACTIVE -> Tone.Info
        ProjectStatus.ON_HOLD -> Tone.Warning
        ProjectStatus.COMPLETED -> Tone.Success
    }

@get:StringRes
val ReportStatus.labelRes: Int
    get() = when (this) {
        ReportStatus.SUBMITTED -> R.string.report_status_submitted
        ReportStatus.APPROVED -> R.string.report_status_approved
    }

val ReportStatus.tone: Tone
    get() = when (this) {
        ReportStatus.SUBMITTED -> Tone.Warning
        ReportStatus.APPROVED -> Tone.Success
    }

// ---------------------------------------------------------------------------
// Inventory and safety
// ---------------------------------------------------------------------------

@get:StringRes
val StockLevel.labelRes: Int
    get() = when (this) {
        StockLevel.IN_STOCK -> R.string.stock_in_stock
        StockLevel.LOW -> R.string.stock_low
        StockLevel.OUT -> R.string.stock_out
    }

val StockLevel.tone: Tone
    get() = when (this) {
        StockLevel.IN_STOCK -> Tone.Success
        StockLevel.LOW -> Tone.Warning
        StockLevel.OUT -> Tone.Danger
    }

@get:StringRes
val IncidentSeverity.labelRes: Int
    get() = when (this) {
        IncidentSeverity.NEAR_MISS -> R.string.incident_near_miss
        IncidentSeverity.MINOR -> R.string.incident_minor
        IncidentSeverity.SERIOUS -> R.string.incident_serious
    }

val IncidentSeverity.tone: Tone
    get() = when (this) {
        IncidentSeverity.NEAR_MISS -> Tone.Neutral
        IncidentSeverity.MINOR -> Tone.Warning
        IncidentSeverity.SERIOUS -> Tone.Danger
    }

// ---------------------------------------------------------------------------
// Notifications
// ---------------------------------------------------------------------------

@get:StringRes
val NotificationType.labelRes: Int
    get() = when (this) {
        NotificationType.TASK -> R.string.notification_type_task
        NotificationType.SAFETY -> R.string.notification_type_safety
        NotificationType.MESSAGE -> R.string.notification_type_message
        NotificationType.INVENTORY -> R.string.notification_type_inventory
        NotificationType.REPORT -> R.string.notification_type_report
        NotificationType.SYSTEM -> R.string.notification_type_system
    }

val NotificationType.icon: ImageVector
    get() = when (this) {
        NotificationType.TASK -> Icons.AutoMirrored.Filled.Assignment
        NotificationType.SAFETY -> Icons.Filled.HealthAndSafety
        NotificationType.MESSAGE -> Icons.AutoMirrored.Filled.Chat
        NotificationType.INVENTORY -> Icons.Filled.Inventory2
        NotificationType.REPORT -> Icons.Filled.Description
        NotificationType.SYSTEM -> Icons.Filled.Info
    }

val NotificationType.tone: Tone
    get() = when (this) {
        NotificationType.TASK -> Tone.Info
        NotificationType.SAFETY -> Tone.Warning
        NotificationType.MESSAGE -> Tone.Info
        NotificationType.INVENTORY -> Tone.Danger
        NotificationType.REPORT -> Tone.Success
        NotificationType.SYSTEM -> Tone.Neutral
    }

// ---------------------------------------------------------------------------
// Sync
// ---------------------------------------------------------------------------

val SyncState.tone: Tone
    get() = when (this) {
        SyncState.SYNCED -> Tone.Success
        SyncState.PENDING -> Tone.Warning
        SyncState.SYNCING -> Tone.Info
        SyncState.OFFLINE -> Tone.Danger
    }

val SyncState.icon: ImageVector
    get() = when (this) {
        SyncState.SYNCED -> Icons.Filled.CloudDone
        SyncState.PENDING -> Icons.Filled.CloudUpload
        SyncState.SYNCING -> Icons.Filled.CloudSync
        SyncState.OFFLINE -> Icons.Filled.CloudOff
    }

@get:StringRes
val PendingChangeType.labelRes: Int
    get() = when (this) {
        PendingChangeType.CLOCK_IN -> R.string.pending_clock_in
        PendingChangeType.CLOCK_OUT -> R.string.pending_clock_out
        PendingChangeType.DAILY_REPORT -> R.string.pending_daily_report
        PendingChangeType.TASK_UPDATE -> R.string.pending_task_update
        PendingChangeType.SAFETY_INSPECTION -> R.string.pending_safety_inspection
        PendingChangeType.INCIDENT_REPORT -> R.string.pending_incident_report
        PendingChangeType.MESSAGE -> R.string.pending_message
    }

val PendingChangeType.icon: ImageVector
    get() = when (this) {
        PendingChangeType.CLOCK_IN -> Icons.AutoMirrored.Filled.Login
        PendingChangeType.CLOCK_OUT -> Icons.AutoMirrored.Filled.Logout
        PendingChangeType.DAILY_REPORT -> Icons.Filled.Description
        PendingChangeType.TASK_UPDATE -> Icons.AutoMirrored.Filled.Assignment
        PendingChangeType.SAFETY_INSPECTION -> Icons.Filled.HealthAndSafety
        PendingChangeType.INCIDENT_REPORT -> Icons.Filled.ReportProblem
        PendingChangeType.MESSAGE -> Icons.AutoMirrored.Filled.Chat
    }

@get:StringRes
val SyncOutcome.labelRes: Int
    get() = when (this) {
        SyncOutcome.COMPLETED -> R.string.sync_outcome_completed
        SyncOutcome.FAILED -> R.string.sync_outcome_failed
    }

val SyncOutcome.tone: Tone
    get() = when (this) {
        SyncOutcome.COMPLETED -> Tone.Success
        SyncOutcome.FAILED -> Tone.Danger
    }
