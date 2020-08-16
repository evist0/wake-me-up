package com.pampam.wakemeup

import android.content.Context
import android.location.LocationListener
import android.location.LocationManager

class LocationService(private val context: Context, private val locationManager: LocationManager) {
    fun subscribe(locationListener: LocationListener) {
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000,
            1.0f,
            locationListener
        )
    }

    fun unsubscribe(locationListener: LocationListener) {
        locationManager.removeUpdates(locationListener)
    }
}

