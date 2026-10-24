package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.actions.RinAction
import com.example.assistant.MimiAssistant
import com.example.data.database.AppDatabase
import com.example.data.database.entities.NotificationRecordEntity
import com.example.notifications.RinNotificationListener
import com.example.phone.PhoneControlManager
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.FuturisticGreen
import com.example.ui.theme.FuturisticPink
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationsScreen(
    assistant: MimiAssistant,
    notifications: List<NotificationRecordEntity>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isAccessGranted = RinNotificationListener.isNotificationAccessGranted(context)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Notification Reader",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Smart voice announcements for WhatsApp, SMS, and apps.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
                if (notifications.isNotEmpty()) {
                    IconButton(onClick = {
                        scope.launch {
                            AppDatabase.getDatabase(context).notificationDao().clearAll()
                        }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear", tint = FuturisticPink)
                    }
                }
            }
        }

        // Access Status Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAccessGranted) DarkSurfaceCard else Color(0xFF23121E)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isAccessGranted) FuturisticGreen.copy(alpha = 0.5f) else FuturisticPink.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isAccessGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = "Status",
                            tint = if (isAccessGranted) FuturisticGreen else FuturisticPink
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text(
                            text = if (isAccessGranted) "Notification Access Granted" else "Notification Access Required",
                            color = if (isAccessGranted) FuturisticGreen else FuturisticPink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isAccessGranted)
                            "RIN AI Mimi is actively listening for incoming notifications and will announce them respectfully."
                        else
                            "To read and announce notifications from WhatsApp and messages, enable RIN AI in Android Notification Access settings.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    if (!isAccessGranted) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                assistant.phoneControl.openSettingsScreen(PhoneControlManager.SettingsScreenType.NOTIFICATION_LISTENER)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FuturisticPink),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Open Notification Settings", color = TextPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Action Buttons Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        assistant.processCommand("mere messages parho")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("read_aloud_btn")
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Read Aloud", tint = NeonCyan)
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Text("Read Aloud", color = NeonCyan, fontSize = 13.sp)
                }
            }
        }

        // History items
        item {
            Text(
                text = "Captured History (${notifications.size})",
                color = ElectricPurple,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        if (notifications.isEmpty()) {
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
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = TextMuted)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No notifications captured yet", color = TextSecondary, fontSize = 14.sp)
                        Text("Incoming messages will appear here.", color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(notifications) { notif ->
                NotificationItemCard(notif)
            }
        }
    }
}

@Composable
fun NotificationItemCard(notification: NotificationRecordEntity) {
    val dateStr = remember(notification.timestamp) {
        val sdf = SimpleDateFormat("h:mm a, dd MMM", Locale.getDefault())
        sdf.format(Date(notification.timestamp))
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeonCyan.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = notification.appName,
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = dateStr,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            if (notification.title.isNotBlank()) {
                Text(
                    text = notification.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
            if (notification.content.isNotBlank()) {
                Text(
                    text = notification.content,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}
