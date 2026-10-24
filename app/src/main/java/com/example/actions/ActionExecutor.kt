package com.example.actions

import android.content.Context
import android.content.Intent
import com.example.accessibility.RinAccessibilityService
import com.example.apps.AppLauncher
import com.example.calls.CallManager
import com.example.data.database.AppDatabase
import com.example.phone.PhoneControlManager
import com.example.reminders.ReminderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ActionExecutor(
    private val context: Context,
    private val appLauncher: AppLauncher,
    private val phoneControl: PhoneControlManager,
    private val callManager: CallManager,
    private val reminderManager: ReminderManager,
    private val database: AppDatabase
) {

    suspend fun execute(action: RinAction, preferredLanguage: String = "Roman Urdu"): ExecutionResult = withContext(Dispatchers.IO) {
        val isUrdu = preferredLanguage.contains("urdu", ignoreCase = true)

        when (action) {
            is RinAction.OpenApp -> {
                when (val result = appLauncher.launchAppByName(action.appName)) {
                    is AppLauncher.LaunchResult.Success -> {
                        val spoken = if (isUrdu) "Ji, ${result.appName} khol rahi hoon." else "Opening ${result.appName}."
                        ExecutionResult(true, spoken, "Opened ${result.appName}")
                    }
                    is AppLauncher.LaunchResult.NotFound -> {
                        val spoken = if (isUrdu) "Aapke phone mein ${action.appName} install nahi hai." else "Could not find ${action.appName} on your device."
                        ExecutionResult(false, spoken, result.reason)
                    }
                    is AppLauncher.LaunchResult.Error -> {
                        val spoken = if (isUrdu) "App open karne mein problem aayi." else "Error opening app: ${result.errorMessage}"
                        ExecutionResult(false, spoken, result.errorMessage)
                    }
                }
            }

            is RinAction.ControlFlashlight -> {
                val turnOn = when (action.targetState.lowercase()) {
                    "on" -> true
                    "off" -> false
                    else -> !phoneControl.isFlashlightOn()
                }
                val success = phoneControl.toggleFlashlight(turnOn)
                val stateText = if (turnOn) "on" else "off"
                val spoken = if (success) {
                    if (isUrdu) "Ji, flashlight $stateText kardi hai." else "Flashlight turned $stateText."
                } else {
                    if (isUrdu) "Flashlight control nahi ho saki. Camera permission check karein." else "Failed to toggle flashlight. Please check camera permission."
                }
                ExecutionResult(success, spoken, "Flashlight $stateText")
            }

            is RinAction.ControlVolume -> {
                val increase = action.direction.lowercase() == "up" || action.direction.lowercase() == "barhao"
                val newLevel = phoneControl.adjustVolume(increase)
                val spoken = if (increase) {
                    if (isUrdu) "Ji, volume barha diya hai." else "Volume increased."
                } else {
                    if (isUrdu) "Ji, volume kam kar diya hai." else "Volume decreased."
                }
                ExecutionResult(true, spoken, "Volume level: $newLevel")
            }

            is RinAction.ControlMedia -> {
                val mediaAction = when (action.command.lowercase()) {
                    "play" -> PhoneControlManager.MediaAction.PLAY
                    "pause" -> PhoneControlManager.MediaAction.PAUSE
                    "next" -> PhoneControlManager.MediaAction.NEXT
                    "previous" -> PhoneControlManager.MediaAction.PREVIOUS
                    else -> PhoneControlManager.MediaAction.PLAY_PAUSE
                }
                val ok = phoneControl.controlMedia(mediaAction)
                val spoken = if (ok) {
                    if (isUrdu) "Ji, media ${action.command} kar diya." else "Media ${action.command} sent."
                } else {
                    if (isUrdu) "Media control nahi ho saka." else "Failed to control media."
                }
                ExecutionResult(ok, spoken, "Media action: ${action.command}")
            }

            is RinAction.OpenSettings -> {
                val type = when (action.settingsType.lowercase()) {
                    "wifi", "wi-fi" -> PhoneControlManager.SettingsScreenType.WIFI
                    "bluetooth" -> PhoneControlManager.SettingsScreenType.BLUETOOTH
                    "sound", "volume" -> PhoneControlManager.SettingsScreenType.SOUND
                    "display" -> PhoneControlManager.SettingsScreenType.DISPLAY
                    "accessibility" -> PhoneControlManager.SettingsScreenType.ACCESSIBILITY
                    "notification", "notifications" -> PhoneControlManager.SettingsScreenType.NOTIFICATION_LISTENER
                    else -> PhoneControlManager.SettingsScreenType.MAIN
                }
                val ok = phoneControl.openSettingsScreen(type)
                val spoken = if (ok) {
                    if (isUrdu) "Ji, ${action.settingsType} settings khol di hain." else "Opening ${action.settingsType} settings."
                } else {
                    if (isUrdu) "Settings nahi khul sakeen." else "Could not open settings."
                }
                ExecutionResult(ok, spoken, "Opened ${action.settingsType} settings")
            }

            is RinAction.NavigateHome -> {
                val accessibility = RinAccessibilityService.instance
                val ok = if (accessibility != null) {
                    accessibility.goHome()
                } else {
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(homeIntent)
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
                val spoken = if (ok) {
                    if (isUrdu) "Ji, home screen par aa gaye hain." else "Navigating home."
                } else {
                    if (isUrdu) "Home screen par nahi ja sake." else "Could not go home."
                }
                ExecutionResult(ok, spoken, "Navigated Home")
            }

            is RinAction.NavigateBack -> {
                val accessibility = RinAccessibilityService.instance
                val ok = accessibility?.goBack() ?: false
                val spoken = if (ok) {
                    if (isUrdu) "Ji, back kar diya." else "Navigating back."
                } else {
                    if (isUrdu) "Back navigation ke liye Accessibility Service enable karein." else "Please enable Accessibility Service for back navigation."
                }
                ExecutionResult(ok, spoken, "Navigated Back")
            }

            is RinAction.MakeCall -> {
                val target = action.contactNameOrNumber
                val contacts = callManager.searchContacts(target)
                val phoneToCall = if (contacts.isNotEmpty()) {
                    contacts.first().phoneNumber
                } else if (target.any { it.isDigit() }) {
                    target
                } else null

                if (phoneToCall != null) {
                    val contactName = contacts.firstOrNull()?.name ?: target
                    val callStarted = callManager.makeCall(phoneToCall)
                    val spoken = if (callStarted) {
                        if (isUrdu) "Ji, $contactName ko call mila rahi hoon." else "Calling $contactName now."
                    } else {
                        if (isUrdu) "Call nahi mil saki. Call permission check karein." else "Call failed. Please verify call permission."
                    }
                    ExecutionResult(callStarted, spoken, "Call to $contactName ($phoneToCall)")
                } else {
                    val spoken = if (isUrdu) "Aapke contacts mein $target nahi mila." else "Contact '$target' was not found."
                    ExecutionResult(false, spoken, "Contact not found")
                }
            }

            is RinAction.PrepareWhatsApp -> {
                val contacts = callManager.searchContacts(action.recipient)
                val phone = contacts.firstOrNull()?.phoneNumber ?: ""
                val ok = phoneControl.prepareWhatsAppMessage(phone, action.message)
                val spoken = if (ok) {
                    if (isUrdu) "Ji, ${action.recipient} ke liye WhatsApp message tayar hai." else "WhatsApp message prepared for ${action.recipient}."
                } else {
                    if (isUrdu) "WhatsApp message nahi ban saka." else "Failed to prepare WhatsApp message."
                }
                ExecutionResult(ok, spoken, "WhatsApp: ${action.message}")
            }

            is RinAction.SendSms -> {
                val contacts = callManager.searchContacts(action.recipient)
                val phone = contacts.firstOrNull()?.phoneNumber ?: action.recipient
                val ok = phoneControl.sendSms(phone, action.message)
                val spoken = if (ok) {
                    if (isUrdu) "Ji, SMS message tayar kar diya hai." else "SMS message prepared."
                } else {
                    if (isUrdu) "SMS nahi bhej sake." else "Could not prepare SMS."
                }
                ExecutionResult(ok, spoken, "SMS: ${action.message}")
            }

            is RinAction.SetReminder -> {
                val delayMs = (action.delayMinutes.coerceAtLeast(1)) * 60 * 1000L
                val time = System.currentTimeMillis() + delayMs
                val id = reminderManager.scheduleReminder(action.title, time)
                val spoken = if (isUrdu) {
                    "Ji, reminder set kar diya hai: ${action.title}."
                } else {
                    "Reminder set for: ${action.title} in ${action.delayMinutes} minutes."
                }
                ExecutionResult(true, spoken, "Reminder #$id: ${action.title}")
            }

            is RinAction.ReadNotifications -> {
                val recent = database.notificationDao().getRecentNotifications(3)
                if (recent.isNotEmpty()) {
                    val first = recent.first()
                    val spoken = if (isUrdu) {
                        "Aapka aakhri notification ${first.appName} se hai. Sender: ${first.title}. Message: ${first.content}."
                    } else {
                        "Your latest notification is from ${first.appName}. Title: ${first.title}. Message: ${first.content}."
                    }
                    ExecutionResult(true, spoken, "Read ${recent.size} notifications")
                } else {
                    val spoken = if (isUrdu) {
                        "Abhi koi naya notification nahi hai."
                    } else {
                        "You have no recent notifications."
                    }
                    ExecutionResult(true, spoken, "No notifications found")
                }
            }

            is RinAction.SearchWeb -> {
                val ok = phoneControl.openWebSearch(action.query)
                val spoken = if (isUrdu) "Ji, Google par '${action.query}' search kar rahi hoon." else "Searching Google for '${action.query}'."
                ExecutionResult(ok, spoken, "Search: ${action.query}")
            }

            is RinAction.GeneralAnswer -> {
                ExecutionResult(true, action.answerText, "AI response generated")
            }
        }
    }
}
