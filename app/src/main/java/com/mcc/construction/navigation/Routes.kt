package com.mcc.construction.navigation

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val HOME = "home"
    const val TASKS = "tasks"
    const val PROJECTS = "projects"
    const val MESSAGES = "messages"
    const val PROFILE = "profile"

    private const val TASK_ID_ARG = "taskId"
    const val TASK_DETAIL_PATTERN = "task_detail/{$TASK_ID_ARG}"
    fun taskDetail(taskId: String) = "task_detail/$taskId"

    /** Destinations that show the adaptive bottom bar / rail chrome. */
    val mainRoutes = setOf(HOME, TASKS, PROJECTS, MESSAGES, PROFILE)
}
