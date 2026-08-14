package com.mcc.construction.navigation

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

    // Worker
    const val WORKER_DASHBOARD = "worker_dashboard"
    const val TASKS = "tasks"
    const val ATTENDANCE = "attendance"
    const val DAILY_REPORT = "daily_report"
    const val SAFETY_CHECKLIST = "safety_checklist"
    const val INCIDENT_REPORT = "incident_report"
    const val MESSAGES = "messages"

    // Manager
    const val MANAGER_DASHBOARD = "manager_dashboard"
    const val PROJECTS = "projects"
    const val EMPLOYEES = "employees"
    const val INVENTORY = "inventory"
    const val REPORTS = "reports"

    // Administrator
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val USER_MANAGEMENT = "user_management"
    const val AUDIT_LOG = "audit_log"

    // Parameterised detail routes
    const val TASK_DETAIL_PATTERN = "task_detail/{taskId}"
    const val PROJECT_DETAIL_PATTERN = "project_detail/{projectId}"
    const val EMPLOYEE_PROFILE_PATTERN = "employee_profile/{employeeId}"
    const val MATERIAL_DETAIL_PATTERN = "material_detail/{materialId}"
    const val CHAT_PATTERN = "chat/{conversationId}"

    const val ARG_TASK_ID = "taskId"
    const val ARG_PROJECT_ID = "projectId"
    const val ARG_EMPLOYEE_ID = "employeeId"
    const val ARG_MATERIAL_ID = "materialId"
    const val ARG_CONVERSATION_ID = "conversationId"

    fun taskDetail(id: String) = "task_detail/$id"
    fun projectDetail(id: String) = "project_detail/$id"
    fun employeeProfile(id: String) = "employee_profile/$id"
    fun materialDetail(id: String) = "material_detail/$id"
    fun chat(id: String) = "chat/$id"
}
