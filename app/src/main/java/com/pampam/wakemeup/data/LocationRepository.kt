package com.pampam.wakemeup.data

import android.location.Location
import androidx.lifecycle.MutableLiveData

class LocationRepository {
    val location = MutableLiveData<Location>()
    val isLocationAvailable = MutableLiveData<Boolean>()
    val hasLocationPermission = MutableLiveData<Boolean>()
}