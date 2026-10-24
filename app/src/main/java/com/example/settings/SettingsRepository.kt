package com.example.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class UserSettings(
    val wakeWord: String = "Hello Mimi",
    val language: String = "English", // "English", "Urdu", "Roman Urdu", "Hindi", "Arabic", "Spanish", "French", "German", "Chinese", "Japanese"
    val voiceGender: String = "Female", // "Female", "Male", "Natural"
    val voiceStyle: String = "Clear & Natural", // "Clear & Natural", "Standard", "High Quality"
    val voicePitch: Float = 1.15f,
    val voiceSpeed: Float = 1.0f,
    val preferredVoice: String = "",
    val announceCallers: Boolean = true,
    val announceNotifications: Boolean = true,
    val lockScreenPrivacy: Boolean = true,
    val continuousListening: Boolean = true,
    val selectedNotificationApps: Set<String> = emptySet()
)

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("rin_ai_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private fun loadSettings(): UserSettings {
        return UserSettings(
            wakeWord = prefs.getString("wake_word", "Hello Mimi") ?: "Hello Mimi",
            language = prefs.getString("language", "English") ?: "English",
            voiceGender = prefs.getString("voice_gender", "Female") ?: "Female",
            voiceStyle = prefs.getString("voice_style", "Clear & Natural") ?: "Clear & Natural",
            voicePitch = prefs.getFloat("voice_pitch", 1.15f),
            voiceSpeed = prefs.getFloat("voice_speed", 1.0f),
            preferredVoice = prefs.getString("preferred_voice", "") ?: "",
            announceCallers = prefs.getBoolean("announce_callers", true),
            announceNotifications = prefs.getBoolean("announce_notifications", true),
            lockScreenPrivacy = prefs.getBoolean("lock_screen_privacy", true),
            continuousListening = prefs.getBoolean("continuous_listening", true),
            selectedNotificationApps = prefs.getStringSet("notif_apps", emptySet()) ?: emptySet()
        )
    }

    fun updateWakeWord(wakeWord: String) {
        prefs.edit().putString("wake_word", wakeWord).apply()
        _settings.value = _settings.value.copy(wakeWord = wakeWord)
    }

    fun updateLanguage(lang: String) {
        prefs.edit().putString("language", lang).apply()
        _settings.value = _settings.value.copy(language = lang)
    }

    fun updateVoiceGender(gender: String) {
        prefs.edit().putString("voice_gender", gender).apply()
        _settings.value = _settings.value.copy(voiceGender = gender)
    }

    fun updateVoiceStyle(style: String) {
        prefs.edit().putString("voice_style", style).apply()
        _settings.value = _settings.value.copy(voiceStyle = style)
    }

    fun updateVoicePitch(pitch: Float) {
        prefs.edit().putFloat("voice_pitch", pitch).apply()
        _settings.value = _settings.value.copy(voicePitch = pitch)
    }

    fun updateVoiceSpeed(speed: Float) {
        prefs.edit().putFloat("voice_speed", speed).apply()
        _settings.value = _settings.value.copy(voiceSpeed = speed)
    }

    fun updatePreferredVoice(voiceName: String) {
        prefs.edit().putString("preferred_voice", voiceName).apply()
        _settings.value = _settings.value.copy(preferredVoice = voiceName)
    }

    fun updateAnnounceCallers(enabled: Boolean) {
        prefs.edit().putBoolean("announce_callers", enabled).apply()
        _settings.value = _settings.value.copy(announceCallers = enabled)
    }

    fun updateAnnounceNotifications(enabled: Boolean) {
        prefs.edit().putBoolean("announce_notifications", enabled).apply()
        _settings.value = _settings.value.copy(announceNotifications = enabled)
    }

    fun updateLockScreenPrivacy(enabled: Boolean) {
        prefs.edit().putBoolean("lock_screen_privacy", enabled).apply()
        _settings.value = _settings.value.copy(lockScreenPrivacy = enabled)
    }

    fun updateContinuousListening(enabled: Boolean) {
        prefs.edit().putBoolean("continuous_listening", enabled).apply()
        _settings.value = _settings.value.copy(continuousListening = enabled)
    }

    companion object {
        val SUPPORTED_LANGUAGES = listOf(
            "English",
            "Urdu",
            "Roman Urdu",
            "Hindi",
            "Arabic",
            "Spanish",
            "French",
            "German",
            "Chinese",
            "Japanese",
            "Russian",
            "Turkish"
        )

        fun getLocaleForLanguage(language: String): Locale {
            return when (language.lowercase()) {
                "urdu" -> Locale("ur", "PK")
                "roman urdu" -> Locale("ur", "PK")
                "hindi" -> Locale("hi", "IN")
                "arabic" -> Locale("ar", "SA")
                "spanish" -> Locale("es", "ES")
                "french" -> Locale("fr", "FR")
                "german" -> Locale("de", "DE")
                "chinese" -> Locale.CHINESE
                "japanese" -> Locale.JAPANESE
                "russian" -> Locale("ru", "RU")
                "turkish" -> Locale("tr", "TR")
                else -> Locale.US
            }
        }

        fun getSpeechRecognitionTag(language: String): String {
            return when (language.lowercase()) {
                "urdu" -> "ur-PK"
                "roman urdu" -> "ur-PK"
                "hindi" -> "hi-IN"
                "arabic" -> "ar-SA"
                "spanish" -> "es-ES"
                "french" -> "fr-FR"
                "german" -> "de-DE"
                "chinese" -> "zh-CN"
                "japanese" -> "ja-JP"
                "russian" -> "ru-RU"
                "turkish" -> "tr-TR"
                else -> "en-US"
            }
        }
    }
}
