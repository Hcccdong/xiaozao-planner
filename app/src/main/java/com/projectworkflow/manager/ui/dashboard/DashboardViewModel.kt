package com.projectworkflow.manager.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.projectworkflow.manager.domain.repository.MeetingRepository
import com.projectworkflow.manager.domain.repository.ProjectRepository
import com.projectworkflow.manager.domain.usecase.GetDashboardOverviewUseCase
import kotlinx.coroutines.flow.*

class DashboardViewModel(
    private val projectRepository: ProjectRepository,
    private val meetingRepository: MeetingRepository
) : ViewModel() {

    private val overviewUseCase = GetDashboardOverviewUseCase(projectRepository, meetingRepository)

    private val _selectedTab = MutableStateFlow(DashboardTab.TODAY)

    val uiState: StateFlow<DashboardUiState> = combine(
        _selectedTab,
        overviewUseCase(),
        projectRepository.getAllProjectsFlow()
    ) { tab, overview, projects ->
        DashboardUiState(
            selectedTab = tab,
            dateText = overview.dateText,
            metrics = overview.metrics,
            focusItems = overview.focusItems,
            activeProjects = projects.filter { it.status.name == "IN_PROGRESS" || it.status.name == "DELAYED" },
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
