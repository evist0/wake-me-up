package com.pampam.wakemeup

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.pampam.wakemeup.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.androidx.viewmodel.ext.android.viewModel


class MainActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleMap.OnCameraMoveStartedListener {

    private var permissionRequestCode = 0

    private lateinit var binding: ActivityMainBinding
    private val mainActivityViewModel by viewModel<MainActivityViewModel>()

    private lateinit var mMap: GoogleMap
    private lateinit var locationMarker: Marker

    private lateinit var locationOffSnackbar: Snackbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        locationOffSnackbar = Snackbar.make(map, "Геолокация выключена", Snackbar.LENGTH_INDEFINITE)

        binding.viewModel = mainActivityViewModel
        binding.lifecycleOwner = this

        showMeButton.setOnClickListener {
            val userFocused = !mainActivityViewModel.isUserFocused.value!!
            mainActivityViewModel.isUserFocused.value = userFocused

            if (userFocused) {
                val location = mainActivityViewModel.location.value!!.second
                location?.apply {
                    mMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                location.latitude,
                                location.longitude
                            ),
                            17.0f
                        )
                    )
                }
            }
        }

        mainActivityViewModel.location.observe(this,
            Observer { location ->
                if (location.first) {
                    locationOffSnackbar.dismiss()
                } else {
                    locationOffSnackbar.show()
                }
                val locationValue = location.second
                if (locationValue != null) {
                    val newLatLng =
                        LatLng(locationValue.latitude, locationValue.longitude)
                    locationMarker.position = newLatLng
                    locationMarker.isVisible = true

                    if (mainActivityViewModel.isUserFocused.value == true) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(newLatLng))
                    }
                }
            })

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap.apply {
            setOnCameraMoveStartedListener(this@MainActivity)

            locationMarker = addMarker(
                MarkerOptions().title("Your position").position(LatLng(0.0, 0.0))
            ).apply {
                isVisible = false
            }
        }

        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {


            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Toast.makeText(this, "Нам необходимо знать, когда вас будить.", Toast.LENGTH_SHORT)
                    .show()
            }
            else -> {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    permissionRequestCode
                )
            }
        }
    }

    override fun onCameraMoveStarted(reason: Int) {
        when (reason) {
            GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE -> {
                mainActivityViewModel.isUserFocused.value = false
            }
        }
    }

}