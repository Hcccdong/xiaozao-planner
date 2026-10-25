package com.projectworkflow.manager.data.repository

import com.projectworkflow.manager.data.local.dao.MeetingDao
import com.projectworkflow.manager.data.local.dao.ProjectDao
import com.projectworkflow.manager.data.local.dao.StageDao
import com.projectworkflow.manager.data.local.entity.MeetingEntity
import com.projectworkflow.manager.data.local.entity.ProjectEntity
import com.projectworkflow.manager.data.local.entity.StageEntity
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

        // 构造楼栋拆分数据 (若用户输入为空则不拆解；若拆解，默认以各单体30天左右起步，绝不机械强行除以5得出186天等怪异数字)
        val subTasksList = if (subTasks.isNotEmpty()) {
            subTasks.map { subName ->
                val estDays = 30
                "$subName|0|$estDays|$estDays|未开始"
            }.joinToString(";;")
        } else {
            ""
        }

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
            buildingTasksJson = subTasksList,
            startTimestamp = startDate.toEpochMillis(),
            targetEndTimestamp = targetEndDate.toEpochMillis(),
            progress = 0
        )
        val projectId = projectDao.insertProject(projectEntity)

        // 插入各专业阶段与对应会议
        stagesToUse.forEachIndexed { index, preset ->
            val stageEnd = currentStageStart.plusDays(preset.durationDays)
            val stageEntity = StageEntity(
                projectId = projectId,
                name = preset.name,
                sortOrder = index + 1,
                startTimestamp = currentStageStart.toEpochMillis(),
                endTimestamp = stageEnd.toEpochMillis(),
                progress = 0,
                status = if (index == 0) StageStatus.IN_PROGRESS.name else StageStatus.NOT_STARTED.name,
                deliverableNote = preset.deliverableNote
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
        val allStages = stageDao.getStagesByProjectId(projectId)
        if (allStages.isNotEmpty()) {
            val avgProgress = allStages.map { it.progress }.average().toInt()
            val projectStatus = if (avgProgress >= 100) ProjectStatus.COMPLETED.name else ProjectStatus.IN_PROGRESS.name
            projectDao.updateProjectProgress(projectId, avgProgress, projectStatus)
        }
    }

    override suspend fun addStageToProject(stage: Stage): Long {
        return stageDao.insertStage(stage.toEntity())
    }

    override suspend fun deleteProject(projectId: Long) {
        projectDao.deleteProjectById(projectId)
    }

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

    override suspend fun addBuildingTask(projectId: Long, task: com.projectworkflow.manager.domain.model.BuildingSubTask) {
        val entity = projectDao.getProjectById(projectId) ?: return
        val currentTasks = DataMappers.parseBuildingTasks(entity.buildingTasksJson)
        val updated = currentTasks + task
        val updatedTasksJson = DataMappers.serializeBuildingTasks(updated)
        projectDao.updateProject(entity.copy(buildingTasksJson = updatedTasksJson))
    }
}
