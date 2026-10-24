package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistant.MimiAssistant
import com.example.data.database.AppDatabase
import com.example.data.database.entities.MemoryEntity
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
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.GraphicEq

@Composable
fun MemoryScreen(
    assistant: MimiAssistant,
    memories: List<MemoryEntity>,
    onOpenVoiceAssistant: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepo = assistant.settingsRepo
    val settings by settingsRepo.settings.collectAsState()

    var showAddMemoryDialog by remember { mutableStateOf(false) }

    var pitchSlider by remember(settings.voicePitch) { mutableFloatStateOf(settings.voicePitch) }
    var speedSlider by remember(settings.voiceSpeed) { mutableFloatStateOf(settings.voiceSpeed) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Voice & AI Memory Settings",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Customize Mimi's voice characteristics, gender, speaking language, and stored preferences.",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        // Dedicated Voice Assistant Launcher Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dedicated Voice Assistant Screen",
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Open full-screen Mimi with voice orb, live waveform, and conversational speech.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = onOpenVoiceAssistant,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = DarkBackground,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Open", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Voice Customization Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Mimi Voice & Audio Settings", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Voice Gender Selection
                    Text("Voice Persona / Gender:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Female", "Male", "Natural").forEach { gender ->
                            val selected = settings.voiceGender.equals(gender, ignoreCase = true)
                            Surface(
                                onClick = {
                                    settingsRepo.updateVoiceGender(gender)
                                    assistant.ttsManager.setVoiceGender(gender)
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) NeonCyan else DarkSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = gender,
                                    color = if (selected) DarkBackground else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Voice Style Selection
                    Text("Voice Clarity & Style:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Clear & Natural", "Standard", "High Quality").forEach { style ->
                            val selected = settings.voiceStyle == style
                            Surface(
                                onClick = {
                                    settingsRepo.updateVoiceStyle(style)
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) ElectricPurple else DarkSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = style,
                                    color = if (selected) TextPrimary else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Language Selector (Full Multilingual Grid)
                    Text("Speaking & Understanding Language:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        com.example.settings.SettingsRepository.SUPPORTED_LANGUAGES.chunked(3).forEach { chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                chunk.forEach { lang ->
                                    val isSelected = settings.language.equals(lang, ignoreCase = true)
                                    Surface(
                                        onClick = {
                                            settingsRepo.updateLanguage(lang)
                                            assistant.setLanguage(lang)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) NeonCyan else DarkSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = lang,
                                            color = if (isSelected) DarkBackground else TextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                                if (chunk.size < 3) {
                                    repeat(3 - chunk.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Pitch
                    Text("Voice Pitch: ${String.format("%.2f", pitchSlider)}", color = TextSecondary, fontSize = 12.sp)
                    Slider(
                        value = pitchSlider,
                        onValueChange = {
                            pitchSlider = it
                            settingsRepo.updateVoicePitch(it)
                            assistant.ttsManager.updateConfig(it, speedSlider, settings.preferredVoice)
                        },
                        valueRange = 0.8f..1.6f,
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                    )

                    // Speed
                    Text("Speech Speed: ${String.format("%.2f", speedSlider)}", color = TextSecondary, fontSize = 12.sp)
                    Slider(
                        value = speedSlider,
                        onValueChange = {
                            speedSlider = it
                            settingsRepo.updateVoiceSpeed(it)
                            assistant.ttsManager.updateConfig(pitchSlider, it, settings.preferredVoice)
                        },
                        valueRange = 0.8f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = ElectricPurple, activeTrackColor = ElectricPurple)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            val testPhrase = when (settings.language.lowercase()) {
                                "urdu" -> "جی! میں ممی ہوں، آپ کی پرسنل وائس اسسٹنٹ۔"
                                "roman urdu" -> "Ji! Main Mimi hoon, aapki personal Android voice assistant. Sab theek chal raha hai!"
                                "hindi" -> "नमस्ते! मैं मिमी हूँ, आपकी पर्सनल वॉयस असिस्टेंट।"
                                "arabic" -> "مرحبا! أنا ميمي، مساعدتك الصوتية الشخصية."
                                "spanish" -> "¡Hola! Soy Mimi, tu asistente personal de voz."
                                "french" -> "Bonjour! Je suis Mimi, votre assistante vocale personnelle."
                                "german" -> "Hallo! Ich bin Mimi, Ihre persönliche Sprachassistentin."
                                else -> "Hello! I am Mimi, your personal Android voice assistant. All systems ready!"
                            }
                            assistant.ttsManager.speak(testPhrase)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text("Test Mimi Voice", color = NeonCyan, fontSize = 13.sp)
                    }
                }
            }
        }

        // Wake Word Selector
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Activation Wake Phrase", color = ElectricPurple, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Current active phrase: ${settings.wakeWord}", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Hey Mimi", "Mimi", "Hey RIN").forEach { phrase ->
                            Surface(
                                onClick = { settingsRepo.updateWakeWord(phrase) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (settings.wakeWord == phrase) ElectricPurple else DarkSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = phrase,
                                    color = if (settings.wakeWord == phrase) TextPrimary else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Privacy & Call Announcements Toggles
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Smart Announcements & Privacy", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Announce Incoming Callers", color = TextPrimary, fontSize = 14.sp)
                            Text("Speaks 'Aapko [Contact] ki call aa rahi hai'", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = settings.announceCallers,
                            onCheckedChange = { settingsRepo.updateAnnounceCallers(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = DarkSurfaceVariant)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Announce Notifications", color = TextPrimary, fontSize = 14.sp)
                            Text("Voice readouts for WhatsApp and incoming messages", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = settings.announceNotifications,
                            onCheckedChange = { settingsRepo.updateAnnounceNotifications(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = DarkSurfaceVariant)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Lock Screen Privacy Mode", color = TextPrimary, fontSize = 14.sp)
                            Text("Hide message text on locked screen; announce only sender", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = settings.lockScreenPrivacy,
                            onCheckedChange = { settingsRepo.updateLockScreenPrivacy(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = FuturisticGreen, checkedTrackColor = DarkSurfaceVariant)
                        )
                    }
                }
            }
        }

        // Stored Memories Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = NeonCyan)
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Text("User-Controlled AI Memory (${memories.size})", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                IconButton(onClick = { showAddMemoryDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = NeonCyan)
                }
            }
        }

        if (memories.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No memory items saved", color = TextSecondary, fontSize = 13.sp)
                        Text("You can store custom facts, preferred nicknames, or routines.", color = TextMuted, fontSize = 11.sp)
                    }
                }
            }
        } else {
            items(memories) { mem ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mem.key, color = NeonCyan, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(mem.value, color = TextPrimary, fontSize = 14.sp)
                        }
                        IconButton(onClick = {
                            scope.launch {
                                AppDatabase.getDatabase(context).memoryDao().deleteMemory(mem)
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = FuturisticPink)
                        }
                    }
                }
            }
        }

        // Danger Zone: Clear History
        item {
            Button(
                onClick = {
                    assistant.clearConversationHistory()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B1218)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = FuturisticPink)
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text("Clear Conversation History", color = FuturisticPink, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showAddMemoryDialog) {
        var key by remember { mutableStateOf("") }
        var value by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddMemoryDialog = false },
            containerColor = DarkSurfaceCard,
            title = { Text("Add Memory Item", color = NeonCyan, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        placeholder = { Text("Topic (e.g. Favorite Friend, City)", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        placeholder = { Text("Value (e.g. Ali, Lahore)", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (key.isNotBlank() && value.isNotBlank()) {
                            scope.launch {
                                AppDatabase.getDatabase(context).memoryDao().insertMemory(
                                    MemoryEntity(key = key.trim(), value = value.trim(), category = "user")
                                )
                                showAddMemoryDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Save Memory", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMemoryDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
