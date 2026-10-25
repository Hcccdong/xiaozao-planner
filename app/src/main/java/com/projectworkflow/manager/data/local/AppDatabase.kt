package com.projectworkflow.manager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.projectworkflow.manager.data.local.dao.MeetingDao
import com.projectworkflow.manager.data.local.dao.ProjectDao
import com.projectworkflow.manager.data.local.dao.StageDao
import com.projectworkflow.manager.data.local.entity.MeetingEntity
import com.projectworkflow.manager.data.local.entity.ProjectEntity
import com.projectworkflow.manager.data.local.entity.StageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

@Database(
    entities = [
        ProjectEntity::class,
        StageEntity::class,
        MeetingEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun stageDao(): StageDao
    abstract fun meetingDao(): MeetingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "project_workflow_cost.db"
                )
                    .addCallback(SeedDataCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class SeedDataCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                INSTANCE?.let { database ->
                    seedInitialData(database)
                }
            }
        }

        private suspend fun seedInitialData(database: AppDatabase) {
            val now = LocalDateTime.now()
            val zoneId = ZoneId.systemDefault()
            fun LocalDateTime.toMillis() = this.atZone(zoneId).toInstant().toEpochMilli()

            // 1. 商业综合体改造 (预算工程)
            val p1Tasks = listOf(
                "1#楼主楼主体及装饰|100|8|0|已完成编制",
                "2#楼副楼及连廊|50|6|3|编制算量中(预计还剩3天)",
                "3#楼商业裙房|0|5|5|未开始(预计耗时5天)",
                "地下车库及室外综合管网|0|6|6|未开始(预计耗时6天)"
            ).joinToString(";;")

            val p1Id = database.projectDao().insertProject(
                ProjectEntity(
                    name = "商业综合体 · 预算编制",
                    projectType = "BUDGET",
                    code = "YS-2024-08",
                    description = "商业综合体改造招标控制价编制，包含3栋单体及地下室",
                    status = "IN_PROGRESS",
                    priority = "HIGH",
                    totalPlanDays = 25,
                    elapsedDays = 12,
                    currentFocusAction = "1号楼已完成；正在算2号楼钢筋及装饰，预计还剩3天；准备明日方案沟通会",
                    documentStatus = "COMPLETE",
                    buildingTasksJson = p1Tasks,
                    startTimestamp = now.minusDays(12).toMillis(),
                    targetEndTimestamp = now.plusDays(13).toMillis(),
                    progress = 68
                )
            )

            val p1s1 = database.stageDao().insertStage(
                StageEntity(
                    projectId = p1Id,
                    name = "图纸接收与深度审核",
                    sortOrder = 1,
                    startTimestamp = now.minusDays(12).toMillis(),
                    endTimestamp = now.minusDays(9).toMillis(),
                    progress = 100,
                    status = "COMPLETED",
                    deliverableNote = "施工图纸深度审查表、图纸疑问清单交底记录"
                )
            )

            val p1s2 = database.stageDao().insertStage(
                StageEntity(
                    projectId = p1Id,
                    name = "分栋分部工程量计算与套价",
                    sortOrder = 2,
                    startTimestamp = now.minusDays(8).toMillis(),
                    endTimestamp = now.plusDays(6).toMillis(),
                    progress = 68,
                    status = "IN_PROGRESS",
                    deliverableNote = "1号楼已出算量稿，2号楼正在背靠背复核"
                )
            )

            val p1s3 = database.stageDao().insertStage(
                StageEntity(
                    projectId = p1Id,
                    name = "预算方案沟通与对账交底",
                    sortOrder = 3,
                    startTimestamp = now.plusDays(7).toMillis(),
                    endTimestamp = now.plusDays(10).toMillis(),
                    progress = 0,
                    status = "NOT_STARTED",
                    deliverableNote = "预算对账沟通备忘录与差额说明"
                )
            )

            // 明天下午 14:30 预算方案沟通会
            val tomorrow230pm = now.plusDays(1).withHour(14).withMinute(30).withSecond(0)
            database.meetingDao().insertMeeting(
                MeetingEntity(
                    projectId = p1Id,
                    stageId = p1s2,
                    title = "预算方案沟通与对账会",
                    meetingType = "BUDGET_ALIGN",
                    meetingTimestamp = tomorrow230pm.toMillis(),
                    durationMinutes = 90,
                    locationOrLink = "3号会议室 (腾讯会议: 889-234-110)",
                    agenda = "核对1号楼暂估材差额、明确2号楼装饰做法标准",
                    reminderMinutesBefore = 15
                )
            )

            // 2. 住宅项目 · 结算审核 (结算工程，逾期1天)
            val p2Tasks = listOf(
                "结算送审资料完整性审查|100|4|0|已审查(缺3份隐蔽单)",
                "现场踏勘收方与事实核验|100|3|0|已踏勘(形成实测底稿)",
                "土建及装饰分部工程量初核|85|15|2|核对尚未完成",
                "安装及机电分项工程初审|60|10|4|核量进行中",
                "争议项三方对账协调与定案|0|8|8|待组织对账会"
            ).joinToString(";;")

            val p2Id = database.projectDao().insertProject(
                ProjectEntity(
                    name = "住宅项目 · 结算审核",
                    projectType = "SETTLEMENT",
                    code = "JS-2024-03",
                    description = "住宅二期景观与安装竣工结算审核，送审金额 3800 万元",
                    status = "DELAYED",
                    priority = "URGENT",
                    totalPlanDays = 40,
                    elapsedDays = 41,
                    currentFocusAction = "工程量核对尚未完成；施工方缺3份隐蔽工程验收单未交，超期1天",
                    documentStatus = "MISSING_CRITICAL",
                    buildingTasksJson = p2Tasks,
                    startTimestamp = now.minusDays(41).toMillis(),
                    targetEndTimestamp = now.minusDays(1).toMillis(),
                    progress = 80
                )
            )

            database.stageDao().insertStage(
                StageEntity(
                    projectId = p2Id,
                    name = "结算资料备齐与现场踏勘",
                    sortOrder = 1,
                    startTimestamp = now.minusDays(41).toMillis(),
                    endTimestamp = now.minusDays(34).toMillis(),
                    progress = 100,
                    status = "COMPLETED",
                    deliverableNote = "已完成实地踏勘收方，资料缺项已发补正函"
                )
            )

            database.stageDao().insertStage(
                StageEntity(
                    projectId = p2Id,
                    name = "工程量核对与争议协调",
                    sortOrder = 2,
                    startTimestamp = now.minusDays(33).toMillis(),
                    endTimestamp = now.minusDays(1).toMillis(), // 昨天到期 -> 逾期1天
                    progress = 80,
                    status = "DELAYED",
                    deliverableNote = "土建核减争议项待开会闭环"
                )
            )

            // 3. 市政项目 · 全过程造价咨询
            val p3Tasks = listOf(
                "前期招标控制价编制与交底|100|20|0|已完成并招标定标",
                "施工图会审与经济性审查|100|10|0|已形成会审纪要",
                "第1期/第2期月度进度款审核|100|60|0|均已出具付款审核单",
                "第3期月度进度款审核|60|30|2|正在复核现场形象进度",
                "工程变更签证经济动态台账|80|40|5|按月动态登记审核"
            ).joinToString(";;")

            val p3Id = database.projectDao().insertProject(
                ProjectEntity(
                    name = "市政项目 · 成果报告",
                    projectType = "WHOLE_PROCESS",
                    code = "QGC-2024-01",
                    description = "市政主干道改造全过程造价咨询，当前进行至第3期进度款月报",
                    status = "IN_PROGRESS",
                    priority = "MEDIUM",
                    totalPlanDays = 180,
                    elapsedDays = 60,
                    currentFocusAction = "正在编制第3期工程进度款审核月报，下一成果: 初审报告 V1 (后天交付)",
                    documentStatus = "COMPLETE",
                    buildingTasksJson = p3Tasks,
                    startTimestamp = now.minusDays(60).toMillis(),
                    targetEndTimestamp = now.plusDays(120).toMillis(),
                    progress = 40
                )
            )

            database.stageDao().insertStage(
                StageEntity(
                    projectId = p3Id,
                    name = "第3期月度进度款审核与月报编制",
                    sortOrder = 1,
                    startTimestamp = now.minusDays(10).toMillis(),
                    endTimestamp = now.plusDays(2).toMillis(), // 后天交付初审报告
                    progress = 60,
                    status = "IN_PROGRESS",
                    deliverableNote = "下一成果: 初审报告 V1"
                )
            )
        }
    }
}
