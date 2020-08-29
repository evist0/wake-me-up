package com.pampam.wakemeup.ui.map

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
import com.pampam.wakemeup.utils.LatLngEvaluator
import com.pampam.wakemeup.utils.RotationEvaluator
import kotlin.math.PI
import kotlin.math.atan2

class LocationMarker(private val marker: Marker, private val options: Options) {
    data class Options(
        val movingBitmap: Bitmap,
        val standingBitmap: Bitmap,
        val offlineBitmap: Bitmap,
        val animationDuration: Long,
        val movementTimeout: Long,
        val movementEpsilon: Double
    )

    private enum class MovementStatus {
        Standing,
        Moving
    }

    private inner class MovementSentry(private val timeout: Long) {
        private var mIsActive = false
        private val mCountDownTimer = object : CountDownTimer(timeout, 1000) {
            override fun onTick(tick: Long) {}

            override fun onFinish() {
                mMovementStatus =
                    MovementStatus.Standing
                mIsActive = false
            }
        }

        fun update() {
            if (mIsActive) {
                mCountDownTimer.cancel()
            }
            mCountDownTimer.start()
        }
    }

    private var mPreviousLatLng: LatLng? = null

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

    private val movementSentry = MovementSentry(options.movementTimeout)

    init {
        marker.apply {
            isVisible = false
            isFlat = true
            setAnchor(0.5f, 0.5f)
        }
        invalidateIcon()
    }

    var locationAvailable: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                invalidateIcon()
            }
        }

    private fun invalidateIcon() {
        val icon = when (locationAvailable) {
            true -> {
                when (mMovementStatus) {
                    MovementStatus.Standing -> options.standingBitmap
                    MovementStatus.Moving -> options.movingBitmap
                }
            }
            else -> {
                options.offlineBitmap
            }
        }

        marker.setIcon(BitmapDescriptorFactory.fromBitmap(icon))
    }

    fun animateLocation(location: LatLng, onAnimationEnd: () -> Unit) {
        val fromLatLng = mPreviousLatLng ?: location

        val lngDifference = location.longitude - fromLatLng.longitude
        val latDifference = location.latitude - fromLatLng.latitude

        val movement = SphericalUtil.computeDistanceBetween(fromLatLng, location)
        if (movement > options.movementEpsilon) {
            mMovementStatus = MovementStatus.Moving
        }

        val newRotation = atan2(lngDifference, latDifference) * 180 / PI
        ObjectAnimator.ofObject(RotationEvaluator, marker.rotation, newRotation).apply {
            duration = options.animationDuration
            addUpdateListener {
                marker.rotation = it.animatedValue as Float
            }
            start()
        }

        ObjectAnimator.ofObject(LatLngEvaluator, fromLatLng, location)
            .apply {
                duration = options.animationDuration
                addUpdateListener {
                    mPreviousLatLng = it.animatedValue as LatLng
                    marker.apply {
                        position = mPreviousLatLng
                        isVisible = true
                    }
                }
                doOnEnd {
                    onAnimationEnd()
                }
                start()
            }
    }
}

fun GoogleMap.addLocationMarker(options: LocationMarker.Options): LocationMarker {
    val marker = addMarker(MarkerOptions().position(LatLng(0.0, 0.0)))
    return LocationMarker(marker, options)
}