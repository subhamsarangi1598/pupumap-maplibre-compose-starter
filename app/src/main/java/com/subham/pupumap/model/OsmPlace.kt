package com.subham.pupumap.model

data class OsmPlace(
    val osm_id: Long,
    val name: String?,
    val place: String,
    val latitude: Double,
    val longitude: Double
)