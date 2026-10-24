package com.example.calls

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.example.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class RinPhoneCallReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                    val incomingNumber = try {
                        intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""
                    } catch (e: Exception) {
                        ""
                    }
                    scope.launch {
                        try {
                            val settingsRepo = SettingsRepository(context)
                            if (!settingsRepo.settings.value.announceCallers) return@launch

                            val contactName = if (incomingNumber.isNotBlank()) {
                                CallManager.resolveContactName(context, incomingNumber)
                            } else null

                            val announcement = if (!contactName.isNullOrBlank()) {
                                "RIN: Aapko $contactName ki call aa rahi hai."
                            } else {
                                "RIN: Aapko ek unknown number se call aa rahi hai."
                            }

                            _incomingCallAnnouncementFlow.emit(announcement)
                        } catch (e: Exception) {
                            Log.e("RinPhoneCallReceiver", "Error processing incoming call announcement", e)
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e("RinPhoneCallReceiver", "Failed onReceive handling", e)
        }
    }

    companion object {
        private val _incomingCallAnnouncementFlow = MutableSharedFlow<String>(extraBufferCapacity = 3)
        val incomingCallAnnouncementFlow = _incomingCallAnnouncementFlow.asSharedFlow()
    }
}
