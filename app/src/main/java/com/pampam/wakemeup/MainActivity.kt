package com.pampam.wakemeup

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.location.Location
import com.yandex.mapkit.location.LocationListener
import com.yandex.mapkit.location.LocationStatus
import com.yandex.mapkit.map.CameraPosition
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MapKitFactory.setApiKey(getString(R.string.mapkit_api_key))
        MapKitFactory.initialize(this)

        setContentView(R.layout.activity_main)

        setOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Manifest.permission.ACCESS_BACKGROUND_LOCATION else null
        ).mapNotNull {
            if (it != null && ContextCompat.checkSelfPermission(
                    this,
                    it
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                it
            } else {
                null
            }
        }.apply {
            if (this.isNotEmpty()) {
                ActivityCompat.requestPermissions(this@MainActivity, this.toTypedArray(), 1)
            } else {
                showMeMe()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.any {
                it == -1
            }) {
            Toast.makeText(this, "ХУЙЛАН", Toast.LENGTH_SHORT).show()
        } else {
            showMeMe()
        }

    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
        MapKitFactory.getInstance().onStart()
    }

    private fun showMeMe() {
        MapKitFactory.getInstance().createLocationManager()
            .requestSingleUpdate(object : LocationListener {
                override fun onLocationStatusUpdated(status: LocationStatus) {
                    Log.i(MainActivity::class.simpleName, "$status")
                }

                override fun onLocationUpdated(location: Location) {
                    mapView.map.move(
                        CameraPosition(
                            location.position,
                            11f,
                            0f,
                            0F
                        ),
                        Animation(Animation.Type.SMOOTH, 0F),
                        null
                    )
                }

            })
    }
}