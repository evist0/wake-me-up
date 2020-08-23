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
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.marginBottom
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.SphericalUtil
import com.mancj.materialsearchbar.MaterialSearchBar.OnSearchActionListener
import com.pampam.wakemeup.BuildConfig
import com.pampam.wakemeup.R
import com.pampam.wakemeup.data.MyLocationService
import com.pampam.wakemeup.data.model.MyLocationStatus
import com.pampam.wakemeup.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34

class MainActivity : AppCompatActivity(), OnMapReadyCallback, OnSearchActionListener {

    private val statusBarHeight by lazy {
        var statusBarHeight = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = resources.getDimensionPixelSize(resourceId)
        }

        statusBarHeight
    }

    private val navBarHeight by lazy {
        var navBarHeight = 0
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            navBarHeight = resources.getDimensionPixelSize(resourceId)
        }

        navBarHeight
    }

    private lateinit var binding: ActivityMainBinding
    private val viewModel by viewModel<MainActivityViewModel>()

    private lateinit var map: GoogleMap
    private lateinit var destinationMarker: Marker
    private lateinit var destinationRadius: Circle
    private lateinit var myLocationMarker: MyLocationMarker

    private lateinit var popupView: PopupView

    private var locationServiceBound = false
    private lateinit var locationServiceConnection: ServiceConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        inflateWithDataBinding()

        adjustControlLayoutTranslucentMargins()

        initMyLocationButton()
        initSearchBar()
        initPopup()
        initMapAsync()

        observeIsFocused()
        observeSuggestedDestinations()
    }

    private fun observeSession() {
        viewModel.currentSession.observe(this, Observer { session ->
            map.setPadding(
                searchBar.marginStart,
                statusBarHeight + myLocationButton.marginBottom,
                searchBar.marginEnd,
                searchDetailsLayout.height + myLocationButton.marginBottom + navBarHeight
            )

            if (session != null) {
                map.uiSettings.setAllGesturesEnabled(false)
            } else {
                map.uiSettings.setAllGesturesEnabled(true)
            }

            if (session?.details != null) {
                focusCamera(myLocationMarker.location.latLng)

                destinationMarker.apply {
                    position = session.details.latLng
                    isVisible = true
                }

                destinationRadius.apply {
                    center = session.details.latLng
                    radius = session.range.toMeters()
                    isVisible = true
                }
            }
        })
    }

    private fun focusCamera(myLocation: LatLng?) {
        val destinationLatLng = viewModel.currentSession.value?.details?.latLng
        val cameraUpdate =
            if (destinationLatLng != null) {
                CameraUpdateFactory.newLatLngBounds(
                    with(LatLngBounds.builder().apply {
                        myLocation?.let {
                            include(myLocation)
                        }
                        include(destinationLatLng)
                    }.build()) {
                        LatLngBounds(
                            SphericalUtil.interpolate(northeast, southwest, 1.25),
                            SphericalUtil.interpolate(southwest, northeast, 1.25)
                        )
                    }, 0
                )
            } else if (myLocation != null) {
                val shouldZoom =
                    !map.projection.visibleRegion.latLngBounds.contains(myLocation) || map.cameraPosition.zoom > 20.0f
                if (shouldZoom) {
                    CameraUpdateFactory.newLatLngZoom(myLocation, 17.0f)
                } else {
                    CameraUpdateFactory.newLatLng(myLocation)
                }
            } else {
                null
            }

        cameraUpdate?.let {
            map.animateCamera(it)
        }
    }

    private fun adjustControlLayoutTranslucentMargins() {
        val controlViewLayoutParams = controlsLayout.layoutParams as FrameLayout.LayoutParams
        controlViewLayoutParams.apply {
            setMargins(
                leftMargin,
                topMargin + statusBarHeight,
                rightMargin,
                bottomMargin + navBarHeight
            )
        }
        controlsLayout.layoutParams = controlViewLayoutParams
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
            viewModel.listenToLocation.value = true
        } else {
            requestForegroundPermissions()
        }
    }

    override fun onStop() {
        super.onStop()

        viewModel.listenToLocation.value = false

        if (locationServiceBound) {
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

        val serviceIntent = Intent(this, MyLocationService::class.java)
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
            showLocationPermissionRequired()
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
                    viewModel.listenToLocation.value = true
                }

                else -> {
                    showLocationPermissionRequired()
                }
            }
        }
    }

    override fun onButtonClicked(buttonCode: Int) {

    }

    override fun onSearchStateChanged(enabled: Boolean) {
        ConstraintSet().apply {
            clone(controlsLayout)
            clear(
                R.id.searchDetailsLayout,
                if (enabled) ConstraintSet.BOTTOM else ConstraintSet.TOP
            )
            connect(
                R.id.searchDetailsLayout,
                if (enabled) ConstraintSet.TOP else ConstraintSet.BOTTOM,
                R.id.controlsLayout,
                if (enabled) ConstraintSet.TOP else ConstraintSet.BOTTOM
            )
            applyTo(controlsLayout)
        }

        if (enabled) {
            viewModel.beginSearch()
        }
    }

    override fun onSearchConfirmed(query: CharSequence?) {

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
                        viewModel.isFocused.value = false
                    }
                }
            }
        }

        initMyLocationMarker()

        observeMyLastLocation()
        observeSession()
    }

    private fun showLocationPermissionRequired() {
        popupView.apply {
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
        }.show()
    }

    private fun showLocationUnavailable() {
        popupView.apply {
            title = getString(R.string.location_unavailable_title)
            message = getString(R.string.location_unavailable_message)
            setOnAcceptClickListener {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        }.show()
    }

    private fun observeMyLastLocation() {
        viewModel.myLastLocation.observe(this, Observer { location ->
            if (location != null) {
                popupView.dismiss()
                if (location.latLng != null) {
                    myLocationMarker.location = location
                }

                if (!location.status.isAvailable()) {
                    showLocationUnavailable()
                }
            }
        })
    }

    private fun initMyLocationMarker() {
        fun decodeScaledBitmap(id: Int): Bitmap {
            return Bitmap.createScaledBitmap(
                BitmapFactory.decodeResource(
                    resources,
                    id
                ), 128, 128, true
            )
        }

        val myLocationMarkerResources = MyLocationMarkerResources(
            movingOnline = decodeScaledBitmap(R.drawable.moving_online),
            standingOnline = decodeScaledBitmap(R.drawable.standing_online),
            movingOffline = decodeScaledBitmap(R.drawable.moving_offline),
            standingOffline = decodeScaledBitmap(R.drawable.standing_offline)
        )

        destinationMarker = map.addMarker(MarkerOptions().position(LatLng(0.0, 0.0)).visible(false))
        destinationRadius = map.addCircle(CircleOptions().apply {
            val typedValue = TypedValue()
            theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
            val fill = ColorUtils.setAlphaComponent(typedValue.data, 0x80)
            fillColor(fill)
            strokeColor(getColor(R.color.white))
            center(LatLng(0.0, 0.0))
            visible(false)
        })
        myLocationMarker =
            map.addLocationMarker(myLocationMarkerResources, 6000, 10.0) { newLocation ->
                if (viewModel.isFocused.value == true || viewModel.currentSession.value != null) {
                    focusCamera(newLocation)
                }
            }
    }

    private fun initMapAsync() {
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragmentView) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun initPopup() {
        popupView = PopupView(this)
        rootView.addView(popupView)
    }

    private fun initSearchBar() {
        searchBar.searchEditText.addTextChangedListener { editable ->
            viewModel.destinationSearchQuery.value = editable.toString()
        }
        val predictionsAdapter = DestinationPredictionsAdapter(layoutInflater).apply {
            onPredictionSelect = { prediction ->
                viewModel.confirmPrediction(prediction)
                searchBar.closeSearch()
            }
            onPredictionDelete = { prediction ->
                viewModel.deleteRecentPrediction(prediction)
            }
        }
        searchBar.setCustomSuggestionAdapter(predictionsAdapter)
        searchBar.setOnSearchActionListener(this)
    }

    private fun initMyLocationButton() {
        myLocationButton.setOnClickListener {
            val myLastLocation = viewModel.myLastLocation.value
            if (myLastLocation != null) {
                if (myLastLocation.status == MyLocationStatus.Unavailable) {
                    showLocationUnavailable()
                } else {
                    viewModel.isFocused.value = !viewModel.isFocused.value!!

                    val latLng = myLocationMarker.location.latLng
                    if (viewModel.isFocused.value == true && latLng != null) {
                        focusCamera(latLng)
                    }
                }
            } else if (viewModel.listenToLocation.value != true) {
                showLocationPermissionRequired()
            }
        }
    }

    private fun observeSuggestedDestinations() {
        viewModel.suggestedDestinations.observe(this, Observer { suggestedDestinations ->
            searchBar.updateLastSuggestions(suggestedDestinations)
        })
    }

    private fun observeIsFocused() {
        viewModel.isFocused.observe(this, Observer { isFocused ->
            val tint = if (isFocused) {
                val typedValue = TypedValue()
                theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
                typedValue.data
            } else {
                getColor(R.color.quantum_grey)
            }
            myLocationButton.imageTintList = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_enabled)),
                intArrayOf(tint)
            )
        })
    }
}