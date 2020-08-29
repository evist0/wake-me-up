package com.pampam.wakemeup.data.model

import com.google.android.gms.maps.model.LatLng

data class Destination(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
    val latLng: LatLng
)