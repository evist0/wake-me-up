package com.pampam.wakemeup

import android.location.Location
import android.location.LocationListener
import androidx.lifecycle.LiveData

class LocationLiveData(private val locationService: LocationService) : LiveData<Location>() {
    private val locationListener = LocationListener { location ->
        value = location
    }

    override fun onActive() {
        super.onActive()
        locationService.subscribe(locationListener)
    }

    override fun onInactive() {
        super.onInactive()
        locationService.unsubscribe(locationListener)
    }
}