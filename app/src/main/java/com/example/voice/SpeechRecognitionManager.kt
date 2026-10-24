package com.example.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpeechRecognitionManager(
    private val context: Context,
    private val onResultReceived: (String) -> Unit,
    private val onErrorReceived: (String) -> Unit,
    private val onSpeechEnded: (() -> Unit)? = null
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        mainHandler.post {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    speechRecognizer?.destroy()
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(createListener())
                    }
                    Log.d("SpeechRecognitionManager", "SpeechRecognizer initialized successfully")
                } else {
                    Log.e("SpeechRecognitionManager", "Speech recognition not available on device")
                }
            } catch (e: Exception) {
                Log.e("SpeechRecognitionManager", "Error initializing recognizer", e)
            }
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
                _partialText.value = ""
                Log.d("SpeechRecognitionManager", "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                _isListening.value = true
                Log.d("SpeechRecognitionManager", "Beginning of speech detected")
            }

            override fun onRmsChanged(rmsdB: Float) {
                _rmsDb.value = (rmsdB.coerceAtLeast(0f) / 10f).coerceIn(0f, 1f)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _isListening.value = false
                Log.d("SpeechRecognitionManager", "End of speech detected")
                onSpeechEnded?.invoke()
            }

            override fun onError(error: Int) {
                _isListening.value = false
                _rmsDb.value = 0f

                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission needed"
                    SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                    else -> "Speech recognition error ($error)"
                }
                Log.d("SpeechRecognitionManager", "Recognition status: $errorMsg ($error)")

                if (error != SpeechRecognizer.ERROR_NO_MATCH &&
                    error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT &&
                    error != SpeechRecognizer.ERROR_CLIENT
                ) {
                    onErrorReceived(errorMsg)
                }
            }

            override fun onResults(results: Bundle?) {
                _isListening.value = false
                _rmsDb.value = 0f
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim() ?: ""
                Log.d("SpeechRecognitionManager", "Speech results: $text")
                if (text.isNotBlank()) {
                    _partialText.value = text
                    onResultReceived(text)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim() ?: ""
                if (text.isNotBlank()) {
                    _partialText.value = text
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun startListening(languageName: String = "English") {
        mainHandler.post {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                Log.w("SpeechRecognitionManager", "Cannot start speech recognition: RECORD_AUDIO not granted")
                _isListening.value = false
                onErrorReceived("Microphone permission required")
                return@post
            }

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                onErrorReceived("Speech recognition is not available on this device")
                return@post
            }

            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }
            }

            try {
                // Cancel any previous turn first to avoid ERROR_CLIENT or RECOGNIZER_BUSY
                speechRecognizer?.cancel()

                val languageTag = SettingsRepository.getSpeechRecognitionTag(languageName)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
                    putExtra(
                        "android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES",
                        arrayOf("en-US", "ur-PK", "hi-IN", "ar-SA", "es-ES", "fr-FR", "de-DE")
                    )
                }

                speechRecognizer?.startListening(intent)
                _isListening.value = true
                _partialText.value = ""
                Log.d("SpeechRecognitionManager", "Listening started for language: $languageTag")
            } catch (e: Exception) {
                Log.e("SpeechRecognitionManager", "Error starting listening", e)
                _isListening.value = false
                onErrorReceived("Could not activate microphone: ${e.message}")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e("SpeechRecognitionManager", "Error stopping listening", e)
            } finally {
                _isListening.value = false
                _rmsDb.value = 0f
            }
        }
    }

    fun cancel() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.e("SpeechRecognitionManager", "Error canceling speech recognizer", e)
            } finally {
                _isListening.value = false
                _rmsDb.value = 0f
            }
        }
    }

    fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.e("SpeechRecognitionManager", "Error destroying speech recognizer", e)
            } finally {
                _isListening.value = false
            }
        }
    }
}
