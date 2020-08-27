package com.pampam.wakemeup.ui

import androidx.lifecycle.*
import com.pampam.wakemeup.data.DestinationRepository
import com.pampam.wakemeup.data.LocationRepository
import com.pampam.wakemeup.data.SessionRepository
import com.pampam.wakemeup.data.model.DestinationPrediction
import com.pampam.wakemeup.data.model.Session
import com.pampam.wakemeup.data.model.SessionRange
import com.pampam.wakemeup.data.model.SessionStatus
import com.pampam.wakemeup.observeOnce
import com.pampam.wakemeup.toLatLng

class SearchViewModel(
    private val sessionRepository: SessionRepository,
    private val destinationRepository: DestinationRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {
    private val mIsSearching = MutableLiveData<Boolean>(false)
    val isSearching: LiveData<Boolean> =
        Transformations.switchMap(sessionRepository.currentSession) { currentSession ->
            if (currentSession != null) {
                MutableLiveData(false)
            } else {
                mIsSearching
            }
        }

    val destinationSearchQuery = MutableLiveData<String>()

    private var autocompleteSession: DestinationRepository.AutocompleteSession? = null
    val suggestedDestinations: LiveData<List<DestinationPrediction>> =
        Transformations.switchMap(isSearching) { isSearching ->
            if (isSearching) {
                val currentAutocompleteSession =
                    autocompleteSession ?: destinationRepository.newAutocompleteSession()
                autocompleteSession = currentAutocompleteSession
                Transformations.switchMap(destinationSearchQuery) { searchQuery ->
                    currentAutocompleteSession.updateQuery(
                        locationRepository.location.value?.toLatLng(),
                        searchQuery
                    )
                    currentAutocompleteSession.autocompletionLiveData
                }
            } else {
                MutableLiveData(emptyList())
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
        sessionRepository.currentSession.value = Session()

        val detailsLiveData = autocompleteSession!!.fetchDetails(prediction)
        detailsLiveData.observeOnce(Observer { details ->
            val previousSession = sessionRepository.currentSession.value
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
}