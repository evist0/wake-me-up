package com.pampam.wakemeup.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.maps.android.SphericalUtil
import com.pampam.wakemeup.data.db.RecentDestinationDao
import com.pampam.wakemeup.data.db.RecentDestinationEntity
import com.pampam.wakemeup.data.model.DestinationDetails
import com.pampam.wakemeup.data.model.DestinationPrediction
import com.pampam.wakemeup.data.model.DestinationPredictionSource
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit

const val REMOTE_TOKEN_MIN_QUERY_LENGTH_INSTANTIATION = 3

class DestinationPredictionRepository(
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

                // не создаём токена пока размер запроса не перевалит
                // что позволяет экономить деньги
                // больше денег больше польза
                // 5 правил настоящего еврея
                // 1. Хорошо пожрать
                // 2. Хорошо посрать
                // 3. Хорошо поспать
                // 4. Хорошо поебать
                // 5. Хорошо набеать
                if (!tokenDelegate.isInitialized() &&
                    query.length < REMOTE_TOKEN_MIN_QUERY_LENGTH_INSTANTIATION
                ) {
                    return@switchMap MutableLiveData<List<AutocompletePrediction>>(emptyList())
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

                val remotePredictionsLiveData = MutableLiveData<List<AutocompletePrediction>>()
                placesClient.findAutocompletePredictions(request)
                    .addOnSuccessListener { response: FindAutocompletePredictionsResponse ->
                        remotePredictionsLiveData.value = response.autocompletePredictions
                    }

                remotePredictionsLiveData
            }

        private val _autocompletionLiveData =
            MediatorLiveData<List<DestinationPrediction>>().apply {
                addSource(_recentPredictionsLiveData) { entities ->
                    updateAutocompletion(entities, _remotePredictionsLiveData.value)
                }

                addSource(_remotePredictionsLiveData) { predictions ->
                    updateAutocompletion(_recentPredictionsLiveData.value, predictions)
                }
            }
        val autocompletionLiveData: LiveData<List<DestinationPrediction>> = _autocompletionLiveData

        private val tokenDelegate = lazy {
            Log.d(
                DestinationPredictionRepository::class.simpleName,
                "Acquiring new AutocompleteSessionRemoteToken"
            )
            AutocompleteSessionToken.newInstance()
        }
        private val token by tokenDelegate

        fun updateQuery(origin: LatLng?, query: String) {
            _queryOriginLiveData.value = Pair(query, origin)
        }

        fun fetchDetails(prediction: DestinationPrediction): LiveData<DestinationDetails> {
            val destinationDetailsLiveData = MutableLiveData<DestinationDetails>()

            val fetchDetailsFromDao = { dao: RecentDestinationDao ->
                val entity = dao.getRecentDestinationById(prediction.placeId)
                destinationDetailsLiveData.postValue(
                    DestinationDetails(
                        entity.placeId,
                        entity.primaryText,
                        entity.secondaryText,
                        LatLng(entity.latitude, entity.longitude)
                    )
                )
            }

            val fetchDetailsFromRemote = { dao: RecentDestinationDao ->
                val fields = listOf(Place.Field.LAT_LNG)
                val request = FetchPlaceRequest.builder(prediction.placeId, fields).apply {
                    setSessionToken(token)
                }.build()
                placesClient.fetchPlace(request).addOnSuccessListener { response ->
                    val latLng = response.place.latLng!!

                    // хоть мы и добавляем колбек на io потоке
                    // но он будет вызван на main потоке поэтому еще разок ага
                    Flowable.just(dao)
                        .subscribeOn(Schedulers.io())
                        .subscribe { dao ->
                            dao.insertRecentDestination(
                                RecentDestinationEntity(
                                    prediction.placeId,
                                    prediction.primaryText,
                                    prediction.secondaryText,
                                    latitude = latLng.latitude,
                                    longitude = latLng.longitude
                                )
                            )
                        }

                    destinationDetailsLiveData.value = DestinationDetails(
                        prediction.placeId,
                        prediction.primaryText,
                        prediction.secondaryText,
                        latLng
                    )
                }
            }

            // Не обращаемся к Places API если place уже есть в бд
            Flowable.just(recentDestinationDao)
                .subscribeOn(Schedulers.io())
                .delay(1000, TimeUnit.MILLISECONDS, Schedulers.io()) //TODO: убрать когда заебёт
                .subscribe { dao ->
                    if (dao.isDestinationExistsById(prediction.placeId)) {
                        Log.d(
                            DestinationPredictionRepository::class.simpleName,
                            "Fetching from DAO"
                        )
                        fetchDetailsFromDao(dao)
                    } else {
                        Log.d(
                            DestinationPredictionRepository::class.simpleName,
                            "Fetching from REMOTE"
                        )
                        fetchDetailsFromRemote(dao)
                    }
                }

            return destinationDetailsLiveData
        }

        private fun updateAutocompletion(
            recent: List<RecentDestinationEntity>?,
            remote: List<AutocompletePrediction>?
        ) {
            val recentDestinations = recent?.recentPredictions() ?: emptyList()
            val remoteDestinations = remote?.remotePredictions() ?: emptyList()

            Flowable.just(recentDestinationDao)
                .subscribeOn(Schedulers.io())
                .subscribe { dao ->
                    val (newRemoteRecents, newRemotes) = remoteDestinations.partition { remote ->
                        dao.isDestinationExistsById(remote.placeId)
                    }
                    val newRecents = newRemoteRecents.map { recent ->
                        DestinationPrediction(
                            recent.placeId,
                            recent.primaryText,
                            recent.secondaryText,
                            DestinationPredictionSource.Recent
                        )
                    }

                    _autocompletionLiveData.postValue(
                        (recentDestinations + newRecents + newRemotes).distinctBy { destination ->
                            destination.placeId
                        })
                }
        }

        private fun List<AutocompletePrediction>.remotePredictions(): List<DestinationPrediction> =
            filter { prediction ->
                prediction.placeTypes.contains(Place.Type.TRANSIT_STATION)
            }.map { prediction ->
                DestinationPrediction(
                    prediction.placeId,
                    prediction.getPrimaryText(null).toString(),
                    prediction.getSecondaryText(null).toString(),
                    DestinationPredictionSource.Remote
                )
            }

        private fun List<RecentDestinationEntity>.recentPredictions(): List<DestinationPrediction> =
            map { prediction ->
                DestinationPrediction(
                    prediction.placeId,
                    prediction.primaryText,
                    prediction.secondaryText,
                    DestinationPredictionSource.Recent
                )
            }

    }

    fun newAutocompleteSession(): AutocompleteSession {
        return AutocompleteSession()
    }

    fun deleteRecentDestinationById(placeId: String) {
        Flowable.just(recentDestinationDao)
            .subscribeOn(Schedulers.io())
            .subscribe {
                it.deleteRecentDestinationById(placeId)
            }
    }
}
