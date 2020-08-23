package com.pampam.wakemeup.ui

import androidx.lifecycle.*
import com.pampam.wakemeup.data.DestinationRepository
import com.pampam.wakemeup.data.MyLocationRepository
import com.pampam.wakemeup.data.model.DestinationPrediction
import com.pampam.wakemeup.data.model.MyLocation

class MainActivityViewModel(
    myLocationRepository: MyLocationRepository,
    private val destinationRepository: DestinationRepository
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

    fun onSearchBegin() {
        if (autocompleteSession != null) {
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

    private fun endSession() {

        _suggestedDestinations.apply {
            removeSource(autocompleteSession!!.autocompletionLiveData)
        }

        autocompleteSession = null
    }
}