package com.example.assistant

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.actions.ActionExecutor
import com.example.actions.ExecutionResult
import com.example.actions.RinAction
import com.example.apps.AppLauncher
import com.example.calls.CallManager
import com.example.calls.RinPhoneCallReceiver
import com.example.data.database.AppDatabase
import com.example.data.database.entities.ConversationEntity
import com.example.network.GeminiBrain
import com.example.notifications.RinNotificationListener
import com.example.phone.PhoneControlManager
import com.example.reminders.ReminderManager
import com.example.reminders.RinAlarmReceiver
import com.example.settings.SettingsRepository
import com.example.voice.SpeechRecognitionManager
import com.example.voice.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AssistantStatus {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    EXECUTING
}

class MimiAssistant(
    private val context: Context,
    val settingsRepo: SettingsRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val database = AppDatabase.getDatabase(context)
    private val mainHandler = Handler(Looper.getMainLooper())

    val phoneControl = PhoneControlManager(context)
    val appLauncher = AppLauncher(context)
    val callManager = CallManager(context)
    val reminderManager = ReminderManager(context)
    val actionExecutor = ActionExecutor(context, appLauncher, phoneControl, callManager, reminderManager, database)
    val geminiBrain = GeminiBrain()

    val ttsManager = TextToSpeechManager(context)
    var speechManager: SpeechRecognitionManager? = null

    // Assistant ON/OFF master state
    private val _isAssistantOn = MutableStateFlow(false)
    val isAssistantOn: StateFlow<Boolean> = _isAssistantOn.asStateFlow()

    // Mute state
    val isMuted: StateFlow<Boolean> = ttsManager.isMuted

    private val _status = MutableStateFlow(AssistantStatus.IDLE)
    val status: StateFlow<AssistantStatus> = _status.asStateFlow()

    private val _activeTranscript = MutableStateFlow("")
    val activeTranscript: StateFlow<String> = _activeTranscript.asStateFlow()

    private val _lastExecutionResult = MutableStateFlow<ExecutionResult?>(null)
    val lastExecutionResult: StateFlow<ExecutionResult?> = _lastExecutionResult.asStateFlow()

    init {
        activeInstance = this

        speechManager = SpeechRecognitionManager(
            context = context,
            onResultReceived = { text ->
                handleUserSpeech(text)
            },
            onErrorReceived = { err ->
                Log.d("MimiAssistant", "Speech recognizer notice: $err")
                if (_status.value == AssistantStatus.LISTENING) {
                    _status.value = AssistantStatus.IDLE
                }
            },
            onSpeechEnded = {
                if (_status.value == AssistantStatus.LISTENING) {
                    _status.value = AssistantStatus.THINKING
                }
            }
        )

        // Observe TTS speaking state
        scope.launch {
            ttsManager.isSpeaking.collect { isSpeaking ->
                if (!isSpeaking && _status.value == AssistantStatus.SPEAKING) {
                    _status.value = AssistantStatus.IDLE
                }
            }
        }

        // Listen for incoming call announcements
        scope.launch {
            RinPhoneCallReceiver.incomingCallAnnouncementFlow.collect { announcement ->
                if (_isAssistantOn.value) {
                    speakAnnouncement(announcement)
                }
            }
        }

        // Listen for incoming notification announcements
        scope.launch {
            RinNotificationListener.incomingNotificationFlow.collect { announcement ->
                if (_isAssistantOn.value) {
                    speakAnnouncement(announcement)
                }
            }
        }

        // Listen for scheduled alarm / reminder voice announcements
        scope.launch {
            RinAlarmReceiver.alarmVoiceAnnouncementFlow.collect { announcement ->
                if (_isAssistantOn.value) {
                    speakAnnouncement(announcement)
                }
            }
        }
    }

    fun setAssistantPower(enabled: Boolean) {
        _isAssistantOn.value = enabled
        if (enabled) {
            val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (hasMic) {
                _status.value = AssistantStatus.IDLE
                val lang = settingsRepo.settings.value.language
                val greeting = when {
                    lang.contains("urdu", ignoreCase = true) -> "Ji, main Mimi hoon. Main sun rahi hoon."
                    lang.contains("hindi", ignoreCase = true) -> "Namaste, main Mimi hoon. Kahiye kya madad karoon?"
                    else -> "Hello, I'm Mimi. I am listening."
                }
                speakAnnouncement(greeting)
            }
        } else {
            stopListening()
            stopSpeaking()
            _status.value = AssistantStatus.IDLE
        }
    }

    fun toggleAssistantPower(): Boolean {
        val newState = !_isAssistantOn.value
        setAssistantPower(newState)
        return newState
    }

    fun setMuted(muted: Boolean) {
        ttsManager.setMuted(muted)
        if (!muted && _isAssistantOn.value) {
            val lang = settingsRepo.settings.value.language
            val text = if (lang.contains("urdu", ignoreCase = true)) "Mimi unmuted." else "Mimi unmuted."
            speakAnnouncement(text)
        }
    }

    fun toggleMute(): Boolean {
        val newMute = !ttsManager.isMuted.value
        setMuted(newMute)
        return newMute
    }

    fun startListening() {
        val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!hasMic) {
            Log.w("MimiAssistant", "Cannot start listening: RECORD_AUDIO permission missing")
            return
        }

        ttsManager.stop()
        _status.value = AssistantStatus.LISTENING
        _activeTranscript.value = ""
        speechManager?.startListening(settingsRepo.settings.value.language)
    }

    fun stopListening() {
        speechManager?.stopListening()
        if (_status.value == AssistantStatus.LISTENING) {
            _status.value = AssistantStatus.IDLE
        }
    }

    fun stopSpeaking() {
        ttsManager.stop()
        _status.value = AssistantStatus.IDLE
    }

    fun handleUserSpeech(spokenText: String) {
        val trimmed = spokenText.trim()
        if (trimmed.isEmpty()) {
            _status.value = AssistantStatus.IDLE
            return
        }

        val lower = trimmed.lowercase()

        // Immediate stop commands: "stop", "chup", "ruko", "bas", "quiet"
        if (lower == "stop" || lower == "chup" || lower == "ruko" || lower == "bas" || lower == "quiet" || lower == "chup raho") {
            stopSpeaking()
            stopListening()
            _activeTranscript.value = "Stopped"
            return
        }

        // Direct voice mute / unmute
        if (lower == "mute" || lower == "mute mimi" || lower == "chup ho jao" || lower == "khamosh ho jao") {
            setMuted(true)
            _activeTranscript.value = "Mimi muted"
            return
        }

        if (lower == "unmute" || lower == "unmute mimi" || lower == "bolna shuru karo") {
            setMuted(false)
            _activeTranscript.value = "Mimi unmuted"
            return
        }

        // Wake word clean up if present
        val prefixes = listOf(
            "hello mimi", "hey mimi", "mimi",
            "hello rin", "hey rin", "rin",
            settingsRepo.settings.value.wakeWord.lowercase()
        ).distinct()

        var commandBody = trimmed
        var isWakeWordTriggered = false

        for (prefix in prefixes) {
            if (lower.startsWith(prefix)) {
                isWakeWordTriggered = true
                var remainder = lower.substring(prefix.length).trim()
                remainder = remainder.removePrefix(",").removePrefix(":").removePrefix(".").trim()
                commandBody = if (remainder.isNotEmpty()) {
                    trimmed.substring(trimmed.length - remainder.length).trim()
                } else {
                    ""
                }
                break
            }
        }

        if (isWakeWordTriggered && commandBody.isBlank()) {
            // User just greeted: "Hello Mimi"
            val lang = settingsRepo.settings.value.language
            val reply = when {
                lang.contains("urdu", ignoreCase = true) -> "Ji, main sun rahi hoon. Farmayein?"
                lang.contains("hindi", ignoreCase = true) -> "Haan, main sun rahi hoon. Kahiye?"
                else -> "Hello! I'm listening. How can I help you?"
            }
            speakAnnouncement(reply)
            return
        }

        processCommand(if (commandBody.isNotBlank()) commandBody else trimmed)
    }

    fun setLanguage(language: String) {
        ttsManager.setLanguage(language)
    }

    fun processCommand(input: String) {
        scope.launch {
            _status.value = AssistantStatus.THINKING
            _activeTranscript.value = input

            // Save user query into Room database
            val userMsg = ConversationEntity(
                role = "user",
                text = input,
                timestamp = System.currentTimeMillis()
            )
            database.conversationDao().insertMessage(userMsg)

            val recent = database.conversationDao().getRecentMessages(6).reversed()
            val historyPairs = recent.map { it.role to it.text }

            val currentLang = settingsRepo.settings.value.language

            val parsedIntent = geminiBrain.analyzeCommand(
                userInput = input,
                conversationHistory = historyPairs,
                preferredLanguage = currentLang
            )

            _status.value = AssistantStatus.EXECUTING

            val executionResult = if (parsedIntent.action is RinAction.GeneralAnswer) {
                ExecutionResult(true, parsedIntent.spokenReply, "AI Response")
            } else {
                actionExecutor.execute(parsedIntent.action, currentLang)
            }

            _lastExecutionResult.value = executionResult

            // Save assistant reply into Room database
            val mimiMsg = ConversationEntity(
                role = "mimi",
                text = executionResult.spokenFeedback,
                actionType = parsedIntent.action::class.simpleName,
                timestamp = System.currentTimeMillis()
            )
            database.conversationDao().insertMessage(mimiMsg)

            // Spoken feedback in user's chosen voice & language
            _status.value = AssistantStatus.SPEAKING
            val settings = settingsRepo.settings.value
            ttsManager.updateConfig(
                pitch = settings.voicePitch,
                speed = settings.voiceSpeed,
                voiceName = settings.preferredVoice,
                gender = settings.voiceGender,
                language = settings.language
            )
            ttsManager.speak(executionResult.spokenFeedback)
        }
    }

    fun speakAnnouncement(text: String) {
        scope.launch {
            _status.value = AssistantStatus.SPEAKING
            val settings = settingsRepo.settings.value
            ttsManager.updateConfig(
                pitch = settings.voicePitch,
                speed = settings.voiceSpeed,
                voiceName = settings.preferredVoice,
                gender = settings.voiceGender,
                language = settings.language
            )
            ttsManager.speak(text)
        }
    }

    fun clearConversationHistory() {
        scope.launch {
            database.conversationDao().clearAll()
        }
    }

    fun destroy() {
        mainHandler.removeCallbacksAndMessages(null)
        speechManager?.destroy()
        ttsManager.shutdown()
        if (activeInstance == this) {
            activeInstance = null
        }
    }

    companion object {
        var activeInstance: MimiAssistant? = null
            private set
    }
}
