package com.pampam.wakemeup.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recentDestination")
data class RecentDestinationEntity(
    @PrimaryKey val placeId: String,
    val primaryText: String,
    val secondaryText: String,
    val latitude: Double,
    val longitude: Double
)