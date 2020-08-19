package com.pampam.wakemeup.ui.animation

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.CountDownTimer
import android.util.Log
import androidx.core.animation.doOnEnd
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.pampam.wakemeup.R
import com.pampam.wakemeup.data.MyLocationService
import com.pampam.wakemeup.data.model.Location
import com.pampam.wakemeup.data.model.LocationStatus
import com.pampam.wakemeup.data.model.MovingStatus
import com.pampam.wakemeup.ui.MainActivityViewModel
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

// ревью кода https://www.youtube.com/watch?v=dQw4w9WgXcQ
// исправь всё с учетом того что я сказал

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
        if(!isActive) {
            countDownTimer.start()
        } else {
            countDownTimer.cancel()
            countDownTimer.start()
        }
    }
}

class LocationMarker(
    private val map: GoogleMap,
    private val context: Context,
    private val viewModel: MainActivityViewModel
) {

    private var marker: Marker

    var location: Location = Location(LocationStatus.Unavailable, LatLng(0.0, 0.0))
        get() = Location(
            locationStatus,
            marker.position
        )
        set(newLocation) {

            locationStatus = newLocation.status

            if (field != newLocation && locationStatus.isAvailable()) {

                //Don't move for the first time
                if(locationStatus != LocationStatus.FirstAvailable) {
                    movingStatus = MovingStatus.Moving
                }

                val lngDifference = newLocation.latLng!!.longitude - location.latLng!!.longitude
                val latDifference = newLocation.latLng.latitude - location.latLng!!.latitude

                var newRotation = (atan2(lngDifference, latDifference) * 180 / PI).toFloat()

                newRotation = if(newRotation < 0) 360 + newRotation else newRotation
                newRotation %= 360

                ObjectAnimator.ofObject(RotationEvaluator, marker.rotation, newRotation).apply {
                    duration = if (newLocation.status == LocationStatus.FirstAvailable) 0 else 1000

                    addUpdateListener {
                        marker.rotation = it.animatedValue as Float
                    }

                    start()
                }
                ObjectAnimator.ofObject(LatLngEvaluator, marker.position, newLocation.latLng).apply {
                        duration =
                            if (newLocation.status == LocationStatus.FirstAvailable) 0 else 1000

                        addUpdateListener {
                            marker.apply {
                                position = it.animatedValue as LatLng
                                isVisible = true
                            }
                        }

                        doOnEnd {
                            if (viewModel.isFocused.value == true) {
                                map.animateCamera(CameraUpdateFactory.newLatLng(location.latLng))
                            }

                        }

                        start()
                    }

                field = newLocation
            }
        }

    private var locationStatus = LocationStatus.Unavailable
        set(newStatus) {
            //Offline -> Online
            if (!field.isAvailable() && newStatus.isAvailable()) {
                currentColor = primaryColor
            }

            //Online -> Offline
            if (field.isAvailable() && !newStatus.isAvailable()) {
                currentColor = neutralColor
            }

            if (field != newStatus) {
                field = newStatus
                redraw()
            }
        }

    var movingStatus: MovingStatus = MovingStatus.Standing
        set(newStatus) {

            if(newStatus == MovingStatus.Moving) {
                movingDisabler.update()
            }

            //Stay -> Move
            if (!movingStatus.isMoving() && newStatus.isMoving()) {
                currentBitmap = movingBitmap
            }

            //Move -> Stay
            if (movingStatus.isMoving() && !newStatus.isMoving()) {
                currentBitmap = standingBitmap
            }

            if (field != newStatus) {
                field = newStatus
                redraw()
            }
        }

    private var movingDisabler = MovingDisabler(this, 6000)

    private var currentBitmap: Bitmap
    private var currentColor: Int

    private var movingBitmap: Bitmap
    private var standingBitmap: Bitmap

    private var primaryColor = context.getColor(R.color.colorPrimary)
    private var neutralColor = context.getColor(R.color.colorText_neutral)

    init {
        movingBitmap = initMarkerBitmap(R.drawable.moving)
        standingBitmap = initMarkerBitmap(R.drawable.stay)

        currentColor = neutralColor
        currentBitmap = replaceColor(standingBitmap, currentColor)

        marker = map.addMarker(MarkerOptions().apply {
            position(LatLng(0.0, 0.0))
            icon(BitmapDescriptorFactory.fromBitmap(currentBitmap))
            visible(false)
        })
    }

    private fun redraw() {
        marker.apply {
            setIcon(BitmapDescriptorFactory.fromBitmap(replaceColor(currentBitmap, currentColor)))
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