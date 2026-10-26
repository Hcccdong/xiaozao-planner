package com.projectworkflow.manager.data.local.dao

import androidx.room.*
import com.projectworkflow.manager.data.local.entity.ProjectEntity
import com.projectworkflow.manager.data.local.relation.ProjectWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Transaction
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjectsWithDetailsFlow(): Flow<List<ProjectWithDetails>>

    @Transaction
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    suspend fun getAllProjectsWithDetailsList(): List<ProjectWithDetails>

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun getProjectCount(): Int

    @Transaction
    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun getProjectWithDetailsByIdFlow(projectId: Long): Flow<ProjectWithDetails?>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectById(projectId: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("UPDATE projects SET progress = :progress, status = :status WHERE id = :projectId")
    suspend fun updateProjectProgress(projectId: Long, progress: Int, status: String)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: Long)
}
