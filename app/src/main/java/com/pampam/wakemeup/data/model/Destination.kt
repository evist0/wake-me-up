package com.pampam.wakemeup.data.model

data class Destination(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
    val src: DestinationSource
)