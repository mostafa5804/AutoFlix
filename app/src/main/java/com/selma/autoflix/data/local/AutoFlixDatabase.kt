package com.selma.autoflix.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ChannelEntity::class, SavedSiteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AutoFlixDatabase : RoomDatabase() {

    abstract fun channelDao(): ChannelDao
    abstract fun savedSiteDao(): SavedSiteDao

    companion object {
        private const val DATABASE_NAME = "autoflix_room.db"

        @Volatile
        private var instance: AutoFlixDatabase? = null

        fun getInstance(context: Context): AutoFlixDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AutoFlixDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
            }
        }
    }
}
