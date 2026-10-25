package com.projectworkflow.manager.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object ProjectList : Screen("project_list")
    object ProjectDetail : Screen("project_detail/{projectId}") {
        fun createRoute(projectId: Long) = "project_detail/$projectId"
    }
    object MeetingSchedule : Screen("meeting_schedule")
}
