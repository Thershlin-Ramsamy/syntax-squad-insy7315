package com.mcc.construction.data.fake

import com.mcc.construction.data.model.UserRole
import com.mcc.construction.di.AppContainer
import com.mcc.construction.di.DemoAccount
import java.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * [AppContainer] backed entirely by in-memory fakes seeded from [FakeSeedData].
 *
 * State lives as long as the container, which `MccApplication` keeps for the
 * life of the process: changes survive navigation and rotation but reset when
 * the app is restarted.
 *
 * @param clock the time everything is measured against. Tests pass a fixed one.
 * @param latency how long sign-in, password reset and sync appear to take, so
 *   loading states can be seen. Tests pass [Duration.ZERO].
 */
class FakeAppContainer(
    override val clock: Clock = Clock.systemDefaultZone(),
    latency: Duration = DEFAULT_LATENCY
) : AppContainer {

    private val seed = FakeSeedData(clock)

    /** Whoever is signed in — the "actor" the fakes put on notifications and audit entries. */
    private val currentUserId: () -> Int? = { authRepository.session.value?.user?.userId }

    override val auditLogRepository = FakeAuditLogRepository(seed, clock)
    override val userRepository = FakeUserRepository(seed, auditLogRepository, currentUserId)
    override val authRepository = FakeAuthRepository(userRepository, clock, latency)
    override val syncRepository = FakeSyncRepository(
        seed = seed,
        clock = clock,
        syncDuration = latency * SYNC_LATENCY_MULTIPLIER,
        currentUserId = currentUserId
    )
    override val notificationRepository = FakeNotificationRepository(seed, clock)
    override val projectRepository = FakeProjectRepository(seed)

    /** Stands in for the notifications the server would send when data changes. */
    private val notifier = FakeNotifier(notificationRepository, userRepository, projectRepository, currentUserId)

    override val taskRepository = FakeTaskRepository(seed, syncRepository, notifier, clock)
    override val attendanceRepository = FakeAttendanceRepository(seed, syncRepository, clock)
    override val dailyReportRepository =
        FakeDailyReportRepository(seed, projectRepository, syncRepository, notifier, clock)
    override val inventoryRepository = FakeInventoryRepository(seed, notifier)
    override val documentRepository = FakeDocumentRepository(seed, clock)
    override val safetyRepository =
        FakeSafetyRepository(seed, projectRepository, syncRepository, notifier, clock)
    override val messageRepository =
        FakeMessageRepository(seed, userRepository, syncRepository, clock)
    override val settingsRepository = FakeSettingsRepository()
    override val systemSettingsRepository = FakeSystemSettingsRepository(auditLogRepository, currentUserId)

    override val demoAccounts = listOf(
        DemoAccount(UserRole.WORKER, FakeSeedData.DEMO_WORKER_EMAIL),
        DemoAccount(UserRole.MANAGER, FakeSeedData.DEMO_MANAGER_EMAIL),
        DemoAccount(UserRole.ADMINISTRATOR, FakeSeedData.DEMO_ADMIN_EMAIL)
    )

    private companion object {
        val DEFAULT_LATENCY = 700.milliseconds

        /** A sync moves more data than a sign-in, so it is shown taking longer. */
        const val SYNC_LATENCY_MULTIPLIER = 2
    }
}
