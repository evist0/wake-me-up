package com.pampam.wakemeup.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.graphics.ColorUtils
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.SphericalUtil

import com.pampam.wakemeup.BuildConfig
import com.pampam.wakemeup.LocationAlarmService
import com.pampam.wakemeup.R
import com.pampam.wakemeup.databinding.ActivityMainBinding
import com.pampam.wakemeup.toLatLng
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    private val viewModel by viewModel<MainActivityViewModel>()

    private lateinit var map: GoogleMap

    private lateinit var camera: EnqueuedCamera

    private lateinit var destinationMarker: DestinationMarker
    private lateinit var locationMarker: LocationMarker

    private lateinit var locationPermissionPopup: PopupView
    private lateinit var locationAvailabilityPopup: PopupView
    private var locationServiceBound = false

    private lateinit var locationServiceConnection: ServiceConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        inflateWithDataBinding()

        initMapAsync()
    }

    private fun observeSession() {
        viewModel.currentSession.observe(this, Observer { session ->
            if (session != null) {
                if (session.details != null) {
                    destinationMarker.apply {
                        position = session.details.latLng
                        radius = session.range.toMeters()
                        isVisible = true
                    }
                }
            } else {
                destinationMarker.apply {
                    isVisible = false
                }
            }
        })
    }

    private fun focusCamera() {
        val myLocation = viewModel.location.value?.toLatLng()
        val destinationLatLng = viewModel.currentSession.value?.details?.latLng
        val cameraUpdate =
            if (destinationLatLng != null) {
                if (myLocation != null) {
                    CameraUpdateFactory.newLatLngBounds(
                        with(LatLngBounds.builder().apply {
                            include(myLocation)
                            include(destinationLatLng)
                        }.build()) {
                            LatLngBounds(
                                SphericalUtil.interpolate(northeast, southwest, 1.25),
                                SphericalUtil.interpolate(southwest, northeast, 1.25)
                            )
                        }, 0
                    )
                } else {
                    CameraUpdateFactory.newLatLngZoom(destinationLatLng, 15.0f)
                }
            } else if (myLocation != null) {
                val shouldZoom =
                    !map.projection.visibleRegion.latLngBounds.contains(myLocation) || map.cameraPosition.zoom < 12.0f
                if (shouldZoom) {
                    CameraUpdateFactory.newLatLngZoom(myLocation, 17.0f)
                } else {
                    CameraUpdateFactory.newLatLng(myLocation)
                }
            } else {
                null
            }

        cameraUpdate?.let {
            camera.enqueue(it)
        }
    }

    private fun inflateWithDataBinding() {
        binding =
            DataBindingUtil.setContentView<ActivityMainBinding>(
                this,
                R.layout.activity_main
            )
                .apply {
                    viewModel = this@MainActivity.viewModel
                    lifecycleOwner = this@MainActivity
                }
    }

    override fun onStart() {
        super.onStart()

        bindLocationService()

        if (foregroundPermissionApproved()) {
            viewModel.hasLocationPermission.value = true
        } else {
            requestForegroundPermissions()
        }
    }

    override fun onStop() {
        super.onStop()

        if (locationServiceBound && !isChangingConfigurations) {
            unbindService(locationServiceConnection)
            locationServiceBound = false
        }
    }

    private fun bindLocationService() {
        locationServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
                locationServiceBound = true
            }

            override fun onServiceDisconnected(p0: ComponentName?) {
                locationServiceBound = false
            }
        }

        val serviceIntent = Intent(this, LocationAlarmService::class.java)
        bindService(serviceIntent, locationServiceConnection, Context.BIND_AUTO_CREATE)
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
            locationPermissionPopup.show()
        } else {
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
                    viewModel.hasLocationPermission.value = true
                }

                else -> {
                    locationPermissionPopup.show()
                }
            }
        }
    }

    override fun onMapReady(it: GoogleMap) {
        map = it.apply {
            val mapStyle =
                MapStyleOptions.loadRawResourceStyle(this@MainActivity, R.raw.mapstyle)
            setMapStyle(mapStyle)

            uiSettings.isIndoorLevelPickerEnabled = false
            uiSettings.isMapToolbarEnabled = false

            setOnCameraMoveStartedListener { reason ->
                when (reason) {
                    GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE -> {
                        camera.clear()
                        viewModel.onGestureMove()
                    }
                }
            }

            setOnPoiClickListener { poi ->
                viewModel.selectPoi(poi)
            }
        }

        initEnqueuedCamera()
        initLocationPermissionPopup()
        initLocationAvailabilityPopup()
        initMyLocationMarker()
        initMyLocationButton()

        observeIsLocationAvailable()
        observeHasLocationPermissionPopupVisible()
        observeIsLocationAvailablePopupVisible()
        observeIsFocused()
        observeMyLastLocation()
        observeSession()
    }

    private fun initEnqueuedCamera() {
        camera = EnqueuedCamera(map)
    }

    private fun observeHasLocationPermissionPopupVisible() {
        viewModel.isLocationPermissionPopupVisible.observe(this, Observer { visible ->
            if (visible) locationPermissionPopup.show() else locationPermissionPopup.dismiss()
        })
    }

    private fun observeIsLocationAvailable() {
        viewModel.isLocationAvailable.observe(this, Observer { available ->
            locationMarker.updateLocationAvailable(available)
        })
    }

    private fun observeIsLocationAvailablePopupVisible() {
        viewModel.isLocationAvailabilityPopupVisible.observe(this, Observer { visible ->
            if (visible) locationAvailabilityPopup.show() else locationAvailabilityPopup.dismiss()
        })
    }

    private fun observeMyLastLocation() {
        viewModel.location.observe(this, Observer { location ->
            locationMarker.animateLocation(location) {
                viewModel.onMarkerMoved()
            }
        })
    }

    private fun initMyLocationMarker() {
        fun decodeScaledBitmap(id: Int, width: Int, height: Int): Bitmap {
            return Bitmap.createScaledBitmap(
                BitmapFactory.decodeResource(
                    resources,
                    id
                ), width, height, true
            )
        }

        val myLocationMarkerResources = MyLocationMarkerResources(
            movingOnline = decodeScaledBitmap(R.drawable.moving_online, 128, 128),
            standingOnline = decodeScaledBitmap(R.drawable.standing_online, 128, 128),
            movingOffline = decodeScaledBitmap(R.drawable.moving_offline, 128, 128),
            standingOffline = decodeScaledBitmap(R.drawable.standing_offline, 128, 128)
        )
        locationMarker = map.addLocationMarker(myLocationMarkerResources, 6000, 10.0)

        val destinationMarkerResources =
            DestinationMarkerResources(
                icon = decodeScaledBitmap(R.drawable.destination, 96, 96),
                fillColor = ColorUtils.setAlphaComponent(getColor(R.color.primaryLightColor), 26),
                strokeColor = getColor(R.color.primaryLightColor)
            )
        destinationMarker = map.addDestinationMarker(destinationMarkerResources, 500)
    }

    private fun initMapAsync() {
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragmentView) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun initLocationPermissionPopup() {
        locationPermissionPopup = PopupView(this).apply {
            title = getString(R.string.location_permission_title)
            message = getString(R.string.location_permission_message)
            setOnAcceptClickListener {
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
        }
        rootView.addView(locationPermissionPopup)
    }

    private fun initLocationAvailabilityPopup() {
        locationAvailabilityPopup = PopupView(this).apply {
            title = getString(R.string.location_unavailable_title)
            message = getString(R.string.location_unavailable_message)
            setOnAcceptClickListener {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        }
        rootView.addView(locationAvailabilityPopup)
    }

    private fun initMyLocationButton() {
        myLocationButton.setOnClickListener {
            viewModel.toggleShowMyLocation()
        }
    }

    private fun observeIsFocused() {
        viewModel.isFocused.observe(this, Observer { isFocused ->
            val tint = if (isFocused) {
                val typedValue = TypedValue()
                theme.resolveAttribute(R.attr.colorControlActivated, typedValue, true)
                getColor(typedValue.resourceId)
            } else {
                val typedValue = TypedValue()
                theme.resolveAttribute(R.attr.colorControlNormal, typedValue, true)
                getColor(typedValue.resourceId)
            }
            myLocationButton.imageTintList = ColorStateList.valueOf(tint)

            if (isFocused) {
                focusCamera()
            }
        })
    }
}