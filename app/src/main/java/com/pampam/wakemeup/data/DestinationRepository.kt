package com.pampam.wakemeup.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.maps.android.SphericalUtil
import com.pampam.wakemeup.data.db.RecentDestinationDao
import com.pampam.wakemeup.data.db.RecentDestinationEntity
import com.pampam.wakemeup.data.model.Destination
import com.pampam.wakemeup.data.model.DestinationSource


class DestinationRepository(
    private val recentDestinationDao: RecentDestinationDao,
    private val placesClient: PlacesClient
) {

    inner class AutocompleteSession {
        private val _queryOriginLiveData = MutableLiveData<Pair<String, LatLng?>>()

        private val _recentPredictionsLiveData =
            Transformations.switchMap(_queryOriginLiveData) { value ->
                val query = value.first
                recentDestinationDao.getRecentDestinations(query)
            }
        private val _remotePredictionsLiveData =
            Transformations.switchMap(_queryOriginLiveData) { value ->
                val query = value.first
                val origin = value.second
                val request =
                    FindAutocompletePredictionsRequest.builder().apply {
                        if (origin != null) {
                            setOrigin(origin)
                            val bias = RectangularBounds.newInstance(
                                SphericalUtil.computeOffset(origin, 100_000.0, 225.0),
                                SphericalUtil.computeOffset(origin, 100_000.0, 45.0)
                            )
                            setLocationBias(bias)
                        }
                        setSessionToken(token)
                        setQuery(query)

                    }.build()

                val remotePredictionsLiveData = MutableLiveData<List<AutocompletePrediction>>()
                placesClient.findAutocompletePredictions(request)
                    .addOnSuccessListener { response: FindAutocompletePredictionsResponse ->
                        remotePredictionsLiveData.value = response.autocompletePredictions
                    }

                remotePredictionsLiveData
            }

        private val _autocompletionLiveData = MediatorLiveData<List<Destination>>().apply {
            addSource(_recentPredictionsLiveData) { entities ->
                value = makeAutocompletion(entities, _remotePredictionsLiveData.value)
            }

            addSource(_remotePredictionsLiveData) { predictions ->
                value = makeAutocompletion(_recentPredictionsLiveData.value, predictions)
            }
        }
        val autocompletionLiveData: LiveData<List<Destination>> = _autocompletionLiveData

        private val token = AutocompleteSessionToken.newInstance()

        fun updateQuery(origin: LatLng?, query: String) {
            _queryOriginLiveData.value = Pair(query, origin)
        }

        private fun makeAutocompletion(
            recent: List<RecentDestinationEntity>?,
            remote: List<AutocompletePrediction>?
        ): List<Destination> {
            val recentDestinations = recent?.recentToDestinations() ?: emptyList()
            val remoteDestinations = remote?.remoteToDestinations() ?: emptyList()

            return (recentDestinations + remoteDestinations).distinctBy { destination ->
                destination.placeId
            }
        }

        private fun List<AutocompletePrediction>.remoteToDestinations(): List<Destination> =
            filter { prediction ->
                prediction.placeTypes.contains(Place.Type.TRANSIT_STATION)
            }.map { prediction ->
                Destination(
                    prediction.placeId,
                    prediction.getPrimaryText(null).toString(),
                    prediction.getSecondaryText(null).toString(),
                    DestinationSource.Remote
                )
            }

        private fun List<RecentDestinationEntity>.recentToDestinations(): List<Destination> =
            map { prediction ->
                Destination(
                    prediction.placeId,
                    prediction.primaryText,
                    prediction.secondaryText,
                    DestinationSource.Recent
                )
            }

    }

    fun newAutocompleteSession(): AutocompleteSession {
        return AutocompleteSession()
    }

    fun addLastDestination(destination: Destination) {
        recentDestinationDao.insertRecentDestination(
            RecentDestinationEntity(
                placeId = destination.placeId,
                primaryText = destination.primaryText,
                secondaryText = destination.secondaryText
            )
        )
    }
}
