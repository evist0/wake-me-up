package com.pampam.wakemeup.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.maps.android.SphericalUtil
import com.pampam.wakemeup.data.db.DestinationCacheDao
import com.pampam.wakemeup.data.db.DestinationCacheEntity
import com.pampam.wakemeup.data.db.RemotePredictionCacheDao
import com.pampam.wakemeup.data.model.Destination
import com.pampam.wakemeup.data.model.Prediction
import com.pampam.wakemeup.data.model.RecentPrediction
import com.pampam.wakemeup.data.model.RemotePrediction
import com.pampam.wakemeup.extensions.observeOnce
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.schedulers.Schedulers

const val REMOTE_TOKEN_MIN_QUERY_LENGTH_INSTANTIATION = 3

class PredictionsDestinationsRepository(
    private val destinationCacheDao: DestinationCacheDao,
    private val remotePredictionCacheDao: RemotePredictionCacheDao,
    private val placesClient: PlacesClient
) {
    inner class AutocompleteSession {
        private val _queryOriginLiveData = MutableLiveData<Pair<String, LatLng?>>()

        private val tokenDelegate = lazy {
            Log.d(
                PredictionsDestinationsRepository::class.simpleName,
                "Acquiring new AutocompleteSessionRemoteToken"
            )
            AutocompleteSessionToken.newInstance()
        }

        private fun fetchPredictionsFromRemote(
            query: String,
            origin: LatLng?
        ): LiveData<List<RemotePrediction>> {
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

            val remotePredictionsLiveData = MutableLiveData<List<RemotePrediction>>()
            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener { response: FindAutocompletePredictionsResponse ->
                    Flowable.just(destinationCacheDao)
                        .subscribeOn(Schedulers.io())
                        .subscribe {
                            val remotePredictions =
                                response.autocompletePredictions.filter { autocomplete ->
                                    autocomplete.placeTypes.contains(Place.Type.TRANSIT_STATION)
                                            &&
                                            !destinationCacheDao.isDestinationExistsById(
                                                autocomplete.placeId
                                            )
                                }.map { autocomplete ->
                                    RemotePrediction(
                                        autocomplete.placeId,
                                        autocomplete.getPrimaryText(null).toString(),
                                        autocomplete.getSecondaryText(null).toString()
                                    )
                                }.apply {
                                    if (origin != null) {
                                        Flowable.just(remotePredictionCacheDao)
                                            .observeOn(Schedulers.io())
                                            .subscribe { dao ->
                                                dao.insertPredictions(this, query, origin)
                                            }
                                    }
                                }
                            remotePredictionsLiveData.postValue(remotePredictions)
                        }

                }

            return remotePredictionsLiveData
        }

        val recentPredictionsLiveData =
            Transformations.switchMap(_queryOriginLiveData) { value ->
                val query = value.first

                val recentPredictionsDataSource =
                    destinationCacheDao.getRecentDestinations(query).map { entity ->
                        RecentPrediction(
                            entity.placeId,
                            entity.primaryText,
                            entity.secondaryText
                        )
                    }
                val recentPredictions = with(recentPredictionsDataSource) {
                    val config = PagedList.Config.Builder()
                        .setPageSize(20)
                        .build()
                    LivePagedListBuilder(
                        this,
                        config
                    ).build()
                }

                recentPredictions
            }

        val remotePredictionsLiveData =
            Transformations.switchMap(_queryOriginLiveData) { value ->
                val query = value.first
                val origin = value.second

                val remotePredictionsLiveData = MutableLiveData<List<RemotePrediction>>()

                if (query.length < REMOTE_TOKEN_MIN_QUERY_LENGTH_INSTANTIATION || origin == null) {
                    remotePredictionsLiveData.value = emptyList()
                } else {
                    Flowable.just(remotePredictionCacheDao)
                        .subscribeOn(Schedulers.io())
                        .subscribe {
                            val searchQueryEntity =
                                remotePredictionCacheDao.selectUpToDateQuery(query, origin)
                            if (searchQueryEntity.isEmpty()) {
                                if (query.length >= REMOTE_TOKEN_MIN_QUERY_LENGTH_INSTANTIATION) {
                                    Log.d(
                                        PredictionsDestinationsRepository::class.simpleName,
                                        "Remote predictions from REMOTE: $query"
                                    )
                                    val remotePredictions =
                                        fetchPredictionsFromRemote(
                                            query,
                                            origin
                                        )
                                    Flowable.just(Any())
                                        .subscribeOn(AndroidSchedulers.mainThread())
                                        .subscribe {
                                            remotePredictions.observeOnce(Observer { predictions ->
                                                remotePredictionsLiveData.value = predictions
                                            })
                                        }
                                } else {
                                    remotePredictionsLiveData.postValue(emptyList())
                                }
                            } else {
                                Log.d(
                                    PredictionsDestinationsRepository::class.simpleName,
                                    "Remote predictions from CACHE: $query"
                                )
                                remotePredictionsLiveData.postValue(
                                    remotePredictionCacheDao.queryCachedPredictions(
                                        searchQueryEntity.first()
                                    )
                                        .map { entity ->
                                            RemotePrediction(
                                                entity.placeId,
                                                entity.primaryText,
                                                entity.secondaryText
                                            )
                                        })
                            }
                        }
                }
                remotePredictionsLiveData
            }

        val token by tokenDelegate

        fun updateQuery(query: String, origin: LatLng? = null) {
            _queryOriginLiveData.value = Pair(query, origin ?: _queryOriginLiveData.value?.second)
        }

        fun fetchDestinationFromPrediction(prediction: Prediction): LiveData<Destination> {
            return fetchDestination(
                prediction.placeId,
                prediction.primaryText,
                prediction.secondaryText
            )
        }
    }

    private var autocompleteSession: AutocompleteSession? = null

    fun requireAutocompleteSession(): AutocompleteSession {
        var session = autocompleteSession
        if (session == null) {
            session = AutocompleteSession()
            autocompleteSession = session
        }

        return session
    }

    fun fetchDestination(
        placeId: String,
        primaryText: String? = null,
        secondaryText: String? = null
    ): LiveData<Destination> {
        val destinationDetailsLiveData = MutableLiveData<Destination>()

        val fetchDetailsFromDao = { dao: DestinationCacheDao ->
            val entity = dao.getDestinationById(placeId)
            destinationDetailsLiveData.postValue(
                Destination(
                    entity.placeId,
                    primaryText ?: entity.primaryText,          // если сменился язык
                    secondaryText ?: entity.secondaryText,    // неплохо обновить
                    LatLng(entity.latitude, entity.longitude)
                )
            )

            val updatedEntity = entity.copy(time = System.currentTimeMillis(), recent = true)
            dao.insertDestination(updatedEntity)
        }

        val fetchDetailsFromRemote = { dao: DestinationCacheDao ->
            val fields = listOf(
                Place.Field.LAT_LNG,
                Place.Field.NAME,
                Place.Field.ADDRESS_COMPONENTS,
                Place.Field.TYPES
            )

            val request = FetchPlaceRequest.builder(placeId, fields).apply {
                autocompleteSession?.let {
                    setSessionToken(it.token)
                    autocompleteSession = null
                }
            }.build()
            placesClient.fetchPlace(request).addOnSuccessListener { response ->
                if (response.place.types?.contains(Place.Type.TRANSIT_STATION) != true) {
                    return@addOnSuccessListener
                }

                val addressComponents = response.place.addressComponents!!.asList()

                val address = "${addressComponents[1].name}, ${addressComponents[0].name}"
                val latLng = response.place.latLng!!
                Flowable.just(dao)
                    .subscribeOn(Schedulers.io())
                    .subscribe { dao ->
                        dao.insertDestination(
                            DestinationCacheEntity(
                                placeId,
                                response.place.name!!,
                                address,
                                latitude = latLng.latitude,
                                longitude = latLng.longitude,
                                time = System.currentTimeMillis(),
                                recent = true
                            )
                        )
                    }

                destinationDetailsLiveData.value = Destination(
                    placeId,
                    response.place.name!!,
                    address,
                    latLng
                )
            }
        }

        Flowable.just(destinationCacheDao)
            .subscribeOn(Schedulers.io())
            .subscribe { dao ->
                if (dao.isDestinationExistsById(placeId)) {
                    Log.d(
                        PredictionsDestinationsRepository::class.simpleName,
                        "Fetching from CACHE"
                    )
                    fetchDetailsFromDao(dao)
                } else {
                    Log.d(
                        PredictionsDestinationsRepository::class.simpleName,
                        "Fetching from REMOTE"
                    )
                    fetchDetailsFromRemote(dao)
                }
            }

        return destinationDetailsLiveData
    }

    fun deleteRecentDestinationById(placeId: String) {
        Flowable.just(destinationCacheDao)
            .subscribeOn(Schedulers.io())
            .subscribe { dao ->
                dao.deleteRecentDestinationById(placeId)
            }
    }
}
