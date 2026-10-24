package com.example.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.database.entities.ReminderEntity

class ReminderManager(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val reminderDao = AppDatabase.getDatabase(context).reminderDao()

    suspend fun scheduleReminder(title: String, timeInMillis: Long, repeatMode: String = "none"): Long {
        val reminder = ReminderEntity(
            title = title,
            timeInMillis = timeInMillis,
            isCompleted = false,
            repeatMode = repeatMode
        )
        val id = reminderDao.insertReminder(reminder)

        val intent = Intent(context, RinAlarmReceiver::class.java).apply {
            putExtra("reminder_id", id)
            putExtra("reminder_title", title)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                }
            }
        } catch (e: Exception) {
            Log.e("ReminderManager", "Error scheduling alarm", e)
        }

        return id
    }

    suspend fun cancelReminder(reminder: ReminderEntity) {
        val intent = Intent(context, RinAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager?.cancel(pendingIntent)
        reminderDao.deleteReminder(reminder)
    }
}
