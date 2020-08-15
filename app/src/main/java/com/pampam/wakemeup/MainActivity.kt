package com.pampam.wakemeup

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
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
import com.pampam.wakemeup.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module


class MainActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleMap.OnCameraMoveStartedListener {

    private var permissionRequestCode = 0

    private lateinit var binding: ActivityMainBinding
    private val mainActivityViewModel by viewModel<MainActivityViewModel>()
    private val appModule = module {
        single { getSystemService(Context.LOCATION_SERVICE) as LocationManager }
        single { LocationService(get(), get()) }
        single { LocationRepository(get()) }
        single { MainActivityViewModel(get()) }
    }

    private lateinit var mMap: GoogleMap
    private var locationMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        with(googleMap) {
            setOnCameraMoveStartedListener(this@MainActivity)
        }

        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                startKoin {
                    androidContext(this@MainActivity)
                    modules(appModule)
                }
                binding.viewModel = mainActivityViewModel
                binding.lifecycleOwner = this

                showMeButton.setOnClickListener {
                    mainActivityViewModel.isUserFocused.value =
                        !mainActivityViewModel.isUserFocused.value!!

                    if (mainActivityViewModel.isUserFocused.value == true) {
                        val userLatLng = LatLng(
                            mainActivityViewModel.location.value!!.latitude,
                            mainActivityViewModel.location.value!!.longitude
                        )
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 17.0f))
                    }
                }

                mainActivityViewModel.location.observe(this,
                    Observer { location ->
                        val newLatLng = LatLng(location.latitude, location.longitude)
                        if (locationMarker == null) {
                            locationMarker = mMap.addMarker(
                                MarkerOptions().title("Your position").position(newLatLng)
                            )
                        } else {
                            locationMarker!!.position =
                                newLatLng
                        }

                        if (mainActivityViewModel.isUserFocused.value == true) {
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(newLatLng))
                        }
                    })
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
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