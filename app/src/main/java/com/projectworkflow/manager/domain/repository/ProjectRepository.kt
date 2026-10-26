package com.projectworkflow.manager.domain.repository

import com.projectworkflow.manager.data.template.CostEngineeringTemplate
import com.projectworkflow.manager.data.template.StagePreset
import com.projectworkflow.manager.domain.model.BuildingSubTask
import com.projectworkflow.manager.domain.model.Project
import com.projectworkflow.manager.domain.model.Stage
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface ProjectRepository {
    fun getAllProjectsFlow(): Flow<List<Project>>
    fun getProjectByIdFlow(projectId: Long): Flow<Project?>
    suspend fun createProject(project: Project): Long
    suspend fun createCostProjectFromTemplate(
        name: String,
        description: String?,
        priority: String,
        planDays: Int,
        template: CostEngineeringTemplate,
        subTasks: List<String> = emptyList(),
        startDate: LocalDateTime,
        customStages: List<StagePreset>? = null
    ): Long
    suspend fun updateProject(project: Project)
    suspend fun updateStageProgress(stageId: Long, progress: Int, projectId: Long)
    suspend fun addStageToProject(stage: Stage): Long
    suspend fun deleteProject(projectId: Long)

    // 阶段专属单体/专业拆解子项管理
    suspend fun updateStageSubTaskProgress(stageId: Long, projectId: Long, taskId: String, progress: Int)
    suspend fun addStageSubTask(stageId: Long, projectId: Long, task: BuildingSubTask)
    suspend fun deleteStageSubTask(stageId: Long, projectId: Long, taskId: String)

    // 兼容历史老接口
    suspend fun updateBuildingTaskProgress(projectId: Long, taskId: String, progress: Int)
    suspend fun deleteBuildingTask(projectId: Long, taskId: String)
    suspend fun addBuildingTask(projectId: Long, task: BuildingSubTask)
}
