package com.pampam.wakemeup.ui

import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.GoogleMap

class DJIMavikPro(private val map: GoogleMap) {
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
    }

    fun clear() {
        nextCameraUpdate = null
        isAnimating = false
    }

    fun enqueue(cameraUpdate: CameraUpdate) {
        nextCameraUpdate = cameraUpdate

        if (!isAnimating) {
            animate()
        }
    }

    private fun animate() {
        map.animateCamera(nextCameraUpdate)
        isAnimating = true

        nextCameraUpdate = null
    }
}