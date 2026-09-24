package com.mcc.construction.di

import com.mcc.construction.data.model.UserRole
import com.mcc.construction.data.repository.AttendanceRepository
import com.mcc.construction.data.repository.AuditLogRepository
import com.mcc.construction.data.repository.AuthRepository
import com.mcc.construction.data.repository.DailyReportRepository
import com.mcc.construction.data.repository.DocumentRepository
import com.mcc.construction.data.repository.InventoryRepository
import com.mcc.construction.data.repository.MessageRepository
import com.mcc.construction.data.repository.NotificationRepository
import com.mcc.construction.data.repository.ProjectRepository
import com.mcc.construction.data.repository.SafetyRepository
import com.mcc.construction.data.repository.SettingsRepository
import com.mcc.construction.data.repository.SyncRepository
import com.mcc.construction.data.repository.TaskRepository
import com.mcc.construction.data.repository.UserRepository
import java.time.Clock

/**
 * Every repository the app uses, in one place — manual dependency injection.
 *
 * ViewModels get their repositories from here (see `ui.viewModelFactory`), so
 * nothing in the UI knows or cares whether the data is fake or real. The
 * backend team adds a real implementation of this interface and changes the
 * one line in `MccApplication` that creates the container; no screen changes.
 */
interface AppContainer {
    val authRepository: AuthRepository
    val userRepository: UserRepository
    val projectRepository: ProjectRepository
    val taskRepository: TaskRepository
    val attendanceRepository: AttendanceRepository
    val dailyReportRepository: DailyReportRepository
    val inventoryRepository: InventoryRepository
    val documentRepository: DocumentRepository
    val safetyRepository: SafetyRepository
    val notificationRepository: NotificationRepository
    val messageRepository: MessageRepository
    val auditLogRepository: AuditLogRepository
    val syncRepository: SyncRepository
    val settingsRepository: SettingsRepository

    /**
     * The time source for anything that needs "now" or "today". ViewModels
     * take it from here rather than reading the system clock, so tests can pin
     * the date.
     */
    val clock: Clock

    /**
     * Sign-in shortcuts shown on the login screen. The fake container lists the
     * demo accounts; a real container returns an empty list, which hides them.
     */
    val demoAccounts: List<DemoAccount>
}

/** A one-tap sign-in shortcut for demonstrating a role. */
data class DemoAccount(
    val role: UserRole,
    val email: String
)
