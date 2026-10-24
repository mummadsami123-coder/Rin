package com.example.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String, // "user" or "mimi"
    val text: String,
    val actionType: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
