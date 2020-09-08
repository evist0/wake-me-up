package com.pampam.wakemeup.smartgps

import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import kotlin.math.abs

class SmartGps(
    private val options: Options,
    private val fusedLocationProviderClient: FusedLocationProviderClient
) {
    data class Options(
        val intervalFunction: (distance: Double) -> Long
    )

    private var mLocationRequest: LocationRequest? = null
    private var mProxyLocationCallback: LocationCallback? = null
    private var mWeakProxyLocationCallback: WeakLocationCallback? = null
    private var mDistance = 0.0

    fun requestLocationUpdates(
        locationCallback: SmartLocationCallback,
        looper: Looper
    ) {
        val proxyLocationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)

                Log.d(this::class.simpleName, "onLocationResult(): $result")

                mDistance = locationCallback.onLocationResult(result) ?: mDistance

                val newMillis = options.intervalFunction(mDistance)
                if (abs(newMillis - mLocationRequest!!.interval) > 1000) {
                    Log.d(
                        SmartGps::class.simpleName,
                        "Updating location request with $newMillis ms"
                    )
                    removeLocationUpdates().addOnSuccessListener {
                        requestLocationUpdates(newMillis, this, looper, locationCallback::onFailure)
                    }.addOnFailureListener {
                        locationCallback.onFailure(failureReason = SmartLocationCallback.FailureReason.Unknown)
                    }
                }
            }

            override fun onLocationAvailability(result: LocationAvailability) {
                super.onLocationAvailability(result)

                Log.d(this::class.simpleName, "onLocationAvailability(): $result")

                locationCallback.onLocationAvailability(result)
            }
        }

        requestLocationUpdates(
            options.intervalFunction(mDistance),
            proxyLocationCallback,
            looper,
            locationCallback::onFailure
        )
    }

    fun removeLocationUpdates(): Task<Void> =
        fusedLocationProviderClient.removeLocationUpdates(mWeakProxyLocationCallback)

    private fun requestLocationUpdates(
        millis: Long,
        proxyLocationCallback: LocationCallback,
        looper: Looper,
        failureCallback: (SmartLocationCallback.FailureReason) -> Unit
    ) {
        mLocationRequest = newLocationRequest(millis)
        try {
            val weakLocationCallback = WeakLocationCallback(proxyLocationCallback)
            fusedLocationProviderClient.requestLocationUpdates(
                mLocationRequest,
                weakLocationCallback,
                looper
            ).addOnSuccessListener {
                Log.d(this::class.simpleName, "requestLocationUpdates successful")

                mProxyLocationCallback = proxyLocationCallback
                mWeakProxyLocationCallback = weakLocationCallback
            }.addOnFailureListener {
                Log.d(this::class.simpleName, "requestLocationUpdates failure")

                failureCallback(SmartLocationCallback.FailureReason.Unknown)
            }
        } catch (e: SecurityException) {
            failureCallback(SmartLocationCallback.FailureReason.Security)
        }
    }

    private fun newLocationRequest(millis: Long): LocationRequest = LocationRequest.create().apply {
        interval = millis
        fastestInterval = millis
        maxWaitTime = millis
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }
}
