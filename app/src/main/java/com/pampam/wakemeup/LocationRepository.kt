package com.pampam.wakemeup

class LocationRepository(locationService: LocationService) {
    val location = LocationLiveData(locationService)
}