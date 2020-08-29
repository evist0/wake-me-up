package com.pampam.wakemeup.extensions

import android.location.Location
import com.google.android.gms.maps.model.LatLng

fun Location.toLatLng(): LatLng = LatLng(latitude, longitude)