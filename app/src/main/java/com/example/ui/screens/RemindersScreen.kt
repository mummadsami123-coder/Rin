package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistant.MimiAssistant
import com.example.data.database.entities.ReminderEntity
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RemindersScreen(
    assistant: MimiAssistant,
    reminders: List<ReminderEntity>,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }

    BoxWithFAB(
        modifier = modifier,
        onFabClick = { showAddDialog = true }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Smart Reminders & Alarms",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Voice scheduled alarms and notifications with exact Android timing.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }

            if (reminders.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Alarm, contentDescription = null, tint = NeonCyan)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No reminders scheduled yet", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("Say 'Mimi mujhe 5 baje yaad dilana' or tap the + button below.", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(reminders) { reminder ->
                    ReminderItemCard(
                        reminder = reminder,
                        onDelete = {
                            scope.launch {
                                assistant.reminderManager.cancelReminder(reminder)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddReminderDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, minutes ->
                scope.launch {
                    val time = System.currentTimeMillis() + (minutes * 60 * 1000L)
                    assistant.reminderManager.scheduleReminder(title, time)
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
fun BoxWithFAB(
    modifier: Modifier = Modifier,
    onFabClick: () -> Unit,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxSize()) {
        content()
        FloatingActionButton(
            onClick = onFabClick,
            containerColor = NeonCyan,
            contentColor = DarkBackground,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_reminder_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Reminder")
        }
    }
}

@Composable
fun ReminderItemCard(
    reminder: ReminderEntity,
    onDelete: () -> Unit
) {
    val dateStr = remember(reminder.timeInMillis) {
        val sdf = SimpleDateFormat("h:mm a, EEE d MMM", Locale.getDefault())
        sdf.format(Date(reminder.timeInMillis))
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (reminder.isCompleted) DarkBorder else NeonCyan.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (reminder.isCompleted) Icons.Default.CheckCircle else Icons.Default.Alarm,
                    contentDescription = null,
                    tint = if (reminder.isCompleted) FuturisticGreen else NeonCyan
                )
                Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                Column {
                    Text(
                        text = reminder.title,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = dateStr,
                        color = if (reminder.isCompleted) TextMuted else ElectricPurple,
                        fontSize = 12.sp
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = FuturisticPink)
            }
        }
    }
}

@Composable
fun AddReminderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var minutes by remember { mutableIntStateOf(5) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceCard,
        title = {
            Text("Set New Reminder", color = NeonCyan, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("What to remember? (e.g. Call Ali, Take meds)", color = TextMuted) },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Remind in:", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(5, 15, 30, 60).forEach { mins ->
                        Surface(
                            onClick = { minutes = mins },
                            shape = RoundedCornerShape(8.dp),
                            color = if (minutes == mins) NeonCyan else DarkSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${mins}m",
                                color = if (minutes == mins) DarkBackground else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) onConfirm(title.trim(), minutes)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Schedule", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
