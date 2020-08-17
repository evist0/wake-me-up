package com.pampam.wakemeup.data.model

import com.google.android.gms.maps.model.LatLng

data class Location(
    val status: LocationStatus,
    val latLng: LatLng?
)