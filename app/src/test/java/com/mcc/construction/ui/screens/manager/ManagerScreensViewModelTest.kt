package com.mcc.construction.ui.screens.manager

import com.mcc.construction.data.model.DocumentCategory
import com.mcc.construction.data.model.ProjectStatus
import com.mcc.construction.data.model.ReportStatus
import com.mcc.construction.data.model.StockLevel
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.ui.MainDispatcherRule
import com.mcc.construction.ui.screens.dashboard.DashboardAlert
import com.mcc.construction.ui.screens.dashboard.ManagerDashboardViewModel
import com.mcc.construction.ui.screens.documents.DocumentsViewModel
import com.mcc.construction.ui.screens.employees.EmployeeListViewModel
import com.mcc.construction.ui.screens.employees.EmployeeProfileViewModel
import com.mcc.construction.ui.screens.employees.PresenceStatus
import com.mcc.construction.ui.screens.inventory.InventoryViewModel
import com.mcc.construction.ui.screens.inventory.MaterialDetailViewModel
import com.mcc.construction.ui.screens.inventory.StockChange
import com.mcc.construction.ui.screens.keepCollecting
import com.mcc.construction.ui.screens.managerContainer
import com.mcc.construction.ui.screens.projects.ProjectDetailViewModel
import com.mcc.construction.ui.screens.projects.ProjectListViewModel
import com.mcc.construction.ui.screens.reports.ReportEntry
import com.mcc.construction.ui.screens.reports.ReportType
import com.mcc.construction.ui.screens.reports.ReportsViewModel
import kotlin.time.Duration
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ManagerScreensViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun dashboardSummarisesWorkAttendanceAndAlerts() = runTest {
        val c = managerContainer()
        val vm = ManagerDashboardViewModel(
            c.authRepository, c.projectRepository, c.taskRepository, c.attendanceRepository, c.userRepository,
            c.inventoryRepository, c.safetyRepository, c.dailyReportRepository, c.notificationRepository, c.clock
        )
        keepCollecting(vm.uiState)
        val state = vm.uiState.value

        assertEquals("Naledi", state.firstName)
        assertTrue(state.projects.all { it.project.status == ProjectStatus.ACTIVE || it.project.status == ProjectStatus.ON_HOLD })
        assertEquals(c.taskRepository.observeTasks().first().size, state.totalTasks)
        assertEquals(3, state.attendance.onSite)
        assertEquals(6, state.attendance.workforce)
        assertTrue(state.alerts.any { it is DashboardAlert.LowStock })
        assertTrue(state.alerts.filterIsInstance<DashboardAlert.LowStock>().all { it.item.stockLevel != StockLevel.IN_STOCK })
        assertEquals(2, state.reportsAwaitingApproval)
        assertEquals(1, state.defaultProjectId)
    }

    @Test
    fun projectListFiltersByStatusAndKeepsTheSelection() = runTest {
        val c = managerContainer()
        val vm = ProjectListViewModel(c.projectRepository, c.taskRepository, c.clock)
        keepCollecting(vm.uiState)

        assertEquals(5, vm.uiState.value.visible.size)
        assertEquals(ProjectStatus.ACTIVE, vm.uiState.value.visible.first().project.status)
        vm.onFilterChange(ProjectStatus.ACTIVE)
        assertEquals(2, vm.uiState.value.visible.size)
        vm.onSelectProject(2)
        assertEquals(2, vm.uiState.value.paneProjectId)
        vm.onFilterChange(ProjectStatus.COMPLETED)
        assertEquals(4, vm.uiState.value.paneProjectId)
    }

    @Test
    fun projectDetailGathersTeamTasksMaterialsAndDocuments() = runTest {
        val c = managerContainer()
        val vm = ProjectDetailViewModel(
            1, c.projectRepository, c.taskRepository, c.userRepository, c.attendanceRepository,
            c.inventoryRepository, c.documentRepository, c.dailyReportRepository, c.clock
        )
        keepCollecting(vm.uiState)
        val state = vm.uiState.value

        assertEquals("Riverside Estate Phase 2", state.project?.projectName)
        assertEquals("Naledi Khumalo", state.managerName)
        assertTrue(state.tasks.all { it.task.projectId == 1 })
        assertTrue(state.team.any { it.user.userId == 5 })
        assertTrue(state.materials.all { it.projectId == 1 })
        assertEquals(StockLevel.LOW, state.materials.first().stockLevel)
        assertTrue(state.documents.all { it.projectId == 1 })
        assertTrue(state.reports.all { it.report.projectId == 1 })
    }

    @Test
    fun unknownProjectIsReportedAsMissing() = runTest {
        val c = managerContainer()
        val vm = ProjectDetailViewModel(
            99, c.projectRepository, c.taskRepository, c.userRepository, c.attendanceRepository,
            c.inventoryRepository, c.documentRepository, c.dailyReportRepository, c.clock
        )
        keepCollecting(vm.uiState)

        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.project)
    }

    @Test
    fun employeesAreSearchableAndShowTodaysAttendance() = runTest {
        val c = managerContainer()
        val vm = EmployeeListViewModel(c.userRepository, c.attendanceRepository, c.taskRepository, c.clock)
        keepCollecting(vm.uiState)

        assertTrue(vm.uiState.value.visible.all { it.user.role == UserRole.WORKER })
        assertEquals(3, vm.uiState.value.countWith(PresenceStatus.ON_SITE))
        vm.onQueryChange("sipho")
        assertEquals(listOf(5), vm.uiState.value.visible.map { it.user.userId })
        vm.onQueryChange("")
        vm.onRoleFilterChange(null)
        assertTrue(vm.uiState.value.visible.none { it.user.role == UserRole.ADMINISTRATOR })
        assertTrue(vm.uiState.value.visible.any { it.user.role == UserRole.MANAGER })
    }

    @Test
    fun employeeProfileTotalsHoursAndListsOpenTasks() = runTest {
        val c = managerContainer()
        val vm = EmployeeProfileViewModel(5, c.userRepository, c.taskRepository, c.projectRepository, c.attendanceRepository, c.clock)
        keepCollecting(vm.uiState)
        val state = vm.uiState.value

        assertEquals("Sipho", state.user?.firstName)
        assertTrue(state.currentTasks.isNotEmpty())
        assertTrue(state.shifts.isNotEmpty())
        assertEquals(state.shifts.sumOf { it.totalHours ?: 0.0 }, state.hoursLastFortnight, 0.001)
    }

    @Test
    fun inventoryFiltersByProjectAndLowStock() = runTest {
        val c = managerContainer()
        val vm = InventoryViewModel(c.inventoryRepository, c.projectRepository)
        keepCollecting(vm.uiState)

        assertEquals(StockLevel.OUT, vm.uiState.value.visible.first().item.stockLevel)
        vm.onProjectFilterChange(1)
        assertTrue(vm.uiState.value.visible.all { it.item.projectId == 1 })
        vm.onLowStockOnlyChange(true)
        assertTrue(vm.uiState.value.visible.isNotEmpty())
        assertTrue(vm.uiState.value.visible.all { it.item.stockLevel != StockLevel.IN_STOCK })
        assertEquals(vm.uiState.value.visible.size, vm.uiState.value.lowStockCount)
    }

    @Test
    fun materialAdjustmentsUpdateStockAndRefuseTooMuchUse() = runTest {
        val c = managerContainer()
        val vm = MaterialDetailViewModel(1, c.inventoryRepository, c.projectRepository)
        keepCollecting(vm.uiState)
        assertEquals("Highveld Cement Supplies", vm.uiState.value.supplier?.companyName)

        vm.onAdjust(StockChange.USE)
        assertTrue(vm.uiState.value.message?.isError == true)

        vm.onAmountChange("1000")
        vm.onAdjust(StockChange.USE)
        assertTrue(vm.uiState.value.message?.isError == true)
        assertEquals(240, vm.uiState.value.item?.quantityAvailable)

        vm.onQuickAmount(50)
        vm.onAdjust(StockChange.DELIVERY)
        assertEquals(290, vm.uiState.value.item?.quantityAvailable)
        assertFalse(vm.uiState.value.message!!.isError)
        assertEquals("", vm.uiState.value.amount)
    }

    @Test
    fun reportsCombineAllThreeKindsAndApprovalNotifiesTheAuthor() = runTest {
        val c = managerContainer()
        val vm = ReportsViewModel(c.authRepository, c.dailyReportRepository, c.safetyRepository, c.projectRepository, c.userRepository, c.clock)
        keepCollecting(vm.uiState)
        val types = vm.uiState.value.entries.orEmpty().map { it.type }.toSet()
        assertEquals(ReportType.entries.toSet(), types)
        assertTrue(vm.uiState.value.canApprove)

        vm.onAwaitingOnlyChange(true)
        val waiting = vm.uiState.value.visible.filterIsInstance<ReportEntry.Daily>()
        assertEquals(2, waiting.size)
        val report = waiting.first().report
        val before = c.notificationRepository.observeUnreadCount(report.userId).first()

        vm.onApprove(report.reportId)

        assertEquals(ReportStatus.APPROVED, c.dailyReportRepository.observeReport(report.reportId).first()?.status)
        assertEquals(1, vm.uiState.value.visible.size)
        assertEquals(before + 1, c.notificationRepository.observeUnreadCount(report.userId).first())
        assertFalse(vm.uiState.value.message!!.isError)
    }

    @Test
    fun reportsFilterByProjectAndType() = runTest {
        val c = managerContainer()
        val vm = ReportsViewModel(c.authRepository, c.dailyReportRepository, c.safetyRepository, c.projectRepository, c.userRepository, c.clock)
        keepCollecting(vm.uiState)

        vm.onProjectFilterChange(2)
        vm.onTypeFilterChange(ReportType.DAILY)
        val visible = vm.uiState.value.visible
        assertTrue(visible.isNotEmpty())
        assertTrue(visible.all { it.projectId == 2 && it.type == ReportType.DAILY })
        assertEquals(visible.sortedByDescending { it.time }, visible)
    }

    @Test
    fun uploadingADocumentAddsAVersionAndConfirms() = runTest {
        val c = managerContainer()
        val vm = DocumentsViewModel(c.authRepository, c.documentRepository, c.projectRepository, uploadDuration = Duration.ZERO)
        keepCollecting(vm.uiState)
        assertTrue(vm.uiState.value.canUpload)

        vm.onOpenDocument(1)
        val versionBefore = vm.uiState.value.openVersions.first().version
        vm.onUploadNewVersion()
        assertEquals("Riverside-Structural-Drawings-RevC.pdf", vm.uiState.value.upload?.fileName)
        assertEquals(DocumentCategory.DRAWING, vm.uiState.value.upload?.category)
        vm.onConfirmUpload()

        assertNull(vm.uiState.value.upload)
        assertEquals(listOf("Riverside-Structural-Drawings-RevC.pdf", versionBefore + 1), vm.uiState.value.message?.formatArgs)
    }

    @Test
    fun uploadNeedsAFileNameAndAddsAnExtension() = runTest {
        val c = managerContainer()
        val vm = DocumentsViewModel(c.authRepository, c.documentRepository, c.projectRepository, uploadDuration = Duration.ZERO)
        keepCollecting(vm.uiState)

        vm.onStartUpload()
        vm.onConfirmUpload()
        assertTrue(vm.uiState.value.upload?.nameMissing == true)

        vm.onUploadNameChange("Block C floor plan")
        vm.onUploadCategoryChange(DocumentCategory.PERMIT)
        vm.onConfirmUpload()
        val newest = vm.uiState.value.documents!!.first().document
        assertEquals("Block C floor plan.pdf", newest.fileName)
        assertEquals(DocumentCategory.PERMIT, newest.category)
        assertEquals(1, newest.version)
    }

    @Test
    fun workersCanReadDocumentsButNotUpload() = runTest {
        val c = com.mcc.construction.ui.screens.workerContainer()
        val vm = DocumentsViewModel(c.authRepository, c.documentRepository, c.projectRepository, uploadDuration = Duration.ZERO)
        keepCollecting(vm.uiState)

        assertFalse(vm.uiState.value.canUpload)
        vm.onCategoryFilterChange(DocumentCategory.PERMIT)
        assertTrue(vm.uiState.value.visible.all { it.document.category == DocumentCategory.PERMIT })
    }
}
