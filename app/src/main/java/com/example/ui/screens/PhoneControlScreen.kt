package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apps.AppInfo
import com.example.assistant.MimiAssistant
import com.example.phone.PhoneControlManager
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
fun PhoneControlScreen(
    assistant: MimiAssistant,
    modifier: Modifier = Modifier
) {
    val phoneControl = assistant.phoneControl
    val appLauncher = assistant.appLauncher

    var isTorchOn by remember { mutableStateOf(phoneControl.isFlashlightOn()) }
    var volumeSlider by remember { mutableFloatStateOf(50f) }
    var appSearchQuery by remember { mutableStateOf("") }

    val installedApps = remember {
        try {
            appLauncher.getInstalledApps()
        } catch (e: Exception) {
            emptyList<AppInfo>()
        }
    }

    val filteredApps = installedApps.filter {
        it.name.contains(appSearchQuery, ignoreCase = true) ||
                it.packageName.contains(appSearchQuery, ignoreCase = true)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Phone Control & Applications",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Control hardware and launch installed apps with authorized Android APIs.",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        // Hardware Controls Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Hardware Controls",
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Flashlight Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                                contentDescription = "Flashlight",
                                tint = if (isTorchOn) FuturisticGreen else TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Flashlight / Torch", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(if (isTorchOn) "Active (Torch ON)" else "Inactive (Torch OFF)", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                        Button(
                            onClick = {
                                val ok = phoneControl.toggleFlashlight()
                                if (ok) isTorchOn = phoneControl.isFlashlightOn()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTorchOn) FuturisticGreen else DarkSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("toggle_torch_btn")
                        ) {
                            Text(if (isTorchOn) "Turn Off" else "Turn On", color = TextPrimary, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Volume Row
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Volume", tint = NeonCyan, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Music / Media Volume", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                            Row {
                                IconButton(onClick = {
                                    val level = phoneControl.adjustVolume(false)
                                    volumeSlider = (level * 10f).coerceIn(0f, 100f)
                                }) {
                                    Icon(Icons.Default.VolumeDown, contentDescription = "Volume Down", tint = TextSecondary)
                                }
                                IconButton(onClick = {
                                    val level = phoneControl.adjustVolume(true)
                                    volumeSlider = (level * 10f).coerceIn(0f, 100f)
                                }) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Volume Up", tint = NeonCyan)
                                }
                            }
                        }

                        Slider(
                            value = volumeSlider,
                            onValueChange = {
                                volumeSlider = it
                                phoneControl.setVolumeLevel(it.toInt())
                            },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonCyan,
                                activeTrackColor = NeonCyan,
                                inactiveTrackColor = DarkSurfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Media Playback Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Media Session Control", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { phoneControl.controlMedia(PhoneControlManager.MediaAction.PREVIOUS) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceVariant)
                            ) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = TextPrimary)
                            }
                            IconButton(
                                onClick = { phoneControl.controlMedia(PhoneControlManager.MediaAction.PLAY_PAUSE) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play/Pause", tint = DarkBackground)
                            }
                            IconButton(
                                onClick = { phoneControl.controlMedia(PhoneControlManager.MediaAction.NEXT) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceVariant)
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = TextPrimary)
                            }
                        }
                    }
                }
            }
        }

        // System Settings Shortcuts
        item {
            Text(
                text = "System Settings Shortcuts",
                color = ElectricPurple,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingsShortcutCard(
                    title = "Wi-Fi",
                    icon = Icons.Default.Wifi,
                    modifier = Modifier.weight(1f)
                ) {
                    phoneControl.openSettingsScreen(PhoneControlManager.SettingsScreenType.WIFI)
                }
                SettingsShortcutCard(
                    title = "Bluetooth",
                    icon = Icons.Default.Bluetooth,
                    modifier = Modifier.weight(1f)
                ) {
                    phoneControl.openSettingsScreen(PhoneControlManager.SettingsScreenType.BLUETOOTH)
                }
                SettingsShortcutCard(
                    title = "Settings",
                    icon = Icons.Default.Settings,
                    modifier = Modifier.weight(1f)
                ) {
                    phoneControl.openSettingsScreen(PhoneControlManager.SettingsScreenType.MAIN)
                }
            }
        }

        // Connected Applications Section
        item {
            Text(
                text = "Installed Applications (${installedApps.size})",
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = appSearchQuery,
                onValueChange = { appSearchQuery = it },
                placeholder = { Text("Search installed apps...", color = TextMuted, fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkSurfaceCard,
                    unfocusedContainerColor = DarkSurfaceCard
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_search_field")
            )
        }

        items(filteredApps.take(25)) { app ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        appLauncher.launchByPackage(app.packageName, app.name)
                    }
                    .testTag("app_item_${app.packageName.take(15)}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.name,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = app.packageName,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = {
                        appLauncher.launchByPackage(app.packageName, app.name)
                    }) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "Launch", tint = NeonCyan)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsShortcutCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = DarkSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = title, tint = NeonCyan, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}
