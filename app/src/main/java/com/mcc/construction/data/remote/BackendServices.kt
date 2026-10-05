package com.mcc.construction.data.remote

import android.content.Context
import com.mcc.construction.data.repository.AttendanceRepository
import com.mcc.construction.data.repository.AuditLogRepository
import com.mcc.construction.data.repository.AuthRepository
import com.mcc.construction.data.repository.DailyReportRepository
import com.mcc.construction.data.repository.FileDocumentRepository
import com.mcc.construction.data.repository.InventoryRepository
import com.mcc.construction.data.repository.MessageRepository
import com.mcc.construction.data.repository.NotificationRepository
import com.mcc.construction.data.repository.ProjectRepository
import com.mcc.construction.data.repository.SafetyRepository
import com.mcc.construction.data.repository.SettingsRepository
import com.mcc.construction.data.repository.SystemSettingsRepository
import com.mcc.construction.data.repository.TaskRepository
import com.mcc.construction.data.repository.UserRepository
import io.github.jan.supabase.SupabaseClient
import java.time.Clock
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class BackendServices(
    context: Context
) {

    private val applicationContext =
        context.applicationContext

    val clock: Clock = Clock.system(
        ZoneId.of("Africa/Johannesburg")
    )

    val scope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    private val provider = SupabaseProvider(
        context = applicationContext,
        configuration = SupabaseConfig.fromBuildConfig()
    )

    val client: SupabaseClient = provider.client

    val refreshBus = BackendRefreshBus()

    val gateway = BackendGateway(
        client,
        refreshBus
    )

    val writes = BackendRepositorySupport(
        client,
        gateway
    )

    val reads = BackendReadStore(
        client,
        refreshBus
    )

    private val uploadReader = AndroidUploadReader(
        applicationContext
    )

    val storage = BackendStorageService(
        client,
        uploadReader,
        writes
    )

    val authentication = SupabaseAuthRepository(
        client,
        scope,
        clock
    )

    val authRepository: AuthRepository = authentication

    val userRepository: UserRepository =
        SupabaseUserRepository(
            reads,
            writes
        )

    val projectRepository: ProjectRepository =
        SupabaseProjectRepository(
            reads
        )

    val taskRepository: TaskRepository =
        SupabaseTaskRepository(
            client,
            reads,
            writes,
            storage
        )

    val attendanceRepository: AttendanceRepository =
        SupabaseAttendanceRepository(
            client,
            reads,
            writes
        )

    val dailyReportRepository: DailyReportRepository =
        SupabaseDailyReportRepository(
            reads,
            writes,
            storage
        )

    val inventoryRepository: InventoryRepository =
        SupabaseInventoryRepository(
            reads,
            writes
        )

    private val documentService = BackendDocumentService(
        client,
        reads,
        writes,
        storage
    )

    val documentRepository: FileDocumentRepository =
        SupabaseDocumentRepository(
            documentService
        )

    val safetyRepository: SafetyRepository =
        SupabaseSafetyRepository(
            reads,
            writes,
            storage
        )

    val notificationRepository: NotificationRepository =
        SupabaseNotificationRepository(
            reads,
            writes
        )

    val messageRepository: MessageRepository =
        SupabaseMessageRepository(
            reads,
            writes,
            userRepository
        )

    val auditLogRepository: AuditLogRepository =
        SupabaseAuditLogRepository(
            reads
        )

    val settingsRepository: SettingsRepository =
        LocalSettingsRepository(
            applicationContext
        )

    val organisationSettings =
        SupabaseSystemSettingsRepository(
            client,
            writes,
            refreshBus,
            scope
        )

    val systemSettingsRepository: SystemSettingsRepository =
        organisationSettings

    val realtime = BackendRealtimeBridge(
        client,
        authRepository,
        refreshBus,
        scope
    )
}