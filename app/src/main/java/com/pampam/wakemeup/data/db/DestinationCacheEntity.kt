package com.pampam.wakemeup.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "destinationCache")
data class DestinationCacheEntity(
    @PrimaryKey val placeId: String,
    val primaryText: String,
    val secondaryText: String,
    val latitude: Double,
    val longitude: Double,
    val time: Long,
    val recent: Boolean
)