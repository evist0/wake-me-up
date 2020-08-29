package com.pampam.wakemeup.ui.map

import android.animation.ObjectAnimator
import android.graphics.Bitmap
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.pampam.wakemeup.utils.DoubleEvaluator

class DestinationMarker(
    private val marker: Marker,
    private val circle: Circle,
    private val options: Options
) {
    data class Options(
        val iconBitmap: Bitmap,
        val fillColor: Int,
        val strokeColor: Int,
        val strokeWidth: Float,
        val animationDuration: Long
    )

    var position: LatLng? = null
        set(value) {
            if (value != field) {
                field = value
                if (field != null) {
                    marker.position = value
                    marker.isVisible = true

                    circle.center = value
                    circle.isVisible = true
                } else {
                    marker.isVisible = false
                    circle.isVisible = false
                }
            }
        }

    var radius: Double
        get() = circle.radius
        set(newRadius) {
            ObjectAnimator.ofObject(DoubleEvaluator, circle.radius, newRadius).apply {
                duration = options.animationDuration

                addUpdateListener {
                    circle.radius = it.animatedValue as Double
                }

                start()
            }
        }

    init {
        marker.apply {
            isVisible = false
            setIcon(BitmapDescriptorFactory.fromBitmap(options.iconBitmap))
        }

        circle.apply {
            isVisible = false
            fillColor = options.fillColor
            strokeColor = options.strokeColor
            strokeWidth = options.strokeWidth
        }
    }
}

fun GoogleMap.addDestinationMarker(options: DestinationMarker.Options): DestinationMarker {
    val marker = addMarker(MarkerOptions().position(LatLng(0.0, 0.0)))
    val circle = addCircle(CircleOptions().center(LatLng(0.0, 0.0)))
    return DestinationMarker(marker, circle, options)
}