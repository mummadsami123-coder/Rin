package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.accessibility.RinAccessibilityService
import com.example.notifications.RinNotificationListener
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
fun PermissionsScreen(
    phoneControl: PhoneControlManager,
    onRequestMicrophone: () -> Unit,
    onRequestContacts: () -> Unit,
    onRequestCalls: () -> Unit,
    onRequestCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresh trigger that bumps on lifecycle resume
    var refreshCount by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshCount++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Dynamic checks evaluated against refreshCount
    val hasMic = remember(refreshCount) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    val hasContacts = remember(refreshCount) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }
    val hasCalls = remember(refreshCount) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
    }
    val hasCamera = remember(refreshCount) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    val hasNotifAccess = remember(refreshCount) {
        RinNotificationListener.isNotificationAccessGranted(context)
    }
    val hasAccessibility = remember(refreshCount) {
        RinAccessibilityService.isAccessibilityEnabled(context)
    }
    val hasOverlay = remember(refreshCount) {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
    }

    val totalGranted = listOf(hasMic, hasContacts, hasCalls, hasCamera, hasNotifAccess, hasAccessibility, hasOverlay).count { it }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Permissions & Privacy Dashboard",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "RIN AI Mimi requests permissions only when needed. Once granted, permissions remain active without repeated prompts.",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        // Summary Status Banner
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = BorderStroke(1.dp, if (hasMic) FuturisticGreen.copy(alpha = 0.5f) else DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (hasMic) FuturisticGreen.copy(alpha = 0.15f) else DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hasMic) Icons.Default.CheckCircle else Icons.Default.Security,
                            contentDescription = null,
                            tint = if (hasMic) FuturisticGreen else NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (hasMic) "Core Voice Permission Active" else "Microphone Permission Required",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$totalGranted of 7 permissions granted • Privacy secured",
                            color = if (hasMic) FuturisticGreen else TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Microphone Active Privacy Notice
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(FuturisticGreen)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Android Privacy Indicator: Whenever Mimi is actively listening, Android displays a green mic dot in the status bar and on the voice orb.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        item {
            PermissionCard(
                title = "Microphone (Voice Input)",
                description = "Required for speech recognition and conversational voice commands with Mimi.",
                icon = Icons.Default.Mic,
                isGranted = hasMic,
                buttonLabel = "Grant Microphone",
                onEnableClick = onRequestMicrophone
            )
        }

        item {
            PermissionCard(
                title = "Floating Shadow/Orb Overlay",
                description = "Keeps Mimi available as a floating shadow/orb on the Android home screen for quick voice commands.",
                icon = Icons.Default.TouchApp,
                isGranted = hasOverlay,
                buttonLabel = "Enable Overlay",
                onEnableClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                }
            )
        }

        item {
            PermissionCard(
                title = "Notification Listener Service",
                description = "Allows Mimi to read and announce incoming WhatsApp, SMS, and app notifications.",
                icon = Icons.Default.Notifications,
                isGranted = hasNotifAccess,
                buttonLabel = "Open Settings",
                onEnableClick = {
                    phoneControl.openSettingsScreen(PhoneControlManager.SettingsScreenType.NOTIFICATION_LISTENER)
                }
            )
        }

        item {
            PermissionCard(
                title = "Accessibility Automation Service",
                description = "Enables voice navigation commands such as 'Home screen par jao' and 'Back jao'.",
                icon = Icons.Default.TouchApp,
                isGranted = hasAccessibility,
                buttonLabel = "Open Accessibility",
                onEnableClick = {
                    phoneControl.openSettingsScreen(PhoneControlManager.SettingsScreenType.ACCESSIBILITY)
                }
            )
        }

        item {
            PermissionCard(
                title = "Camera & Flashlight",
                description = "Enables voice flashlight toggling ('Mimi, flashlight on karo').",
                icon = Icons.Default.CameraAlt,
                isGranted = hasCamera,
                buttonLabel = "Grant Camera",
                onEnableClick = onRequestCamera
            )
        }

        item {
            PermissionCard(
                title = "Contacts Access",
                description = "Allows caller ID identification and finding contacts ('Ali ko call karo').",
                icon = Icons.Default.Contacts,
                isGranted = hasContacts,
                buttonLabel = "Grant Contacts",
                onEnableClick = onRequestContacts
            )
        }

        item {
            PermissionCard(
                title = "Phone Calling",
                description = "Permits direct dialing when authorized by voice ('Call Mummy').",
                icon = Icons.Default.Call,
                isGranted = hasCalls,
                buttonLabel = "Grant Calling",
                onEnableClick = onRequestCalls
            )
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    buttonLabel: String = "Grant",
    onEnableClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = BorderStroke(
            1.dp,
            if (isGranted) FuturisticGreen.copy(alpha = 0.35f) else DarkBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isGranted) NeonCyan else ElectricPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                // Status Badge: When granted, show calm green Granted tag. No red warnings on granted items.
                if (isGranted) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = FuturisticGreen.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = FuturisticGreen,
                                modifier = Modifier
                                    .size(13.dp)
                                    .padding(end = 2.dp)
                            )
                            Text(
                                text = "Granted",
                                color = FuturisticGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurfaceVariant
                    ) {
                        Text(
                            text = "Optional",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(description, color = TextSecondary, fontSize = 12.sp)

            // Only show Grant action if not already granted!
            if (!isGranted) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onEnableClick,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.End)
                        .testTag("grant_button_${title.take(6).lowercase()}")
                ) {
                    Text(buttonLabel, color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
