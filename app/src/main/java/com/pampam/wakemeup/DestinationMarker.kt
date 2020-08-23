package com.pampam.wakemeup

import android.animation.ObjectAnimator
import android.graphics.Bitmap
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import com.pampam.wakemeup.ui.animation.DoubleEvaluator

data class DestinationMarkerResources(
    val icon: Bitmap,
    val fillColor: Int,
    val strokeColor: Int
)

class DestinationMarker(
    private val marker: Marker,
    private val circle: Circle,
    private val animationDuration: Long
) {
    var position: LatLng
        get() = marker.position
        set(newPosition) {
            marker.position = newPosition
            circle.center = newPosition
        }

    var radius: Double
        get() = circle.radius
        set(newRadius) {

            ObjectAnimator.ofObject(DoubleEvaluator, circle.radius, newRadius).apply {
                duration = animationDuration

                addUpdateListener {
                    circle.radius = it.animatedValue as Double
                }

                start()
            }
        }

    var isVisible: Boolean
        get() = marker.isVisible
        set(newIsVisible) {
            marker.isVisible = newIsVisible
            circle.isVisible = newIsVisible
        }
}

fun GoogleMap.addDestinationMarker(
    resources: DestinationMarkerResources,
    animationDuration: Long
): DestinationMarker {
    val marker = addMarker(
        MarkerOptions().position(LatLng(0.0, 0.0))
            .icon(BitmapDescriptorFactory.fromBitmap(resources.icon))
            .visible(false)
    )

    val circle = addCircle(CircleOptions().apply {
        fillColor(resources.fillColor)
        strokeColor(resources.strokeColor)
        strokeWidth(5.0f)
        center(LatLng(0.0, 0.0))
        visible(false)
    })

    return DestinationMarker(marker, circle, animationDuration)
}