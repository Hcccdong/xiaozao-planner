package com.projectworkflow.manager.data.local.dao

import androidx.room.*
import com.projectworkflow.manager.data.local.entity.StageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StageDao {

    @Query("SELECT COUNT(*) FROM stages")
    suspend fun getStageCount(): Int

    @Query("SELECT * FROM stages WHERE projectId = :projectId ORDER BY sortOrder ASC")
    fun getStagesByProjectFlow(projectId: Long): Flow<List<StageEntity>>

    @Query("SELECT * FROM stages WHERE projectId = :projectId ORDER BY sortOrder ASC")
    suspend fun getStagesByProjectId(projectId: Long): List<StageEntity>

    @Query("SELECT * FROM stages WHERE id = :stageId")
    suspend fun getStageById(stageId: Long): StageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStage(stage: StageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStages(stages: List<StageEntity>): List<Long>

    @Update
    suspend fun updateStage(stage: StageEntity)

    @Query("UPDATE stages SET progress = :progress, status = :status WHERE id = :stageId")
    suspend fun updateStageProgress(stageId: Long, progress: Int, status: String)

    @Query("UPDATE stages SET progress = :progress, status = :status, subTasksJson = :subTasksJson WHERE id = :stageId")
    suspend fun updateStageProgressAndSubTasks(stageId: Long, progress: Int, status: String, subTasksJson: String)

    @Delete
    suspend fun deleteStage(stage: StageEntity)

    @Query("DELETE FROM stages WHERE projectId = :projectId")
    suspend fun deleteStagesByProjectId(projectId: Long)
}
