package com.projectworkflow.manager

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.projectworkflow.manager.data.local.AppDatabase
import com.projectworkflow.manager.data.repository.MeetingRepositoryImpl
import com.projectworkflow.manager.data.repository.ProjectRepositoryImpl
import com.projectworkflow.manager.domain.repository.MeetingRepository
import com.projectworkflow.manager.domain.repository.ProjectRepository
import com.projectworkflow.manager.reminder.AlarmScheduler
import com.projectworkflow.manager.reminder.AndroidAlarmScheduler

class ProjectManagerApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var projectRepository: ProjectRepository
        private set

    lateinit var meetingRepository: MeetingRepository
        private set

    lateinit var alarmScheduler: AlarmScheduler
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        projectRepository = ProjectRepositoryImpl(
            database.projectDao(),
            database.stageDao(),
            database.meetingDao()
        )
        meetingRepository = MeetingRepositoryImpl(
            database.meetingDao(),
            database.projectDao(),
            database.stageDao()
        )
        alarmScheduler = AndroidAlarmScheduler(this)

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "meeting_reminder_channel",
                "工程会议与节点提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "在工程项目阶段会议开始前及时弹出通知"
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        lateinit var instance: ProjectManagerApp
            private set
    }
}
