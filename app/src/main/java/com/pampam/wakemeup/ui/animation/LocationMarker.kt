package com.pampam.wakemeup.ui.animation

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.CountDownTimer
import android.util.Log
import androidx.core.animation.doOnEnd
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.pampam.wakemeup.R
import com.pampam.wakemeup.data.MyLocationService
import com.pampam.wakemeup.data.model.Location
import kotlin.math.PI
import kotlin.math.atan2

class LocationMarker(private val map: GoogleMap, private val context: Context) {

    private var marker: Marker

    private var mRotation = 0.0f

    private var isMoving = false
    private var isOnline = false

    private var isMovingDisablerActive = false
    private var isMovingDisabler = object : CountDownTimer(6000, 1000) {

        override fun onTick(p0: Long) {
            Log.d(
                MyLocationService::class.simpleName,
                "Milliseconds remaining until isMoving became false: $p0"
            )
        }

        override fun onFinish() {
            isMoving = false
            redraw()
        }
    }

    private lateinit var movingBitmap: Bitmap
    private lateinit var stayBitmap: Bitmap

    private var primaryColor = context.getColor(R.color.colorPrimary)
    private var neutralColor = context.getColor(R.color.colorText_neutral)

    init {
        movingBitmap = initMarkerBitmap(R.drawable.moving)
        stayBitmap = initMarkerBitmap(R.drawable.stay)

        marker = map.addMarker(MarkerOptions().apply {
            position(LatLng(0.0, 0.0))
            visible(false)
        })
    }

    fun setLocation(newLocation: Location, onAnimEnd: () -> Unit) {

        if (isOnline != newLocation.status.isAvailable()) {
            isOnline = newLocation.status.isAvailable()
            redraw()
        }
        if (marker.position != newLocation.latLng && newLocation.latLng != null) {
            calculateMove(marker.position, newLocation.latLng)
        }

        ObjectAnimator.ofObject(LatLngEvaluator, marker.position, newLocation.latLng).apply {
            duration = if (newLocation.status.isAvailable()) 0 else 1000
            addUpdateListener {
                marker.apply {
                    position = it.animatedValue as LatLng
                    isVisible = true
                }
            }
            doOnEnd {
                onAnimEnd()
            }
            start()
        }
    }

    fun getLocation(): LatLng {
        return marker.position
    }

    private fun calculateMove(oldPosition: LatLng, newPosition: LatLng) {
        isMoving = true

        if (isMovingDisablerActive) {
            isMovingDisabler.cancel()
            isMovingDisablerActive = false
        }

        val lngDifference = newPosition.longitude - oldPosition.longitude
        val latDifference = newPosition.latitude - oldPosition.latitude

        val arcth = atan2(lngDifference, latDifference)
        mRotation = (arcth * 180 / PI).toFloat()

        redraw()

        isMovingDisabler.start()
        isMovingDisablerActive = true
    }

    private fun redraw() {
        var newMarker = if (isMoving) movingBitmap else stayBitmap
        newMarker = if (isOnline) replaceColor(newMarker, primaryColor) else replaceColor(
            newMarker,
            neutralColor
        )

        marker.apply {
            setIcon(BitmapDescriptorFactory.fromBitmap(newMarker))
            rotation = mRotation
        }

    }

    private fun initMarkerBitmap(icon: Int): Bitmap {
        val rawBitMap = BitmapFactory.decodeResource(context.resources, icon)

        return Bitmap.createScaledBitmap(rawBitMap, 64, 64, false)
    }

    private fun replaceColor(src: Bitmap, targetColor: Int): Bitmap {
        // Source image size
        val width = src.width
        val height = src.height
        val pixels = IntArray(width * height)

        //get pixels
        src.getPixels(pixels, 0, width, 0, 0, width, height)
        for (x in pixels.indices) {
            pixels[x] = if (pixels[x] == Color.BLACK) targetColor else pixels[x]
        }

        // create result bitmap output
        val result = Bitmap.createBitmap(width, height, src.config)

        //set pixels
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }
}