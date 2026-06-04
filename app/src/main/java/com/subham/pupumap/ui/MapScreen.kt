package com.subham.pupumap.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.LocationManager
import android.provider.Settings
import android.widget.Toast
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
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.subham.pupumap.model.OsmPlace
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
import org.maplibre.android.style.layers.PropertyFactory.circleBlur
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point
import java.util.Locale

@Composable
fun MapScreen(
    hasLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
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

        if (!isLocationServicesEnabled(context)) {
            showLocationServicesOff()
            if (showPermissionPrompt) {
                Toast.makeText(
                    context,
                    "Turn on location services for accurate map",
                    Toast.LENGTH_SHORT
                ).show()
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            return
        }

        val map = mapHolder[0] ?: return
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
    }

    LaunchedEffect(hasLocationPermission) {
        when {
            !hasLocationPermission -> showLocationPermissionNeeded()
            !isLocationServicesEnabled(context) -> showLocationServicesOff()
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
            }
        )
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
