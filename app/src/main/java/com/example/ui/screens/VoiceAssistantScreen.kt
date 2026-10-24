package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistant.AssistantStatus
import com.example.assistant.MimiAssistant
import com.example.data.database.entities.ConversationEntity
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
fun VoiceAssistantScreen(
    assistant: MimiAssistant,
    conversations: List<ConversationEntity>,
    onBackClick: () -> Unit,
    onRequestMicPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status by assistant.status.collectAsState()
    val activeTranscript by assistant.activeTranscript.collectAsState()
    val rmsDb by assistant.speechManager?.rmsDb?.collectAsState() ?: remember { mutableStateOf(0f) }
    val isTtsSpeaking by assistant.ttsManager.isSpeaking.collectAsState()
    val isAssistantOn by assistant.isAssistantOn.collectAsState()
    val isMuted by assistant.isMuted.collectAsState()
    val isMicActive by assistant.speechManager?.isListening?.collectAsState() ?: remember { mutableStateOf(false) }

    var textInput by remember { mutableStateOf("") }

    val quickCommands = listOf(
        "Tell me a story",
        "What can you do?",
        "YouTube kholo",
        "Flashlight on karo",
        "Volume barhao",
        "Stop",
        "Chup"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.testTag("voice_assistant_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = NeonCyan
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Mimi Voice Assistant",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                // Live Android Microphone Privacy Indicator
                if (isMicActive) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(FuturisticGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Microphone Active",
                            color = FuturisticGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Text(
                        text = if (isMuted) "Muted" else if (isAssistantOn) "Online & Ready" else "Tap Mic to Talk",
                        color = if (isMuted) FuturisticPink else TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Top Quick Mute Button
            IconButton(
                onClick = { assistant.toggleMute() },
                modifier = Modifier.testTag("voice_assistant_mute_btn")
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Mute",
                    tint = if (isMuted) FuturisticPink else NeonCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Large Animated AI Orb
        AnimatedAiOrb(
            status = if (!isAssistantOn) AssistantStatus.IDLE else status,
            audioLevel = if (isMicActive) rmsDb else 0f,
            size = 200.dp,
            onClick = {
                if (status == AssistantStatus.LISTENING) {
                    assistant.stopListening()
                } else if (status == AssistantStatus.SPEAKING) {
                    assistant.stopSpeaking()
                } else {
                    onRequestMicPermission()
                    assistant.startListening()
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Live Audio Waveform
        VoiceWaveform(
            isActive = isMicActive || status == AssistantStatus.SPEAKING,
            audioLevel = rmsDb,
            maxHeight = 24.dp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Status Banner
        val (statusText, statusColor) = when {
            isMuted -> "Mimi is Muted (Audio Silenced)" to FuturisticPink
            status == AssistantStatus.LISTENING -> "Listening to your voice... Speak now" to NeonCyan
            status == AssistantStatus.THINKING -> "Thinking..." to ElectricPurple
            status == AssistantStatus.SPEAKING -> "Mimi is speaking (Say 'Stop' or 'Chup' to silence)" to FuturisticPink
            status == AssistantStatus.EXECUTING -> "Executing phone command..." to NeonCyan
            else -> "Tap the microphone below to talk with Mimi" to FuturisticGreen
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

        // Live Transcript Card
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

        // Quick Suggestion Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickCommands) { cmd ->
                CommandChip(
                    text = cmd,
                    onClick = {
                        if (cmd.equals("stop", ignoreCase = true) || cmd.equals("chup", ignoreCase = true)) {
                            assistant.stopSpeaking()
                        } else {
                            assistant.processCommand(cmd)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Conversation List
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

        // Bottom Controls: Text field + Stop + Mic button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Ask Mimi anything or type command...", color = TextMuted, fontSize = 13.sp) },
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
                    .testTag("voice_screen_text_input"),
                trailingIcon = {
                    if (textInput.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val cmd = textInput.trim()
                                textInput = ""
                                assistant.processCommand(cmd)
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = NeonCyan)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Stop speaking button (Say Stop or tap)
            if (isTtsSpeaking || status == AssistantStatus.SPEAKING) {
                IconButton(
                    onClick = { assistant.stopSpeaking() },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(FuturisticPink)
                        .testTag("voice_screen_stop_btn")
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            // Main Microphone Button
            val micBg = if (isMicActive) {
                Brush.linearGradient(listOf(FuturisticPink, ElectricPurple))
            } else {
                Brush.linearGradient(listOf(NeonCyan, ElectricPurple))
            }

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(micBg)
                    .border(2.dp, NeonCyan.copy(alpha = 0.7f), CircleShape)
                    .testTag("voice_screen_mic_btn"),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        if (isMicActive) {
                            assistant.stopListening()
                        } else {
                            onRequestMicPermission()
                            assistant.startListening()
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isMicActive) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Microphone",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
