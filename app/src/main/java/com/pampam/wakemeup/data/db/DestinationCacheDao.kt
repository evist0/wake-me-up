package com.pampam.wakemeup.data.db

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DestinationCacheDao {

    @Query("SELECT * FROM destinationCache WHERE recent AND primaryText LIKE '%' || :query || '%' ORDER BY time DESC")
    fun getRecentDestinations(query: String): DataSource.Factory<Int, DestinationCacheEntity>

    @Query("SELECT * FROM destinationCache WHERE placeId = :placeId")
    fun getDestinationById(placeId: String): DestinationCacheEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDestination(destinationCacheEntity: DestinationCacheEntity)

    @Query("SELECT EXISTS (SELECT * FROM destinationCache WHERE placeId = :placeId)")
    fun isDestinationExistsById(placeId: String): Boolean

    @Query("UPDATE destinationCache SET recent = 0 WHERE placeId = :placeId")
    fun deleteRecentDestinationById(placeId: String)
}