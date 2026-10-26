package com.projectworkflow.manager.data.local.mapper

import com.projectworkflow.manager.data.local.entity.MeetingEntity
import com.projectworkflow.manager.data.local.entity.ProjectEntity
import com.projectworkflow.manager.data.local.entity.StageEntity
import com.projectworkflow.manager.data.local.relation.ProjectWithDetails
import com.projectworkflow.manager.domain.model.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object DataMappers {

    private val zoneId = ZoneId.systemDefault()

    fun Long.toLocalDateTime(): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(this), zoneId)

    fun LocalDateTime.toEpochMillis(): Long =
        this.atZone(zoneId).toInstant().toEpochMilli()

    fun ProjectEntity.toDomain(stages: List<Stage> = emptyList(), meetings: List<Meeting> = emptyList()): Project {
        val parsedTasks = parseBuildingTasks(buildingTasksJson)
        val remaining = (totalPlanDays - elapsedDays).coerceAtLeast(0)
        return Project(
            id = id,
            name = name,
            projectType = runCatching { CostProjectType.valueOf(projectType) }.getOrDefault(CostProjectType.BUDGET),
            code = code,
            description = description,
            status = runCatching { ProjectStatus.valueOf(status) }.getOrDefault(ProjectStatus.IN_PROGRESS),
            priority = runCatching { Priority.valueOf(priority) }.getOrDefault(Priority.MEDIUM),
            totalPlanDays = totalPlanDays,
            elapsedDays = elapsedDays,
            remainingDays = remaining,
            currentFocusAction = currentFocusAction,
            documentStatus = runCatching { DocumentStatus.valueOf(documentStatus) }.getOrDefault(DocumentStatus.COMPLETE),
            buildingTasks = parsedTasks,
            startDate = startTimestamp.toLocalDateTime(),
            targetEndDate = targetEndTimestamp.toLocalDateTime(),
            progress = progress,
            stages = stages,
            meetings = meetings
        )
    }

    fun Project.toEntity(): ProjectEntity =
        ProjectEntity(
            id = id,
            name = name,
            projectType = projectType.name,
            code = code,
            description = description,
            status = status.name,
            priority = priority.name,
            totalPlanDays = totalPlanDays,
            elapsedDays = elapsedDays,
            currentFocusAction = currentFocusAction,
            documentStatus = documentStatus.name,
            buildingTasksJson = serializeBuildingTasks(buildingTasks),
            startTimestamp = startDate.toEpochMillis(),
            targetEndTimestamp = targetEndDate.toEpochMillis(),
            progress = progress
        )

    fun StageEntity.toDomain(meetings: List<Meeting> = emptyList()): Stage =
        Stage(
            id = id,
            projectId = projectId,
            name = name,
            sortOrder = sortOrder,
            startDate = startTimestamp.toLocalDateTime(),
            endDate = endTimestamp.toLocalDateTime(),
            progress = progress,
            status = runCatching { StageStatus.valueOf(status) }.getOrDefault(StageStatus.NOT_STARTED),
            deliverableNote = deliverableNote,
            meetings = meetings,
            subTasks = parseBuildingTasks(subTasksJson)
        )

    fun Stage.toEntity(): StageEntity =
        StageEntity(
            id = id,
            projectId = projectId,
            name = name,
            sortOrder = sortOrder,
            startTimestamp = startDate.toEpochMillis(),
            endTimestamp = endDate.toEpochMillis(),
            progress = progress,
            status = status.name,
            deliverableNote = deliverableNote,
            subTasksJson = serializeBuildingTasks(subTasks)
        )

    fun MeetingEntity.toDomain(projectName: String? = null, stageName: String? = null): Meeting =
        Meeting(
            id = id,
            projectId = projectId,
            projectName = projectName,
            stageId = stageId,
            stageName = stageName,
            title = title,
            meetingType = runCatching { MeetingType.valueOf(meetingType) }.getOrDefault(MeetingType.REVIEW),
            meetingTime = meetingTimestamp.toLocalDateTime(),
            durationMinutes = durationMinutes,
            locationOrLink = locationOrLink,
            agenda = agenda,
            minutesSummary = minutesSummary,
            reminderMinutesBefore = reminderMinutesBefore,
            isCompleted = isCompleted
        )

    fun Meeting.toEntity(): MeetingEntity =
        MeetingEntity(
            id = id,
            projectId = projectId,
            stageId = stageId,
            title = title,
            meetingType = meetingType.name,
            meetingTimestamp = meetingTime.toEpochMillis(),
            durationMinutes = durationMinutes,
            locationOrLink = locationOrLink,
            agenda = agenda,
            minutesSummary = minutesSummary,
            reminderMinutesBefore = reminderMinutesBefore,
            isCompleted = isCompleted
        )

    fun ProjectWithDetails.toDomain(): Project {
        val domainMeetings = meetings.map { it.toDomain(projectName = project.name) }
        val domainStages = stages.map { stageEntity ->
            val stageMeetings = domainMeetings.filter { it.stageId == stageEntity.id }
            stageEntity.toDomain(meetings = stageMeetings)
        }
        return project.toDomain(stages = domainStages, meetings = domainMeetings)
    }

    fun parseBuildingTasks(json: String): List<BuildingSubTask> {
        if (json.isBlank()) return emptyList()
        // 简易分隔符序列化: "name|progress|estDays|remDays|status;;name2|..."
        return json.split(";;").filter { it.isNotBlank() }.mapIndexed { index, item ->
            val parts = item.split("|")
            val name = parts.getOrNull(0) ?: "分项工程"
            val progress = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val estDays = parts.getOrNull(2)?.toIntOrNull() ?: 5
            val remDays = parts.getOrNull(3)?.toIntOrNull() ?: 0
            val status = parts.getOrNull(4) ?: (if (progress >= 100) "已完成" else "编制中")
            BuildingSubTask(
                id = "task_$index",
                name = name,
                progress = progress,
                estimatedDays = estDays,
                remainingDays = remDays,
                statusText = status,
                isCompleted = progress >= 100
            )
        }
    }

    fun serializeBuildingTasks(tasks: List<BuildingSubTask>): String {
        return tasks.joinToString(";;") { task ->
            "${task.name}|${task.progress}|${task.estimatedDays}|${task.remainingDays}|${task.statusText}"
        }
    }
}
