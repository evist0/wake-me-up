package com.pampam.wakemeup.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        DestinationCacheEntity::class,
        RemotePredictionCacheEntity::class,
        SearchQueryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getLastDestinationDao(): DestinationCacheDao
    abstract fun getRemotePredictionCacheDao(): RemotePredictionCacheDao
}