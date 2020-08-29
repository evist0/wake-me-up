package com.pampam.wakemeup.extensions

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.pampam.wakemeup.data.model.Session
import com.pampam.wakemeup.data.model.SessionStatus

fun Session.inRange(position: LatLng): Boolean {
    return if (details != null) {
        val distance = SphericalUtil.computeDistanceBetween(details.latLng, position)
        distance <= range.toMeters()
    } else {
        false
    }
}

fun Session.isDismissRationaleRequired(position: LatLng?): Boolean =
    status == SessionStatus.Active && position != null && !inRange(position)
