package com.pampam.wakemeup.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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

class DestinationRepository(private val recentDestinationDao: RecentDestinationDao) {

    inner class AutocompleteSession {

        private val _autocompletionLiveData = MutableLiveData<List<Destination>>()
        val autocompletionLiveData: LiveData<List<Destination>> = _autocompletionLiveData

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

            val remoteDestinations =
                if (query.length > 2) DUMMY_REMOTE_DESTINATION_ENTITIES.mapNotNull {
                    if (it.fullText.toLowerCase(Locale.getDefault())
                            .contains(query.toLowerCase(Locale.getDefault()))
                    )
                        it
                    else
                        null
                } else emptyList()

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

    fun newAutocompleteSession(): AutocompleteSession {
        return AutocompleteSession()
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