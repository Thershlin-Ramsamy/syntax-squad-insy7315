package com.mcc.construction.di

import android.content.Context
import com.mcc.construction.data.remote.*
import com.mcc.construction.data.repository.MessageRepository
import com.mcc.construction.data.repository.SyncRepository

class SupabaseAppContainer(
    context: Context
) : AppContainer {

    private val applicationContext = context.applicationContext
    private val services = BackendServices(applicationContext)
    private val network = BackendNetworkMonitor(applicationContext)

    private val outbox = MessageOutbox(
        storage = EncryptedOutboxStorage(
            applicationContext,
            SupabaseConfig.fromBuildConfig().url
        ),
        scope = services.scope,
        clock = services.clock
    )

    private val remoteSync = SupabaseSyncRepository(
        context = applicationContext,
        client = services.client,
        auth = services.authRepository,
        network = network,
        outbox = outbox,
        gateway = services.gateway,
        reads = services.reads,
        scope = services.scope,
        clock = services.clock
    )

    override val syncRepository: SyncRepository =
        PolicySyncRepository(
            delegate = remoteSync,
            organisation = services.systemSettingsRepository,
            scope = services.scope
        )

    override val messageRepository: MessageRepository =
        OfflineMessageRepository(
            services.client,
            services.authRepository,
            services.messageRepository,
            services.userRepository,
            outbox,
            services.gateway,
            syncRepository
        )

    override val authRepository = services.authRepository
    override val userRepository = services.userRepository
    override val projectRepository = services.projectRepository
    override val taskRepository = services.taskRepository
    override val attendanceRepository = services.attendanceRepository
    override val dailyReportRepository = services.dailyReportRepository
    override val inventoryRepository = services.inventoryRepository
    override val documentRepository = services.documentRepository
    override val safetyRepository = services.safetyRepository
    override val notificationRepository = services.notificationRepository
    override val auditLogRepository = services.auditLogRepository
    override val settingsRepository = services.settingsRepository
    override val systemSettingsRepository = services.systemSettingsRepository
    override val clock = services.clock
    override val demoAccounts: List<DemoAccount> = emptyList()

    init {
        services.gateway.setWriteGuard {
            WritePolicy.requireOnline(syncRepository.status.value)
        }
    }
}