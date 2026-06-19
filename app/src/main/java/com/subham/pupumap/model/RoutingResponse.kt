package com.subham.pupumap.model

data class RoutingResponse(
    val routes: List<RoutingRoute>
)

data class RoutingRoute(
    val geometry: RoutingGeometry,
    val distance: Double,
    val duration: Double
)

data class RoutingGeometry(
    val type: String,
    val coordinates: List<List<Double>>
)
