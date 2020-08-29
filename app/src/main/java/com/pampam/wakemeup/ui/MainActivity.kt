package com.pampam.wakemeup.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.graphics.ColorUtils
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pampam.wakemeup.BuildConfig
import com.pampam.wakemeup.LocationAlarmService
import com.pampam.wakemeup.R
import com.pampam.wakemeup.databinding.ActivityMainBinding
import com.pampam.wakemeup.extensions.toLatLng
import com.pampam.wakemeup.extensions.updatePadding
import com.pampam.wakemeup.ui.map.*
import com.pampam.wakemeup.utils.BitmapFactoryExt
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityMainBinding
    private val viewModel by viewModel<MainViewModel>()

    private lateinit var navController: NavController

    private lateinit var locationPermissionDialog: AlertDialog
    private lateinit var locationAvailabilityDialog: AlertDialog

    private lateinit var googleMap: GoogleMap

    private lateinit var locationMarker: LocationMarker
    private lateinit var destinationMarker: DestinationMarker
    private lateinit var locationCamera: LocationCamera

    private var locationServiceBound = false
    private lateinit var locationServiceConnection: ServiceConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        navController =
            with(supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment) {
                navController
            }

        locationPermissionDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.location_permission_dialog_title)
            .setMessage(R.string.location_permission_dialog_message)
            .setNegativeButton(R.string.dialog_negative_text) { _, _ -> }
            .setPositiveButton(R.string.dialog_positive_text) { _, _ ->
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
            }.create()

        locationAvailabilityDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.location_unavailable_dialog_title)
            .setMessage(R.string.location_unavailable_dialog_message)
            .setNegativeButton(R.string.dialog_negative_text) { _, _ -> }
            .setPositiveButton(R.string.dialog_positive_text) { _, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
            .create()

        with(supportFragmentManager.findFragmentById(R.id.supportMapFragment) as SupportMapFragment) {
            getMapAsync(this@MainActivity)
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

        val serviceIntent = Intent(this, LocationAlarmService::class.java)
        bindService(serviceIntent, locationServiceConnection, Context.BIND_AUTO_CREATE)

        if (foregroundPermissionApproved()) {
            viewModel.onLocationPermissionApproved()
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

    override fun onMapReady(map: GoogleMap) {
        googleMap = map.apply {
            val mapStyle = MapStyleOptions.loadRawResourceStyle(this@MainActivity, R.raw.mapstyle)
            setMapStyle(mapStyle)

            uiSettings.isIndoorLevelPickerEnabled = false
            uiSettings.isMapToolbarEnabled = false

            setOnPoiClickListener { poi/*bota*/ ->
                if (viewModel.onPoiSelect(poi)) {
                    navController.navigate(R.id.action_global_session)
                }
            }

            with(BitmapFactoryExt(resources)) {
                val locationMarkerOptions = LocationMarker.Options(
                    movingBitmap = decodeResourceScaled(
                        R.drawable.moving_online,
                        128,
                        128
                    ),
                    standingBitmap = decodeResourceScaled(
                        R.drawable.standing_online,
                        128,
                        128
                    ),
                    offlineBitmap = decodeResourceScaled(
                        R.drawable.standing_offline,
                        128,
                        128
                    ),
                    animationDuration = 1000/*MILLIS*/,
                    movementEpsilon = 10.0/*METERS*/,
                    movementTimeout = 5000/*MILLIS*/
                )
                locationMarker = addLocationMarker(locationMarkerOptions)

                val destinationMarkerOptions =
                    DestinationMarker.Options(
                        iconBitmap = decodeResourceScaled(
                            R.drawable.ic_destination,
                            96,
                            96
                        ),
                        fillColor = ColorUtils.setAlphaComponent(
                            getColor(R.color.primaryLightColor),
                            26
                        ),
                        strokeColor = getColor(R.color.primaryLightColor),
                        strokeWidth = 5.0f,
                        animationDuration = 600/*MILLIS*/
                    )
                destinationMarker = addDestinationMarker(destinationMarkerOptions)
            }

            val locationCameraOptions = LocationCamera.Options(
                defaultZoom = 17.0f,
                minZoom = 12.0f
            )
            locationCamera = addLocationCamera(locationCameraOptions).apply {
                onGestureCameraMove = viewModel::onGestureCameraMove
            }
        }

        myLocationButton.setOnClickListener {
            viewModel.onMyLocationClick()
        }

        with(viewModel) {
            location.observe(this@MainActivity, Observer { location ->
                val latLng = location.toLatLng()
                locationMarker.animateLocation(latLng) {
                    locationCamera.location = latLng
                    viewModel.onMarkerMoved()
                }
            })

            isLocationAvailable.observe(this@MainActivity, Observer { available ->
                locationMarker.locationAvailable = available
            })

            isLocationAvailabilityDialogVisible.observe(this@MainActivity, Observer { visible ->
                with(locationAvailabilityDialog) {
                    if (visible) {
                        show()
                    } else {
                        hide()
                    }
                }
            })

            isLocationPermissionDialogVisible.observe(this@MainActivity, Observer { visible ->
                with(locationPermissionDialog) {
                    if (visible) {
                        show()
                    } else {
                        hide()
                    }
                }
            })

            session.observe(this@MainActivity, Observer { session ->
                destinationMarker.apply {
                    position = session?.details?.latLng
                    if (session != null) {
                        radius = session.range.toMeters()
                    }
                }
                locationCamera.destination = session?.details?.latLng
            })

            isFocused.observe(this@MainActivity, Observer { isFocused ->
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
                    locationCamera.focus()
                }
            })



            mapPadding.observe(this@MainActivity, Observer { padding ->
                googleMap.setPadding(padding.left, padding.top, padding.right, padding.bottom)
                myLocationButtonLayout.updatePadding(padding)
            })
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
                    viewModel.onLocationPermissionGranted()
                }
                else -> {
                    viewModel.onLocationPermissionDenied()
                }
            }
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
            viewModel.onLocationPermissionProvideRationale()
        } else {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            )
        }
    }
}