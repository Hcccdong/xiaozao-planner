package com.projectworkflow.manager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.projectworkflow.manager.data.local.dao.MeetingDao
import com.projectworkflow.manager.data.local.dao.ProjectDao
import com.projectworkflow.manager.data.local.dao.StageDao
import com.projectworkflow.manager.data.local.entity.MeetingEntity
import com.projectworkflow.manager.data.local.entity.ProjectEntity
import com.projectworkflow.manager.data.local.entity.StageEntity

@Database(
    entities = [
        ProjectEntity::class,
        StageEntity::class,
        MeetingEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun stageDao(): StageDao
    abstract fun meetingDao(): MeetingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * v1 -> v2 平滑迁移策略
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 保持表结构兼容
            }
        }

        /**
         * v2 -> v3 平滑增量迁移策略：
         * 在 stages 表中安全增量追加 subTasksJson 字段，保留所有已存在项目与阶段数据，绝不删除任何用户数据
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE stages ADD COLUMN subTasksJson TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {
                    // 若字段已存在则静默忽略
                }
            }
        }

        /**
         * v3 -> v4 平滑增量迁移策略：
         * 在 stages 表中安全增量追加 elapsedDays 字段，支持补流程与手动精准调整阶段实际耗时
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE stages ADD COLUMN elapsedDays INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // 若字段已存在则静默忽略
                }
            }
        }

        /**
         * v4 -> v5 平滑增量迁移策略：
         * 在 projects 表中安全增量追加 businessClosureJson 字段，支持商务闭环追踪 (是否开票、是否收费到账、是否出具回访单、是否收到成果确认单)
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE projects ADD COLUMN businessClosureJson TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {
                    // 若字段已存在则静默忽略
                }
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "project_management_clean.db"
                )
                    // 彻底移除破坏性重置指令，通过显式增量迁移保证历史数据永久安全
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
