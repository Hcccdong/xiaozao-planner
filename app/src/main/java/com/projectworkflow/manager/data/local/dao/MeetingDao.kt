package com.projectworkflow.manager.data.local.dao

import androidx.room.*
import com.projectworkflow.manager.data.local.entity.MeetingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetingDao {

    @Query("SELECT * FROM meetings ORDER BY meetingTimestamp ASC")
    fun getAllMeetingsFlow(): Flow<List<MeetingEntity>>

    @Query("SELECT * FROM meetings WHERE meetingTimestamp >= :fromTimestamp ORDER BY meetingTimestamp ASC")
    fun getUpcomingMeetingsFlow(fromTimestamp: Long): Flow<List<MeetingEntity>>

    @Query("SELECT * FROM meetings WHERE meetingTimestamp >= :fromTimestamp AND isCompleted = 0 ORDER BY meetingTimestamp ASC")
    suspend fun getActiveUpcomingMeetings(fromTimestamp: Long): List<MeetingEntity>

    @Query("SELECT * FROM meetings WHERE projectId = :projectId ORDER BY meetingTimestamp ASC")
    fun getMeetingsByProjectFlow(projectId: Long): Flow<List<MeetingEntity>>

    @Query("SELECT * FROM meetings WHERE stageId = :stageId ORDER BY meetingTimestamp ASC")
    fun getMeetingsByStageFlow(stageId: Long): Flow<List<MeetingEntity>>

    @Query("SELECT * FROM meetings WHERE id = :meetingId")
    suspend fun getMeetingById(meetingId: Long): MeetingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeeting(meeting: MeetingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeetings(meetings: List<MeetingEntity>): List<Long>

    @Update
    suspend fun updateMeeting(meeting: MeetingEntity)

    @Query("UPDATE meetings SET isCompleted = :completed WHERE id = :meetingId")
    suspend fun updateMeetingCompletion(meetingId: Long, completed: Boolean)

    @Delete
    suspend fun deleteMeeting(meeting: MeetingEntity)

    @Query("DELETE FROM meetings WHERE id = :meetingId")
    suspend fun deleteMeetingById(meetingId: Long)
}
