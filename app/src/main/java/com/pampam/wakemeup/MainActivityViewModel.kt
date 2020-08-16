package com.pampam.wakemeup

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainActivityViewModel(locationRepository: LocationRepository) : ViewModel() {
    val location = locationRepository.location
    val isUserFocused = MutableLiveData<Boolean>(false)
}