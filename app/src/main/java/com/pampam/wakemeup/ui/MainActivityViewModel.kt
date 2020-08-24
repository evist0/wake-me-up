package com.pampam.wakemeup.ui

import android.location.Location
import androidx.lifecycle.*
import com.pampam.wakemeup.data.DestinationPredictionRepository
import com.pampam.wakemeup.data.LocationRepository
import com.pampam.wakemeup.data.SessionRepository
import com.pampam.wakemeup.data.model.*
import com.pampam.wakemeup.observeOnce
import com.pampam.wakemeup.toLatLng

class MainActivityViewModel(
    locationRepository: LocationRepository,
    private val destinationPredictionRepository: DestinationPredictionRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    val location: LiveData<Location> = locationRepository.location
    val isLocationAvailable: LiveData<Boolean> = locationRepository.isLocationAvailable
    val hasLocationPermission = locationRepository.hasLocationPermission
    val isShowMyLocation = MutableLiveData<Boolean>(false)

    private val mIsSearching = MutableLiveData<Boolean>(false)
    val isSearching: LiveData<Boolean> = mIsSearching

    val destinationSearchQuery = MutableLiveData<String>()

    private var autocompleteSession: DestinationPredictionRepository.AutocompleteSession? = null
    val suggestedDestinations: LiveData<List<DestinationPrediction>> =
        Transformations.switchMap(isSearching) { isSearching ->
            if (isSearching) {
                val currentAutocompleteSession =
                    destinationPredictionRepository.newAutocompleteSession()
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
        addSource(hasLocationPermission, Observer { isHas ->
            this.value = !isHas
        })

        addSource(mIsLocationPermissionPopupVisible, Observer { isVisible ->
            this.value = isVisible
        })
    }

    private val mIsLocationAvailabilityPopupVisible = MutableLiveData<Boolean>(false)
    val isLocationAvailabilityPopupVisible = MediatorLiveData<Boolean>().apply {
        addSource(isLocationAvailable, Observer { isAvailable ->
            this.value = !isAvailable
        })

        addSource(mIsLocationAvailabilityPopupVisible, Observer { isVisible ->
            this.value = isVisible
        })
    }

    fun showMyLocation(focusFunction: () -> Unit) {
        if (hasLocationPermission.value != true) {
            mIsLocationPermissionPopupVisible.value = true
        } else if (isLocationAvailable.value != true) {
            mIsLocationAvailabilityPopupVisible.value = true
        } else {
            if (location.value != null) {
                isShowMyLocation.value = !isShowMyLocation.value!!
                if (isShowMyLocation.value == true) {
                    focusFunction()
                }
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
            sessionRepository.currentSession.value =
                Session(details, SessionStatus.Inactive, SessionRange.Default)
        })
    }

    fun deleteRecentPrediction(prediction: DestinationPrediction) {
        destinationPredictionRepository.deleteRecentDestinationById(prediction.placeId)
    }

    fun setSessionRange(range: SessionRange) {
        sessionRepository.currentSession.value = currentSession.value!!.mutated(range)
    }

    fun setSessionActive() {
        sessionRepository.currentSession.value =
            currentSession.value!!.mutated(SessionStatus.Active)
    }

    fun cancelSession() {
        sessionRepository.currentSession.value = null
    }
}