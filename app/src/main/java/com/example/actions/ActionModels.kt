package com.example.actions

sealed class RinAction {
    data class OpenApp(val appName: String) : RinAction()
    data class OpenSettings(val settingsType: String) : RinAction()
    object ReadNotifications : RinAction()
    data class MakeCall(val contactNameOrNumber: String) : RinAction()
    data class SendSms(val recipient: String, val message: String) : RinAction()
    data class PrepareWhatsApp(val recipient: String, val message: String) : RinAction()
    data class SetReminder(val title: String, val delayMinutes: Int = 10, val rawTime: String = "") : RinAction()
    data class ControlFlashlight(val targetState: String) : RinAction() // "on", "off", "toggle"
    data class ControlVolume(val direction: String) : RinAction() // "up", "down", "mute"
    data class ControlMedia(val command: String) : RinAction() // "play", "pause", "toggle", "next", "previous"
    object NavigateHome : RinAction()
    object NavigateBack : RinAction()
    data class SearchWeb(val query: String) : RinAction()
    data class GeneralAnswer(val answerText: String) : RinAction()
}

data class ExecutionResult(
    val success: Boolean,
    val spokenFeedback: String,
    val detailMessage: String
)
