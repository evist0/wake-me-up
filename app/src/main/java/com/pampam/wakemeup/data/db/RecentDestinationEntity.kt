package com.pampam.wakemeup.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recentDestination")
data class RecentDestinationEntity(
    @PrimaryKey val placeId: String,
    val fullText: String
)