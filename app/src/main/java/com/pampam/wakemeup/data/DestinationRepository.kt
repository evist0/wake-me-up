package com.pampam.wakemeup.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.pampam.wakemeup.data.db.RecentDestinationDao
import com.pampam.wakemeup.data.db.RecentDestinationEntity
import com.pampam.wakemeup.data.model.Destination
import com.pampam.wakemeup.data.model.DestinationSource
import java.util.*

// TODO: Places SDK
private val DUMMY_REMOTE_DESTINATION_ENTITIES = listOf(
    RecentDestinationEntity("0", "Металлургов"),
    RecentDestinationEntity("1", "Верхняя островская"),
    RecentDestinationEntity("2", "Нижняя островская")
)

class DestinationRepository(
    private val recentDestinationDao: RecentDestinationDao,
    private val placesClient: PlacesClient
) {

    inner class AutocompleteSession(private val origin: LatLng?) {
        private val _autocompletionLiveData = MutableLiveData<List<Destination>>()
        val autocompletionLiveData: LiveData<List<Destination>> = _autocompletionLiveData
        private val token = AutocompleteSessionToken.newInstance()

        fun updateQuery(query: String) {
            val recentDestinations =
                (recentDestinationDao.getRecentDestinations().value ?: emptyList()).mapNotNull {
                    if (it.fullText.toLowerCase(Locale.getDefault())
                            .contains(query.toLowerCase(Locale.getDefault()))
                    )
                        it
                    else
                        null
                }

            val request =
                FindAutocompletePredictionsRequest.builder()
                    .setOrigin(origin)
                    .setSessionToken(token)
                    .setQuery(query)
                    .build()

            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener { response: FindAutocompletePredictionsResponse ->
                    Log.i("", response.toString())
                    val remoteDestinations = response.autocompletePredictions.sortedBy {
                        it.distanceMeters
                    }.mapNotNull {
                        if (it.placeTypes.contains(Place.Type.BUS_STATION)) {
                            Destination(
                                it.placeId,
                                it.getPrimaryText(null).toString(),
                                DestinationSource.Remote
                            )
                        } else null
                    }

                    val autocompleteDestinations =
                        recentDestinations.map {
                            Destination(it.placeId, it.fullText, DestinationSource.Recent)
                        } + remoteDestinations.map {
                            Destination(it.placeId, it.fullText, DestinationSource.Remote)
                        }
                    _autocompletionLiveData.value = autocompleteDestinations
                        .distinctBy { d -> d.placeId }
                }
        }
    }

    fun newAutocompleteSession(origin: LatLng?): AutocompleteSession {
        return AutocompleteSession(origin)
    }

    fun addLastDestination(destination: Destination) {
        recentDestinationDao.insertRecentDestination(
            RecentDestinationEntity(
                placeId = destination.placeId,
                fullText = destination.fullText
            )
        )
    }
}