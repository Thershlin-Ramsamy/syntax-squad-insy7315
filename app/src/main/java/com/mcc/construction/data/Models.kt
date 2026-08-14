package com.mcc.construction.data

/**
 * Domain types for the prototype.
 *
 * These are plain data classes with no persistence or serialisation concerns —
 * the whole data layer lives in memory (see [MockRepository]). They are shaped
 * the way a real API would return them so that swapping the repository for a
 * network client in Task 2 does not require touching the screens.
 */

// ---------------------------------------------------------------------------
// Identity
// ---------------------------------------------------------------------------

enum class UserRole(val label: String, val blurb: String) {
    WORKER("Worker", "Today's tasks, clock in/out, reports"),
    MANAGER("Manager", "Projects, teams, inventory, reports"),
    ADMINISTRATOR("Administrator", "Users, settings, audit logs")
}

data class UserProfile(
    val name: String,
    val role: UserRole,
    val jobTitle: String,
    val site: String,
    val employeeId: String,
    val email: String,
    val phone: String
)

// ---------------------------------------------------------------------------
// Tasks
// ---------------------------------------------------------------------------

enum class TaskStatus(val label: String) {
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    BLOCKED("Blocked")
}

enum class TaskPriority(val label: String) {
    LOW("Low"), MEDIUM("Medium"), HIGH("High")
}

data class Task(
    val id: String,
    val title: String,
    val projectName: String,
    val location: String,
    val dueTime: String,
    val description: String,
    val assignedTo: String,
    val assignedBy: String,
    val priority: TaskPriority,
    val status: TaskStatus,
    val safetyNote: String? = null
)

// ---------------------------------------------------------------------------
// Projects
// ---------------------------------------------------------------------------

enum class ProjectStatus(val label: String) {
    PLANNING("Planning"),
    ACTIVE("Active"),
    ON_HOLD("On Hold"),
    COMPLETE("Complete")
}

data class Project(
    val id: String,
    val name: String,
    val client: String,
    val location: String,
    val status: ProjectStatus,
    val progress: Float,
    val startDate: String,
    val targetDate: String,
    val budget: String,
    val spent: String,
    val manager: String,
    val teamSize: Int,
    val openTasks: Int,
    val description: String
)

// ---------------------------------------------------------------------------
// People
// ---------------------------------------------------------------------------

enum class EmployeeStatus(val label: String) {
    ON_SITE("On Site"),
    OFF_DUTY("Off Duty"),
    ON_LEAVE("On Leave")
}

data class Employee(
    val id: String,
    val name: String,
    val jobTitle: String,
    val role: UserRole,
    val project: String,
    val status: EmployeeStatus,
    val phone: String,
    val email: String,
    val startDate: String,
    val certifications: List<String>,
    val hoursThisWeek: Int
)

// ---------------------------------------------------------------------------
// Attendance
// ---------------------------------------------------------------------------

data class AttendanceRecord(
    val date: String,
    val clockIn: String,
    val clockOut: String?,
    val hours: String,
    val site: String
)

// ---------------------------------------------------------------------------
// Inventory
// ---------------------------------------------------------------------------

enum class StockLevel(val label: String) {
    IN_STOCK("In Stock"),
    LOW("Low Stock"),
    OUT("Out of Stock")
}

data class Material(
    val id: String,
    val name: String,
    val category: String,
    val quantity: Int,
    val unit: String,
    val reorderLevel: Int,
    val location: String,
    val supplier: String,
    val unitCost: String,
    val lastDelivery: String
) {
    val stockLevel: StockLevel
        get() = when {
            quantity == 0 -> StockLevel.OUT
            quantity <= reorderLevel -> StockLevel.LOW
            else -> StockLevel.IN_STOCK
        }
}

// ---------------------------------------------------------------------------
// Messaging
// ---------------------------------------------------------------------------

data class Conversation(
    val id: String,
    val name: String,
    val lastMessage: String,
    val timestamp: String,
    val unread: Int,
    val isGroup: Boolean
)

data class ChatMessage(
    val id: String,
    val sender: String,
    val body: String,
    val timestamp: String,
    val fromMe: Boolean
)

// ---------------------------------------------------------------------------
// Safety
// ---------------------------------------------------------------------------

data class ChecklistItem(
    val id: String,
    val text: String,
    val detail: String,
    val checked: Boolean
)

enum class IncidentSeverity(val label: String) {
    NEAR_MISS("Near Miss"),
    MINOR("Minor"),
    SERIOUS("Serious")
}

data class Incident(
    val id: String,
    val title: String,
    val site: String,
    val reportedBy: String,
    val date: String,
    val severity: IncidentSeverity,
    val status: String
)

// ---------------------------------------------------------------------------
// Reports and documents
// ---------------------------------------------------------------------------

data class DailyReportEntry(
    val id: String,
    val date: String,
    val project: String,
    val submittedBy: String,
    val hours: String,
    val summary: String,
    val status: String
)

data class DocumentItem(
    val id: String,
    val name: String,
    val type: String,
    val sizeLabel: String,
    val project: String,
    val updated: String
)

// ---------------------------------------------------------------------------
// Notifications
// ---------------------------------------------------------------------------

enum class NotificationKind { TASK, SAFETY, MESSAGE, SYSTEM, INVENTORY }

data class AppNotification(
    val id: String,
    val kind: NotificationKind,
    val title: String,
    val body: String,
    val timestamp: String,
    val read: Boolean
)

// ---------------------------------------------------------------------------
// Administration
// ---------------------------------------------------------------------------

enum class AccountStatus(val label: String) {
    ACTIVE("Active"),
    SUSPENDED("Suspended"),
    INVITED("Invited")
}

data class ManagedUser(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val status: AccountStatus,
    val lastActive: String
)

enum class AuditSeverity { INFO, WARNING, CRITICAL }

data class AuditEntry(
    val id: String,
    val actor: String,
    val action: String,
    val target: String,
    val timestamp: String,
    val severity: AuditSeverity,
    val ipAddress: String
)

// ---------------------------------------------------------------------------
// Sync
// ---------------------------------------------------------------------------

data class SyncItem(
    val id: String,
    val label: String,
    val detail: String,
    val queuedAt: String
)
