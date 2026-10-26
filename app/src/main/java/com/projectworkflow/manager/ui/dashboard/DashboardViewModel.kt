package com.projectworkflow.manager.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.projectworkflow.manager.domain.model.TagType
import com.projectworkflow.manager.domain.repository.MeetingRepository
import com.projectworkflow.manager.domain.repository.ProjectRepository
import com.projectworkflow.manager.domain.usecase.GetDashboardOverviewUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DashboardViewModel(
    private val projectRepository: ProjectRepository,
    private val meetingRepository: MeetingRepository
) : ViewModel() {

    private val overviewUseCase = GetDashboardOverviewUseCase(projectRepository, meetingRepository)

    private val _selectedTab = MutableStateFlow(DashboardTab.TODAY)
    private val _selectedNodeFilter = MutableStateFlow(NodeFilter.ALL)

    val uiState: StateFlow<DashboardUiState> = combine(
        _selectedTab,
        _selectedNodeFilter,
        overviewUseCase(),
        projectRepository.getAllProjectsFlow(),
        meetingRepository.getAllMeetingsFlow()
    ) { tab, nodeFilter, overview, projects, meetings ->
        val today = LocalDate.now()
        val timeFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")

        // 提取各工程中的阶段交付节点
        val stageNodes = projects.flatMap { p ->
            p.stages.filter { it.progress < 100 }.map { s ->
                val isCurrent = s.status.name == "IN_PROGRESS"
                val days = s.planDays
                DashboardNodeItem(
                    id = "stage_${s.id}",
                    projectId = p.id,
                    projectName = p.name,
                    title = "交付节点: ${s.name}",
                    detail = s.deliverableNote?.ifBlank { null } ?: "阶段编制推进中 (${s.progress}%)",
                    timeText = "工期约 $days 天",
                    statusText = if (isCurrent) "进行中" else "待启动",
                    tagType = if (isCurrent) TagType.WARNING_AMBER else TagType.NEUTRAL_GRAY,
                    isMeeting = false
                )
            }
        }

        // 提取排期的对账会议
        val meetingNodes = meetings.map { m ->
            val mDate = m.meetingTime.toLocalDate()
            val isToday = mDate == today
            val isTomorrow = mDate == today.plusDays(1)

            val statusText = when {
                m.isCompleted -> "已开完"
                isToday -> "今日对账"
                isTomorrow -> "明日开会"
                else -> m.meetingType.label
            }

            val tagType = when {
                m.isCompleted -> TagType.SUCCESS_GREEN
                isToday -> TagType.URGENT_RED
                isTomorrow -> TagType.WARNING_AMBER
                else -> TagType.NEUTRAL_GRAY
            }

            DashboardNodeItem(
                id = "meeting_${m.id}",
                projectId = m.projectId ?: 0L,
                projectName = m.projectName ?: "工程对账",
                title = "对账会议: ${m.title}",
                detail = m.locationOrLink?.ifBlank { null } ?: m.agenda ?: "造价核对与方案沟通",
                timeText = m.meetingTime.format(timeFormatter),
                statusText = statusText,
                tagType = tagType,
                isMeeting = true
            )
        }

        // 构建按项目分级归类的结构（彻底解决“项目和项目混在一起太杂乱”问题）
        val projectGroups = projects.mapNotNull { p ->
            val pMeetings = meetings.filter { it.projectId == p.id }
            val pStages = p.stages.filter { it.progress < 100 }.map { s ->
                val isCurrent = s.status.name == "IN_PROGRESS"
                val days = s.planDays
                DashboardNodeItem(
                    id = "stage_${s.id}",
                    projectId = p.id,
                    projectName = p.name,
                    title = "交付节点: ${s.name}",
                    detail = s.deliverableNote?.ifBlank { null } ?: "阶段编制推进中 (${s.progress}%)",
                    timeText = "工期约 $days 天",
                    statusText = if (isCurrent) "进行中" else "待启动",
                    tagType = if (isCurrent) TagType.WARNING_AMBER else TagType.NEUTRAL_GRAY,
                    isMeeting = false
                )
            }

            val filteredMeetings = when (nodeFilter) {
                NodeFilter.STAGES -> emptyList()
                else -> pMeetings
            }
            val filteredStages = when (nodeFilter) {
                NodeFilter.MEETINGS -> emptyList()
                else -> pStages
            }

            if (filteredMeetings.isNotEmpty() || filteredStages.isNotEmpty()) {
                ProjectNodeGroup(
                    project = p,
                    meetings = filteredMeetings,
                    stageNodes = filteredStages
                )
            } else {
                null
            }
        }

        // 扁平列表保留作为兼容
        val combinedNodes = (meetingNodes + stageNodes).take(30)

        DashboardUiState(
            selectedTab = tab,
            selectedNodeFilter = nodeFilter,
            dateText = overview.dateText,
            metrics = overview.metrics,
            focusItems = overview.focusItems,
            allProjects = projects,
            activeProjects = projects.filter { it.status.name == "IN_PROGRESS" || it.status.name == "DELAYED" },
            allMeetings = meetings,
            projectNodeGroups = projectGroups,
            milestoneNodes = combinedNodes,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun onSelectTab(tab: DashboardTab) {
        _selectedTab.value = tab
    }

    fun onSelectNodeFilter(filter: NodeFilter) {
        _selectedNodeFilter.value = filter
    }

    fun toggleMeetingCompleted(meetingId: Long, completed: Boolean) {
        viewModelScope.launch {
            meetingRepository.markMeetingCompleted(meetingId, completed)
        }
    }

    class Factory(
        private val projectRepository: ProjectRepository,
        private val meetingRepository: MeetingRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(projectRepository, meetingRepository) as T
        }
    }
}
