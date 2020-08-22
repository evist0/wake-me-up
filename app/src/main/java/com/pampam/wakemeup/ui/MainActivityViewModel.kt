package com.pampam.wakemeup.ui

import androidx.lifecycle.*
import com.pampam.wakemeup.data.DestinationRepository
import com.pampam.wakemeup.data.MyLocationRepository
import com.pampam.wakemeup.data.model.Destination
import com.pampam.wakemeup.data.model.Location

class MainActivityViewModel(
    private val myLocationRepository: MyLocationRepository,
    private val destinationRepository: DestinationRepository
) : ViewModel() {

    val myLastLocation: LiveData<Location> = myLocationRepository.myLastLocation
    val isListenToLocation = myLocationRepository.isListenToLocation
    val isFocused = MutableLiveData<Boolean>(true)

    val destinationSearchQuery = MutableLiveData<String>("")
    private val destinationSearchQueryObserver = Observer<String> { query ->
        autocompleteSession?.updateQuery(myLastLocation.value?.latLng, query)
    }

    private var autocompleteSession: DestinationRepository.AutocompleteSession? = null

    private val _suggestedDestinations = MediatorLiveData<List<Destination>>()
    val suggestedDestinations: LiveData<List<Destination>> = _suggestedDestinations

    fun onSearchBegin() {
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

    fun onSearchConfirmed(query: String) {

    }

    fun onSearchEnd() {

        _suggestedDestinations.apply {
            removeSource(autocompleteSession!!.autocompletionLiveData)
        }

        autocompleteSession = null

        destinationSearchQuery.removeObserver(destinationSearchQueryObserver)
    }
}