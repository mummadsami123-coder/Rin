package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.database.daos.ConversationDao
import com.example.data.database.daos.MemoryDao
import com.example.data.database.daos.NotificationDao
import com.example.data.database.daos.ReminderDao
import com.example.data.database.entities.ConversationEntity
import com.example.data.database.entities.MemoryEntity
import com.example.data.database.entities.NotificationRecordEntity
import com.example.data.database.entities.ReminderEntity

@Database(
    entities = [
        ConversationEntity::class,
        ReminderEntity::class,
        NotificationRecordEntity::class,
        MemoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun reminderDao(): ReminderDao
    abstract fun notificationDao(): NotificationDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rin_ai_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
