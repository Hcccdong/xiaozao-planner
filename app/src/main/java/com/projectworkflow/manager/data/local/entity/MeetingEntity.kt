package com.projectworkflow.manager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meetings",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StageEntity::class,
            parentColumns = ["id"],
            childColumns = ["stageId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["stageId"]),
        Index(value = ["meetingTimestamp"])
    ]
)
data class MeetingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long?,
    val stageId: Long?,
    val title: String,
    val meetingType: String, // KICK_OFF, REVIEW, SYNC, ACCEPTANCE, OTHER
    val meetingTimestamp: Long,
    val durationMinutes: Int = 60,
    val locationOrLink: String? = null,
    val agenda: String? = null,
    val minutesSummary: String? = null,
    val reminderMinutesBefore: Int = 15,
    val isCompleted: Boolean = false
)
