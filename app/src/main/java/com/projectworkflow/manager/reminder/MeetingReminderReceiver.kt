package com.projectworkflow.manager.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.projectworkflow.manager.MainActivity

class MeetingReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val meetingId = intent.getLongExtra("EXTRA_MEETING_ID", 0L)
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "会议提醒"
        val projectName = intent.getStringExtra("EXTRA_PROJECT_NAME") ?: "工程项目"
        val location = intent.getStringExtra("EXTRA_LOCATION") ?: "请准时参会"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "meeting_reminder_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "工程会议与节点提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "在工程项目阶段会议开始前及时弹出通知"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_MEETING_ID", meetingId)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            meetingId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ 会议提醒: $title")
            .setContentText("[$projectName] $location")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("【$projectName】即将召开会议：\n主题：$title\n地点/链接：$location\n请提前准备会议资料！")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        notificationManager.notify(meetingId.toInt(), notification)
    }
}
