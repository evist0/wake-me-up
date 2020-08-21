package com.pampam.wakemeup.ui

import android.animation.ObjectAnimator
import android.graphics.Bitmap
import android.os.CountDownTimer
import androidx.core.animation.doOnEnd
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.pampam.wakemeup.data.model.Location
import com.pampam.wakemeup.data.model.LocationStatus
import com.pampam.wakemeup.data.model.MovingStatus
import com.pampam.wakemeup.ui.animation.LatLngEvaluator
import com.pampam.wakemeup.ui.animation.RotationEvaluator
import kotlin.math.PI
import kotlin.math.atan2

class MovingDisabler(private val locationMarker: LocationMarker, private val timeout: Long) {
    private var isActive = false

    private val countDownTimer = object : CountDownTimer(timeout, 1000) {
        override fun onTick(p0: Long) {
        }

        override fun onFinish() {
            locationMarker.movingStatus = MovingStatus.Standing
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

data class LocationMarkerResources(
    val movingOnline: Bitmap,
    val standingOnline: Bitmap,
    val movingOffline: Bitmap,
    val standingOffline: Bitmap
)

class LocationMarker(
    map: GoogleMap,
    private val resources: LocationMarkerResources,
    private val onMoveEnd: (newLocation: LatLng) -> Unit
) {

    private var marker: Marker = map.addMarker(MarkerOptions().apply {
        position(LatLng(0.0, 0.0))
        icon(BitmapDescriptorFactory.fromBitmap(resources.standingOffline))
        visible(false)
    })

    var location: Location = Location(LocationStatus.Unavailable, LatLng(0.0, 0.0))
        get() = Location(
            locationStatus,
            marker.position
        )
        set(newLocation) {

            locationStatus = newLocation.status

            if (field != newLocation && locationStatus.isAvailable()) {
                if (locationStatus != LocationStatus.FirstAvailable) {
                    movingStatus = MovingStatus.Moving
                }

                val lngDifference = newLocation.latLng!!.longitude - location.latLng!!.longitude
                val latDifference = newLocation.latLng.latitude - location.latLng!!.latitude

                val newRotation = atan2(lngDifference, latDifference) * 180 / PI

                ObjectAnimator.ofObject(RotationEvaluator, marker.rotation, newRotation).apply {
                    duration = if (newLocation.status == LocationStatus.FirstAvailable) 0 else 1000

                    addUpdateListener {
                        marker.rotation = it.animatedValue as Float
                    }

                    start()
                }
                ObjectAnimator.ofObject(LatLngEvaluator, marker.position, newLocation.latLng)
                    .apply {
                        duration =
                            if (newLocation.status == LocationStatus.FirstAvailable) 0 else 1000

                        addUpdateListener {
                            marker.apply {
                                position = it.animatedValue as LatLng
                                isVisible = true
                            }
                        }
                        doOnEnd {
                            onMoveEnd(newLocation.latLng)
                        }
                        start()
                    }

                field = newLocation
            }
        }

    private var locationStatus = LocationStatus.Unavailable
        set(newStatus) {
            if (field != newStatus) {
                field = newStatus
                redraw()
            }
        }

    var movingStatus: MovingStatus = MovingStatus.Standing
        set(newStatus) {
            if (newStatus == MovingStatus.Moving) {
                movingDisabler.update()
            }

            if (field != newStatus) {
                field = newStatus
                redraw()
            }
        }

    private val movingDisabler =
        MovingDisabler(this, 6000)

    private fun redraw() {
        val icon = when (Pair(locationStatus.isAvailable(), movingStatus.isMoving())) {
            Pair(first = true, second = true) -> resources.movingOnline
            Pair(first = true, second = false) -> resources.standingOnline
            Pair(first = false, second = true) -> resources.movingOffline
            Pair(first = false, second = false) -> resources.standingOffline

            else -> throw Exception("Icon 101")
        }

        marker.apply {
            setIcon(BitmapDescriptorFactory.fromBitmap(icon))
        }
    }
}