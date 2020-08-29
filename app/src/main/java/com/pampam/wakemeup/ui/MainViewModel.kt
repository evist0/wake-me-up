package com.pampam.wakemeup.ui

import android.location.Location
import androidx.lifecycle.*
import com.google.android.gms.maps.model.PointOfInterest
import com.pampam.wakemeup.data.LocationRepository
import com.pampam.wakemeup.data.PredictionsDestinationsRepository
import com.pampam.wakemeup.data.SessionRepository
import com.pampam.wakemeup.data.model.Session
import com.pampam.wakemeup.data.model.SessionRange
import com.pampam.wakemeup.data.model.SessionStatus
import com.pampam.wakemeup.extensions.isDismissRationaleRequired
import com.pampam.wakemeup.extensions.observeOnce
import com.pampam.wakemeup.extensions.toLatLng

class MainViewModel(
    private val locationRepository: LocationRepository,
    private val predictionsDestinationsRepository: PredictionsDestinationsRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {
    val location: LiveData<Location> = locationRepository.location
    val isLocationAvailable: LiveData<Boolean> = locationRepository.isLocationAvailable
    val hasLocationPermission: LiveData<Boolean> = locationRepository.hasLocationPermission

    val session: LiveData<Session?> = sessionRepository.currentSession

    private val mMarkerMoved = MutableLiveData<Boolean>()

    private val mFocusRequested = MutableLiveData<Boolean>(false)
    val isFocused: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(mFocusRequested) { requested ->
            value = requested
        }
        addSource(isLocationAvailable) { available ->
            if (value == true && !available) {
                value = false
            }
        }
        addSource(hasLocationPermission) { permission ->
            if (value == true && !permission) {
                value = false
            }
        }
        addSource(mMarkerMoved) {
            if (value == true) {
                value = true
            }
        }
        addSource(session) { session ->
            if (session?.details != null) {
                value = true
            }
        }
    }

    private val mIsLocationPermissionDialogVisible = MutableLiveData<Boolean>(false)
    val isLocationPermissionDialogVisible: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(hasLocationPermission) { isHas ->
            this.value = !isHas
        }

        addSource(mIsLocationPermissionDialogVisible) { isVisible ->
            this.value = isVisible
        }
    }

    private val mIsLocationAvailabilityDialogVisible = MutableLiveData<Boolean>(false)
    val isLocationAvailabilityDialogVisible: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(locationRepository.isLocationAvailable) { isAvailable ->
            this.value = !isAvailable
        }

        addSource(mIsLocationAvailabilityDialogVisible) { isVisible ->
            this.value = isVisible
        }
    }

    private val mMapPadding = MutableLiveData<Padding>()
    val mapPadding: LiveData<Padding> = mMapPadding

    private fun requireLocation(invokable: () -> Unit) = when {
        hasLocationPermission.value != true -> {
            mIsLocationPermissionDialogVisible.value = true
        }
        isLocationAvailable.value != true -> {
            mIsLocationAvailabilityDialogVisible.value = true
        }
        location.value == null -> {
        }
        else -> {
            invokable()
        }
    }

    fun onLocationPermissionApproved() {
        locationRepository.hasLocationPermission.value = true
    }

    fun onLocationPermissionProvideRationale() {
        mIsLocationPermissionDialogVisible.value = true
    }

    fun onLocationPermissionGranted() {
        locationRepository.hasLocationPermission.value = true
    }

    fun onLocationPermissionDenied() {
        mIsLocationPermissionDialogVisible.value = true
    }

    fun onMyLocationClick() {
        requireLocation {
            mFocusRequested.value = !mFocusRequested.value!!
        }
    }

    fun onMarkerMoved() {
        mMarkerMoved.value = true
    }

    fun onGestureCameraMove() {
        mFocusRequested.value = false
    }

    fun onPoiSelect(pointOfInterest: PointOfInterest): Boolean {
        val session = sessionRepository.currentSession.value
        val location = location.value
        if (session?.isDismissRationaleRequired(location?.toLatLng()) != true) {
            sessionRepository.currentSession.value = Session()

            val detailsLiveData =
                predictionsDestinationsRepository.fetchDestination(pointOfInterest.placeId)
            detailsLiveData.observeOnce(Observer { details ->
                val previousSession = sessionRepository.currentSession.value
                if (previousSession != null) {
                    sessionRepository.currentSession.value =
                        Session(details, SessionStatus.Inactive, SessionRange.Default)
                }
            })

            return true
        }
        return false
    }

    fun setMapPadding(padding: Padding) {
        mMapPadding.value = padding
    }
}