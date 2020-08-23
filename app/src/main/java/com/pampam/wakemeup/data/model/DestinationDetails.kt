package com.pampam.wakemeup.data.model

import com.google.android.gms.maps.model.LatLng

data class DestinationDetails(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
    val latLng: LatLng
)