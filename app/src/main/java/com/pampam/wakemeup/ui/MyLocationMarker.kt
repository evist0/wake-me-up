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
import com.google.maps.android.SphericalUtil
import com.pampam.wakemeup.data.model.MyLocation
import com.pampam.wakemeup.data.model.MyLocationStatus
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

class MyLocationMarker(
    private val marker: Marker,
    private val resources: MyLocationMarkerResources,
    movementTimeout: Long,
    private val movementEpsilon: Double,
    private val onMoveAnimationEnd: (newLocation: LatLng) -> Unit
) {

    var location: MyLocation = MyLocation(MyLocationStatus.Unavailable, LatLng(0.0, 0.0))
        get() = MyLocation(
            locationStatus,
            marker.position
        )
        set(newLocation) {

            locationStatus = newLocation.status

            if (field != newLocation && locationStatus.isAvailable()) {

                val oldLatLng = location.latLng!!
                val newLatLng = newLocation.latLng!!
                val lngDifference = newLatLng.longitude - oldLatLng.longitude
                val latDifference = newLatLng.latitude - oldLatLng.latitude

                val movement = SphericalUtil.computeDistanceBetween(oldLatLng, newLatLng)
                if (locationStatus != MyLocationStatus.FirstAvailable && movement > movementEpsilon) {
                    movementStatus = MovementStatus.Moving
                }

                val newRotation = atan2(lngDifference, latDifference) * 180 / PI
                ObjectAnimator.ofObject(RotationEvaluator, marker.rotation, newRotation).apply {
                    duration =
                        if (newLocation.status == MyLocationStatus.FirstAvailable) 0 else 1000

                    addUpdateListener {
                        marker.rotation = it.animatedValue as Float
                    }

                    start()
                }
                ObjectAnimator.ofObject(LatLngEvaluator, marker.position, newLocation.latLng)
                    .apply {
                        duration =
                            if (newLocation.status == MyLocationStatus.FirstAvailable) 0 else 1000

                        addUpdateListener {
                            marker.apply {
                                position = it.animatedValue as LatLng
                                isVisible = true
                            }
                        }
                        doOnEnd {
                            onMoveAnimationEnd(newLocation.latLng)
                        }
                        start()
                    }

                field = newLocation
            }
        }

    private var locationStatus = MyLocationStatus.Unavailable
        set(newStatus) {
            if (field != newStatus) {
                field = newStatus
                invalidateIcon()
            }
        }

    private enum class MovementStatus {
        Standing,
        Moving
    }

    private var movementStatus: MovementStatus = MovementStatus.Standing
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
                movementStatus = MovementStatus.Standing
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

        val icon = when (locationStatus) {
            MyLocationStatus.Unavailable -> when (movementStatus) {
                MovementStatus.Standing -> resources.standingOffline
                MovementStatus.Moving -> resources.movingOffline
            }
            MyLocationStatus.FirstAvailable, MyLocationStatus.Available -> when (movementStatus) {
                MovementStatus.Standing -> resources.standingOnline
                MovementStatus.Moving -> resources.movingOnline
            }
        }

        marker.setIcon(BitmapDescriptorFactory.fromBitmap(icon))
    }
}

fun GoogleMap.addLocationMarker(
    resources: MyLocationMarkerResources,
    movementTimeout: Long,
    movementEpsilon: Double,
    onMoveAnimationEnd: (newLocation: LatLng) -> Unit
): MyLocationMarker {

    val marker = addMarker(MarkerOptions().apply {
        flat(true)
        anchor(0.5f, 0.5f)
        position(LatLng(0.0, 0.0))
        icon(BitmapDescriptorFactory.fromBitmap(resources.standingOffline))
        visible(false)
    })

    return MyLocationMarker(marker, resources, movementTimeout, movementEpsilon, onMoveAnimationEnd)
}
