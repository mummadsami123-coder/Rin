package com.example.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class RinAlarmReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("reminder_id", 0L)
        val title = intent.getStringExtra("reminder_title") ?: "Reminder"

        val channelId = "rin_ai_reminders_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "RIN AI Reminders & Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies scheduled reminders from Mimi"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("RIN AI Mimi Reminder")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(reminderId.toInt(), notification)

        // Mark as completed in DB
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                db.reminderDao().setCompleted(reminderId, true)
                _alarmVoiceAnnouncementFlow.emit("RIN: Mimi ka reminder: $title")
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    companion object {
        private val _alarmVoiceAnnouncementFlow = MutableSharedFlow<String>(extraBufferCapacity = 3)
        val alarmVoiceAnnouncementFlow = _alarmVoiceAnnouncementFlow.asSharedFlow()
    }
}
