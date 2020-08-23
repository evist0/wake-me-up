package com.pampam.wakemeup.ui

import androidx.lifecycle.*
import com.pampam.wakemeup.data.DestinationPredictionRepository
import com.pampam.wakemeup.data.MyLocationRepository
import com.pampam.wakemeup.data.SessionRepository
import com.pampam.wakemeup.data.model.DestinationPrediction
import com.pampam.wakemeup.data.model.MyLocation
import com.pampam.wakemeup.data.model.Session
import com.pampam.wakemeup.data.model.SessionStatus
import com.pampam.wakemeup.observeOnce

enum class MainActivityViewModelState {
    Browsing,

}

class MainActivityViewModel(
    myLocationRepository: MyLocationRepository,
    private val destinationPredictionRepository: DestinationPredictionRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    val myLastLocation: LiveData<MyLocation> = myLocationRepository.myLastLocation
    val listenToLocation = myLocationRepository.isListenToLocation
    val isFocused = MutableLiveData<Boolean>(false)

    private val _isSearching = MutableLiveData<Boolean>(false)
    val isSearching: LiveData<Boolean> = Transformations.distinctUntilChanged(_isSearching)

    val destinationSearchQuery = MutableLiveData<String>("")
    private val destinationSearchQueryObserver = Observer<String> { query ->
        autocompleteSession?.updateQuery(myLastLocation.value?.latLng, query)
    }

    private var autocompleteSession: DestinationPredictionRepository.AutocompleteSession? = null

    private val _suggestedDestinations = MediatorLiveData<List<DestinationPrediction>>()
    val suggestedDestinations: LiveData<List<DestinationPrediction>> =
        Transformations.switchMap(_isSearching) {
            if (it) _suggestedDestinations
            else MutableLiveData<List<DestinationPrediction>>(
                emptyList()
            )
        }

    val currentSession: MutableLiveData<Session?> = sessionRepository.currentSession

    fun beginSearch() {
        _isSearching.value = true
        destinationSearchQuery.value = ""
        if (autocompleteSession == null) {
            autocompleteSession =
                destinationPredictionRepository.newAutocompleteSession().apply {
                    _suggestedDestinations.addSource(autocompletionLiveData) { currentSuggestions ->
                        if (currentSuggestions != _suggestedDestinations.value) {
                            _suggestedDestinations.value = currentSuggestions
                        }
                    }
                }

            destinationSearchQuery.observeForever(destinationSearchQueryObserver)
        }
    }

    fun closeSearch() {
        _isSearching.value = false
        destinationSearchQuery.value = ""
    }

    fun endSearch(prediction: DestinationPrediction) {
        closeSearch()

        sessionRepository.currentSession.value = Session()

        val detailsLiveData = autocompleteSession!!.fetchDetails(prediction)
        detailsLiveData.observeOnce(Observer { details ->
            sessionRepository.currentSession.value = Session(details, SessionStatus.Inactive)
        })

        _suggestedDestinations.apply {
            removeSource(autocompleteSession!!.autocompletionLiveData)
            value = emptyList()
        }
        autocompleteSession = null
    }

    fun deleteRecentPrediction(prediction: DestinationPrediction) {
        destinationPredictionRepository.deleteRecentDestinationById(prediction.placeId)
    }

    private fun finishAutocompletion() {

    }

}