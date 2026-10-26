package com.projectworkflow.manager.ui.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.projectworkflow.manager.data.template.CostEngineeringTemplate
import com.projectworkflow.manager.data.template.StagePreset
import com.projectworkflow.manager.domain.model.Project
import com.projectworkflow.manager.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class ProjectDetailUiState(
    val project: Project? = null,
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false
)

class ProjectViewModel(
    private val projectRepository: ProjectRepository
) : ViewModel() {

    val allProjects: StateFlow<List<Project>> = projectRepository.getAllProjectsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getProjectDetail(projectId: Long): Flow<ProjectDetailUiState> {
        return projectRepository.getProjectByIdFlow(projectId).map { project ->
            ProjectDetailUiState(project = project, isLoading = false)
        }
    }

    fun createCostProjectFromTemplate(
        name: String,
        description: String?,
        priority: String,
        planDays: Int,
        template: CostEngineeringTemplate,
        subTasks: List<String>,
        customStages: List<StagePreset>? = null,
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val projectId = projectRepository.createCostProjectFromTemplate(
                name = name,
                description = description,
                priority = priority,
                planDays = planDays,
                template = template,
                subTasks = subTasks,
                startDate = LocalDateTime.now(),
                customStages = customStages
            )
            onSuccess(projectId)
        }
    }

    fun updateStageProgress(stageId: Long, newProgress: Int, projectId: Long) {
        viewModelScope.launch {
            projectRepository.updateStageProgress(stageId, newProgress, projectId)
        }
    }

    // ========== 阶段内单体/专业拆解管理 ==========

    fun updateStageSubTaskProgress(stageId: Long, projectId: Long, taskId: String, progress: Int) {
        viewModelScope.launch {
            projectRepository.updateStageSubTaskProgress(stageId, projectId, taskId, progress)
        }
    }

    fun addStageSubTask(stageId: Long, projectId: Long, name: String, estDays: Int = 5) {
        viewModelScope.launch {
            val newTask = com.projectworkflow.manager.domain.model.BuildingSubTask(
                id = "task_${System.currentTimeMillis()}",
                name = name,
                progress = 0,
                estimatedDays = estDays.coerceAtLeast(1),
                remainingDays = estDays.coerceAtLeast(1),
                statusText = "未开始",
                isCompleted = false
            )
            projectRepository.addStageSubTask(stageId, projectId, newTask)
        }
    }

    fun deleteStageSubTask(stageId: Long, projectId: Long, taskId: String) {
        viewModelScope.launch {
            projectRepository.deleteStageSubTask(stageId, projectId, taskId)
        }
    }

    fun updateBuildingTaskProgress(projectId: Long, taskId: String, progress: Int) {
        viewModelScope.launch {
            projectRepository.updateBuildingTaskProgress(projectId, taskId, progress)
        }
    }

    fun deleteBuildingTask(projectId: Long, taskId: String) {
        viewModelScope.launch {
            projectRepository.deleteBuildingTask(projectId, taskId)
        }
    }

    fun addBuildingTask(projectId: Long, name: String, estDays: Int) {
        viewModelScope.launch {
            val newTask = com.projectworkflow.manager.domain.model.BuildingSubTask(
                id = "task_${System.currentTimeMillis()}",
                name = name,
                progress = 0,
                estimatedDays = estDays.coerceAtLeast(1),
                remainingDays = estDays.coerceAtLeast(1),
                statusText = "未开始",
                isCompleted = false
            )
            projectRepository.addBuildingTask(projectId, newTask)
        }
    }

    fun deleteProject(projectId: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            projectRepository.deleteProject(projectId)
            onDone()
        }
    }

    class Factory(
        private val projectRepository: ProjectRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProjectViewModel(projectRepository) as T
        }
    }
}
