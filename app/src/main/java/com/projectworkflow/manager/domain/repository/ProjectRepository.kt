package com.projectworkflow.manager.domain.repository

import com.projectworkflow.manager.data.template.CostEngineeringTemplate
import com.projectworkflow.manager.data.template.StagePreset
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
        subTasks: List<String>,
        startDate: LocalDateTime,
        customStages: List<StagePreset>? = null
    ): Long
    suspend fun updateProject(project: Project)
    suspend fun updateStageProgress(stageId: Long, progress: Int, projectId: Long)
    suspend fun addStageToProject(stage: Stage): Long
    suspend fun deleteProject(projectId: Long)
}
