package com.projectworkflow.manager.domain.repository

import com.projectworkflow.manager.data.template.CostEngineeringTemplate
import com.projectworkflow.manager.data.template.StagePreset
import com.projectworkflow.manager.domain.model.BuildingSubTask
import com.projectworkflow.manager.domain.model.BusinessClosureStatus
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
        customStages: List<StagePreset>? = null,
        includeDefaultSubTasks: Boolean = true
    ): Long
    suspend fun updateProject(project: Project)
    suspend fun updateStageProgress(stageId: Long, progress: Int, projectId: Long)
    suspend fun updateStageDays(stageId: Long, projectId: Long, planDays: Int, elapsedDays: Int)
    suspend fun updateProjectDays(projectId: Long, totalPlanDays: Int, elapsedDays: Int)
    suspend fun addStageToProject(stage: Stage): Long
    suspend fun deleteProject(projectId: Long)

    // 阶段专属单体/专业拆解子项管理与整体推进
    suspend fun updateStageSubTaskProgress(stageId: Long, projectId: Long, taskId: String, progress: Int)
    suspend fun addStageSubTask(stageId: Long, projectId: Long, task: BuildingSubTask)
    suspend fun editStageSubTask(stageId: Long, projectId: Long, taskId: String, name: String, estimatedDays: Int)
    suspend fun deleteStageSubTask(stageId: Long, projectId: Long, taskId: String)
    suspend fun clearStageSubTasks(stageId: Long, projectId: Long)

    // 商务结算与成果归档追踪 (开票、到账、回访、成果确认单)
    suspend fun updateProjectBusinessClosure(projectId: Long, status: BusinessClosureStatus)

    // 关联会议完成状态与删除
    suspend fun toggleMeetingCompleted(meetingId: Long, completed: Boolean)
    suspend fun deleteMeeting(meetingId: Long)

    // 兼容历史老接口
    suspend fun updateBuildingTaskProgress(projectId: Long, taskId: String, progress: Int)
    suspend fun deleteBuildingTask(projectId: Long, taskId: String)
    suspend fun addBuildingTask(projectId: Long, task: BuildingSubTask)
}
