package com.pampam.wakemeup.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "remotePredictionCache",
    foreignKeys = [
        ForeignKey(
            entity = SearchQueryEntity::class,
            parentColumns = [
                "text",
                "originSectorLat",
                "originSectorLng"
            ],
            childColumns = [
                "text",
                "originSectorLat",
                "originSectorLng"
            ],
            onDelete = ForeignKey.CASCADE
        )
    ],
    primaryKeys = ["placeId", "text"],
    indices = [Index(value = ["text", "originSectorLat", "originSectorLng"])]
)
data class RemotePredictionCacheEntity(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
    val text: String,
    val originSectorLat: Int,
    val originSectorLng: Int
)