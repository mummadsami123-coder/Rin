package com.example.phone

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent

class PhoneControlManager(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var isTorchOn = false

    fun toggleFlashlight(forceState: Boolean? = null): Boolean {
        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return false

            val newState = forceState ?: !isTorchOn
            cameraManager.setTorchMode(cameraId, newState)
            isTorchOn = newState
            return true
        } catch (e: Exception) {
            Log.e("PhoneControlManager", "Error toggling flashlight", e)
            return false
        }
    }

    fun isFlashlightOn(): Boolean = isTorchOn

    fun adjustVolume(increase: Boolean): Int {
        val direction = if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }

    fun setVolumeLevel(percent: Int): Int {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = ((percent / 100f) * maxVol).toInt().coerceIn(0, maxVol)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
        return target
    }

    fun controlMedia(action: MediaAction): Boolean {
        val keyCode = when (action) {
            MediaAction.PLAY_PAUSE -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            MediaAction.PLAY -> KeyEvent.KEYCODE_MEDIA_PLAY
            MediaAction.PAUSE -> KeyEvent.KEYCODE_MEDIA_PAUSE
            MediaAction.NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT
            MediaAction.PREVIOUS -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
        }

        return try {
            val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            audioManager.dispatchMediaKeyEvent(eventDown)
            val eventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
            audioManager.dispatchMediaKeyEvent(eventUp)
            true
        } catch (e: Exception) {
            Log.e("PhoneControlManager", "Error controlling media", e)
            false
        }
    }

    fun openSettingsScreen(type: SettingsScreenType): Boolean {
        val action = when (type) {
            SettingsScreenType.MAIN -> Settings.ACTION_SETTINGS
            SettingsScreenType.WIFI -> Settings.ACTION_WIFI_SETTINGS
            SettingsScreenType.BLUETOOTH -> Settings.ACTION_BLUETOOTH_SETTINGS
            SettingsScreenType.SOUND -> Settings.ACTION_SOUND_SETTINGS
            SettingsScreenType.DISPLAY -> Settings.ACTION_DISPLAY_SETTINGS
            SettingsScreenType.ACCESSIBILITY -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            SettingsScreenType.NOTIFICATION_LISTENER -> Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
        }

        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("PhoneControlManager", "Error opening settings screen: $type", e)
            false
        }
    }

    fun openWebSearch(query: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("PhoneControlManager", "Error searching web", e)
            false
        }
    }

    fun sendSms(phoneNumber: String, message: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("PhoneControlManager", "Error preparing SMS", e)
            false
        }
    }

    fun prepareWhatsAppMessage(phoneNumber: String = "", message: String): Boolean {
        return try {
            val cleanPhone = phoneNumber.replace(Regex("[^0-9]"), "")
            val uri = if (cleanPhone.isNotEmpty()) {
                Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // Fallback without package restriction if WhatsApp wasn't found directly
            try {
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(fallbackIntent, "Send Message").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                true
            } catch (ex: Exception) {
                Log.e("PhoneControlManager", "Error sharing message", ex)
                false
            }
        }
    }

    enum class MediaAction {
        PLAY_PAUSE, PLAY, PAUSE, NEXT, PREVIOUS
    }

    enum class SettingsScreenType {
        MAIN, WIFI, BLUETOOTH, SOUND, DISPLAY, ACCESSIBILITY, NOTIFICATION_LISTENER
    }
}
