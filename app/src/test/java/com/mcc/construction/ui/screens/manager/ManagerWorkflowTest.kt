package com.mcc.construction.ui.screens.manager

import com.mcc.construction.data.fake.FakeSeedData
import com.mcc.construction.data.model.NotificationType
import com.mcc.construction.data.model.TaskPriority
import com.mcc.construction.data.model.TaskStatus
import com.mcc.construction.ui.MainDispatcherRule
import com.mcc.construction.ui.screens.MANAGER_ID
import com.mcc.construction.ui.screens.WORKER_ID
import com.mcc.construction.ui.screens.dashboard.ManagerDashboardViewModel
import com.mcc.construction.ui.screens.keepCollecting
import com.mcc.construction.ui.screens.managerContainer
import com.mcc.construction.ui.screens.projects.AssignField
import com.mcc.construction.ui.screens.projects.AssignTaskViewModel
import com.mcc.construction.ui.screens.switchTo
import com.mcc.construction.ui.screens.tasks.TaskListViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The demo's centrepiece, end to end through the ViewModels: the manager
 * assigns a task, the worker is notified and sees it, the worker moves it on,
 * and the manager's dashboard shows the progress.
 */
class ManagerWorkflowTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun assignNotifyUpdateAndSeeProgress() = runTest {
        val c = managerContainer()

        // 1. The manager assigns a task on Riverside to Sipho.
        val assign = AssignTaskViewModel(1, c.projectRepository, c.taskRepository, c.userRepository, c.attendanceRepository, c.clock)
        keepCollecting(assign.uiState)
        assign.onTitleChange("Strip formwork on Block C")
        assign.onDeadlineSelected(c.clock.instant().atZone(c.clock.zone).toLocalDate().plusDays(1))
        assign.onPrioritySelected(TaskPriority.HIGH)
        assign.onToggleWorker(WORKER_ID)
        assign.onSave()
        val saved = assign.uiState.value.form.saved
        assertNotNull(saved)
        assertEquals(listOf("Sipho"), saved!!.workerNames)

        // 2. Sipho signs in: the notification is waiting and the task is on his list.
        c.switchTo(FakeSeedData.DEMO_WORKER_EMAIL)
        val newest = c.notificationRepository.observeNotifications(WORKER_ID).first().first()
        assertEquals(NotificationType.TASK, newest.type)
        assertTrue(newest.messageContent.contains("Strip formwork on Block C"))
        val tasks = TaskListViewModel(c.authRepository, c.taskRepository, c.projectRepository, c.clock)
        keepCollecting(tasks.uiState)
        assertTrue(tasks.uiState.value.items.orEmpty().any { it.task.taskId == saved.task.taskId })

        // 3. Sipho starts the task.
        c.taskRepository.updateStatus(saved.task.taskId, TaskStatus.IN_PROGRESS)

        // 4. Naledi's dashboard shows the update and the new count.
        c.switchTo(FakeSeedData.DEMO_MANAGER_EMAIL)
        val dashboard = ManagerDashboardViewModel(
            c.authRepository, c.projectRepository, c.taskRepository, c.attendanceRepository, c.userRepository,
            c.inventoryRepository, c.safetyRepository, c.dailyReportRepository, c.notificationRepository, c.clock
        )
        keepCollecting(dashboard.uiState)
        val state = dashboard.uiState.value
        assertTrue(state.updates.first().messageContent.startsWith("Sipho Dlamini started"))
        assertTrue(state.updates.first().userId == MANAGER_ID)
        val all = c.taskRepository.observeTasks().first()
        assertEquals(all.count { it.status == TaskStatus.IN_PROGRESS }, state.taskCounts[TaskStatus.IN_PROGRESS])
    }

    @Test
    fun assignTaskFlagsMissingFieldsAndSavesNothing() = runTest {
        val c = managerContainer()
        val before = c.taskRepository.observeTasks().first().size
        val assign = AssignTaskViewModel(1, c.projectRepository, c.taskRepository, c.userRepository, c.attendanceRepository, c.clock)
        keepCollecting(assign.uiState)

        assign.onSave()

        assertEquals(setOf(AssignField.TITLE, AssignField.DEADLINE, AssignField.WORKERS), assign.uiState.value.form.missing)
        assertTrue(assign.uiState.value.message?.isError == true)
        assertNull(assign.uiState.value.form.saved)
        assertEquals(before, c.taskRepository.observeTasks().first().size)
    }

    @Test
    fun assigningSeveralWorkersNotifiesEachOne() = runTest {
        val c = managerContainer()
        val crew = listOf(5, 6, 10)
        val before = crew.associateWith { c.notificationRepository.observeUnreadCount(it).first() }
        val assign = AssignTaskViewModel(2, c.projectRepository, c.taskRepository, c.userRepository, c.attendanceRepository, c.clock)
        keepCollecting(assign.uiState)

        assign.onTitleChange("Waterproof Block B roof")
        assign.onDeadlineSelected(c.clock.instant().atZone(c.clock.zone).toLocalDate())
        crew.forEach(assign::onToggleWorker)
        assign.onSave()

        val task = assign.uiState.value.form.saved!!.task
        assertEquals(2, task.projectId)
        assertEquals(crew.toSet(), c.taskRepository.observeAssignments(task.taskId).first().map { it.userId }.toSet())
        crew.forEach { id ->
            assertEquals(before.getValue(id) + 1, c.notificationRepository.observeUnreadCount(id).first())
        }
    }

    @Test
    fun onlyActiveWorkersCanBeAssigned() = runTest {
        val c = managerContainer()
        c.userRepository.setActive(8, false)
        val assign = AssignTaskViewModel(1, c.projectRepository, c.taskRepository, c.userRepository, c.attendanceRepository, c.clock)
        keepCollecting(assign.uiState)

        val ids = assign.uiState.value.workers.map { it.user.userId }
        assertTrue(8 !in ids)
        assertTrue(WORKER_ID in ids)
        assertTrue(assign.uiState.value.projects.none { it.status == com.mcc.construction.data.model.ProjectStatus.COMPLETED })
    }
}
