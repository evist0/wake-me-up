package com.pampam.wakemeup.ui

import androidx.lifecycle.*
import com.pampam.wakemeup.data.DestinationRepository
import com.pampam.wakemeup.data.MyLocationRepository
import com.pampam.wakemeup.data.SessionRepository
import com.pampam.wakemeup.data.model.DestinationPrediction
import com.pampam.wakemeup.data.model.MyLocation
import com.pampam.wakemeup.data.model.Session
import com.pampam.wakemeup.data.model.SessionStatus
import com.pampam.wakemeup.observeOnce

class MainActivityViewModel(
    myLocationRepository: MyLocationRepository,
    private val destinationRepository: DestinationRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    val myLastLocation: LiveData<MyLocation> = myLocationRepository.myLastLocation
    val listenToLocation = myLocationRepository.isListenToLocation
    val isFocused = MutableLiveData<Boolean>(false)

    val destinationSearchQuery = MutableLiveData<String>("")
    private val destinationSearchQueryObserver = Observer<String> { query ->
        autocompleteSession?.updateQuery(myLastLocation.value?.latLng, query)
    }

    private var autocompleteSession: DestinationRepository.AutocompleteSession? = null

    private val _suggestedDestinations = MediatorLiveData<List<DestinationPrediction>>()
    val suggestedDestinations: LiveData<List<DestinationPrediction>> = _suggestedDestinations

    val currentSession: MutableLiveData<Session?> = sessionRepository.currentSession

    fun beginSearch() {
        if (autocompleteSession == null) {
            autocompleteSession =
                destinationRepository.newAutocompleteSession().apply {
                    _suggestedDestinations.addSource(autocompletionLiveData) { currentSuggestions ->
                        if (currentSuggestions != _suggestedDestinations.value) {
                            _suggestedDestinations.value = currentSuggestions
                        }
                    }
                }

            destinationSearchQuery.observeForever(destinationSearchQueryObserver)
        }
    }

    fun confirmPrediction(prediction: DestinationPrediction) {
        sessionRepository.currentSession.value = Session()

        val detailsLiveData = autocompleteSession!!.fetchDetails(prediction)
        detailsLiveData.observeOnce(Observer { details ->
            sessionRepository.currentSession.value = Session(details, SessionStatus.Inactive)
        })
        finishAutocompletion()
    }

    fun deleteRecentPrediction(prediction: DestinationPrediction) {
        destinationRepository.deleteRecentDestinationById(prediction.placeId)
    }

    private fun finishAutocompletion() {

        _suggestedDestinations.apply {
            removeSource(autocompleteSession!!.autocompletionLiveData)
            value = emptyList()
        }

        autocompleteSession = null
    }
}