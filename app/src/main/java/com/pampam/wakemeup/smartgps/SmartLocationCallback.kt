package com.pampam.wakemeup.smartgps

import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationResult

interface SmartLocationCallback {
    enum class FailureReason {
        Unknown,
        Security
    }

    fun onLocationResult(locationResult: LocationResult): Double?
    fun onLocationAvailability(locationAvailability: LocationAvailability)
    fun onFailure(failureReason: FailureReason)
}
