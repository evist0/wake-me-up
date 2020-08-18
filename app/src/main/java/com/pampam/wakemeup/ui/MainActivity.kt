package com.pampam.wakemeup.ui

import android.Manifest
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
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.material.snackbar.Snackbar
import com.mancj.materialsearchbar.MaterialSearchBar.OnSearchActionListener
import com.pampam.wakemeup.BuildConfig
import com.pampam.wakemeup.R
import com.pampam.wakemeup.data.MyLocationService
import com.pampam.wakemeup.databinding.ActivityMainBinding
import com.pampam.wakemeup.ui.animation.LocationMarker
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
    private lateinit var myLocationMarker: LocationMarker

    private lateinit var locationAvailabilityPopUp: PopupView
    private lateinit var locationAvailabilitySnackbar: Snackbar
    private lateinit var locationPermissionSnackbar: Snackbar

    private var locationServiceBound = false
    private lateinit var locationServiceConnection: ServiceConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        inflateWithDataBinding()

        adjustControlLayoutTranslucentMargins()

        initMyLocationButton()
        initSearchBar()
        initLocationAvailabilityPopup()
        initLocationPermissionSnackbar()
        initMapAsync()

        observeSuggestedDestinations()

    }

    private fun adjustControlLayoutTranslucentMargins() {
        val controlViewLayoutParams = controlView.layoutParams as FrameLayout.LayoutParams
        controlViewLayoutParams.apply {
            setMargins(
                leftMargin,
                topMargin + statusBarHeight,
                rightMargin,
                bottomMargin + navBarHeight
            )
        }
        controlView.layoutParams = controlViewLayoutParams
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

    override fun onButtonClicked(buttonCode: Int) {
        TODO("Not yet implemented")
    }

    override fun onSearchStateChanged(enabled: Boolean) {
        ConstraintSet().apply {
            clone(controlView)
            clear(R.id.searchBar, if (enabled) ConstraintSet.BOTTOM else ConstraintSet.TOP)
            connect(
                R.id.searchBar,
                if (enabled) ConstraintSet.TOP else ConstraintSet.BOTTOM,
                R.id.controlView,
                if (enabled) ConstraintSet.TOP else ConstraintSet.BOTTOM
            )
            applyTo(controlView)
        }

        if (enabled) viewModel.onSearchBegin() else viewModel.onSearchEnd()
    }

    override fun onSearchConfirmed(query: CharSequence?) {
        viewModel.onSearchConfirmed(query.toString())
    }

    override fun onMapReady(it: GoogleMap) {
        map = it.apply {
            setPadding(
                searchBar.marginStart,
                statusBarHeight,
                searchBar.marginEnd,
                searchBar.height + navBarHeight
            )

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
    }

    private fun observeMyLastLocation() {
        viewModel.myLastLocation.observe(this, Observer { location ->
            if (location != null) {
                if (location.latLng != null) {
                    myLocationMarker.setLocation(location) {
                        if (viewModel.isFocused.value == true) {
                            map.animateCamera(
                                CameraUpdateFactory.newLatLng(
                                    myLocationMarker.getLocation()
                                )
                            )
                        }
                    }
                }

                if (location.status.isAvailable()) {
                    locationAvailabilityPopUp.hide()
                } else {
                    locationAvailabilityPopUp.show(rootView)
                }
            }
        })
    }

    private fun initMyLocationMarker() {
        myLocationMarker = LocationMarker(map, this)
    }

    private fun initMapAsync() {
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragmentView) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun initLocationPermissionSnackbar() {
        locationPermissionSnackbar = Snackbar.make(
            mapFragmentView,
            "Location permission required",
            Snackbar.LENGTH_INDEFINITE
        )
    }

    private fun initLocationAvailabilityPopup() {
        locationAvailabilityPopUp =
            PopupView(this).setTitle(getString(R.string.location_unavailable))
                .setCallback { result ->
                    when (result) {
                        PopupAction.ACCEPT -> {
                            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            startActivity(intent)
                            locationAvailabilityPopUp.hide()
                        }

                        PopupAction.DENY -> {
                            locationAvailabilityPopUp.hide()
                        }
                    }
                }
    }

    private fun initSearchBar() {
        searchBar.searchEditText.addTextChangedListener { editable ->
            viewModel.destinationSearchQuery.value = editable.toString()
        }
        searchBar.setCustomSuggestionAdapter(DestinationsAdapter(layoutInflater))
        searchBar.setOnSearchActionListener(this)
    }

    private fun initMyLocationButton() {
        myLocationButton.setOnClickListener {
            val myLastLocation = viewModel.myLastLocation.value
            if (myLastLocation != null) {
                viewModel.isFocused.value = !viewModel.isFocused.value!!

                if (viewModel.isFocused.value == true) {
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            myLocationMarker.getLocation(),
                            17.0f
                        )
                    )
                }
            }
        }
    }

    private fun observeSuggestedDestinations() {
        viewModel.suggestedDestinations.observe(this, Observer { suggestedDestinations ->
            searchBar.updateLastSuggestions(suggestedDestinations)
        })
    }
}