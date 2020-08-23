package com.pampam.wakemeup.data

import androidx.lifecycle.MutableLiveData
import com.pampam.wakemeup.data.model.MyLocation

class MyLocationRepository {

    private val _myLastLocation = MutableLiveData<MyLocation>()
    val myLastLocation = _myLastLocation

    private val _isListenToLocation = MutableLiveData<Boolean>()
    val isListenToLocation = _isListenToLocation
}