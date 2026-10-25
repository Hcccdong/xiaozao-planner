package com.projectworkflow.manager.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.projectworkflow.manager.data.local.AppDatabase
import com.projectworkflow.manager.data.repository.MeetingRepositoryImpl
import com.projectworkflow.manager.data.repository.ProjectRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val projectRepo = ProjectRepositoryImpl(db.projectDao(), db.stageDao(), db.meetingDao())
                    val meetingRepo = MeetingRepositoryImpl(db.meetingDao(), db.projectDao(), db.stageDao())
                    val alarmScheduler = AndroidAlarmScheduler(context)

                    val upcomingMeetings = meetingRepo.getActiveUpcomingMeetings(LocalDateTime.now())
                    upcomingMeetings.forEach { meeting ->
                        alarmScheduler.schedule(meeting)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
