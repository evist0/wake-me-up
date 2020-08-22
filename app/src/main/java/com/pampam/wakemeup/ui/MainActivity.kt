package com.pampam.wakemeup.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.mancj.materialsearchbar.MaterialSearchBar.OnSearchActionListener
import com.pampam.wakemeup.BuildConfig
import com.pampam.wakemeup.R
import com.pampam.wakemeup.data.MyLocationService
import com.pampam.wakemeup.data.model.LocationStatus
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
    private lateinit var myLocationMarker: LocationMarker

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
                ), 64, 64, true
            )
        }

        val myLocationMarkerResources = LocationMarkerResources(
            movingOnline = decodeScaledBitmap(R.drawable.moving_online),
            standingOnline = decodeScaledBitmap(R.drawable.standing_online),
            movingOffline = decodeScaledBitmap(R.drawable.moving_offline),
            standingOffline = decodeScaledBitmap(R.drawable.standing_offline)
        )

        myLocationMarker =
            map.addLocationMarker(myLocationMarkerResources, 6000, 10.0) { newLocation ->
                if (viewModel.isFocused.value == true) {
                    map.animateCamera(CameraUpdateFactory.newLatLng(newLocation))
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
        searchBar.setCustomSuggestionAdapter(DestinationsAdapter(layoutInflater))
        searchBar.setOnSearchActionListener(this)
    }

    private fun initMyLocationButton() {
        myLocationButton.setOnClickListener {
            val myLastLocation = viewModel.myLastLocation.value
            if (myLastLocation != null) {
                if (myLastLocation.status == LocationStatus.Unavailable) {
                    showLocationUnavailable()
                } else {
                    popupView.dismiss()
                    viewModel.isFocused.value = !viewModel.isFocused.value!!
                    if (viewModel.isFocused.value == true) {
                        map.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                myLocationMarker.location.latLng,
                                17.0f
                            )
                        )
                    }
                }
            } else if (viewModel.listenToLocation.value == false) {
                showLocationPermissionRequired()
            }
        }
    }

    private fun observeSuggestedDestinations() {
        viewModel.suggestedDestinations.observe(this, Observer { suggestedDestinations ->
            searchBar.updateLastSuggestions(suggestedDestinations)
        })
    }
}