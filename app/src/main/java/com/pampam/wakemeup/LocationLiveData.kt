package com.pampam.wakemeup

import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import androidx.lifecycle.LiveData

class LocationLiveData(private val locationService: LocationService) :
    LiveData<Pair<Boolean, Location?>>() {
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            value = Pair(true, location)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

        }

        override fun onProviderEnabled(provider: String) {
            value = Pair(true, value?.second)
        }

        override fun onProviderDisabled(provider: String) {
            value = Pair(false, value?.second)
        }
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