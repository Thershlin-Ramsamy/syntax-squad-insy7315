package com.mcc.construction.data.legacy

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.mcc.construction.data.model.UserRole

/**
 * Legacy in-memory store from the first prototype pass.
 *
 * Superseded by the repository layer (`data.repository` interfaces backed by
 * `data.fake` implementations). It stays only so the screens that have not
 * been migrated yet keep compiling and showing data; nothing new should be
 * added here. See docs/ARCHITECTURE.md, "Migrating a legacy screen".
 *
 * Original notes:
 *
 * In-memory data store standing in for both the API and local persistence.
 *
 * Task 1 has no backend by design, so this object holds every list the UI reads
 * and every piece of session state it mutates. State that screens change —
 * clock-in, checklist ticks, task completion, the signed-in role — is held in
 * Compose state so the UI recomposes exactly as it would against a real
 * observable data source.
 */
object MockRepository {

    // -----------------------------------------------------------------------
    // Session
    // -----------------------------------------------------------------------

    /**
     * Mirror of the signed-in role from the AuthRepository, kept in step by the
     * app root so legacy screens show the right user.
     */
    val currentRole = mutableStateOf(UserRole.WORKER)

    /**
     * Mirror of the SyncRepository's online state, kept in step by the app
     * root. Read-only for screens: offline mode is switched in Settings or on
     * the Offline & sync screen.
     */
    val isOnline = mutableStateOf(true)
    val isClockedIn = mutableStateOf(false)

    fun toggleClockedIn() {
        isClockedIn.value = !isClockedIn.value
    }

    private val profiles = mapOf(
        UserRole.WORKER to UserProfile(
            name = "Sipho Dlamini",
            role = UserRole.WORKER,
            jobTitle = "Site Operative",
            site = "Riverside Estate — Block C",
            employeeId = "MCC-2041",
            email = "s.dlamini@mccconstruction.co.za",
            phone = "+27 82 445 1120"
        ),
        UserRole.MANAGER to UserProfile(
            name = "Naledi Khumalo",
            role = UserRole.MANAGER,
            jobTitle = "Site Manager",
            site = "Riverside Estate",
            employeeId = "MCC-1008",
            email = "n.khumalo@mccconstruction.co.za",
            phone = "+27 83 210 7744"
        ),
        UserRole.ADMINISTRATOR to UserProfile(
            name = "Riaan van Wyk",
            role = UserRole.ADMINISTRATOR,
            jobTitle = "Systems Administrator",
            site = "Head Office — Midrand",
            employeeId = "MCC-0002",
            email = "r.vanwyk@mccconstruction.co.za",
            phone = "+27 81 902 3318"
        )
    )

    val currentUser: UserProfile
        get() = profiles.getValue(currentRole.value)

    // -----------------------------------------------------------------------
    // Tasks
    // -----------------------------------------------------------------------

    val tasks = mutableStateListOf(
        Task(
            id = "T-101",
            title = "Pour concrete foundation — Unit 4",
            projectName = "Riverside Estate",
            location = "Block C, Unit 4",
            dueTime = "Today, 09:00",
            description = "Prepare formwork and pour the concrete foundation for Unit 4. " +
                "Confirm rebar placement with the site supervisor before pouring.",
            assignedTo = "Sipho Dlamini",
            assignedBy = "Johan Botha",
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
            assignedTo = "Sipho Dlamini",
            assignedBy = "Johan Botha",
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
            assignedTo = "Sipho Dlamini",
            assignedBy = "Naledi Khumalo",
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
            assignedTo = "Thabo Mokoena",
            assignedBy = "Johan Botha",
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
            assignedTo = "Sipho Dlamini",
            assignedBy = "Johan Botha",
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
            assignedTo = "Thabo Mokoena",
            assignedBy = "Naledi Khumalo",
            priority = TaskPriority.LOW,
            status = TaskStatus.COMPLETED
        ),
        Task(
            id = "T-107",
            title = "Waterproofing — Block B roof slab",
            projectName = "Harbour View Offices",
            location = "Block B, roof",
            dueTime = "Blocked — awaiting materials",
            description = "Apply waterproofing membrane to the Block B roof slab. Currently held " +
                "pending the delayed membrane delivery.",
            assignedTo = "Lerato Nkosi",
            assignedBy = "Naledi Khumalo",
            priority = TaskPriority.HIGH,
            status = TaskStatus.BLOCKED,
            safetyNote = "Roof edge protection must be in place before any work starts."
        )
    )

    fun getTask(id: String): Task? = tasks.firstOrNull { it.id == id }

    fun markComplete(id: String) {
        val index = tasks.indexOfFirst { it.id == id }
        if (index != -1) tasks[index] = tasks[index].copy(status = TaskStatus.COMPLETED)
    }

    // -----------------------------------------------------------------------
    // Projects
    // -----------------------------------------------------------------------

    val projects = listOf(
        Project(
            id = "P-01",
            name = "Riverside Estate",
            client = "Riverside Developments",
            location = "Centurion, Gauteng",
            status = ProjectStatus.ACTIVE,
            progress = 0.68f,
            startDate = "12 Jan 2026",
            targetDate = "30 Nov 2026",
            budget = "R 48.2m",
            spent = "R 31.7m",
            manager = "Naledi Khumalo",
            teamSize = 34,
            openTasks = 12,
            description = "Residential estate of 96 units across four blocks, including " +
                "associated roadworks and a clubhouse."
        ),
        Project(
            id = "P-02",
            name = "Harbour View Offices",
            client = "Cape Harbour Property",
            location = "Cape Town, Western Cape",
            status = ProjectStatus.ACTIVE,
            progress = 0.41f,
            startDate = "03 Mar 2026",
            targetDate = "18 Feb 2027",
            budget = "R 72.5m",
            spent = "R 29.9m",
            manager = "Naledi Khumalo",
            teamSize = 51,
            openTasks = 23,
            description = "Six-storey commercial office block with two basement parking levels."
        ),
        Project(
            id = "P-03",
            name = "N3 Bridge Rehabilitation",
            client = "SANRAL",
            location = "Pietermaritzburg, KZN",
            status = ProjectStatus.ON_HOLD,
            progress = 0.22f,
            startDate = "20 May 2026",
            targetDate = "14 Apr 2027",
            budget = "R 19.8m",
            spent = "R 4.4m",
            manager = "Johan Botha",
            teamSize = 18,
            openTasks = 5,
            description = "Structural rehabilitation of two bridge decks, on hold pending " +
                "environmental sign-off."
        ),
        Project(
            id = "P-04",
            name = "Sandton Retail Fit-out",
            client = "Grove Retail Group",
            location = "Sandton, Gauteng",
            status = ProjectStatus.COMPLETE,
            progress = 1f,
            startDate = "08 Aug 2025",
            targetDate = "27 Feb 2026",
            budget = "R 11.3m",
            spent = "R 10.9m",
            manager = "Johan Botha",
            teamSize = 12,
            openTasks = 0,
            description = "Interior fit-out of 14 retail units, handed over ahead of schedule."
        )
    )

    fun getProject(id: String): Project? = projects.firstOrNull { it.id == id }

    // -----------------------------------------------------------------------
    // Employees
    // -----------------------------------------------------------------------

    val employees = listOf(
        Employee(
            "E-01", "Sipho Dlamini", "Site Operative", UserRole.WORKER,
            "Riverside Estate", EmployeeStatus.ON_SITE, "+27 82 445 1120",
            "s.dlamini@mccconstruction.co.za", "14 Feb 2023",
            listOf("Working at Heights", "First Aid Level 1"), 38
        ),
        Employee(
            "E-02", "Thabo Mokoena", "General Labourer", UserRole.WORKER,
            "Riverside Estate", EmployeeStatus.ON_SITE, "+27 84 771 2093",
            "t.mokoena@mccconstruction.co.za", "02 Jun 2024",
            listOf("Site Induction"), 41
        ),
        Employee(
            "E-03", "Lerato Nkosi", "Waterproofing Specialist", UserRole.WORKER,
            "Harbour View Offices", EmployeeStatus.ON_SITE, "+27 82 118 6640",
            "l.nkosi@mccconstruction.co.za", "19 Sep 2022",
            listOf("Working at Heights", "Confined Space"), 36
        ),
        Employee(
            "E-04", "Johan Botha", "Foreman", UserRole.MANAGER,
            "Riverside Estate", EmployeeStatus.ON_SITE, "+27 83 664 9021",
            "j.botha@mccconstruction.co.za", "11 Mar 2019",
            listOf("Supervisor Certification", "First Aid Level 2"), 45
        ),
        Employee(
            "E-05", "Naledi Khumalo", "Site Manager", UserRole.MANAGER,
            "Riverside Estate", EmployeeStatus.ON_SITE, "+27 83 210 7744",
            "n.khumalo@mccconstruction.co.za", "05 Jul 2018",
            listOf("Pr.CM Registration", "Health & Safety Officer"), 44
        ),
        Employee(
            "E-06", "Anele Mthembu", "Crane Operator", UserRole.WORKER,
            "Harbour View Offices", EmployeeStatus.OFF_DUTY, "+27 81 330 5512",
            "a.mthembu@mccconstruction.co.za", "23 Jan 2021",
            listOf("Crane Licence", "Working at Heights"), 40
        ),
        Employee(
            "E-07", "Pieter Coetzee", "Quantity Surveyor", UserRole.MANAGER,
            "Harbour View Offices", EmployeeStatus.ON_LEAVE, "+27 82 907 4471",
            "p.coetzee@mccconstruction.co.za", "30 Oct 2020",
            listOf("Pr.QS Registration"), 0
        ),
        Employee(
            "E-08", "Zanele Mahlangu", "Safety Officer", UserRole.MANAGER,
            "Riverside Estate", EmployeeStatus.ON_SITE, "+27 84 226 1187",
            "z.mahlangu@mccconstruction.co.za", "08 Apr 2022",
            listOf("SAMTRAC", "First Aid Level 3"), 42
        )
    )

    fun getEmployee(id: String): Employee? = employees.firstOrNull { it.id == id }

    // -----------------------------------------------------------------------
    // Attendance
    // -----------------------------------------------------------------------

    val attendance = listOf(
        AttendanceRecord("Today", "06:52", null, "In progress", "Riverside Estate"),
        AttendanceRecord("Yesterday", "06:48", "16:12", "9h 24m", "Riverside Estate"),
        AttendanceRecord("Tue, 12 Aug", "06:55", "16:05", "9h 10m", "Riverside Estate"),
        AttendanceRecord("Mon, 11 Aug", "07:02", "15:58", "8h 56m", "Riverside Estate"),
        AttendanceRecord("Fri, 08 Aug", "06:50", "16:20", "9h 30m", "Riverside Estate")
    )

    // -----------------------------------------------------------------------
    // Inventory
    // -----------------------------------------------------------------------

    val materials = listOf(
        Material(
            "M-01", "Portland Cement 42.5N", "Cement & Aggregates", 240, "bags", 100,
            "Riverside — Central Store", "PPC Cement", "R 118.00", "09 Aug 2026"
        ),
        Material(
            "M-02", "Rebar Y12 (6m)", "Steel", 84, "bundles", 90,
            "Riverside — Yard B", "Cape Steel Supplies", "R 742.00", "05 Aug 2026"
        ),
        Material(
            "M-03", "Waterproofing Membrane", "Finishes", 0, "rolls", 15,
            "Harbour View — Store 2", "SikaSA", "R 1 340.00", "22 Jul 2026"
        ),
        Material(
            "M-04", "Scaffold Boards", "Access Equipment", 310, "boards", 120,
            "Riverside — Yard A", "Formscaff", "R 265.00", "11 Aug 2026"
        ),
        Material(
            "M-05", "Building Sand", "Cement & Aggregates", 46, "m³", 50,
            "Riverside — Yard A", "AfriSam", "R 385.00", "12 Aug 2026"
        ),
        Material(
            "M-06", "Electrical Conduit 20mm", "Electrical", 520, "lengths", 150,
            "Riverside — Central Store", "Voltex", "R 44.50", "07 Aug 2026"
        )
    )

    fun getMaterial(id: String): Material? = materials.firstOrNull { it.id == id }

    // -----------------------------------------------------------------------
    // Messaging
    // -----------------------------------------------------------------------

    val conversations = listOf(
        Conversation("C-01", "Johan Botha", "Confirm the rebar spacing before you pour.", "08:14", 2, false),
        Conversation("C-02", "Block C Crew", "Lerato: Scaffold is up on the east side.", "07:52", 0, true),
        Conversation("C-03", "Naledi Khumalo", "Well done on yesterday's walkaround.", "Yesterday", 0, false),
        Conversation("C-04", "Safety Notices", "Zanele: Reminder — harness checks Friday.", "Yesterday", 1, true),
        Conversation("C-05", "Stores — Riverside", "Membrane delivery pushed to Monday.", "Tue", 0, false)
    )

    fun getConversation(id: String): Conversation? = conversations.firstOrNull { it.id == id }

    fun messagesFor(conversationId: String): List<ChatMessage> = when (conversationId) {
        "C-01" -> listOf(
            ChatMessage("m1", "Johan Botha", "Morning Sipho — are you on site yet?", "08:02", false),
            ChatMessage("m2", "Sipho Dlamini", "Morning. Yes, just clocked in.", "08:05", true),
            ChatMessage("m3", "Johan Botha", "Good. Unit 4 pour is going ahead today.", "08:11", false),
            ChatMessage("m4", "Johan Botha", "Confirm the rebar spacing before you pour.", "08:14", false)
        )

        "C-02" -> listOf(
            ChatMessage("m1", "Thabo Mokoena", "Boards are staged at Yard A.", "07:31", false),
            ChatMessage("m2", "Sipho Dlamini", "Thanks Thabo, I'll fetch them shortly.", "07:40", true),
            ChatMessage("m3", "Lerato Nkosi", "Scaffold is up on the east side.", "07:52", false)
        )

        else -> listOf(
            ChatMessage("m1", "Naledi Khumalo", "Well done on yesterday's walkaround.", "Yesterday", false),
            ChatMessage("m2", "Sipho Dlamini", "Thank you.", "Yesterday", true)
        )
    }

    // -----------------------------------------------------------------------
    // Safety
    // -----------------------------------------------------------------------

    val checklist = mutableStateListOf(
        ChecklistItem("S-1", "PPE inspected", "Hard hat, boots, hi-vis and gloves checked and undamaged", true),
        ChecklistItem("S-2", "Work area cordoned", "Barriers and signage in place around the pour zone", true),
        ChecklistItem("S-3", "Scaffold tags current", "All scaffold bays show a green tag dated this week", false),
        ChecklistItem("S-4", "Fire extinguishers accessible", "Nothing stacked in front of extinguisher points", false),
        ChecklistItem("S-5", "Plant pre-use check done", "Mixer and hoist checked and logged before starting", false),
        ChecklistItem("S-6", "First aid kit stocked", "Site office kit sealed and within expiry", true)
    )

    fun toggleChecklistItem(id: String) {
        val index = checklist.indexOfFirst { it.id == id }
        if (index != -1) checklist[index] = checklist[index].copy(checked = !checklist[index].checked)
    }

    val incidents = listOf(
        Incident("I-01", "Dropped hand tool from level 2", "Riverside Estate", "Thabo Mokoena", "11 Aug 2026", IncidentSeverity.NEAR_MISS, "Closed"),
        Incident("I-02", "Minor laceration — rebar tie", "Riverside Estate", "Sipho Dlamini", "04 Aug 2026", IncidentSeverity.MINOR, "Closed"),
        Incident("I-03", "Scaffold bay tag missing", "Harbour View Offices", "Lerato Nkosi", "29 Jul 2026", IncidentSeverity.NEAR_MISS, "Under review")
    )

    // -----------------------------------------------------------------------
    // Reports and documents
    // -----------------------------------------------------------------------

    val dailyReports = listOf(
        DailyReportEntry("R-01", "13 Aug 2026", "Riverside Estate", "Sipho Dlamini", "9h 24m", "Foundation prep for Unit 4, scaffold assist on east wall.", "Approved"),
        DailyReportEntry("R-02", "12 Aug 2026", "Riverside Estate", "Sipho Dlamini", "9h 10m", "Rebar fixing Unit 4, material movement to Block B.", "Approved"),
        DailyReportEntry("R-03", "11 Aug 2026", "Riverside Estate", "Thabo Mokoena", "8h 56m", "Lay-down area clearance and waste segregation.", "Submitted"),
        DailyReportEntry("R-04", "08 Aug 2026", "Harbour View Offices", "Lerato Nkosi", "9h 30m", "Roof slab prep, membrane delivery delayed.", "Approved")
    )

    val documents = listOf(
        DocumentItem("D-01", "Riverside — Structural Drawings Rev C", "PDF", "8.4 MB", "Riverside Estate", "11 Aug 2026"),
        DocumentItem("D-02", "Health & Safety File 2026", "PDF", "2.1 MB", "All projects", "01 Aug 2026"),
        DocumentItem("D-03", "Block C Method Statement", "DOCX", "640 KB", "Riverside Estate", "28 Jul 2026"),
        DocumentItem("D-04", "Harbour View — Geotechnical Report", "PDF", "12.7 MB", "Harbour View Offices", "14 Jun 2026"),
        DocumentItem("D-05", "Site Induction Pack", "PDF", "1.3 MB", "All projects", "02 Jun 2026")
    )

    // -----------------------------------------------------------------------
    // Administration
    // -----------------------------------------------------------------------

    val managedUsers = mutableStateListOf(
        ManagedUser("U-01", "Sipho Dlamini", "s.dlamini@mccconstruction.co.za", UserRole.WORKER, AccountStatus.ACTIVE, "Today, 08:05"),
        ManagedUser("U-02", "Thabo Mokoena", "t.mokoena@mccconstruction.co.za", UserRole.WORKER, AccountStatus.ACTIVE, "Today, 07:31"),
        ManagedUser("U-03", "Naledi Khumalo", "n.khumalo@mccconstruction.co.za", UserRole.MANAGER, AccountStatus.ACTIVE, "Today, 08:22"),
        ManagedUser("U-04", "Johan Botha", "j.botha@mccconstruction.co.za", UserRole.MANAGER, AccountStatus.ACTIVE, "Today, 06:44"),
        ManagedUser("U-05", "Pieter Coetzee", "p.coetzee@mccconstruction.co.za", UserRole.MANAGER, AccountStatus.SUSPENDED, "22 Jul 2026"),
        ManagedUser("U-06", "Riaan van Wyk", "r.vanwyk@mccconstruction.co.za", UserRole.ADMINISTRATOR, AccountStatus.ACTIVE, "Today, 08:40"),
        ManagedUser("U-07", "Amara Dube", "a.dube@mccconstruction.co.za", UserRole.WORKER, AccountStatus.INVITED, "Never")
    )

    fun changeUserRole(userId: String, role: UserRole) {
        val index = managedUsers.indexOfFirst { it.id == userId }
        if (index != -1) managedUsers[index] = managedUsers[index].copy(role = role)
    }

    fun toggleUserSuspended(userId: String) {
        val index = managedUsers.indexOfFirst { it.id == userId }
        if (index == -1) return
        val user = managedUsers[index]
        managedUsers[index] = user.copy(
            status = if (user.status == AccountStatus.SUSPENDED) AccountStatus.ACTIVE
            else AccountStatus.SUSPENDED
        )
    }

    val auditLog = listOf(
        AuditEntry("A-01", "Riaan van Wyk", "Changed role", "Amara Dube → Worker", "Today, 08:41", AuditSeverity.INFO, "10.4.2.18"),
        AuditEntry("A-02", "System", "Failed sign-in ×5", "p.coetzee@mccconstruction.co.za", "Today, 07:12", AuditSeverity.CRITICAL, "197.88.140.6"),
        AuditEntry("A-03", "Naledi Khumalo", "Approved daily report", "R-02 — Riverside Estate", "Yesterday, 17:02", AuditSeverity.INFO, "10.4.2.31"),
        AuditEntry("A-04", "Riaan van Wyk", "Suspended account", "Pieter Coetzee", "22 Jul 2026, 09:15", AuditSeverity.WARNING, "10.4.2.18"),
        AuditEntry("A-05", "Johan Botha", "Exported employee data", "34 records — Riverside Estate", "21 Jul 2026, 14:48", AuditSeverity.WARNING, "10.4.2.77"),
        AuditEntry("A-06", "System", "Backup completed", "Nightly snapshot", "21 Jul 2026, 02:00", AuditSeverity.INFO, "—")
    )
}
