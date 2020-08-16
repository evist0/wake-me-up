package com.pampam.wakemeup.data.model

import com.google.android.gms.maps.model.LatLng

data class Location(
    val available: Boolean,
    val first: Boolean,
    val latLng: LatLng?
)