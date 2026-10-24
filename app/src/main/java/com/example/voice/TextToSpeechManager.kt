package com.example.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<VoiceInfo>>(emptyList())
    val availableVoices: StateFlow<List<VoiceInfo>> = _availableVoices.asStateFlow()

    data class VoiceInfo(
        val name: String,
        val locale: String,
        val gender: String, // "Female", "Male", "Natural"
        val isNetwork: Boolean
    )

    private var currentPitch: Float = 1.15f
    private var currentSpeed: Float = 1.0f
    private var currentGender: String = "Female"
    private var currentLanguage: String = "English"
    private var preferredVoiceName: String = ""

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            setupVoice()
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    abandonAudioFocus()
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    abandonAudioFocus()
                }
            })
        } else {
            Log.e("TextToSpeechManager", "TTS initialization failed with code: $status")
        }
    }

    fun setupVoice() {
        val engine = tts ?: return
        try {
            val targetLocale = SettingsRepository.getLocaleForLanguage(currentLanguage)
            val langResult = engine.setLanguage(targetLocale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.language = Locale.US
            }

            val voices = engine.voices
            val voiceList = mutableListOf<VoiceInfo>()

            var bestMatchedVoice: Voice? = null

            voices?.forEach { voice ->
                val nameLower = voice.name.lowercase()
                val isFemale = nameLower.contains("female") ||
                        nameLower.contains("f0") ||
                        nameLower.contains("woman") ||
                        nameLower.contains("eva") ||
                        nameLower.contains("samantha") ||
                        nameLower.contains("zira") ||
                        nameLower.contains("sfg") ||
                        nameLower.contains("hie")

                val isMale = nameLower.contains("male") ||
                        nameLower.contains("m0") ||
                        nameLower.contains("man") ||
                        nameLower.contains("david") ||
                        nameLower.contains("guy")

                val gender = when {
                    isFemale -> "Female"
                    isMale -> "Male"
                    else -> "Natural"
                }

                voiceList.add(
                    VoiceInfo(
                        name = voice.name,
                        locale = voice.locale.displayName,
                        gender = gender,
                        isNetwork = voice.isNetworkConnectionRequired
                    )
                )

                if (preferredVoiceName.isNotEmpty() && voice.name == preferredVoiceName) {
                    bestMatchedVoice = voice
                } else if (bestMatchedVoice == null) {
                    val matchesGender = when (currentGender) {
                        "Male" -> isMale
                        "Female" -> isFemale
                        else -> true
                    }
                    val matchesLocale = voice.locale.language == targetLocale.language
                    if (matchesLocale && matchesGender) {
                        bestMatchedVoice = voice
                    }
                }
            }

            _availableVoices.value = voiceList

            if (bestMatchedVoice != null) {
                engine.voice = bestMatchedVoice
            }

            engine.setPitch(currentPitch)
            engine.setSpeechRate(currentSpeed)
        } catch (e: Exception) {
            Log.e("TextToSpeechManager", "Error setting up voice: ${e.message}")
            engine.language = Locale.US
        }
    }

    fun updateConfig(
        pitch: Float,
        speed: Float,
        voiceName: String = "",
        gender: String = "Female",
        language: String = "English"
    ) {
        currentPitch = pitch
        currentSpeed = speed
        preferredVoiceName = voiceName
        currentGender = gender
        currentLanguage = language

        tts?.let { engine ->
            engine.setPitch(pitch)
            engine.setSpeechRate(speed)
            val targetLocale = SettingsRepository.getLocaleForLanguage(language)
            try {
                engine.language = targetLocale
            } catch (e: Exception) {
                // Ignore fallback
            }

            if (voiceName.isNotEmpty()) {
                engine.voices?.find { it.name == voiceName }?.let {
                    engine.voice = it
                }
            } else {
                setupVoice()
            }
        }
    }

    fun setVoiceGender(gender: String) {
        currentGender = gender
        updateConfig(currentPitch, currentSpeed, preferredVoiceName, gender, currentLanguage)
    }

    fun setLanguage(language: String) {
        currentLanguage = language
        updateConfig(currentPitch, currentSpeed, preferredVoiceName, currentGender, language)
    }

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        if (muted) {
            stop()
        }
    }

    fun toggleMute(): Boolean {
        val newState = !_isMuted.value
        setMuted(newState)
        return newState
    }

    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (_isMuted.value) {
            Log.d("TextToSpeechManager", "Mimi is muted, skipping speech audio output")
            _isSpeaking.value = false
            onComplete?.invoke()
            return
        }

        if (!isInitialized || tts == null) {
            Log.w("TextToSpeechManager", "TTS not initialized, ignoring speak request")
            return
        }

        stop()
        requestAudioFocus()

        val utteranceId = "utterance_${System.currentTimeMillis()}"
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        try {
            if (tts?.isSpeaking == true) {
                tts?.stop()
            }
            _isSpeaking.value = false
            abandonAudioFocus()
        } catch (e: Exception) {
            Log.e("TextToSpeechManager", "Error stopping TTS: ${e.message}")
        }
    }

    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener { /* handle duck */ }
                    .build()
                audioManager?.requestAudioFocus(audioFocusRequest!!)
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        } catch (e: Exception) {
            Log.e("TextToSpeechManager", "Error requesting audio focus", e)
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.e("TextToSpeechManager", "Error abandoning audio focus", e)
        }
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
