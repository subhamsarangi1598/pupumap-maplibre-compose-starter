package com.subham.pupumap.ui

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
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
import java.util.Locale

@Composable
fun MapScreen(
    hasLocationPermission: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val osmPlaces = remember { mutableStateListOf<OsmPlace>() }
    val mapHolder = remember { arrayOfNulls<MapLibreMap>(1) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var addressValue by remember { mutableStateOf("Finding your exact place...") }
    var coordinateValue by remember { mutableStateOf("Waiting for GPS") }
    var areaValue by remember { mutableStateOf("Waiting for GPS") }

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

                            if (hasLocationPermission) {
                                addUserLocationMarker(context, map) { location ->
                                    userLocation = location
                                }
                            }
                        }
                    }
                }
            },
            update = { mapView ->
                mapView.getMapAsync { map ->
                    if (hasLocationPermission) {
                        addUserLocationMarker(mapView.context, map) { location ->
                            userLocation = location
                        }
                    }
                }
            }
        )

        MapScreenDesign(
            addressValue = addressValue,
            coordinateValue = coordinateValue,
            areaValue = areaValue
        )
    }
}

@SuppressLint("MissingPermission")
private fun addUserLocationMarker(
    context: Context,
    map: MapLibreMap,
    onLocationFound: (LatLng) -> Unit
) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            val userLatLng = LatLng(location.latitude, location.longitude)
            onLocationFound(userLatLng)

            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(userLatLng)
                        .zoom(14.0)
                        .build()
                ),
                2000
            )
        }
    }
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
