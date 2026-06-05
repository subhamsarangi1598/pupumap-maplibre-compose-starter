package com.subham.pupumap.network

import com.subham.pupumap.model.Place
import retrofit2.http.GET
import com.subham.pupumap.model.OsmPlace
import com.subham.pupumap.model.SearchResult
import retrofit2.http.Query

interface ApiService {
    @GET("places")
    suspend fun getPlaces() : List<Place>

    @GET("osm/places")
    suspend fun getOsmPlaces(): List<OsmPlace>

    @GET("api/search")
    suspend fun searchPlaces(
        @Query("q") query: String,
        @Query("lat") latitude: Double? = null,
        @Query("lng") longitude: Double? = null,
        @Query("limit") limit: Int = 25
    ): List<SearchResult>
}
