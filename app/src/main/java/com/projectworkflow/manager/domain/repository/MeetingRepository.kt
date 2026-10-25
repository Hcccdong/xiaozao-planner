package com.projectworkflow.manager.domain.repository

import com.projectworkflow.manager.domain.model.Meeting
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface MeetingRepository {
    fun getAllMeetingsFlow(): Flow<List<Meeting>>
    fun getUpcomingMeetingsFlow(from: LocalDateTime): Flow<List<Meeting>>
    suspend fun getActiveUpcomingMeetings(from: LocalDateTime): List<Meeting>
    fun getMeetingsByProjectFlow(projectId: Long): Flow<List<Meeting>>
    fun getMeetingsByStageFlow(stageId: Long): Flow<List<Meeting>>
    suspend fun getMeetingById(meetingId: Long): Meeting?
    suspend fun scheduleMeeting(meeting: Meeting): Long
    suspend fun updateMeeting(meeting: Meeting)
    suspend fun markMeetingCompleted(meetingId: Long, completed: Boolean)
    suspend fun deleteMeeting(meetingId: Long)
}
