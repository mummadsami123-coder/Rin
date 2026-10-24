package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistant.MimiAssistant
import com.example.data.database.AppDatabase
import com.example.settings.SettingsRepository
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.PhoneControlScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.VoiceAssistantScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RinAiTheme
import com.example.ui.theme.TextMuted

enum class RinAiTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    PHONE("Phone", Icons.Default.PhoneAndroid),
    NOTIFICATIONS("Alerts", Icons.Default.Notifications),
    REMINDERS("Remind", Icons.Default.Alarm),
    SETTINGS("Settings", Icons.Default.Psychology),
    PERMISSIONS("Access", Icons.Default.Security)
}

class MainActivity : ComponentActivity() {
    private var assistantInstance: MimiAssistant? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsRepo = SettingsRepository(this)
        val assistant = MimiAssistant(this, settingsRepo)
        assistantInstance = assistant

        setContent {
            RinAiTheme {
                RinAiApp(assistant = assistant)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        assistantInstance?.destroy()
    }
}

@Composable
fun RinAiApp(assistant: MimiAssistant) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(RinAiTab.HOME) }
    var isVoiceAssistantOpen by remember { mutableStateOf(false) }

    // Database flows
    val db = remember { AppDatabase.getDatabase(context) }
    val conversations by db.conversationDao().getAllMessages().collectAsState(initial = emptyList())
    val notifications by db.notificationDao().getAllNotifications().collectAsState(initial = emptyList())
    val reminders by db.reminderDao().getAllReminders().collectAsState(initial = emptyList())
    val memories by db.memoryDao().getAllMemories().collectAsState(initial = emptyList())

    // Permission launchers
    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            assistant.startListening()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Updated in UI */ }

    val contactsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Updated in UI */ }

    val callsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Updated in UI */ }

    val notifPostLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Android 13+ notification permission */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPostLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Fullscreen dedicated Voice Assistant Screen
    if (isVoiceAssistantOpen) {
        BackHandler {
            isVoiceAssistantOpen = false
        }

        VoiceAssistantScreen(
            assistant = assistant,
            conversations = conversations,
            onBackClick = { isVoiceAssistantOpen = false },
            onRequestMicPermission = {
                micLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        // Handle back button: return to HOME tab before exiting
        BackHandler(enabled = currentTab != RinAiTab.HOME) {
            currentTab = RinAiTab.HOME
        }

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = DarkBackground,
            bottomBar = {
                NavigationBar(
                    containerColor = DarkSurfaceCard,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("rin_ai_bottom_nav")
                ) {
                    RinAiTab.values().forEach { tab ->
                        val selected = currentTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    tint = if (selected) NeonCyan else TextMuted
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    fontSize = 10.sp,
                                    color = if (selected) NeonCyan else TextMuted
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = NeonCyan.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            val screenModifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)

            when (currentTab) {
                RinAiTab.HOME -> {
                    HomeScreen(
                        assistant = assistant,
                        conversations = conversations,
                        onOpenVoiceAssistant = {
                            isVoiceAssistantOpen = true
                        },
                        onNavigateToTab = { tabIndex ->
                            if (tabIndex in RinAiTab.values().indices) {
                                currentTab = RinAiTab.values()[tabIndex]
                            }
                        },
                        modifier = screenModifier
                    )
                }

                RinAiTab.PHONE -> {
                    PhoneControlScreen(
                        assistant = assistant,
                        modifier = screenModifier
                    )
                }

                RinAiTab.NOTIFICATIONS -> {
                    NotificationsScreen(
                        assistant = assistant,
                        notifications = notifications,
                        modifier = screenModifier
                    )
                }

                RinAiTab.REMINDERS -> {
                    RemindersScreen(
                        assistant = assistant,
                        reminders = reminders,
                        modifier = screenModifier
                    )
                }

                RinAiTab.SETTINGS -> {
                    MemoryScreen(
                        assistant = assistant,
                        memories = memories,
                        onOpenVoiceAssistant = {
                            isVoiceAssistantOpen = true
                        },
                        modifier = screenModifier
                    )
                }

                RinAiTab.PERMISSIONS -> {
                    PermissionsScreen(
                        phoneControl = assistant.phoneControl,
                        onRequestMicrophone = {
                            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onRequestContacts = {
                            contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                        },
                        onRequestCalls = {
                            callsLauncher.launch(Manifest.permission.CALL_PHONE)
                        },
                        onRequestCamera = {
                            cameraLauncher.launch(Manifest.permission.CAMERA)
                        },
                        modifier = screenModifier
                    )
                }
            }
        }
    }
}
