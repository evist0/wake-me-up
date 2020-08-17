package com.pampam.wakemeup.ui

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.pampam.wakemeup.BuildConfig
import com.pampam.wakemeup.R
import com.pampam.wakemeup.data.MyLocationService
import com.pampam.wakemeup.data.model.LocationStatus
import com.pampam.wakemeup.databinding.ActivityMainBinding
import com.pampam.wakemeup.ui.animation.LatLngEvaluator
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.androidx.viewmodel.ext.android.viewModel


private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel by viewModel<MainActivityViewModel>()

    private lateinit var map: GoogleMap
    private lateinit var myLocationMarker: Marker
    private lateinit var myLocationMarkerAnimator: ValueAnimator

    private lateinit var locationAvailabilityPopUp: PopupView
    private lateinit var locationAvailabilitySnackbar: Snackbar
    private lateinit var locationPermissionSnackbar: Snackbar

    private var locationServiceBound = false
    private lateinit var locationServiceConnection: ServiceConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding =
            DataBindingUtil.setContentView<ActivityMainBinding>(
                this,
                R.layout.activity_main
            )
                .apply {
                    viewModel = this@MainActivity.viewModel
                    lifecycleOwner = this@MainActivity
                }

        myLocationButton.setOnClickListener {
            val myLastLocation = viewModel.myLastLocation.value
            if (myLastLocation != null) {
                viewModel.isFocused.value = !viewModel.isFocused.value!!

                if (viewModel.isFocused.value == true) {
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            myLocationMarker.position,
                            17.0f
                        )
                    )
                }
            }
        }

        locationAvailabilityPopUp =
            PopupView(this).setTitle(getString(R.string.location_unavailable))
                .setCallback { result ->
                    when (result) {
                        PopupAction.ACCEPT -> {
                            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            startActivity(intent)
                            locationAvailabilityPopUp.hide()
                        }

                        PopupAction.DENI -> {
                            locationAvailabilityPopUp.hide()
                        }
                    }
                }

        locationPermissionSnackbar = Snackbar.make(
            mapFragmentView,
            "Location permission required",
            Snackbar.LENGTH_INDEFINITE
        )

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragmentView) as SupportMapFragment
        mapFragment.getMapAsync {
            map = it.apply {
                setOnCameraMoveStartedListener { reason ->
                    when (reason) {
                        GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE -> {
                            viewModel.isFocused.value = false
                        }
                    }
                }
            }

            myLocationMarker =
                map.addMarker(MarkerOptions().position(LatLng(0.0, 0.0)).visible(false))

            viewModel.myLastLocation.observe(this, Observer { location ->
                if (location != null) {
                    if (location.latLng != null) {
                        myLocationMarkerAnimator =
                            ObjectAnimator.ofObject(
                                LatLngEvaluator,
                                myLocationMarker.position,
                                location.latLng
                            ).apply {
                                duration = if (location.first) 0 else 1000
                                addUpdateListener {
                                    myLocationMarker.apply {
                                        position = it.animatedValue as LatLng
                                        isVisible = true
                                    }
                                }
                                doOnEnd {
                                    if (viewModel.isFocused.value == true) {
                                        map.animateCamera(
                                            CameraUpdateFactory.newLatLng(
                                                myLocationMarker.position
                                            )
                                        )
                                    }
                                }
                                start()
                            }
                    }

                    if (location.status.isAvailable()) {
                        locationAvailabilityPopUp.hide()
                    } else {
                        locationAvailabilityPopUp.show(rootOfRootView)
                    }
                }
            })
        }
    }

    override fun onStart() {
        super.onStart()

        locationServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
                locationServiceBound = true
            }

            override fun onServiceDisconnected(p0: ComponentName?) {
                locationServiceBound = false
            }
        }

        val serviceIntent = Intent(this, MyLocationService::class.java)
        bindService(serviceIntent, locationServiceConnection, Context.BIND_AUTO_CREATE)

        if (foregroundPermissionApproved()) {
            viewModel.isListenToLocation.value = true
        } else {
            requestForegroundPermissions()
        }
    }

    override fun onStop() {
        super.onStop()

        viewModel.isListenToLocation.value = false

        if (locationServiceBound) {
            unbindService(locationServiceConnection)
            locationServiceBound = false
        }
    }

    private fun foregroundPermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun requestForegroundPermissions() {
        val provideRationale = foregroundPermissionApproved()

        if (provideRationale) {
            locationAvailabilitySnackbar
                .setAction("Ok") {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
                    )
                }
                .show()
        } else {
            Log.d(MainActivity::class.simpleName, "Request foreground only permission")
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d(MainActivity::class.simpleName, "onRequestPermissionResult")

        when (requestCode) {
            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE -> when {
                grantResults.isEmpty() -> {
                    Log.d(MainActivity::class.simpleName, "User interaction was cancelled.")
                }

                grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                    viewModel.isListenToLocation.value = true
                }

                else -> {
                    locationPermissionSnackbar
                        .setAction("Settings") {
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts(
                                "package",
                                BuildConfig.APPLICATION_ID,
                                null
                            )
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                        .show()
                }
            }
        }
    }
}