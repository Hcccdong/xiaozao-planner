package com.projectworkflow.manager.data.repository

import com.projectworkflow.manager.data.local.dao.MeetingDao
import com.projectworkflow.manager.data.local.dao.ProjectDao
import com.projectworkflow.manager.data.local.dao.StageDao
import com.projectworkflow.manager.data.local.mapper.DataMappers.toDomain
import com.projectworkflow.manager.data.local.mapper.DataMappers.toEpochMillis
import com.projectworkflow.manager.data.local.mapper.DataMappers.toEntity
import com.projectworkflow.manager.domain.model.Meeting
import com.projectworkflow.manager.domain.repository.MeetingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class MeetingRepositoryImpl(
    private val meetingDao: MeetingDao,
    private val projectDao: ProjectDao,
    private val stageDao: StageDao
) : MeetingRepository {

    override fun getAllMeetingsFlow(): Flow<List<Meeting>> {
        return meetingDao.getAllMeetingsFlow().map { entities ->
            entities.map { entity ->
                val project = entity.projectId?.let { projectDao.getProjectById(it) }
                val stage = entity.stageId?.let { stageDao.getStageById(it) }
                entity.toDomain(projectName = project?.name, stageName = stage?.name)
            }
        }
    }

    override fun getUpcomingMeetingsFlow(from: LocalDateTime): Flow<List<Meeting>> {
        return meetingDao.getUpcomingMeetingsFlow(from.toEpochMillis()).map { entities ->
            entities.map { entity ->
                val project = entity.projectId?.let { projectDao.getProjectById(it) }
                val stage = entity.stageId?.let { stageDao.getStageById(it) }
                entity.toDomain(projectName = project?.name, stageName = stage?.name)
            }
        }
    }

    override suspend fun getActiveUpcomingMeetings(from: LocalDateTime): List<Meeting> {
        return meetingDao.getActiveUpcomingMeetings(from.toEpochMillis()).map { entity ->
            val project = entity.projectId?.let { projectDao.getProjectById(it) }
            val stage = entity.stageId?.let { stageDao.getStageById(it) }
            entity.toDomain(projectName = project?.name, stageName = stage?.name)
        }
    }

    override fun getMeetingsByProjectFlow(projectId: Long): Flow<List<Meeting>> {
        return meetingDao.getMeetingsByProjectFlow(projectId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getMeetingsByStageFlow(stageId: Long): Flow<List<Meeting>> {
        return meetingDao.getMeetingsByStageFlow(stageId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getMeetingById(meetingId: Long): Meeting? {
        val entity = meetingDao.getMeetingById(meetingId) ?: return null
        val project = entity.projectId?.let { projectDao.getProjectById(it) }
        val stage = entity.stageId?.let { stageDao.getStageById(it) }
        return entity.toDomain(projectName = project?.name, stageName = stage?.name)
    }

    override suspend fun scheduleMeeting(meeting: Meeting): Long {
        return meetingDao.insertMeeting(meeting.toEntity())
    }

    override suspend fun updateMeeting(meeting: Meeting) {
        meetingDao.updateMeeting(meeting.toEntity())
    }

    override suspend fun markMeetingCompleted(meetingId: Long, completed: Boolean) {
        meetingDao.updateMeetingCompletion(meetingId, completed)
    }

    override suspend fun deleteMeeting(meetingId: Long) {
        meetingDao.deleteMeetingById(meetingId)
    }
}
