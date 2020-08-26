package com.pampam.wakemeup.ui

import android.location.Location
import androidx.lifecycle.*
import com.google.android.gms.maps.model.PointOfInterest
import com.pampam.wakemeup.data.DestinationRepository
import com.pampam.wakemeup.data.LocationRepository
import com.pampam.wakemeup.data.SessionRepository
import com.pampam.wakemeup.data.model.DestinationPrediction
import com.pampam.wakemeup.data.model.Session
import com.pampam.wakemeup.data.model.SessionRange
import com.pampam.wakemeup.data.model.SessionStatus
import com.pampam.wakemeup.observeOnce
import com.pampam.wakemeup.toLatLng

class MainActivityViewModel(
    locationRepository: LocationRepository,
    private val destinationRepository: DestinationRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    val location: LiveData<Location> = locationRepository.location
    val isLocationAvailable: LiveData<Boolean> = locationRepository.isLocationAvailable
    val hasLocationPermission = locationRepository.hasLocationPermission
    val isShowMyLocation = MutableLiveData<Boolean>(false)

    private val mIsSearching = MutableLiveData<Boolean>(false)
    val isSearching: LiveData<Boolean> = mIsSearching

    val destinationSearchQuery = MutableLiveData<String>()

    private var autocompleteSession: DestinationRepository.AutocompleteSession? = null
    val suggestedDestinations: LiveData<List<DestinationPrediction>> =
        Transformations.switchMap(isSearching) { isSearching ->
            if (isSearching) {
                val currentAutocompleteSession =
                    autocompleteSession ?: destinationRepository.newAutocompleteSession()
                autocompleteSession = currentAutocompleteSession
                Transformations.switchMap(destinationSearchQuery) { searchQuery ->
                    currentAutocompleteSession.updateQuery(location.value?.toLatLng(), searchQuery)
                    currentAutocompleteSession.autocompletionLiveData
                }
            } else {
                MutableLiveData(emptyList())
            }
        }

    val currentSession: LiveData<Session?> = sessionRepository.currentSession

    private val mIsLocationPermissionPopupVisible = MutableLiveData<Boolean>(false)
    val isLocationPermissionPopupVisible = MediatorLiveData<Boolean>().apply {
        addSource(hasLocationPermission) { isHas ->
            this.value = !isHas
        }

        addSource(mIsLocationPermissionPopupVisible) { isVisible ->
            this.value = isVisible
        }
    }

    private val mIsLocationAvailabilityPopupVisible = MutableLiveData<Boolean>(false)
    val isLocationAvailabilityPopupVisible = MediatorLiveData<Boolean>().apply {
        addSource(isLocationAvailable) { isAvailable ->
            this.value = !isAvailable
        }

        addSource(mIsLocationAvailabilityPopupVisible) { isVisible ->
            this.value = isVisible
        }
    }

    private fun requireLocation(invokable: () -> Unit): Boolean = when {
        hasLocationPermission.value != true -> {
            mIsLocationPermissionPopupVisible.value = true
            false
        }
        isLocationAvailable.value != true -> {
            mIsLocationAvailabilityPopupVisible.value = true
            false
        }
        else -> {
            invokable()
            true
        }
    }

    fun showMyLocation(focusFunction: () -> Unit) {
        requireLocation {
            isShowMyLocation.value = !isShowMyLocation.value!!
            if (isShowMyLocation.value == true) {
                focusFunction()
            }
        }
    }

    fun beginSearch() {
        mIsSearching.value = true
        destinationSearchQuery.value = ""
    }

    fun closeSearch() {
        mIsSearching.value = false
    }

    fun endSearch(prediction: DestinationPrediction) {
        closeSearch()

        sessionRepository.currentSession.value = Session()

        val detailsLiveData = autocompleteSession!!.fetchDetails(prediction)
        detailsLiveData.observeOnce(Observer { details ->
            val previousSession = currentSession.value
            if (previousSession != null) {
                sessionRepository.currentSession.value =
                    Session(details, SessionStatus.Inactive, SessionRange.Default)
            }
        })

        autocompleteSession = null
    }

    fun deleteRecentPrediction(prediction: DestinationPrediction) {
        destinationRepository.deleteRecentDestinationById(prediction.placeId)
    }

    fun clickPoi(poi: PointOfInterest) {
        sessionRepository.currentSession.value = Session()

        val detailsLiveData = destinationRepository.fetchDetails(poi)
        detailsLiveData.observeOnce(Observer { details ->
            val previousSession = currentSession.value
            if (previousSession != null) {
                sessionRepository.currentSession.value =
                    Session(details, SessionStatus.Inactive, SessionRange.Default)
            }
        })
    }

    override fun onCleared() {
        super.onCleared()

        isLocationAvailabilityPopupVisible.removeSource(isLocationAvailable)
        isLocationAvailabilityPopupVisible.removeSource(mIsLocationAvailabilityPopupVisible)

        isLocationPermissionPopupVisible.removeSource(hasLocationPermission)
        isLocationPermissionPopupVisible.removeSource(mIsLocationPermissionPopupVisible)
    }
}