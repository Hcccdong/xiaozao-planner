package com.projectworkflow.manager.ui.dashboard

import com.projectworkflow.manager.domain.model.DashboardMetrics
import com.projectworkflow.manager.domain.model.Meeting
import com.projectworkflow.manager.domain.model.Project
import com.projectworkflow.manager.domain.model.TagType
import com.projectworkflow.manager.domain.model.TodayFocusItem

enum class DashboardTab(val title: String) {
    TODAY("今日"),
    PROJECTS("项目"),
    NODES("节点")
}

/**
 * 节点视角下的单条阶段节点或对账会议
 */
data class DashboardNodeItem(
    val id: String,
    val projectId: Long,
    val projectName: String,
    val title: String,
    val detail: String,
    val timeText: String,
    val statusText: String,
    val tagType: TagType,
    val isMeeting: Boolean
)

data class DashboardUiState(
    val selectedTab: DashboardTab = DashboardTab.TODAY,
    val dateText: String = "",
    val metrics: DashboardMetrics = DashboardMetrics(),
    val focusItems: List<TodayFocusItem> = emptyList(),
    val activeProjects: List<Project> = emptyList(),
    val allProjects: List<Project> = emptyList(),
    val allMeetings: List<Meeting> = emptyList(),
    val milestoneNodes: List<DashboardNodeItem> = emptyList(),
    val isLoading: Boolean = false
)
