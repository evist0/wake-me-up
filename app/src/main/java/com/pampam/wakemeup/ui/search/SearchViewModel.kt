package com.pampam.wakemeup.ui.search

import androidx.lifecycle.*
import com.pampam.wakemeup.data.LocationRepository
import com.pampam.wakemeup.data.PredictionsDestinationsRepository
import com.pampam.wakemeup.data.SessionRepository
import com.pampam.wakemeup.data.model.*
import com.pampam.wakemeup.extensions.observeOnce
import com.pampam.wakemeup.extensions.toLatLng


class SearchViewModel(
    private val sessionRepository: SessionRepository,
    private val predictionsDestinationsRepository: PredictionsDestinationsRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {
    val query = MutableLiveData<String>("")

    private val updateQueryTrigger = MediatorLiveData<Any>().apply {
        addSource(query) {
            value = Any()
        }

        addSource(locationRepository.location) {
            value = Any()
        }
    }

    private val predictions = Transformations.map(updateQueryTrigger) {
        val session = predictionsDestinationsRepository.requireAutocompleteSession()
        session.updateQuery(query.value!!, locationRepository.location.value?.toLatLng())
        Pair(session.recentPredictionsLiveData, session.remotePredictionsLiveData)
    }

    val recentPredictions = Transformations.switchMap(predictions) { recentRemotePair ->
        recentRemotePair.first
    }

    val remotePredictions = Transformations.switchMap(predictions) { recentRemotePair ->
        recentRemotePair.second
    }

    fun onPredictionSelect(prediction: Prediction) {
        sessionRepository.currentSession.value = Session()

        val session = predictionsDestinationsRepository.requireAutocompleteSession()
        val detailsLiveData = session.fetchDestinationFromPrediction(prediction)
        detailsLiveData.observeOnce(Observer { details ->
            val previousSession = sessionRepository.currentSession.value
            if (previousSession != null) {
                sessionRepository.currentSession.value =
                    Session(details, SessionStatus.Inactive, SessionRange.Default)
            }
        })
    }

    fun onRecentPredictionDelete(prediction: RecentPrediction) {
        predictionsDestinationsRepository.deleteRecentDestinationById(prediction.placeId)
    }
}