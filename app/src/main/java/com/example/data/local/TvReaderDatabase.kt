package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [BookEntity::class, BookmarkEntity::class, NetworkSourceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TvReaderDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun networkSourceDao(): NetworkSourceDao

    companion object {
        @Volatile
        private var instance: TvReaderDatabase? = null

        fun getInstance(context: Context): TvReaderDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TvReaderDatabase::class.java,
                    "tv_reader.db"
                ).build().also { instance = it }
            }
        }
    }
}
