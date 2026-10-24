package com.example.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.database.entities.NotificationRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationRecordEntity>>

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentNotifications(limit: Int): List<NotificationRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationRecordEntity): Long

    @Query("UPDATE notifications SET isReadAloud = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("DELETE FROM notifications")
    suspend fun clearAll()
}
