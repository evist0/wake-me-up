package com.pampam.wakemeup.data.db

import androidx.room.Entity
import com.google.android.gms.maps.model.LatLng
import kotlin.math.cos
import kotlin.math.roundToInt

const val SEARCH_QUERY_ENTITY_SECTOR_LAT_DIM = 10/*KM*/
const val SEARCH_QUERY_ENTITY_SECTOR_LNG_DIM = 10/*KM*/
const val SEARCH_QUERY_ENTITY_CACHE_LIFETIME: Long =
    14/*DAYS*/ * 24/*HOURS*/ * 3600/*SECONDS*/ * 1000/*MILLIS*/

@Entity(
    tableName = "searchQuery",
    primaryKeys = ["text", "originSectorLat", "originSectorLng"]
)
data class SearchQueryEntity(
    val text: String,
    val originSectorLat: Int,
    val originSectorLng: Int,
    val time: Long
)

fun LatLng.toQuerySector(): Pair<Int, Int> {
    val kmPerLat = 111.32/*KM*/
    val kmPerLng = 40_075/*KM*/ * cos(latitude) / 360
    return Pair(
        (latitude * kmPerLat / SEARCH_QUERY_ENTITY_SECTOR_LAT_DIM).roundToInt(),
        (longitude * kmPerLng / SEARCH_QUERY_ENTITY_SECTOR_LNG_DIM).roundToInt()
    )
}