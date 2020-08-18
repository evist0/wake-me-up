package com.pampam.wakemeup.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RecentDestinationDao {

    @Query("SELECT * FROM recentDestination")
    fun getRecentDestinations(): LiveData<List<RecentDestinationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecentDestination(recentDestinationEntity: RecentDestinationEntity)
}