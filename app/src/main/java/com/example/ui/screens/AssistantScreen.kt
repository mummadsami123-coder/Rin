package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.assistant.AssistantStatus
import com.example.assistant.MimiAssistant
import com.example.data.database.entities.ConversationEntity
import com.example.service.FloatingMimiService
import com.example.ui.components.AnimatedAiOrb
import com.example.ui.components.CommandChip
import com.example.ui.components.VoiceWaveform
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.FuturisticGreen
import com.example.ui.theme.FuturisticPink
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AssistantScreen(
    assistant: MimiAssistant,
    conversations: List<ConversationEntity>,
    onRequestMicPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val status by assistant.status.collectAsState()
    val activeTranscript by assistant.activeTranscript.collectAsState()
    val rmsDb by assistant.speechManager?.rmsDb?.collectAsState() ?: remember { mutableStateOf(0f) }
    val isTtsSpeaking by assistant.ttsManager.isSpeaking.collectAsState()
    val isAssistantOn by assistant.isAssistantOn.collectAsState()
    val isMuted by assistant.isMuted.collectAsState()

    var isFloatingActive by remember { mutableStateOf(FloatingMimiService.isRunning) }
    var textInput by remember { mutableStateOf("") }

    val quickCommands = listOf(
        "Hello Mimi, YouTube kholo",
        "Hello Mimi, flashlight on karo",
        "Mimi, volume barhao",
        "Hello Mimi, Ali ko call karo",
        "Mimi, meri notifications parho",
        "Hello Mimi, mujhe ek story sunao",
        "Mimi, home screen par jao",
        "Mute",
        "Unmute"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        // Top Status & Master Controls Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.assistant_greeting_title),
                    color = NeonCyan,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isAssistantOn) "Say \"Hello Mimi\" or tap orb" else "Assistant is currently OFF",
                    color = if (isAssistantOn) FuturisticGreen else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Floating Orb Toggle
                Surface(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } else {
                            if (isFloatingActive) {
                                FloatingMimiService.stop(context)
                                isFloatingActive = false
                            } else {
                                FloatingMimiService.start(context)
                                isFloatingActive = true
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isFloatingActive) NeonCyan.copy(alpha = 0.2f) else DarkSurfaceVariant,
                    border = BorderStroke(1.dp, if (isFloatingActive) NeonCyan else DarkBorder),
                    modifier = Modifier.testTag("floating_orb_toggle_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = "Floating Orb",
                            tint = if (isFloatingActive) NeonCyan else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isFloatingActive) "Orb ON" else "Floating Orb",
                            color = if (isFloatingActive) NeonCyan else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Mute / Unmute Button
                Surface(
                    onClick = { assistant.toggleMute() },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isMuted) FuturisticPink.copy(alpha = 0.2f) else ElectricPurple.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, if (isMuted) FuturisticPink else ElectricPurple),
                    modifier = Modifier.testTag("mute_unmute_top_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            tint = if (isMuted) FuturisticPink else ElectricPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isMuted) "Muted" else "Voice ON",
                            color = if (isMuted) FuturisticPink else ElectricPurple,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Power ON / OFF Switch
                Surface(
                    onClick = {
                        onRequestMicPermission()
                        assistant.toggleAssistantPower()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isAssistantOn) FuturisticGreen.copy(alpha = 0.2f) else Color(0xFF23121E),
                    border = BorderStroke(1.dp, if (isAssistantOn) FuturisticGreen else FuturisticPink),
                    modifier = Modifier.testTag("assistant_power_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Power",
                            tint = if (isAssistantOn) FuturisticGreen else FuturisticPink,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAssistantOn) "ON" else "OFF",
                            color = if (isAssistantOn) FuturisticGreen else FuturisticPink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Large Animated AI Orb (Clickable)
        AnimatedAiOrb(
            status = if (!isAssistantOn) AssistantStatus.IDLE else status,
            audioLevel = if (!isAssistantOn) 0f else rmsDb,
            size = 190.dp,
            onClick = {
                if (!isAssistantOn) {
                    onRequestMicPermission()
                    assistant.setAssistantPower(true)
                } else if (status == AssistantStatus.LISTENING) {
                    assistant.stopListening()
                } else if (status == AssistantStatus.SPEAKING) {
                    assistant.stopSpeaking()
                } else {
                    onRequestMicPermission()
                    assistant.startListening()
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Status Indicator Pill
        val (statusText, statusColor) = when {
            !isAssistantOn -> "Assistant is OFF • Tap power or orb to turn ON" to FuturisticPink
            isMuted -> "Mimi is Muted (Audio Silenced) • Say Unmute or tap icon" to FuturisticPink
            status == AssistantStatus.LISTENING -> "Listening for voice • Say \"Hello Mimi\" or command" to NeonCyan
            status == AssistantStatus.THINKING -> "Thinking with RIN AI Brain..." to ElectricPurple
            status == AssistantStatus.EXECUTING -> "Executing phone action..." to NeonCyan
            status == AssistantStatus.SPEAKING -> "Mimi is speaking..." to FuturisticPink
            else -> "Assistant Active • Say \"Hello Mimi\" anytime" to FuturisticGreen
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurfaceCard,
            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Live Audio Waveform Animation
        VoiceWaveform(
            isActive = isAssistantOn && (status == AssistantStatus.LISTENING || status == AssistantStatus.SPEAKING),
            audioLevel = rmsDb,
            maxHeight = 22.dp
        )

        // Live Transcript / Spoken Text Card
        AnimatedVisibility(visible = activeTranscript.isNotBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Text(
                    text = "“$activeTranscript”",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Horizontal Quick Action Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickCommands) { cmd ->
                CommandChip(
                    text = cmd,
                    onClick = {
                        if (cmd.equals("mute", ignoreCase = true)) {
                            assistant.setMuted(true)
                        } else if (cmd.equals("unmute", ignoreCase = true)) {
                            assistant.setMuted(false)
                        } else {
                            assistant.handleUserSpeech(cmd)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Recent Conversation Stream
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(conversations.reversed()) { msg ->
                ConversationBubble(msg)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Bottom Controls Bar: Text input + Mute Toggle + Stop + Mic Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Ask Mimi or enter command...", color = TextMuted, fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkSurfaceCard,
                    unfocusedContainerColor = DarkSurfaceCard
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("command_input_field"),
                trailingIcon = {
                    if (textInput.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val cmd = textInput.trim()
                                textInput = ""
                                assistant.handleUserSpeech(cmd)
                            },
                            modifier = Modifier.testTag("send_command_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = NeonCyan)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Bottom Quick Mute / Unmute Toggle Button
            IconButton(
                onClick = { assistant.toggleMute() },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (isMuted) Color(0xFF2E121D) else DarkSurfaceVariant)
                    .border(1.dp, if (isMuted) FuturisticPink else DarkBorder, CircleShape)
                    .testTag("quick_mute_toggle_btn")
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Mute/Unmute",
                    tint = if (isMuted) FuturisticPink else NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Stop Speaking button if active
            if (isTtsSpeaking || status == AssistantStatus.SPEAKING) {
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = { assistant.stopSpeaking() },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(FuturisticPink)
                        .testTag("stop_speaking_button")
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Main Mic Button
            val micBg = when {
                !isAssistantOn -> Brush.linearGradient(listOf(Color(0xFF333333), Color(0xFF222222)))
                status == AssistantStatus.LISTENING -> Brush.linearGradient(listOf(FuturisticPink, ElectricPurple))
                else -> Brush.linearGradient(listOf(NeonCyan, ElectricPurple))
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(micBg)
                    .border(2.dp, if (isAssistantOn) NeonCyan.copy(alpha = 0.6f) else Color.Gray, CircleShape)
                    .testTag("mic_toggle_button"),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        if (!isAssistantOn) {
                            onRequestMicPermission()
                            assistant.setAssistantPower(true)
                        } else if (status == AssistantStatus.LISTENING) {
                            assistant.stopListening()
                        } else {
                            onRequestMicPermission()
                            assistant.startListening()
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (status == AssistantStatus.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Microphone",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ConversationBubble(message: ConversationEntity) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) Color(0xFF13203C) else DarkSurfaceCard
            ),
            border = BorderStroke(
                1.dp,
                if (isUser) NeonCyan.copy(alpha = 0.3f) else ElectricPurple.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isUser) "You" else "Mimi",
                        color = if (isUser) NeonCyan else ElectricPurple,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    if (!message.actionType.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NeonCyan.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = message.actionType,
                                color = NeonCyan,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.text,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
            }
        }
    }
}
