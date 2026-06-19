package com.subham.pupumap.ui

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.subham.pupumap.model.OsmPlace
import com.subham.pupumap.model.SearchResult
import com.subham.pupumap.network.BackendConfig
import com.subham.pupumap.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleBlur
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun MapScreen(
    hasLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
    onResolveLocationSettings: (IntentSenderRequest) -> Unit,
    locationSettingsResolutionCount: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val osmPlaces = remember { mutableStateListOf<OsmPlace>() }
    val mapHolder = remember { arrayOfNulls<MapLibreMap>(1) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var addressValue by remember { mutableStateOf("Finding your exact place...") }
    var coordinateValue by remember { mutableStateOf("Waiting for GPS") }
    var areaValue by remember { mutableStateOf("Waiting for GPS") }
    var hasCenteredOnUser by remember { mutableStateOf(false) }
    var selectedPlace by remember { mutableStateOf<SearchResult?>(null) }
    var isRoutePreviewOpen by remember { mutableStateOf(false) }
    var roadDistanceValue by remember { mutableStateOf("Calculating...") }
    var roadDurationValue by remember { mutableStateOf("") }

    fun showLocationPermissionNeeded() {
        addressValue = "Location permission needed"
        coordinateValue = "Allow location access for accurate map"
        areaValue = "Permission required"
    }

    fun showLocationServicesOff() {
        addressValue = "Location is turned off"
        coordinateValue = "Turn on location for accurate map"
        areaValue = "Enable device location"
    }

    fun showLocationUnavailable() {
        addressValue = "Current location unavailable"
        coordinateValue = "Try again near a window or open area"
        areaValue = "GPS unavailable"
    }

    fun requestAccurateCurrentLocation(showPermissionPrompt: Boolean) {
        if (!hasLocationPermission) {
            showLocationPermissionNeeded()
            if (showPermissionPrompt) {
                onRequestLocationPermission()
            }
            return
        }

        val map = mapHolder[0] ?: return
        ensureHighAccuracyLocationSettings(
            context = context,
            onReady = {
                addressValue = "Finding your exact place..."
                coordinateValue = "Getting fresh GPS fix"
                areaValue = "Waiting for GPS"

                requestCurrentLocation(
                    context = context,
                    map = map,
                    animateCamera = true,
                    onLocationFound = { location ->
                        userLocation = location
                        hasCenteredOnUser = true
                    },
                    onLocationUnavailable = {
                        showLocationUnavailable()
                        Toast.makeText(
                            context,
                            "Unable to get current location",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            },
            onResolutionRequired = { request ->
                showLocationServicesOff()
                if (showPermissionPrompt) {
                    onResolveLocationSettings(request)
                }
            },
            onUnavailable = {
                showLocationServicesOff()
                Toast.makeText(
                    context,
                    "Turn on location services for accurate map",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    LaunchedEffect(hasLocationPermission) {
        when {
            !hasLocationPermission -> showLocationPermissionNeeded()
            !isLocationServicesEnabled(context) -> showLocationServicesOff()
        }
    }

    LaunchedEffect(locationSettingsResolutionCount) {
        if (locationSettingsResolutionCount > 0 && hasLocationPermission) {
            requestAccurateCurrentLocation(showPermissionPrompt = false)
        }
    }

    LaunchedEffect(Unit) {
        try {
            val result = RetrofitClient.api.getOsmPlaces()
            osmPlaces.clear()
            osmPlaces.addAll(result)

            val map = mapHolder[0]
            if (map != null && osmPlaces.isNotEmpty()) {
                val firstPlace = osmPlaces.first()
                val fallbackLocation = LatLng(firstPlace.latitude, firstPlace.longitude)

                map.animateCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(fallbackLocation)
                            .zoom(10.5)
                            .build()
                    ),
                    2000
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(osmPlaces.size) {
        val map = mapHolder[0] ?: return@LaunchedEffect
        if (osmPlaces.isNotEmpty()) {
            updateOsmPlacesMarkers(map, osmPlaces)
        }
    }

    LaunchedEffect(isRoutePreviewOpen, selectedPlace, userLocation) {
        val map = mapHolder[0] ?: return@LaunchedEffect
        val start = userLocation
        val end = selectedPlace
        if (isRoutePreviewOpen && start != null && end != null) {
            roadDistanceValue = "Calculating..."
            roadDurationValue = ""
            try {
                val startParam = "${start.longitude},${start.latitude}"
                val endParam = "${end.longitude},${end.latitude}"
                val response = RetrofitClient.api.getRoute(start = startParam, end = endParam)
                val route = response.routes.firstOrNull()
                if (route != null) {
                    val distanceKm = route.distance / 1000.0
                    val durationMin = (route.duration / 60.0).roundToInt()

                    roadDistanceValue = if (distanceKm < 1.0) {
                        "${route.distance.roundToInt()} m"
                    } else {
                        String.format(Locale.US, "%.1f km", distanceKm)
                    }

                    roadDurationValue = when {
                        durationMin < 60 -> "$durationMin mins"
                        else -> {
                            val hours = durationMin / 60
                            val mins = durationMin % 60
                            if (mins > 0) "$hours hr $mins mins" else "$hours hr"
                        }
                    }

                    val pointsJson = route.geometry.coordinates.joinToString(",") { coord ->
                        "[${coord[0]}, ${coord[1]}]"
                    }
                    val geoJson = """
                        {
                            "type": "Feature",
                            "geometry": {
                                "type": "LineString",
                                "coordinates": [$pointsJson]
                            }
                        }
                    """.trimIndent()

                    drawRouteOnMap(map, geoJson)
                } else {
                    roadDistanceValue = "Route error"
                    roadDurationValue = "No routes"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                roadDistanceValue = "Offline"
                roadDurationValue = "Error"
            }
        } else {
            clearRouteFromMap(map)
        }
    }

    LaunchedEffect(userLocation) {
        val location = userLocation ?: return@LaunchedEffect

        coordinateValue = formatCoordinates(location)
        addressValue = "Detecting address"
        areaValue = "Detecting address"

        val address = withContext(Dispatchers.IO) {
            reverseGeocode(context, location.latitude, location.longitude)
        }

        if (address != null) {
            addressValue = address.address
            areaValue = address.area
        } else {
            addressValue = "Address unavailable"
            areaValue = "Address unavailable"
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                MapView(context).apply {
                    getMapAsync { map ->
                        mapHolder[0] = map

                        map.setStyle(BackendConfig.STYLE_URL) {
                            if (osmPlaces.isNotEmpty()) {
                                updateOsmPlacesMarkers(map, osmPlaces)

                                val firstPlace = osmPlaces.first()
                                val fallbackLocation = LatLng(firstPlace.latitude, firstPlace.longitude)

                                map.animateCamera(
                                    CameraUpdateFactory.newCameraPosition(
                                        CameraPosition.Builder()
                                            .target(fallbackLocation)
                                            .zoom(10.5)
                                            .build()
                                    ),
                                    2000
                                )
                            }

                            if (hasLocationPermission && !hasCenteredOnUser) {
                                requestAccurateCurrentLocation(showPermissionPrompt = false)
                            }
                        }
                    }
                }
            },
            update = {}
        )

        MapScreenDesign(
            addressValue = addressValue,
            coordinateValue = coordinateValue,
            areaValue = areaValue,
            onCurrentLocationClick = {
                requestAccurateCurrentLocation(showPermissionPrompt = true)
            },
            selectedPlace = selectedPlace,
            selectedPlaceDistance = formatDistanceFromUser(userLocation, selectedPlace),
            roadDistanceValue = roadDistanceValue,
            roadDurationValue = roadDurationValue,
            currentLatitude = userLocation?.latitude,
            currentLongitude = userLocation?.longitude,
            isRoutePreviewOpen = isRoutePreviewOpen,
            onSearchResultSelected = { result ->
                val map = mapHolder[0] ?: return@MapScreenDesign
                selectedPlace = result
                isRoutePreviewOpen = false
                focusSearchResult(map, result)
            },
            onClearSelectedPlace = {
                selectedPlace = null
                isRoutePreviewOpen = false
                mapHolder[0]?.let {
                    clearDestinationIndicator(it)
                    clearRouteFromMap(it)
                }
            },
            onDirectionsClick = {
                isRoutePreviewOpen = true
            },
            onChangeRouteClick = {
                isRoutePreviewOpen = false
                mapHolder[0]?.let { clearRouteFromMap(it) }
            },
            onStartRouteClick = {
                Toast.makeText(
                    context,
                    "Route navigation started",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onZoomInClick = {
                mapHolder[0]?.let { map ->
                    val currentZoom = map.cameraPosition.zoom
                    map.animateCamera(CameraUpdateFactory.zoomTo(currentZoom + 1.0))
                }
            },
            onZoomOutClick = {
                mapHolder[0]?.let { map ->
                    val currentZoom = map.cameraPosition.zoom
                    map.animateCamera(CameraUpdateFactory.zoomTo(currentZoom - 1.0))
                }
            },
            onStyleSwitchClick = {
                Toast.makeText(
                    context,
                    "Style Switcher coming soon",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}

private fun ensureHighAccuracyLocationSettings(
    context: Context,
    onReady: () -> Unit,
    onResolutionRequired: (IntentSenderRequest) -> Unit,
    onUnavailable: () -> Unit
) {
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
        .setMinUpdateIntervalMillis(1000L)
        .build()

    val settingsRequest = LocationSettingsRequest.Builder()
        .addLocationRequest(locationRequest)
        .setAlwaysShow(true)
        .build()

    LocationServices.getSettingsClient(context)
        .checkLocationSettings(settingsRequest)
        .addOnSuccessListener {
            onReady()
        }
        .addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                onResolutionRequired(IntentSenderRequest.Builder(exception.resolution).build())
            } else {
                onUnavailable()
            }
        }
}

@SuppressLint("MissingPermission")
private fun requestCurrentLocation(
    context: Context,
    map: MapLibreMap,
    animateCamera: Boolean,
    onLocationFound: (LatLng) -> Unit,
    onLocationUnavailable: () -> Unit
) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val cancellationTokenSource = CancellationTokenSource()

    fusedLocationClient.getCurrentLocation(
        Priority.PRIORITY_HIGH_ACCURACY,
        cancellationTokenSource.token
    ).addOnSuccessListener { location ->
        if (location != null) {
            val userLatLng = LatLng(location.latitude, location.longitude)
            updateUserLocationIndicator(map, userLatLng)
            onLocationFound(userLatLng)

            if (animateCamera) {
                recenterMap(map, userLatLng)
            }
        } else {
            requestLastKnownLocationFallback(
                context = context,
                map = map,
                animateCamera = animateCamera,
                onLocationFound = onLocationFound,
                onLocationUnavailable = onLocationUnavailable
            )
        }
    }.addOnFailureListener {
        requestLastKnownLocationFallback(
            context = context,
            map = map,
            animateCamera = animateCamera,
            onLocationFound = onLocationFound,
            onLocationUnavailable = onLocationUnavailable
        )
    }
}

@SuppressLint("MissingPermission")
private fun requestLastKnownLocationFallback(
    context: Context,
    map: MapLibreMap,
    animateCamera: Boolean,
    onLocationFound: (LatLng) -> Unit,
    onLocationUnavailable: () -> Unit
) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        if (location == null) {
            onLocationUnavailable()
            return@addOnSuccessListener
        }

        val userLatLng = LatLng(location.latitude, location.longitude)
        updateUserLocationIndicator(map, userLatLng)
        onLocationFound(userLatLng)

        if (animateCamera) {
            recenterMap(map, userLatLng)
        }
    }.addOnFailureListener {
        onLocationUnavailable()
    }
}

private fun recenterMap(map: MapLibreMap, location: LatLng) {
    map.animateCamera(
        CameraUpdateFactory.newCameraPosition(
            CameraPosition.Builder()
                .target(location)
                .zoom(16.5)
                .build()
        ),
        1400
    )
}

private fun focusSearchResult(map: MapLibreMap, result: SearchResult) {
    val location = LatLng(result.latitude, result.longitude)
    updateDestinationIndicator(map, location)

    map.animateCamera(
        CameraUpdateFactory.newCameraPosition(
            CameraPosition.Builder()
                .target(location)
                .zoom(16.0)
                .build()
        ),
        1300
    )
}

private fun updateDestinationIndicator(map: MapLibreMap, location: LatLng) {
    val feature = Feature.fromGeometry(Point.fromLngLat(location.longitude, location.latitude))

    map.getStyle { style ->
        val haloSource = style.getSourceAs<GeoJsonSource>("destination-halo-source")
        if (haloSource == null) {
            style.addSource(GeoJsonSource("destination-halo-source", feature))
            style.addLayer(
                CircleLayer("destination-halo", "destination-halo-source").withProperties(
                    circleColor("#FFC857"),
                    circleRadius(18f),
                    circleOpacity(0.20f),
                    circleBlur(0.25f)
                )
            )
        } else {
            haloSource.setGeoJson(feature)
        }

        val dotSource = style.getSourceAs<GeoJsonSource>("destination-dot-source")
        if (dotSource == null) {
            style.addSource(GeoJsonSource("destination-dot-source", feature))
            style.addLayer(
                CircleLayer("destination-dot", "destination-dot-source").withProperties(
                    circleColor("#FFC857"),
                    circleRadius(7.5f),
                    circleStrokeColor("#F2FFFFFF"),
                    circleStrokeWidth(2.0f),
                    circleOpacity(1.0f)
                )
            )
        } else {
            dotSource.setGeoJson(feature)
        }
    }
}

private fun clearDestinationIndicator(map: MapLibreMap) {
    map.getStyle { style ->
        runCatching { style.removeLayer("destination-dot") }
        runCatching { style.removeLayer("destination-halo") }
        runCatching { style.removeSource("destination-dot-source") }
        runCatching { style.removeSource("destination-halo-source") }
    }
}

private fun updateUserLocationIndicator(map: MapLibreMap, location: LatLng) {
    val feature = Feature.fromGeometry(Point.fromLngLat(location.longitude, location.latitude))

    map.getStyle { style ->
        val pulseSource = style.getSourceAs<GeoJsonSource>("user-location-pulse-source")
        if (pulseSource == null) {
            style.addSource(GeoJsonSource("user-location-pulse-source", feature))
            style.addLayer(
                CircleLayer("user-location-pulse", "user-location-pulse-source").withProperties(
                    circleColor("#00D9FF"),
                    circleRadius(20f),
                    circleOpacity(0.18f),
                    circleBlur(0.35f)
                )
            )
        } else {
            pulseSource.setGeoJson(feature)
        }

        val dotSource = style.getSourceAs<GeoJsonSource>("user-location-dot-source")
        if (dotSource == null) {
            style.addSource(GeoJsonSource("user-location-dot-source", feature))
            style.addLayer(
                CircleLayer("user-location-dot", "user-location-dot-source").withProperties(
                    circleColor("#00D9FF"),
                    circleRadius(6.5f),
                    circleStrokeColor("#F2FFFFFF"),
                    circleStrokeWidth(2.0f),
                    circleOpacity(1.0f)
                )
            )
        } else {
            dotSource.setGeoJson(feature)
        }
    }
}

private fun isLocationServicesEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    return runCatching {
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }.getOrDefault(false)
}

private data class CurrentAddress(
    val address: String,
    val area: String
)

private fun reverseGeocode(
    context: Context,
    latitude: Double,
    longitude: Double
): CurrentAddress? {
    return runCatching {
        val address = Geocoder(context, Locale.getDefault())
            .getFromLocation(latitude, longitude, 1)
            ?.firstOrNull()
            ?: return null

        val shortAddress = listOfNotNull(
            address.featureName,
            address.thoroughfare,
            address.subLocality
        )
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() && it != latitude.toString() && it != longitude.toString() }
            ?: address.getAddressLine(0)?.substringBefore(",")?.trim()
            ?: formatCoordinates(LatLng(latitude, longitude))

        val state = address.adminArea?.trim().orEmpty()
        val fullAddress = address.getAddressLine(0)?.trim().orEmpty()
        val addressValue = when {
            fullAddress.isNotEmpty() -> fullAddress
            state.isNotEmpty() && !shortAddress.contains(state, ignoreCase = true) -> "$shortAddress, $state"
            else -> shortAddress
        }

        val district = address.subAdminArea?.trim().orEmpty()
        val locality = address.locality?.trim().orEmpty()
        val area = if (district.isNotEmpty()) {
            district
        } else if (locality.isNotEmpty() && state.isNotEmpty() && !locality.equals(state, ignoreCase = true)) {
            "$locality, $state"
        } else if (locality.isNotEmpty()) {
            locality
        } else if (state.isNotEmpty()) {
            state
        } else {
            address.countryName ?: "Address found"
        }

        CurrentAddress(address = addressValue, area = area)
    }.getOrNull()
}

private fun formatCoordinates(location: LatLng): String {
    return String.format(
        Locale.US,
        "Lat %.5f - Lng %.5f",
        location.latitude,
        location.longitude
    )
}

private fun formatDistanceFromUser(userLocation: LatLng?, selectedPlace: SearchResult?): String {
    if (userLocation == null || selectedPlace == null) {
        return "Distance needs current location"
    }

    val distanceMeters = haversineMeters(
        startLatitude = userLocation.latitude,
        startLongitude = userLocation.longitude,
        endLatitude = selectedPlace.latitude,
        endLongitude = selectedPlace.longitude
    )

    return if (distanceMeters < 1000) {
        "${distanceMeters.roundToInt()} m away"
    } else {
        String.format(Locale.US, "%.1f km away", distanceMeters / 1000.0)
    }
}

private fun haversineMeters(
    startLatitude: Double,
    startLongitude: Double,
    endLatitude: Double,
    endLongitude: Double
): Double {
    val earthRadiusMeters = 6371000.0
    val startLatRad = Math.toRadians(startLatitude)
    val endLatRad = Math.toRadians(endLatitude)
    val deltaLatRad = Math.toRadians(endLatitude - startLatitude)
    val deltaLngRad = Math.toRadians(endLongitude - startLongitude)

    val haversine = sin(deltaLatRad / 2) * sin(deltaLatRad / 2) +
        cos(startLatRad) * cos(endLatRad) *
        sin(deltaLngRad / 2) * sin(deltaLngRad / 2)

    return earthRadiusMeters * 2 * atan2(sqrt(haversine), sqrt(1 - haversine))
}

private fun updateOsmPlacesMarkers(map: MapLibreMap, places: List<OsmPlace>) {
    map.getStyle { style ->
        val sourceId = "osm-places-source"
        val layerId = "osm-places-layer"

        runCatching { style.removeLayer(layerId) }
        runCatching { style.removeSource(sourceId) }

        val features = places.map { place ->
            val point = Point.fromLngLat(place.longitude, place.latitude)
            val feature = Feature.fromGeometry(point)
            feature.addStringProperty("name", place.name ?: "Unnamed Place")
            feature.addStringProperty("place", place.place)
            feature
        }

        val featureCollection = FeatureCollection.fromFeatures(features)
        style.addSource(GeoJsonSource(sourceId, featureCollection))

        val circleLayer = CircleLayer(layerId, sourceId).withProperties(
            circleColor("#00D9FF"),
            circleRadius(6.5f),
            circleStrokeColor("#FFFFFF"),
            circleStrokeWidth(1.5f),
            circleOpacity(0.85f)
        )
        style.addLayer(circleLayer)
    }
}

private fun drawRouteOnMap(map: MapLibreMap, geoJson: String) {
    map.getStyle { style ->
        val sourceId = "route-source"
        val layerId = "route-layer"

        runCatching { style.removeLayer(layerId) }
        runCatching { style.removeSource(sourceId) }

        style.addSource(GeoJsonSource(sourceId, geoJson))

        val lineLayer = LineLayer(layerId, sourceId).withProperties(
            lineColor("#00D9FF"),
            lineWidth(5f),
            lineOpacity(0.85f),
            lineJoin(Property.LINE_JOIN_ROUND),
            lineCap(Property.LINE_CAP_ROUND)
        )
        style.addLayer(lineLayer)
    }
}

private fun clearRouteFromMap(map: MapLibreMap) {
    map.getStyle { style ->
        runCatching { style.removeLayer("route-layer") }
        runCatching { style.removeSource("route-source") }
    }
}
