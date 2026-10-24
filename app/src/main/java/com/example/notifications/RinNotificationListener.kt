package com.example.notifications

import android.app.KeyguardManager
import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.data.database.AppDatabase
import com.example.data.database.entities.NotificationRecordEntity
import com.example.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class RinNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        // Ignore self notifications
        if (packageName == applicationContext.packageName) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        val pm = applicationContext.packageManager
        val appName = try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }

        val record = NotificationRecordEntity(
            packageName = packageName,
            appName = appName,
            title = title,
            content = text,
            timestamp = sbn.postTime
        )

        // Store into database
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                db.notificationDao().insertNotification(record)

                val settingsRepo = SettingsRepository(applicationContext)
                val settings = settingsRepo.settings.value

                if (settings.announceNotifications) {
                    val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                    val isLocked = keyguardManager?.isKeyguardLocked == true

                    val announcement = when {
                        isLocked && settings.lockScreenPrivacy -> {
                            "RIN: Aapko $appName se ek naya notification aaya hai."
                        }
                        packageName == "com.whatsapp" -> {
                            "RIN: Aapko WhatsApp par $title ka message aaya hai."
                        }
                        packageName.contains("messaging") || packageName.contains("mms") -> {
                            "RIN: Aapko SMS par $title ka message aaya hai."
                        }
                        else -> {
                            "RIN: Aapko $appName se message aaya hai. Title: $title."
                        }
                    }

                    _incomingNotificationFlow.emit(announcement)
                }
            } catch (e: Exception) {
                Log.e("RinNotificationListener", "Failed saving notification", e)
            }
        }
    }

    companion object {
        private val _incomingNotificationFlow = MutableSharedFlow<String>(extraBufferCapacity = 5)
        val incomingNotificationFlow = _incomingNotificationFlow.asSharedFlow()

        fun isNotificationAccessGranted(context: Context): Boolean {
            val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
            return enabledPackages.contains(context.packageName)
        }
    }
}
