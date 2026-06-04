package com.subham.pupumap.network

import com.subham.pupumap.model.Place
import retrofit2.http.GET
import com.subham.pupumap.model.OsmPlace

interface ApiService {
    @GET("places")
    suspend fun getPlaces() : List<Place>

    @GET("osm/places")
    suspend fun getOsmPlaces(): List<OsmPlace>



}