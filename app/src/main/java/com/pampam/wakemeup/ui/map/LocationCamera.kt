package com.pampam.wakemeup.ui.map

import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.SphericalUtil

class LocationCamera(private val map: GoogleMap, private val options: Options) {
    data class Options(
        val defaultZoom: Float,
        val minZoom: Float
    )

    private inner class Dispatcher {
        private var nextCameraUpdate: CameraUpdate? = null
        private var isAnimating = false

        init {
            map.setOnCameraIdleListener {
                if (nextCameraUpdate != null) {
                    animate()
                } else {
                    isAnimating = false
                }
            }

            map.setOnCameraMoveStartedListener { reason ->
                if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    clear()
                    onGestureCameraMove?.invoke()
                }
            }
        }

        private fun animate() {
            map.animateCamera(nextCameraUpdate)

            nextCameraUpdate = null
            isAnimating = true
        }

        fun dispatch(cameraUpdate: CameraUpdate) {
            nextCameraUpdate = cameraUpdate
            if (!isAnimating) {
                animate()
            }
        }

        fun clear() {
            nextCameraUpdate = null
            isAnimating = false
        }
    }

    private val dispatcher = Dispatcher()

    var location: LatLng? = null

    var destination: LatLng? = null

    var onGestureCameraMove: (() -> Unit)? = null

    fun focus() {
        val location = location
        val destination = destination

        val cameraUpdate =
            if (destination != null) {
                if (location != null) {
                    CameraUpdateFactory.newLatLngBounds(
                        with(LatLngBounds.builder().apply {
                            include(location)
                            include(destination)
                        }.build()) {
                            LatLngBounds(
                                SphericalUtil.interpolate(northeast, southwest, 1.25),
                                SphericalUtil.interpolate(southwest, northeast, 1.25)
                            )
                        }, 0
                    )
                } else {
                    CameraUpdateFactory.newLatLngZoom(destination, options.defaultZoom)
                }
            } else if (location != null) {
                val shouldZoom =
                    !map.projection.visibleRegion.latLngBounds.contains(location) || map.cameraPosition.zoom < options.minZoom
                if (shouldZoom) {
                    CameraUpdateFactory.newLatLngZoom(location, options.defaultZoom)
                } else {
                    CameraUpdateFactory.newLatLng(location)
                }
            } else {
                null
            }

        cameraUpdate?.let { update ->
            dispatcher.dispatch(update)
        }
    }
}

fun GoogleMap.addLocationCamera(options: LocationCamera.Options): LocationCamera {
    return LocationCamera(this, options)
}