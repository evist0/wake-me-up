package com.pampam.wakemeup.data.model

import com.google.android.gms.maps.model.LatLng

data class MyLocation(
    val status: MyLocationStatus,
    val latLng: LatLng?
)