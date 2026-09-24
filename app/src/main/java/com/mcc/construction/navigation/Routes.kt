package com.mcc.construction.navigation

/**
 * Every route in the app. One constant per screen in the screen list, plus the
 * administrator screens.
 *
 * To add a screen: add its route here, add a `composable(...)` entry in
 * [AppNavHost] (a [com.mcc.construction.ui.components.PlaceholderScreen] is
 * fine to start with), and — if it belongs in the navigation bar or rail — an
 * item in [NavMenus].
 */
object Routes {
    // Entry
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val FORGOT_PASSWORD = "forgot_password"

    // Shared
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val SYNC_STATUS = "sync_status"
    const val DOCUMENTS = "documents"
    const val MESSAGES = "messages"

    // Worker
    const val WORKER_DASHBOARD = "worker_dashboard"
    const val TASKS = "tasks"
    const val ATTENDANCE = "attendance"
    const val DAILY_REPORT = "daily_report"
    const val SAFETY_CHECKLIST = "safety_checklist"
    const val INCIDENT_REPORT = "incident_report"

    // Manager
    const val MANAGER_DASHBOARD = "manager_dashboard"
    const val PROJECTS = "projects"
    const val EMPLOYEES = "employees"
    const val INVENTORY = "inventory"

    /** Reports history: submitted daily reports and incidents. */
    const val REPORTS = "reports"

    // Administrator
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val USER_MANAGEMENT = "user_management"
    const val AUDIT_LOG = "audit_log"
    const val SYSTEM_SETTINGS = "system_settings"

    // Parameterised detail routes
    const val TASK_DETAIL_PATTERN = "task_detail/{taskId}"
    const val PROJECT_DETAIL_PATTERN = "project_detail/{projectId}"
    const val EMPLOYEE_PROFILE_PATTERN = "employee_profile/{employeeId}"
    const val MATERIAL_DETAIL_PATTERN = "material_detail/{materialId}"
    const val CHAT_PATTERN = "chat/{conversationId}"

    /** [INCIDENT_REPORT] with an optional link to the checklist that found the problem. */
    const val INCIDENT_REPORT_PATTERN = "incident_report?inspectionId={inspectionId}"

    const val ARG_TASK_ID = "taskId"
    const val ARG_PROJECT_ID = "projectId"
    const val ARG_EMPLOYEE_ID = "employeeId"
    const val ARG_MATERIAL_ID = "materialId"
    const val ARG_CONVERSATION_ID = "conversationId"
    const val ARG_INSPECTION_ID = "inspectionId"

    fun taskDetail(id: String) = "task_detail/$id"
    fun projectDetail(id: String) = "project_detail/$id"
    fun employeeProfile(id: String) = "employee_profile/$id"
    fun materialDetail(id: String) = "material_detail/$id"
    fun chat(id: String) = "chat/$id"
    fun incidentReport(inspectionId: Int) = "incident_report?inspectionId=$inspectionId"

    /** Routes that can be shown without a signed-in user. */
    val public: Set<String> = setOf(SPLASH, LOGIN, FORGOT_PASSWORD)
}
