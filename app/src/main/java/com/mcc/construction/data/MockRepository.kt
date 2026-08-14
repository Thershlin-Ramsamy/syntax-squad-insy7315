package com.mcc.construction.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

/**
 * In-memory mock data store. Task 1 prototype has no backend: this object stands in
 * for both the network layer and persistence, and simply lives for the process lifetime.
 */
object MockRepository {

    val currentWorker = WorkerProfile(
        name = "Sipho Dlamini",
        role = "Worker",
        site = "Riverside Estate — Block C"
    )

    val isOnline = mutableStateOf(true)
    val isClockedIn = mutableStateOf(false)

    val tasks = mutableStateListOf(
        Task(
            id = "T-101",
            title = "Pour concrete foundation — Unit 4",
            projectName = "Riverside Estate",
            location = "Block C, Unit 4",
            dueTime = "Today, 09:00",
            description = "Prepare formwork and pour the concrete foundation for Unit 4. " +
                "Confirm rebar placement with the site supervisor before pouring.",
            assignedBy = "Foreman: Johan Botha",
            priority = TaskPriority.HIGH,
            status = TaskStatus.IN_PROGRESS,
            safetyNote = "Hard hat and steel-toe boots required. Keep clear of the mixer swing radius."
        ),
        Task(
            id = "T-102",
            title = "Install scaffolding — East wall",
            projectName = "Riverside Estate",
            location = "Block C, East elevation",
            dueTime = "Today, 11:30",
            description = "Erect scaffolding along the east wall ahead of tomorrow's brickwork crew.",
            assignedBy = "Foreman: Johan Botha",
            priority = TaskPriority.MEDIUM,
            status = TaskStatus.PENDING,
            safetyNote = "Harness required above 2m. Inspect base plates before loading."
        ),
        Task(
            id = "T-103",
            title = "Daily safety walkaround",
            projectName = "Riverside Estate",
            location = "Full site",
            dueTime = "Today, 07:30",
            description = "Complete the morning safety walkaround and flag any hazards to the site office.",
            assignedBy = "Site Manager: Naledi Khumalo",
            priority = TaskPriority.HIGH,
            status = TaskStatus.COMPLETED
        ),
        Task(
            id = "T-104",
            title = "Deliver materials to Block B",
            projectName = "Riverside Estate",
            location = "Block B storage",
            dueTime = "Today, 13:00",
            description = "Move cement bags and rebar bundles from central storage to the Block B lay-down area.",
            assignedBy = "Foreman: Johan Botha",
            priority = TaskPriority.LOW,
            status = TaskStatus.PENDING
        ),
        Task(
            id = "T-105",
            title = "Electrical conduit rough-in",
            projectName = "Riverside Estate",
            location = "Block C, Unit 2",
            dueTime = "Tomorrow, 08:00",
            description = "Rough in electrical conduit runs for Unit 2 ahead of the inspection on Friday.",
            assignedBy = "Foreman: Johan Botha",
            priority = TaskPriority.MEDIUM,
            status = TaskStatus.PENDING,
            safetyNote = "Confirm power is isolated before working near the distribution board."
        ),
        Task(
            id = "T-106",
            title = "Clean up Block A lay-down area",
            projectName = "Riverside Estate",
            location = "Block A",
            dueTime = "Yesterday, 16:00",
            description = "Clear offcuts and packaging waste from the Block A lay-down area.",
            assignedBy = "Site Manager: Naledi Khumalo",
            priority = TaskPriority.LOW,
            status = TaskStatus.COMPLETED
        )
    )

    fun getTask(id: String): Task? = tasks.firstOrNull { it.id == id }

    fun markComplete(id: String) {
        val index = tasks.indexOfFirst { it.id == id }
        if (index != -1) {
            tasks[index] = tasks[index].copy(status = TaskStatus.COMPLETED)
        }
    }

    fun toggleOnline() {
        isOnline.value = !isOnline.value
    }

    fun toggleClockedIn() {
        isClockedIn.value = !isClockedIn.value
    }
}
