package com.projectworkflow.manager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stages",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class StageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val name: String,
    val sortOrder: Int,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val progress: Int = 0, // 0 - 100
    val status: String, // NOT_STARTED, IN_PROGRESS, DELAYED, COMPLETED
    val deliverableNote: String? = null,
    val subTasksJson: String = "", // 融入该流程阶段的各单体或专业分项拆解数据 (持久化)
    val elapsedDays: Int = 0 // 该阶段实际已用/耗时天数 (支持补流程与手动精准调整)
)
