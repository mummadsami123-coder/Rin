package com.example.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isReadAloud: Boolean = false
)
