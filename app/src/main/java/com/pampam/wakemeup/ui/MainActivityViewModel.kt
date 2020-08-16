package com.pampam.wakemeup.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pampam.wakemeup.data.MyLocationRepository
import com.pampam.wakemeup.data.model.Location

class MainActivityViewModel(
    private val myLocationRepository: MyLocationRepository
) : ViewModel() {

    val myLastLocation: LiveData<Location> = myLocationRepository.myLastLocation
    val isListenToLocation = myLocationRepository.isListenToLocation
    val isFocused = MutableLiveData<Boolean>(false)
}