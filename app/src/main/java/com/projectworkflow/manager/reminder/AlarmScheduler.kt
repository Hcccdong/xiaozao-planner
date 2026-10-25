package com.projectworkflow.manager.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.projectworkflow.manager.domain.model.Meeting
import java.time.ZoneId

interface AlarmScheduler {
    fun schedule(meeting: Meeting)
    fun cancel(meetingId: Long)
}

class AndroidAlarmScheduler(
    private val context: Context
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(meeting: Meeting) {
        val reminderTime = meeting.meetingTime.minusMinutes(meeting.reminderMinutesBefore.toLong())
        val triggerAtMillis = reminderTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // 如果提醒时间已经过去，则不再调度
        if (triggerAtMillis <= System.currentTimeMillis()) {
            return
        }

        val intent = Intent(context, MeetingReminderReceiver::class.java).apply {
            putExtra("EXTRA_MEETING_ID", meeting.id)
            putExtra("EXTRA_TITLE", meeting.title)
            putExtra("EXTRA_PROJECT_NAME", meeting.projectName ?: "工程项目")
            putExtra("EXTRA_LOCATION", meeting.locationOrLink ?: "请准时参会")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            meeting.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 适配 Android 12 (API 31)+ 精确闹钟权限检查
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    override fun cancel(meetingId: Long) {
        val intent = Intent(context, MeetingReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            meetingId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
