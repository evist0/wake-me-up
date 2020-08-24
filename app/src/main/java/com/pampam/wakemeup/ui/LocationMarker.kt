package com.pampam.wakemeup.ui

import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.location.Location
import android.os.CountDownTimer
import androidx.core.animation.doOnEnd
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.SphericalUtil
import com.pampam.wakemeup.toLatLng
import com.pampam.wakemeup.ui.animation.LatLngEvaluator
import com.pampam.wakemeup.ui.animation.RotationEvaluator
import kotlin.math.PI
import kotlin.math.atan2

data class MyLocationMarkerResources(
    val movingOnline: Bitmap,
    val standingOnline: Bitmap,
    val movingOffline: Bitmap,
    val standingOffline: Bitmap
)

class LocationMarker(
    private val marker: Marker,
    private val resources: MyLocationMarkerResources,
    movementTimeout: Long,
    private val movementEpsilon: Double
) {

    private var previousLatLng: LatLng? = null

    fun animateLocation(location: Location, onAnimationEnd: () -> Unit) {

        val toLatLng = location.toLatLng()
        val fromLatLng = previousLatLng ?: toLatLng

        val lngDifference = toLatLng.longitude - fromLatLng.longitude
        val latDifference = toLatLng.latitude - fromLatLng.latitude

        val movement = SphericalUtil.computeDistanceBetween(fromLatLng, toLatLng)
        if (movement > movementEpsilon) {
            mMovementStatus = MovementStatus.Moving
        }

        val newRotation = atan2(lngDifference, latDifference) * 180 / PI
        ObjectAnimator.ofObject(RotationEvaluator, marker.rotation, newRotation).apply {
            duration = 1000
            addUpdateListener {
                marker.rotation = it.animatedValue as Float
            }
            start()
        }

        ObjectAnimator.ofObject(LatLngEvaluator, fromLatLng, toLatLng)
            .apply {
                duration = 1000
                addUpdateListener {
                    previousLatLng = it.animatedValue as LatLng
                    marker.apply {
                        position = previousLatLng
                        isVisible = true
                    }
                }
                doOnEnd {
                    onAnimationEnd()
                }
                start()
            }
    }

    private var mLocationAvailable: Boolean? = null

    fun updateLocationAvailable(available: Boolean) {
        if (mLocationAvailable != available) {
            mLocationAvailable = available
            invalidateIcon()
        }
    }

    private enum class MovementStatus {
        Standing,
        Moving
    }

    private var mMovementStatus: MovementStatus = MovementStatus.Standing
        set(newStatus) {
            if (newStatus == MovementStatus.Moving) {
                movementSentry.update()
            }

            if (field != newStatus) {
                field = newStatus
                invalidateIcon()
            }
        }

    private inner class MovementSentry(private val timeout: Long) {

        private var isActive = false

        private val countDownTimer = object : CountDownTimer(timeout, 1000) {
            override fun onTick(tick: Long) {
            }

            override fun onFinish() {
                mMovementStatus = MovementStatus.Standing
                isActive = false
            }
        }

        fun update() {
            if (!isActive) {
                countDownTimer.start()
            } else {
                countDownTimer.cancel()
                countDownTimer.start()
            }
        }
    }

    private val movementSentry = MovementSentry(movementTimeout)

    private fun invalidateIcon() {

        val icon = when (mLocationAvailable) {
            false -> {
                when (mMovementStatus) {
                    MovementStatus.Standing -> resources.standingOffline
                    MovementStatus.Moving -> resources.movingOffline
                }
            }
            true -> {
                when (mMovementStatus) {
                    MovementStatus.Standing -> resources.standingOnline
                    MovementStatus.Moving -> resources.movingOnline
                }
            }
            else -> {
                null
            }
        }

        icon?.let {
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(it))
        }
    }

}

fun GoogleMap.addLocationMarker(
    resources: MyLocationMarkerResources,
    movementTimeout: Long,
    movementEpsilon: Double
): LocationMarker {

    val marker = addMarker(MarkerOptions().apply {
        flat(true)
        anchor(0.5f, 0.5f)
        position(LatLng(0.0, 0.0))
        rotation(0.0f)
        visible(false)
    })

    return LocationMarker(marker, resources, movementTimeout, movementEpsilon)
}
