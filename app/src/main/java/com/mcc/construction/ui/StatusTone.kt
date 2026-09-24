package com.mcc.construction.ui

import com.mcc.construction.data.legacy.AccountStatus
import com.mcc.construction.data.legacy.AuditSeverity
import com.mcc.construction.data.legacy.EmployeeStatus
import com.mcc.construction.data.legacy.IncidentSeverity
import com.mcc.construction.data.legacy.ProjectStatus
import com.mcc.construction.data.legacy.StockLevel
import com.mcc.construction.data.legacy.TaskPriority
import com.mcc.construction.data.legacy.TaskStatus
import com.mcc.construction.ui.theme.Tone

/**
 * Single place where domain status maps to colour.
 *
 * Kept out of the data layer so models stay UI-agnostic, and kept out of the
 * individual screens so that "blocked" is the same red on the dashboard, the
 * task list and the project detail without anyone having to remember.
 */

val TaskStatus.tone: Tone
    get() = when (this) {
        TaskStatus.PENDING -> Tone.Warning
        TaskStatus.IN_PROGRESS -> Tone.Info
        TaskStatus.COMPLETED -> Tone.Success
        TaskStatus.BLOCKED -> Tone.Danger
    }

val TaskPriority.tone: Tone
    get() = when (this) {
        TaskPriority.LOW -> Tone.Neutral
        TaskPriority.MEDIUM -> Tone.Info
        TaskPriority.HIGH -> Tone.Danger
    }

val ProjectStatus.tone: Tone
    get() = when (this) {
        ProjectStatus.PLANNING -> Tone.Neutral
        ProjectStatus.ACTIVE -> Tone.Info
        ProjectStatus.ON_HOLD -> Tone.Warning
        ProjectStatus.COMPLETE -> Tone.Success
    }

val EmployeeStatus.tone: Tone
    get() = when (this) {
        EmployeeStatus.ON_SITE -> Tone.Success
        EmployeeStatus.OFF_DUTY -> Tone.Neutral
        EmployeeStatus.ON_LEAVE -> Tone.Warning
    }

val StockLevel.tone: Tone
    get() = when (this) {
        StockLevel.IN_STOCK -> Tone.Success
        StockLevel.LOW -> Tone.Warning
        StockLevel.OUT -> Tone.Danger
    }

val IncidentSeverity.tone: Tone
    get() = when (this) {
        IncidentSeverity.NEAR_MISS -> Tone.Neutral
        IncidentSeverity.MINOR -> Tone.Warning
        IncidentSeverity.SERIOUS -> Tone.Danger
    }

val AccountStatus.tone: Tone
    get() = when (this) {
        AccountStatus.ACTIVE -> Tone.Success
        AccountStatus.SUSPENDED -> Tone.Danger
        AccountStatus.INVITED -> Tone.Neutral
    }

val AuditSeverity.tone: Tone
    get() = when (this) {
        AuditSeverity.INFO -> Tone.Neutral
        AuditSeverity.WARNING -> Tone.Warning
        AuditSeverity.CRITICAL -> Tone.Danger
    }

val AuditSeverity.label: String
    get() = when (this) {
        AuditSeverity.INFO -> "Info"
        AuditSeverity.WARNING -> "Warning"
        AuditSeverity.CRITICAL -> "Critical"
    }
