package com.pampam.wakemeup.data

import androidx.lifecycle.MutableLiveData
import com.pampam.wakemeup.data.model.Location

class MyLocationRepository {

    private val _myLastLocation = MutableLiveData<Location>()
    val myLastLocation = _myLastLocation

    private val _isListenToLocation = MutableLiveData<Boolean>()
    val isListenToLocation = _isListenToLocation
}