package com.pampam.wakemeup.data.model

data class DestinationPrediction(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
    val src: DestinationPredictionSource
)