package com.pampam.wakemeup

import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import java.lang.ref.WeakReference

class WeakLocationCallback(locationCallback: LocationCallback) : LocationCallback() {
    private val weakReference = WeakReference(locationCallback)

    override fun onLocationResult(locationResult: LocationResult) {
        weakReference.get()?.onLocationResult(locationResult)
    }

    override fun onLocationAvailability(locationAvailability: LocationAvailability) {
        weakReference.get()?.onLocationAvailability(locationAvailability)
    }
}