package com.pampam.wakemeup.data.db

import androidx.room.*
import com.google.android.gms.maps.model.LatLng
import com.pampam.wakemeup.data.model.RemotePrediction

@Dao
interface RemotePredictionCacheDao {
    @Query("SELECT * FROM searchQuery WHERE text = :query AND originSectorLat = :originSectorLat AND originSectorLng = :originSectorLng AND :time - time < $SEARCH_QUERY_ENTITY_CACHE_LIFETIME")
    fun selectUpToDateQuery(
        query: String,
        originSectorLat: Int,
        originSectorLng: Int,
        time: Long
    ): List<SearchQueryEntity>

    fun selectUpToDateQuery(query: String, origin: LatLng): List<SearchQueryEntity> {
        val sector = origin.toQuerySector()
        return selectUpToDateQuery(query, sector.first, sector.second, System.currentTimeMillis())
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSearchQueries(searchQueries: List<SearchQueryEntity>)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPredictions(predictions: List<RemotePredictionCacheEntity>)

    @Transaction
    fun insertPredictions(
        predictions: List<RemotePrediction>,
        searchQueryText: String,
        searchQueryOrigin: LatLng
    ) {
        val sector = searchQueryOrigin.toQuerySector()
        val searchQueryEntity = SearchQueryEntity(
            searchQueryText,
            sector.first,
            sector.second,
            System.currentTimeMillis()
        )
        insertSearchQueries(listOf(searchQueryEntity))

        insertPredictions(predictions.map { prediction ->
            RemotePredictionCacheEntity(
                prediction.placeId,
                prediction.primaryText,
                prediction.secondaryText,
                searchQueryEntity.text,
                searchQueryEntity.originSectorLat,
                searchQueryEntity.originSectorLng
            )
        })
    }

    @Transaction
    @Query("SELECT * FROM remotePredictionCache WHERE text = :text AND originSectorLat = :originSectorLat AND originSectorLng = :originSectorLng")
    fun queryCachedPredictions(
        text: String,
        originSectorLat: Int,
        originSectorLng: Int
    ): List<RemotePredictionCacheEntity>

    @Transaction
    fun queryCachedPredictions(
        searchQueryEntity: SearchQueryEntity
    ): List<RemotePredictionCacheEntity> {
        return queryCachedPredictions(
            searchQueryEntity.text,
            searchQueryEntity.originSectorLat,
            searchQueryEntity.originSectorLng
        )
    }
}