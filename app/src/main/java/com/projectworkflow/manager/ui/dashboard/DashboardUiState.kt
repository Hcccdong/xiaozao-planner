package com.projectworkflow.manager.ui.dashboard

import com.projectworkflow.manager.domain.model.DashboardMetrics
import com.projectworkflow.manager.domain.model.Project
import com.projectworkflow.manager.domain.model.TodayFocusItem

enum class DashboardTab(val title: String) {
    TODAY("今日"),
    PROJECTS("项目"),
    NODES("节点")
}

data class DashboardUiState(
    val selectedTab: DashboardTab = DashboardTab.TODAY,
    val dateText: String = "",
    val metrics: DashboardMetrics = DashboardMetrics(),
    val focusItems: List<TodayFocusItem> = emptyList(),
    val activeProjects: List<Project> = emptyList(),
    val isLoading: Boolean = false
)
