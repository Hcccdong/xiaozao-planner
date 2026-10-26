package com.projectworkflow.manager.data.repository

import com.projectworkflow.manager.data.local.dao.MeetingDao
import com.projectworkflow.manager.data.local.dao.ProjectDao
import com.projectworkflow.manager.data.local.dao.StageDao
import com.projectworkflow.manager.data.local.entity.MeetingEntity
import com.projectworkflow.manager.data.local.entity.ProjectEntity
import com.projectworkflow.manager.data.local.entity.StageEntity
import com.projectworkflow.manager.data.local.mapper.DataMappers
import com.projectworkflow.manager.data.local.mapper.DataMappers.toDomain
import com.projectworkflow.manager.data.local.mapper.DataMappers.toEpochMillis
import com.projectworkflow.manager.data.local.mapper.DataMappers.toEntity
import com.projectworkflow.manager.data.template.CostEngineeringTemplate
import com.projectworkflow.manager.data.template.StagePreset
import com.projectworkflow.manager.domain.model.BuildingSubTask
import com.projectworkflow.manager.domain.model.Project
import com.projectworkflow.manager.domain.model.ProjectStatus
import com.projectworkflow.manager.domain.model.Stage
import com.projectworkflow.manager.domain.model.StageStatus
import com.projectworkflow.manager.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class ProjectRepositoryImpl(
    private val projectDao: ProjectDao,
    private val stageDao: StageDao,
    private val meetingDao: MeetingDao
) : ProjectRepository {

    override fun getAllProjectsFlow(): Flow<List<Project>> {
        return projectDao.getAllProjectsWithDetailsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getProjectByIdFlow(projectId: Long): Flow<Project?> {
        return projectDao.getProjectWithDetailsByIdFlow(projectId).map {
            it?.toDomain()
        }
    }

    override suspend fun createProject(project: Project): Long {
        return projectDao.insertProject(project.toEntity())
    }

    override suspend fun createCostProjectFromTemplate(
        name: String,
        description: String?,
        priority: String,
        planDays: Int,
        template: CostEngineeringTemplate,
        subTasks: List<String>,
        startDate: LocalDateTime,
        customStages: List<StagePreset>?
    ): Long {
        var currentStageStart = startDate
        val targetEndDate = startDate.plusDays(planDays.toLong())

        val stagesToUse = if (!customStages.isNullOrEmpty()) customStages else template.stages

        val projectEntity = ProjectEntity(
            name = name,
            projectType = template.projectType.name,
            code = null,
            description = description,
            status = ProjectStatus.IN_PROGRESS.name,
            priority = priority,
            totalPlanDays = planDays,
            elapsedDays = 0,
            currentFocusAction = "立项完成，开始进行${stagesToUse.firstOrNull()?.name ?: "首阶段工作"}",
            documentStatus = "COMPLETE",
            buildingTasksJson = "",
            startTimestamp = startDate.toEpochMillis(),
            targetEndTimestamp = targetEndDate.toEpochMillis(),
            progress = 0
        )
        val projectId = projectDao.insertProject(projectEntity)

        // 插入各专业阶段与对应会议，并为各阶段初始化专属单体/专业子项拆解
        stagesToUse.forEachIndexed { index, preset ->
            val stageEnd = currentStageStart.plusDays(preset.durationDays)

            // 为该阶段初始化单体或专业分项
            val stageSubTasksJson = if (preset.defaultSubTasks.isNotEmpty()) {
                preset.defaultSubTasks.map { taskName ->
                    "$taskName|0|5|5|未开始"
                }.joinToString(";;")
            } else {
                ""
            }

            val stageEntity = StageEntity(
                projectId = projectId,
                name = preset.name,
                sortOrder = index + 1,
                startTimestamp = currentStageStart.toEpochMillis(),
                endTimestamp = stageEnd.toEpochMillis(),
                progress = 0,
                status = if (index == 0) StageStatus.IN_PROGRESS.name else StageStatus.NOT_STARTED.name,
                deliverableNote = preset.deliverableNote,
                subTasksJson = stageSubTasksJson
            )
            val stageId = stageDao.insertStage(stageEntity)

            // 若有预置会议，自动排会
            preset.defaultMeeting?.let { meetingPreset ->
                val meetingTime = currentStageStart.plusDays(meetingPreset.offsetDaysFromStageStart)
                    .withHour(14).withMinute(30).withSecond(0)
                val meetingEntity = MeetingEntity(
                    projectId = projectId,
                    stageId = stageId,
                    title = meetingPreset.title,
                    meetingType = meetingPreset.type.name,
                    meetingTimestamp = meetingTime.toEpochMillis(),
                    durationMinutes = meetingPreset.durationMinutes,
                    agenda = meetingPreset.agenda,
                    reminderMinutesBefore = 15
                )
                meetingDao.insertMeeting(meetingEntity)
            }

            currentStageStart = stageEnd
        }

        return projectId
    }

    override suspend fun updateProject(project: Project) {
        projectDao.updateProject(project.toEntity())
    }

    override suspend fun updateStageProgress(stageId: Long, progress: Int, projectId: Long) {
        val stageStatus = when {
            progress >= 100 -> StageStatus.COMPLETED.name
            progress > 0 -> StageStatus.IN_PROGRESS.name
            else -> StageStatus.NOT_STARTED.name
        }
        stageDao.updateStageProgress(stageId, progress.coerceIn(0, 100), stageStatus)

        // 阶段进度联动：自动重新计算全项目综合平均进度
        syncProjectAverageProgress(projectId)
    }

    override suspend fun updateStageDays(stageId: Long, projectId: Long, planDays: Int, elapsedDays: Int) {
        val stage = stageDao.getStageById(stageId) ?: return
        val newEndTimestamp = stage.startTimestamp + planDays.toLong().coerceAtLeast(1L) * 86400000L
        stageDao.updateStageDays(stageId, newEndTimestamp, elapsedDays.coerceAtLeast(0))

        // 联动更新全项目总工期与实际已用天数（完美支撑补流程场景）
        syncProjectDaysFromStages(projectId)
    }

    override suspend fun updateProjectDays(projectId: Long, totalPlanDays: Int, elapsedDays: Int) {
        val projectEntity = projectDao.getProjectById(projectId) ?: return
        val updated = projectEntity.copy(
            totalPlanDays = totalPlanDays.coerceAtLeast(1),
            elapsedDays = elapsedDays.coerceAtLeast(0)
        )
        projectDao.updateProject(updated)
    }

    private suspend fun syncProjectDaysFromStages(projectId: Long) {
        val project = projectDao.getProjectById(projectId) ?: return
        val stages = stageDao.getStagesByProjectId(projectId)
        if (stages.isNotEmpty()) {
            val totalPlan = stages.sumOf { ((it.endTimestamp - it.startTimestamp) / 86400000L).toInt().coerceAtLeast(1) }
            val totalElapsed = stages.sumOf { it.elapsedDays }
            val updated = project.copy(
                totalPlanDays = if (totalPlan > 0) totalPlan else project.totalPlanDays,
                elapsedDays = totalElapsed
            )
            projectDao.updateProject(updated)
        }
    }

    override suspend fun addStageToProject(stage: Stage): Long {
        return stageDao.insertStage(stage.toEntity())
    }

    override suspend fun deleteProject(projectId: Long) {
        projectDao.deleteProjectById(projectId)
    }

    // ========== 阶段专属单体/专业子项拆解管理 ==========

    override suspend fun updateStageSubTaskProgress(
        stageId: Long,
        projectId: Long,
        taskId: String,
        progress: Int
    ) {
        val stage = stageDao.getStageById(stageId) ?: return
        val currentTasks = DataMappers.parseBuildingTasks(stage.subTasksJson)
        val p = progress.coerceIn(0, 100)
        val updatedTasks = currentTasks.map { task ->
            if (task.id == taskId) {
                val status = if (p >= 100) "已完成" else if (p > 0) "编制中" else "未开始"
                task.copy(
                    progress = p,
                    statusText = status,
                    isCompleted = p >= 100
                )
            } else task
        }

        // 联动计算本阶段平均进度
        val stageAvgProgress = if (updatedTasks.isNotEmpty()) {
            updatedTasks.map { it.progress }.average().toInt()
        } else {
            stage.progress
        }
        val stageStatus = when {
            stageAvgProgress >= 100 -> StageStatus.COMPLETED.name
            stageAvgProgress > 0 -> StageStatus.IN_PROGRESS.name
            else -> StageStatus.NOT_STARTED.name
        }
        val updatedJson = DataMappers.serializeBuildingTasks(updatedTasks)
        stageDao.updateStageProgressAndSubTasks(stageId, stageAvgProgress, stageStatus, updatedJson)

        // 联动计算项目整体进度
        syncProjectAverageProgress(projectId)
    }

    override suspend fun addStageSubTask(
        stageId: Long,
        projectId: Long,
        task: BuildingSubTask
    ) {
        val stage = stageDao.getStageById(stageId) ?: return
        val currentTasks = DataMappers.parseBuildingTasks(stage.subTasksJson)
        val updatedTasks = currentTasks + task

        val stageAvgProgress = if (updatedTasks.isNotEmpty()) {
            updatedTasks.map { it.progress }.average().toInt()
        } else {
            stage.progress
        }
        val stageStatus = when {
            stageAvgProgress >= 100 -> StageStatus.COMPLETED.name
            stageAvgProgress > 0 -> StageStatus.IN_PROGRESS.name
            else -> StageStatus.NOT_STARTED.name
        }
        val updatedJson = DataMappers.serializeBuildingTasks(updatedTasks)
        stageDao.updateStageProgressAndSubTasks(stageId, stageAvgProgress, stageStatus, updatedJson)

        syncProjectAverageProgress(projectId)
    }

    override suspend fun deleteStageSubTask(
        stageId: Long,
        projectId: Long,
        taskId: String
    ) {
        val stage = stageDao.getStageById(stageId) ?: return
        val currentTasks = DataMappers.parseBuildingTasks(stage.subTasksJson)
        val updatedTasks = currentTasks.filterNot { it.id == taskId }

        val stageAvgProgress = if (updatedTasks.isNotEmpty()) {
            updatedTasks.map { it.progress }.average().toInt()
        } else {
            stage.progress
        }
        val stageStatus = when {
            stageAvgProgress >= 100 -> StageStatus.COMPLETED.name
            stageAvgProgress > 0 -> StageStatus.IN_PROGRESS.name
            else -> StageStatus.NOT_STARTED.name
        }
        val updatedJson = DataMappers.serializeBuildingTasks(updatedTasks)
        stageDao.updateStageProgressAndSubTasks(stageId, stageAvgProgress, stageStatus, updatedJson)

        syncProjectAverageProgress(projectId)
    }

    private suspend fun syncProjectAverageProgress(projectId: Long) {
        val allStages = stageDao.getStagesByProjectId(projectId)
        if (allStages.isNotEmpty()) {
            val avgProgress = allStages.map { it.progress }.average().toInt()
            val projectStatus = if (avgProgress >= 100) ProjectStatus.COMPLETED.name else ProjectStatus.IN_PROGRESS.name
            projectDao.updateProjectProgress(projectId, avgProgress, projectStatus)
        }
    }

    // ========== 兼容历史老接口 ==========

    override suspend fun updateBuildingTaskProgress(projectId: Long, taskId: String, progress: Int) {
        val entity = projectDao.getProjectById(projectId) ?: return
        val currentTasks = DataMappers.parseBuildingTasks(entity.buildingTasksJson)
        val updated = currentTasks.map { task ->
            if (task.id == taskId) {
                val p = progress.coerceIn(0, 100)
                val status = if (p >= 100) "已完成" else if (p > 0) "编制中" else "未开始"
                task.copy(
                    progress = p,
                    statusText = status,
                    isCompleted = p >= 100
                )
            } else task
        }
        val updatedTasksJson = DataMappers.serializeBuildingTasks(updated)
        projectDao.updateProject(entity.copy(buildingTasksJson = updatedTasksJson))
    }

    override suspend fun deleteBuildingTask(projectId: Long, taskId: String) {
        val entity = projectDao.getProjectById(projectId) ?: return
        val currentTasks = DataMappers.parseBuildingTasks(entity.buildingTasksJson)
        val updated = currentTasks.filterNot { it.id == taskId }
        val updatedTasksJson = DataMappers.serializeBuildingTasks(updated)
        projectDao.updateProject(entity.copy(buildingTasksJson = updatedTasksJson))
    }

    override suspend fun addBuildingTask(projectId: Long, task: BuildingSubTask) {
        val entity = projectDao.getProjectById(projectId) ?: return
        val currentTasks = DataMappers.parseBuildingTasks(entity.buildingTasksJson)
        val updated = currentTasks + task
        val updatedTasksJson = DataMappers.serializeBuildingTasks(updated)
        projectDao.updateProject(entity.copy(buildingTasksJson = updatedTasksJson))
    }
}
