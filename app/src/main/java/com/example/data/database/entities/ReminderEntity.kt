package com.example.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val timeInMillis: Long,
    val isCompleted: Boolean = false,
    val repeatMode: String = "none" // "none", "daily", "weekly"
)
