package com.pampam.wakemeup.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RecentDestinationDao {

    @Query("SELECT * FROM recentDestination WHERE primaryText LIKE '%' || :query || '%'")
    fun getRecentDestinations(query: String): LiveData<List<RecentDestinationEntity>>

    @Query("SELECT * FROM recentDestination WHERE placeId = :placeId")
    fun getRecentDestinationById(placeId: String): RecentDestinationEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecentDestination(recentDestinationEntity: RecentDestinationEntity)

    @Query("SELECT EXISTS (SELECT 1 FROM recentDestination WHERE placeId = :placeId)")
    fun isDestinationExistsById(placeId: String): Boolean

    @Query("DELETE FROM recentDestination WHERE placeId = :placeId")
    fun deleteRecentDestinationById(placeId: String)
}