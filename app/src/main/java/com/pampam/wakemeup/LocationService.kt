package com.pampam.wakemeup

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.content.ContextCompat

class LocationService(private val context: Context, private val locationManager: LocationManager) {
    fun subscribe(locationListener: LocationListener) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000,
                1.0f,
                locationListener
            )
        } else {
            throw Exception("Permission error")
        }
    }

    fun unsubscribe(locationListener: LocationListener) {
        locationManager.removeUpdates(locationListener)
    }
}

