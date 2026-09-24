package com.mcc.construction.data.fake

import com.mcc.construction.data.model.Attendance
import com.mcc.construction.data.model.AuditLog
import com.mcc.construction.data.model.DailyReport
import com.mcc.construction.data.model.Document
import com.mcc.construction.data.model.IncidentReport
import com.mcc.construction.data.model.IncidentSeverity
import com.mcc.construction.data.model.Inventory
import com.mcc.construction.data.model.Message
import com.mcc.construction.data.model.Notification
import com.mcc.construction.data.model.NotificationType
import com.mcc.construction.data.model.OfflineSync
import com.mcc.construction.data.model.PendingChange
import com.mcc.construction.data.model.PendingChangeType
import com.mcc.construction.data.model.Project
import com.mcc.construction.data.model.ProjectStatus
import com.mcc.construction.data.model.ReportStatus
import com.mcc.construction.data.model.Role
import com.mcc.construction.data.model.SafetyChecklistItem
import com.mcc.construction.data.model.SafetyInspection
import com.mcc.construction.data.model.Supplier
import com.mcc.construction.data.model.SyncOutcome
import com.mcc.construction.data.model.Task
import com.mcc.construction.data.model.TaskAssignment
import com.mcc.construction.data.model.TaskPriority
import com.mcc.construction.data.model.TaskStatus
import com.mcc.construction.data.model.User
import com.mcc.construction.data.model.UserRole
import java.math.BigDecimal
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * The demo dataset every fake repository starts from.
 *
 * Everything lives in one place so ids line up across tables: a task's
 * `projectId` is a project below, an assignment's `userId` is a user below, and
 * so on. `FakeSeedDataTest` checks every one of those links.
 *
 * Dates are worked out from [clock] rather than hard-coded, so "today's tasks"
 * are always today's and the demo never looks stale. Tests pass a fixed clock.
 *
 * Company and supplier names, phone numbers and email addresses are made up.
 */
class FakeSeedData(clock: Clock) {

    private val now: LocalDateTime = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES)
    private val today: LocalDate = now.toLocalDate()

    private fun daysAgo(days: Long, hour: Int, minute: Int): LocalDateTime =
        today.minusDays(days).atTime(hour, minute)

    private fun minutesAgo(minutes: Long): LocalDateTime = now.minusMinutes(minutes)

    // -----------------------------------------------------------------------
    // Roles and users
    // -----------------------------------------------------------------------

    val roles = listOf(
        Role(
            UserRole.ADMINISTRATOR.roleId,
            "Administrator",
            "User management, system settings and audit logs"
        ),
        Role(
            UserRole.MANAGER.roleId,
            "Manager",
            "Projects, task assignment, inventory, reports, employees and attendance"
        ),
        Role(
            UserRole.WORKER.roleId,
            "Worker",
            "Daily tasks, clock in and out, daily reports and safety checklists"
        )
    )

    val users = listOf(
        User(1, UserRole.ADMINISTRATOR, "Riaan", "van Wyk", DEMO_ADMIN_EMAIL, "+27 81 902 3318", "Systems Administrator", "MCC-0002"),
        User(2, UserRole.MANAGER, "Naledi", "Khumalo", DEMO_MANAGER_EMAIL, "+27 83 210 7744", "Site Manager", "MCC-1008"),
        User(3, UserRole.MANAGER, "Johan", "Botha", "johan.botha@mcc.co.za", "+27 83 664 9021", "Site Foreman", "MCC-1012"),
        User(4, UserRole.MANAGER, "Zanele", "Mahlangu", "zanele.mahlangu@mcc.co.za", "+27 84 226 1187", "Health & Safety Officer", "MCC-1031"),
        User(5, UserRole.WORKER, "Sipho", "Dlamini", DEMO_WORKER_EMAIL, "+27 82 445 1120", "Site Operative", "MCC-2041"),
        User(6, UserRole.WORKER, "Thabo", "Mokoena", "thabo.mokoena@mcc.co.za", "+27 84 771 2093", "General Labourer", "MCC-2077"),
        User(7, UserRole.WORKER, "Lerato", "Nkosi", "lerato.nkosi@mcc.co.za", "+27 82 118 6640", "Waterproofing Specialist", "MCC-2019"),
        User(8, UserRole.WORKER, "Anele", "Mthembu", "anele.mthembu@mcc.co.za", "+27 81 330 5512", "Tower Crane Operator", "MCC-2026"),
        User(9, UserRole.MANAGER, "Pieter", "Coetzee", "pieter.coetzee@mcc.co.za", "+27 82 907 4471", "Quantity Surveyor", "MCC-1020"),
        User(10, UserRole.WORKER, "Ayanda", "Zulu", "ayanda.zulu@mcc.co.za", "+27 73 552 8804", "Steel Fixer", "MCC-2088"),
        User(11, UserRole.WORKER, "Kagiso", "Molefe", "kagiso.molefe@mcc.co.za", "+27 71 604 2219", "Bricklayer", "MCC-2093")
    )

    // -----------------------------------------------------------------------
    // Projects
    // -----------------------------------------------------------------------

    val projects = listOf(
        Project(
            projectId = 1, managerId = 2,
            projectName = "Riverside Estate Phase 2",
            location = "Centurion, Gauteng",
            budget = BigDecimal("48200000.00"),
            startDate = today.minusDays(255), endDate = today.plusDays(68),
            status = ProjectStatus.ACTIVE, progressPercent = 68.0
        ),
        Project(
            projectId = 2, managerId = 2,
            projectName = "Harbour View Offices",
            location = "Foreshore, Cape Town",
            budget = BigDecimal("72500000.00"),
            startDate = today.minusDays(205), endDate = today.plusDays(512),
            status = ProjectStatus.ACTIVE, progressPercent = 41.5
        ),
        Project(
            projectId = 3, managerId = 3,
            projectName = "N3 Bridge Rehabilitation",
            location = "Pietermaritzburg, KwaZulu-Natal",
            budget = BigDecimal("19800000.00"),
            startDate = today.minusDays(127), endDate = today.plusDays(202),
            status = ProjectStatus.ON_HOLD, progressPercent = 22.0
        ),
        Project(
            projectId = 4, managerId = 3,
            projectName = "Sandton Retail Fit-out",
            location = "Sandton, Gauteng",
            budget = BigDecimal("11300000.00"),
            startDate = today.minusDays(412), endDate = today.minusDays(209),
            status = ProjectStatus.COMPLETED, progressPercent = 100.0
        ),
        Project(
            projectId = 5, managerId = 2,
            projectName = "Umhlanga Ridge Apartments",
            location = "Umhlanga, KwaZulu-Natal",
            budget = BigDecimal("36750000.00"),
            startDate = today.plusDays(21), endDate = today.plusDays(540),
            status = ProjectStatus.PLANNING, progressPercent = 0.0
        )
    )

    // -----------------------------------------------------------------------
    // Tasks and assignments
    // -----------------------------------------------------------------------

    val tasks = listOf(
        Task(
            101, 1, "Pour concrete foundation — Unit 4",
            "Prepare formwork and pour the concrete foundation for Unit 4. Confirm rebar " +
                "spacing with the foreman before the pour. Hard hat and steel-toe boots " +
                "required; keep clear of the mixer swing radius.",
            today, TaskPriority.HIGH, TaskStatus.IN_PROGRESS
        ),
        Task(
            102, 1, "Erect scaffolding — east elevation",
            "Erect scaffolding along the Block C east wall ahead of tomorrow's brickwork " +
                "crew. Harness required above 2 m; inspect base plates before loading.",
            today, TaskPriority.MEDIUM, TaskStatus.PENDING
        ),
        Task(
            103, 1, "Morning safety walkaround",
            "Complete the morning safety walkaround and report any hazards to the site office.",
            today, TaskPriority.HIGH, TaskStatus.COMPLETED
        ),
        Task(
            104, 1, "Move cement and rebar to Block B",
            "Move 40 bags of cement and six bundles of Y12 rebar from the central store to " +
                "the Block B lay-down area.",
            today, TaskPriority.LOW, TaskStatus.PENDING
        ),
        Task(
            105, 1, "Electrical conduit rough-in — Unit 2",
            "Rough in 20 mm conduit runs for Unit 2 ahead of Friday's inspection. Confirm " +
                "the supply is isolated before working near the distribution board.",
            today.plusDays(1), TaskPriority.MEDIUM, TaskStatus.PENDING
        ),
        Task(
            106, 1, "Clear Block A lay-down area",
            "Clear offcuts and packaging from the Block A lay-down area and separate the waste.",
            today.minusDays(1), TaskPriority.LOW, TaskStatus.COMPLETED
        ),
        Task(
            107, 2, "Waterproofing — Block B roof slab",
            "Apply torch-on membrane to the Block B roof slab. Held until the delayed " +
                "membrane delivery arrives. Roof edge protection must be in place first.",
            today.plusDays(2), TaskPriority.HIGH, TaskStatus.BLOCKED
        ),
        Task(
            108, 2, "Fix Y16 rebar — level 3 columns",
            "Fix Y16 starter bars and links for level 3 columns C1 to C16 to drawing S-304 Rev B.",
            today.plusDays(3), TaskPriority.HIGH, TaskStatus.IN_PROGRESS
        ),
        Task(
            109, 2, "Brickwork — ground floor partitions",
            "Lay the ground floor partition walls in NFP bricks, gridlines 2 to 6.",
            today.plusDays(5), TaskPriority.MEDIUM, TaskStatus.PENDING
        ),
        Task(
            110, 3, "Survey bridge deck spalling",
            "Mark out and measure concrete spalling on the northbound deck soffit for the " +
                "repair quantities. On hold until environmental sign-off.",
            today.plusDays(14), TaskPriority.MEDIUM, TaskStatus.PENDING
        ),
        Task(
            111, 1, "Check in face brick delivery",
            "Count and inspect the face brick delivery against the delivery note; log any " +
                "breakages with stores.",
            today.plusDays(1), TaskPriority.LOW, TaskStatus.PENDING
        )
    )

    val assignments = listOf(
        TaskAssignment(1, 5, 101, daysAgo(2, 16, 10)),
        TaskAssignment(2, 10, 101, daysAgo(2, 16, 10)),
        TaskAssignment(3, 5, 102, daysAgo(1, 15, 30)),
        TaskAssignment(4, 5, 103, daysAgo(1, 17, 0)),
        TaskAssignment(5, 6, 104, daysAgo(1, 15, 45)),
        TaskAssignment(6, 5, 105, minutesAgo(12)),
        TaskAssignment(7, 6, 106, daysAgo(3, 7, 30)),
        TaskAssignment(8, 7, 107, daysAgo(6, 8, 0)),
        TaskAssignment(9, 10, 108, daysAgo(4, 9, 15)),
        TaskAssignment(10, 11, 109, daysAgo(2, 11, 0)),
        TaskAssignment(11, 8, 110, daysAgo(20, 10, 0)),
        TaskAssignment(12, 6, 111, daysAgo(1, 16, 20))
    )

    // -----------------------------------------------------------------------
    // Attendance
    // -----------------------------------------------------------------------

    /**
     * Sipho (the demo worker) has a week of finished shifts and is not yet clocked
     * in today, so the clock-in flow can be shown straight after signing in.
     * Three colleagues are already on site today.
     */
    val attendance: List<Attendance> = buildList {
        var id = 1
        var daysBack = 1L
        var shifts = 0
        while (shifts < 6) {
            val day = today.minusDays(daysBack)
            if (day.dayOfWeek != DayOfWeek.SUNDAY) {
                val clockIn = day.atTime(6, 45 + (shifts % 3) * 4)
                val clockOut = day.atTime(16, 5 + (shifts % 4) * 5)
                add(
                    Attendance(
                        id++, 5, day, clockIn, clockOut,
                        hoursBetween(clockIn, clockOut)
                    )
                )
                shifts++
            }
            daysBack++
        }
        add(Attendance(id++, 6, today, today.atTime(6, 48), null, null))
        add(Attendance(id++, 7, today, today.atTime(6, 55), null, null))
        add(Attendance(id, 10, today, today.atTime(7, 2), null, null))
    }

    // -----------------------------------------------------------------------
    // Daily reports
    // -----------------------------------------------------------------------

    val dailyReports = listOf(
        DailyReport(
            1, 1, 5, today.minusDays(1), "Sunny, 27 °C",
            "Formwork and rebar fixing for the Unit 4 foundation. Helped the scaffold crew " +
                "on the east wall after lunch.",
            "None.", ReportStatus.APPROVED
        ),
        DailyReport(
            2, 1, 5, today.minusDays(2), "Overcast, 22 °C",
            "Rebar fixing on Unit 4. Moved 40 bags of cement to Block B.",
            "Mixer out of action for an hour — drive belt replaced.", ReportStatus.APPROVED
        ),
        DailyReport(
            3, 1, 6, today.minusDays(2), "Overcast, 22 °C",
            "Cleared the Block A lay-down area and separated the waste for collection.",
            "Waste skip was full by 14:00; second skip requested.", ReportStatus.SUBMITTED
        ),
        DailyReport(
            4, 2, 7, today.minusDays(3), "Strong south-easter, 18 °C",
            "Surface preparation on the Block B roof slab for waterproofing.",
            "Membrane delivery delayed — roof work stopped at 11:00.", ReportStatus.APPROVED
        ),
        DailyReport(
            5, 2, 10, today.minusDays(1), "Clear, 21 °C",
            "Fixed Y16 starter bars on level 3 columns C1 to C8.",
            "Short of 12 lengths of Y16 — requested from stores.", ReportStatus.SUBMITTED
        )
    )

    // -----------------------------------------------------------------------
    // Suppliers and inventory
    // -----------------------------------------------------------------------

    val suppliers = listOf(
        Supplier(1, "Highveld Cement Supplies", "Mandla Nxumalo", "+27 11 555 0142", "orders@highveldcement.co.za"),
        Supplier(2, "Cape Steel & Rebar", "Fatima Adams", "+27 21 555 0187", "sales@capesteelrebar.co.za"),
        Supplier(3, "Rand Aggregates", "Johan Pretorius", "+27 12 555 0163", "dispatch@randaggregates.co.za"),
        Supplier(4, "SealTech Waterproofing", "Priya Naidoo", "+27 31 555 0120", "priya@sealtech.co.za"),
        Supplier(5, "Volt Electrical Wholesale", "Themba Sithole", "+27 11 555 0199", "trade@voltwholesale.co.za"),
        Supplier(6, "Ubuntu Brick & Block", "Nomsa Dube", "+27 16 555 0134", "sales@ubuntubrick.co.za"),
        Supplier(7, "Bushveld Timber & Board", "Chris Venter", "+27 13 555 0158", "orders@bushveldtimber.co.za")
    )

    val inventory = listOf(
        Inventory(1, 1, 1, "Portland cement CEM II 42.5N (50 kg)", 240, 100, "bags", BigDecimal("118.50")),
        Inventory(2, 1, 2, "Rebar Y12 (6 m lengths)", 84, 90, "bundles", BigDecimal("742.00")),
        Inventory(3, 2, 2, "Rebar Y16 (6 m lengths)", 36, 60, "bundles", BigDecimal("1318.00")),
        Inventory(4, 2, 4, "Torch-on waterproofing membrane (4 mm)", 0, 15, "rolls", BigDecimal("1340.00")),
        Inventory(5, 1, 3, "Building sand", 46, 50, "m³", BigDecimal("385.00")),
        Inventory(6, 1, 3, "19 mm crushed stone", 72, 40, "m³", BigDecimal("465.00")),
        Inventory(7, 1, 6, "NFP face bricks", 18500, 10000, "bricks", BigDecimal("3.85")),
        Inventory(8, 1, 5, "PVC conduit 20 mm (4 m)", 520, 150, "lengths", BigDecimal("44.50")),
        Inventory(9, 2, 1, "Portland cement CEM II 42.5N (50 kg)", 180, 150, "bags", BigDecimal("118.50")),
        Inventory(10, 1, 7, "Shutter ply 18 mm (1.2 × 2.4 m)", 64, 30, "sheets", BigDecimal("689.00"))
    )

    // -----------------------------------------------------------------------
    // Documents
    // -----------------------------------------------------------------------

    val documents = listOf(
        Document(1, 1, "Riverside-Structural-Drawings-RevC.pdf", 3, daysAgo(13, 9, 40)),
        Document(2, 1, "Riverside-Block-C-Method-Statement.docx", 2, daysAgo(27, 14, 5)),
        Document(3, 1, "Riverside-Health-and-Safety-File.pdf", 5, daysAgo(4, 8, 15)),
        Document(4, 2, "HarbourView-Geotechnical-Report.pdf", 1, daysAgo(102, 11, 30)),
        Document(5, 2, "HarbourView-Waterproofing-Specification.pdf", 2, daysAgo(20, 16, 45)),
        Document(6, 3, "N3-Bridge-Environmental-Authorisation.pdf", 1, daysAgo(40, 10, 0)),
        Document(7, 1, "Site-Induction-Pack.pdf", 4, daysAgo(114, 7, 30))
    )

    // -----------------------------------------------------------------------
    // Safety
    // -----------------------------------------------------------------------

    val checklistItems = listOf(
        SafetyChecklistItem(1, "PPE inspected", "Hard hat, boots, hi-vis and gloves checked and undamaged"),
        SafetyChecklistItem(2, "Work area cordoned off", "Barriers and signage in place around the pour zone"),
        SafetyChecklistItem(3, "Scaffold tags current", "Every scaffold bay shows a green tag dated this week"),
        SafetyChecklistItem(4, "Fire extinguishers accessible", "Nothing stacked in front of extinguisher points"),
        SafetyChecklistItem(5, "Plant pre-use checks done", "Mixer and hoist checked and logged before starting"),
        SafetyChecklistItem(6, "First aid kit stocked", "Site office kit sealed and within its expiry date")
    )

    val inspections = listOf(
        SafetyInspection(1, 1, 4, today.minusDays(1), "All scaffold tags current; PPE compliance good across Block C.", true),
        SafetyInspection(2, 2, 4, today.minusDays(6), "Scaffold bay 4 had no inspection tag — bay closed until re-inspected.", false),
        SafetyInspection(3, 1, 4, today.minusDays(12), "Dropped-object near miss on level 2; toe boards fitted the same day.", false)
    )

    val incidents = listOf(
        IncidentReport(
            1, 3, IncidentSeverity.NEAR_MISS.level,
            "Hand tool dropped from the level 2 scaffold. The exclusion zone was in place and nobody was hurt.",
            daysAgo(12, 10, 20)
        ),
        IncidentReport(
            2, 2, IncidentSeverity.NEAR_MISS.level,
            "Scaffold bay 4 found in use without an inspection tag.",
            daysAgo(6, 9, 5)
        ),
        IncidentReport(
            3, 3, IncidentSeverity.MINOR.level,
            "Minor cut to the forearm from rebar tie wire; treated on site from the first aid kit.",
            daysAgo(11, 14, 35)
        )
    )

    // -----------------------------------------------------------------------
    // Notifications and messages
    // -----------------------------------------------------------------------

    val notifications = listOf(
        // Sipho (worker)
        Notification(1, 5, NotificationType.TASK, "Johan Botha assigned you “Electrical conduit rough-in — Unit 2”.", minutesAgo(12), false),
        Notification(2, 5, NotificationType.SAFETY, "Your daily safety checklist for Riverside Estate Phase 2 is due.", minutesAgo(64), false),
        Notification(3, 5, NotificationType.MESSAGE, "Johan Botha: Confirm the rebar spacing before you pour.", minutesAgo(118), true),
        Notification(4, 5, NotificationType.REPORT, "Your daily report for yesterday was approved.", minutesAgo(160), true),
        Notification(5, 5, NotificationType.SYSTEM, "4 changes saved offline were uploaded.", daysAgo(1, 17, 4), true),
        // Naledi (manager)
        Notification(6, 2, NotificationType.INVENTORY, "Torch-on waterproofing membrane is out of stock at Harbour View Offices.", minutesAgo(41), false),
        Notification(7, 2, NotificationType.TASK, "Lerato Nkosi marked “Waterproofing — Block B roof slab” as blocked.", minutesAgo(185), false),
        Notification(8, 2, NotificationType.REPORT, "Ayanda Zulu's daily report is waiting for your approval.", minutesAgo(290), false),
        Notification(9, 2, NotificationType.SAFETY, "Scaffold bay 4 failed inspection at Harbour View Offices.", daysAgo(6, 9, 10), true),
        Notification(10, 2, NotificationType.INVENTORY, "Rebar Y12 is below its minimum level at Riverside Estate Phase 2.", daysAgo(1, 13, 25), true),
        // Riaan (administrator)
        Notification(11, 1, NotificationType.SYSTEM, "5 failed sign-in attempts for pieter.coetzee@mcc.co.za.", minutesAgo(96), false),
        Notification(12, 1, NotificationType.SYSTEM, "Nightly backup completed.", minutesAgo(310), true)
    )

    val messages = listOf(
        Message(1, 3, 5, "Morning Sipho — are you on site yet?", minutesAgo(134)),
        Message(2, 5, 3, "Morning. Yes, on my way to Block C now.", minutesAgo(131)),
        Message(3, 3, 5, "Good. The Unit 4 pour is going ahead today.", minutesAgo(125), isRead = false),
        Message(4, 3, 5, "Confirm the rebar spacing before you pour.", minutesAgo(118), isRead = false),
        Message(5, 2, 5, "Well done on yesterday's walkaround.", daysAgo(1, 16, 40)),
        Message(6, 5, 2, "Thank you.", daysAgo(1, 16, 52)),
        Message(7, 7, 2, "Membrane delivery pushed to Monday — roof is on hold.", daysAgo(1, 11, 3)),
        Message(8, 2, 7, "Noted. Move the crew to the level 3 columns until then.", daysAgo(1, 11, 20)),
        Message(9, 3, 2, "Riverside pour for Unit 4 is scheduled for today.", minutesAgo(200), isRead = false),
        Message(10, 6, 5, "Cement for Block B is staged at the lay-down area.", minutesAgo(75), isRead = false)
    )

    // -----------------------------------------------------------------------
    // Administration
    // -----------------------------------------------------------------------

    val auditLog = listOf(
        AuditLog(1, 1, "Changed role of Kagiso Molefe to Worker", minutesAgo(35)),
        AuditLog(2, 2, "Approved daily report #2 — Riverside Estate Phase 2", daysAgo(1, 17, 2)),
        AuditLog(3, 1, "Suspended account pieter.coetzee@mcc.co.za", daysAgo(2, 9, 15)),
        AuditLog(4, 3, "Exported employee attendance — Riverside Estate Phase 2", daysAgo(3, 14, 48)),
        AuditLog(5, 1, "Updated system setting: session timeout 30 minutes", daysAgo(5, 8, 5))
    )

    // -----------------------------------------------------------------------
    // Sync
    // -----------------------------------------------------------------------

    val pendingChanges = listOf(
        PendingChange(1, PendingChangeType.SAFETY_INSPECTION, "Riverside Estate Phase 2", minutesAgo(40)),
        PendingChange(2, PendingChangeType.TASK_UPDATE, "Morning safety walkaround", minutesAgo(25))
    )

    val lastSyncedAt: LocalDateTime = minutesAgo(125)

    val syncHistory = listOf(
        OfflineSync(1, 5, DEMO_DEVICE_ID, SyncOutcome.COMPLETED, minutesAgo(125)),
        OfflineSync(2, 5, DEMO_DEVICE_ID, SyncOutcome.COMPLETED, daysAgo(1, 17, 4)),
        OfflineSync(3, 5, DEMO_DEVICE_ID, SyncOutcome.FAILED, daysAgo(1, 12, 10)),
        OfflineSync(4, 5, DEMO_DEVICE_ID, SyncOutcome.COMPLETED, daysAgo(2, 16, 30))
    )

    companion object {
        const val DEMO_WORKER_EMAIL = "worker@mcc.co.za"
        const val DEMO_MANAGER_EMAIL = "manager@mcc.co.za"
        const val DEMO_ADMIN_EMAIL = "admin@mcc.co.za"
        const val DEMO_DEVICE_ID = "MCC-TAB-0417"
    }
}

/** Shift length in hours, rounded to two decimals like `TotalHours decimal(5,2)`. */
internal fun hoursBetween(start: LocalDateTime, end: LocalDateTime): Double {
    val minutes = ChronoUnit.MINUTES.between(start, end)
    return Math.round(minutes / 60.0 * 100) / 100.0
}
