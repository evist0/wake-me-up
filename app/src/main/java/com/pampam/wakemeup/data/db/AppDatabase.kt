package com.pampam.wakemeup.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [RecentDestinationEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getLastDestinationDao(): RecentDestinationDao
}