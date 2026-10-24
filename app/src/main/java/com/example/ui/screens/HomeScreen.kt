package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistant.MimiAssistant
import com.example.data.database.entities.ConversationEntity
import com.example.service.FloatingMimiService
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.FuturisticGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    assistant: MimiAssistant,
    conversations: List<ConversationEntity>,
    onOpenVoiceAssistant: () -> Unit,
    onNavigateToTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by assistant.settingsRepo.settings.collectAsState()
    val isAssistantOn by assistant.isAssistantOn.collectAsState()
    val isSpeaking by assistant.ttsManager.isSpeaking.collectAsState()
    var isFloatingActive by remember { mutableStateOf(FloatingMimiService.isRunning) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top App Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RIN AI",
                        color = NeonCyan,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Mimi Personal Voice Assistant",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isAssistantOn) FuturisticGreen.copy(alpha = 0.15f) else DarkSurfaceVariant,
                    border = BorderStroke(1.dp, if (isAssistantOn) FuturisticGreen.copy(alpha = 0.5f) else DarkBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isAssistantOn) FuturisticGreen else TextMuted)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAssistantOn) "Mimi Online" else "Mimi Standby",
                            color = if (isAssistantOn) FuturisticGreen else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Welcome Hero Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.6f), ElectricPurple.copy(alpha = 0.6f)))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Hello, I'm Mimi.",
                        color = TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "How can I help you today?",
                        color = NeonCyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Your natural voice companion. Speaks Urdu, Roman Urdu, Hindi, English, and more. Control apps, phone settings, and ask anything.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Action: Open Dedicated Voice Assistant Screen
                    Button(
                        onClick = onOpenVoiceAssistant,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_voice_assistant_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = DarkBackground,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open Voice Assistant",
                            color = DarkBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = DarkBackground,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Voice Status & Quick Profile Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Active Voice Profile",
                        color = ElectricPurple,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ProfileAttribute(label = "Language", value = settings.language)
                        ProfileAttribute(label = "Voice", value = "${settings.voiceGender} • ${settings.voiceStyle}")
                        ProfileAttribute(label = "Wake Phrase", value = settings.wakeWord)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Floating Orb Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Floating Mimi Orb on Home Screen",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Access Mimi from any app or home screen",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Switch(
                            checked = isFloatingActive,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        ).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } else {
                                        FloatingMimiService.start(context)
                                        isFloatingActive = true
                                    }
                                } else {
                                    FloatingMimiService.stop(context)
                                    isFloatingActive = false
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = NeonCyan.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkSurfaceCard
                            )
                        )
                    }
                }
            }
        }

        // Quick Navigation Tiles
        item {
            Text(
                text = "Assistant Features",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureTile(
                    title = "Phone Controls",
                    description = "Flashlight, Apps, Volume",
                    icon = Icons.Default.PhoneAndroid,
                    accentColor = NeonCyan,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToTab(1) }
                )

                FeatureTile(
                    title = "Smart Alerts",
                    description = "WhatsApp & Notifications",
                    icon = Icons.Default.Notifications,
                    accentColor = ElectricPurple,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToTab(2) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeatureTile(
                    title = "Reminders",
                    description = "Voice-activated tasks",
                    icon = Icons.Default.Alarm,
                    accentColor = FuturisticGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToTab(3) }
                )

                FeatureTile(
                    title = "Voice Settings",
                    description = "Gender, pitch, speed",
                    icon = Icons.Default.Psychology,
                    accentColor = NeonCyan,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToTab(4) }
                )
            }
        }

        // Recent Conversation Preview
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Interactions",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Open Voice Screen",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onOpenVoiceAssistant() }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (conversations.isEmpty()) {
                        Text(
                            text = "No recent conversations. Tap 'Open Voice Assistant' or say 'Hello Mimi' to speak!",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    } else {
                        conversations.takeLast(2).reversed().forEach { msg ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = if (msg.role == "user") "You: " else "Mimi: ",
                                    color = if (msg.role == "user") NeonCyan else ElectricPurple,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = msg.text,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileAttribute(label: String, value: String) {
    Column {
        Text(text = label, color = TextMuted, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FeatureTile(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = BorderStroke(1.dp, DarkBorder),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}
