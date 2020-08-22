package com.pampam.wakemeup.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
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
import java.util.*


class DestinationRepository(
    private val recentDestinationDao: RecentDestinationDao,
    private val placesClient: PlacesClient
) {

    inner class AutocompleteSession {
        private val _autocompletionLiveData = MutableLiveData<List<Destination>>()
        val autocompletionLiveData: LiveData<List<Destination>> = _autocompletionLiveData

        private val token = AutocompleteSessionToken.newInstance()

        fun updateQuery(origin: LatLng?, query: String) {
            val recentDestinations =
                (recentDestinationDao.getRecentDestinations().value ?: emptyList()).mapNotNull {
                    if (it.primaryText.toLowerCase(Locale.getDefault())
                            .contains(query.toLowerCase(Locale.getDefault()))
                    )
                        it
                    else
                        null
                }

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
            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener { response: FindAutocompletePredictionsResponse ->
                    val remoteDestinations = response.autocompletePredictions.sortedBy {
                        it.distanceMeters
                    }.filter {
                        it.placeTypes.contains(Place.Type.TRANSIT_STATION)
                    }.map {
                        Destination(
                            it.placeId,
                            it.getPrimaryText(null).toString(),
                            it.getSecondaryText(null).toString(),
                            DestinationSource.Remote
                        )
                    }

                    val autocompleteDestinations =
                        recentDestinations.map {
                            Destination(
                                it.placeId,
                                it.primaryText,
                                it.secondaryText,
                                DestinationSource.Recent
                            )
                        } + remoteDestinations.map {
                            Destination(
                                it.placeId,
                                it.primaryText,
                                it.secondaryText,
                                DestinationSource.Remote
                            )
                        }
                    _autocompletionLiveData.value = autocompleteDestinations.distinctBy {
                        it.placeId
                    }
                }
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