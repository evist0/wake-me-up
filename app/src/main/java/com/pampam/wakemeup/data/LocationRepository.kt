package com.pampam.wakemeup.data

import android.location.Location
import androidx.lifecycle.MutableLiveData

class LocationRepository {
    val location = MutableLiveData<Location>()
    val isLocationAvailable = MutableLiveData<Boolean>()
    val hasLocationPermission = MutableLiveData<Boolean>()

    fun requireLocation(invokable: () -> Unit) = when {
        hasLocationPermission.value != true -> {
            hasLocationPermission.value = false
        }
        isLocationAvailable.value != true -> {
            isLocationAvailable.value = false
        }
        location.value == null -> {
        }
        else -> {
            invokable()
        }
    }
}