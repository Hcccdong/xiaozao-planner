package com.projectworkflow.manager.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.projectworkflow.manager.data.local.entity.MeetingEntity
import com.projectworkflow.manager.data.local.entity.ProjectEntity
import com.projectworkflow.manager.data.local.entity.StageEntity

data class ProjectWithDetails(
    @Embedded
    val project: ProjectEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val stages: List<StageEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val meetings: List<MeetingEntity>
)
