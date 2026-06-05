package com.subham.pupumap.model

data class SearchResult(
    val osm_id: Long,
    val name: String,
    val subtitle: String,
    val type: String,
    val source: String,
    val latitude: Double,
    val longitude: Double,
    val distance_meters: Int?
)
