package com.projectworkflow.manager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val projectType: String = "BUDGET", // BUDGET, SETTLEMENT, WHOLE_PROCESS
    val code: String? = null,
    val description: String? = null,
    val status: String = "IN_PROGRESS", // PLANNING, IN_PROGRESS, AUDITING, DELAYED, COMPLETED
    val priority: String = "MEDIUM", // LOW, MEDIUM, HIGH, URGENT
    
    // 造价工期计划与进度
    val totalPlanDays: Int = 30,
    val elapsedDays: Int = 0,
    val currentFocusAction: String = "",
    val documentStatus: String = "COMPLETE", // COMPLETE, MISSING_CRITICAL, PENDING_VERIFY
    val buildingTasksJson: String = "",     // 序列化的楼栋单体任务列表

    val startTimestamp: Long,
    val targetEndTimestamp: Long,
    val progress: Int = 0, // 0 - 100
    val createdAt: Long = System.currentTimeMillis()
)
