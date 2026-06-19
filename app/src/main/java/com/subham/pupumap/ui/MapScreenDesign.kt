package com.subham.pupumap.ui

import android.os.Build
import android.view.WindowInsets as AndroidWindowInsets
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subham.pupumap.model.SearchResult
import java.util.Locale

private val GlassPanel = Color(0xD5101820)
private val GlassPanelSoft = Color(0x99101820)
private val GlassBorder = Color(0x33FFFFFF)
private val TextPrimary = Color(0xF2FFFFFF)
private val TextSecondary = Color(0xB8FFFFFF)
private val NeonCyan = Color(0xFF00D9FF)
private val AmberPin = Color(0xFFFFC857)
private val SuccessGreen = Color(0xFF41E6A4)
private val SoftRed = Color(0xFFFF6B8A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreenDesign(
    addressValue: String = "Finding your exact place...",
    coordinateValue: String = "Waiting for GPS",
    areaValue: String = "Detecting area",
    onCurrentLocationClick: () -> Unit = {},
    selectedPlace: SearchResult? = null,
    selectedPlaceDistance: String = "",
    roadDistanceValue: String = "Calculating...",
    roadDurationValue: String = "",
    currentLatitude: Double? = null,
    currentLongitude: Double? = null,
    isRoutePreviewOpen: Boolean = false,
    isNavigationActive: Boolean = false,
    onSearchResultSelected: (SearchResult) -> Unit = {},
    onClearSelectedPlace: () -> Unit = {},
    onDirectionsClick: () -> Unit = {},
    onChangeRouteClick: () -> Unit = {},
    onStartRouteClick: () -> Unit = {},
    onStopNavigationClick: () -> Unit = {},
    onRecenterNavigationClick: () -> Unit = {},
    onZoomInClick: () -> Unit = {},
    onZoomOutClick: () -> Unit = {},
    onStyleSwitchClick: () -> Unit = {}
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val statusBarPadding = with(density) {
        val topInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.rootWindowInsets
                ?.getInsets(AndroidWindowInsets.Type.statusBars())
                ?.top ?: 0
        } else {
            @Suppress("DEPRECATION")
            view.rootWindowInsets?.systemWindowInsetTop ?: 0
        }
        topInset.toDp()
    }
    var isSearchOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusBarPadding + 14.dp, start = 14.dp, end = 14.dp)
        ) {
            if (isNavigationActive && selectedPlace != null) {
                ActiveNavigationTopCard(
                    selectedPlace = selectedPlace,
                    roadDuration = roadDurationValue,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            } else {
                TopSearchOverlay(
                    modifier = Modifier.align(Alignment.TopCenter),
                    onSearchClick = { isSearchOpen = true }
                )
            }

            if (isNavigationActive) {
                CircleIconButton(
                    size = 54.dp,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(bottom = 90.dp)
                        .clickable(onClick = onRecenterNavigationClick)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MyLocation,
                        contentDescription = "Recenter route",
                        tint = NeonCyan,
                        modifier = Modifier.size(23.dp)
                    )
                }
            } else {
                MapControlsColumn(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(bottom = 120.dp),
                    onCurrentLocationClick = onCurrentLocationClick,
                    onZoomInClick = onZoomInClick,
                    onZoomOutClick = onZoomOutClick,
                    onStyleSwitchClick = onStyleSwitchClick
                )
            }

            when {
                isNavigationActive && selectedPlace != null -> {
                    ActiveNavigationBottomCard(
                        selectedPlace = selectedPlace,
                        roadDistance = roadDistanceValue,
                        roadDuration = roadDurationValue,
                        straightLineDistance = selectedPlaceDistance,
                        onStopClick = onStopNavigationClick,
                        onRecenterClick = onRecenterNavigationClick,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

                selectedPlace == null -> {
                    CurrentLocationSheet(
                        addressValue = addressValue,
                        coordinateValue = coordinateValue,
                        areaValue = areaValue,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

                isRoutePreviewOpen -> {
                    RoutePreviewSheet(
                        selectedPlace = selectedPlace,
                        straightLineDistance = selectedPlaceDistance,
                        roadDistance = roadDistanceValue,
                        roadDuration = roadDurationValue,
                        fromValue = addressValue,
                        onStartClick = onStartRouteClick,
                        onChangeClick = onChangeRouteClick,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

                else -> {
                    // Replaced static SelectedPlaceSheet with ModalBottomSheet below
                }
            }
        }

        if (selectedPlace != null && !isRoutePreviewOpen && !isNavigationActive) {
            ModalBottomSheet(
                onDismissRequest = {
                    onClearSelectedPlace()
                },
                sheetState = sheetState,
                containerColor = Color(0xFF121821),
                scrimColor = Color.Black.copy(alpha = 0.3f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                SelectedPlaceSheetExpandedContent(
                    selectedPlace = selectedPlace,
                    distanceValue = selectedPlaceDistance,
                    onDirectionsClick = onDirectionsClick,
                    onClearClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                onClearSelectedPlace()
                            }
                        }
                    }
                )
            }
        }

        if (isSearchOpen && !isNavigationActive) {
            SearchOverlay(
                onClose = { isSearchOpen = false },
                onResultSelected = { result ->
                    isSearchOpen = false
                    onSearchResultSelected(result)
                },
                currentLatitude = currentLatitude,
                currentLongitude = currentLongitude,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ActiveNavigationTopCard(
    selectedPlace: SearchResult,
    roadDuration: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(GlassPanel, RoundedCornerShape(26.dp))
            .border(1.dp, Color(0x6600D9FF), RoundedCornerShape(26.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(Color(0x2200D9FF), CircleShape)
                .border(1.dp, Color(0x8800D9FF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Route,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveRoutePill()

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (roadDuration.isNotBlank()) "Arrive in $roadDuration" else "Route active",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Continue to ${selectedPlace.name}",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ActiveNavigationBottomCard(
    selectedPlace: SearchResult,
    roadDistance: String,
    roadDuration: String,
    straightLineDistance: String,
    onStopClick: () -> Unit,
    onRecenterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val navBarPadding = with(density) {
        val bottomInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.rootWindowInsets
                ?.getInsets(AndroidWindowInsets.Type.navigationBars())
                ?.bottom ?: 0
        } else {
            @Suppress("DEPRECATION")
            view.rootWindowInsets?.systemWindowInsetBottom ?: 0
        }
        bottomInset.toDp()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0xE6111821),
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0x6600D9FF),
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
            )
            .padding(start = 18.dp, top = 14.dp, end = 18.dp, bottom = 16.dp + navBarPadding)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 42.dp, height = 4.dp)
                .background(Color(0x5500D9FF), RoundedCornerShape(999.dp))
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (roadDuration.isNotBlank()) roadDuration else "--",
                    color = TextPrimary,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "ETA to destination",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = roadDistance.ifBlank { "Calculating" },
                    color = NeonCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "road distance",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x66131A24), RoundedCornerShape(18.dp))
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .background(AmberPin, CircleShape)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "To",
                    color = Color(0x99FFFFFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = selectedPlace.name,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            Text(
                text = straightLineDistance,
                color = AmberPin,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PlaceActionButton(
                label = "Stop",
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                backgroundColor = Color(0x66FF3B5C),
                borderColor = Color(0x99FF6B8A),
                textColor = TextPrimary,
                modifier = Modifier.weight(1f),
                onClick = onStopClick
            )

            PlaceActionButton(
                label = "Recenter",
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.MyLocation,
                        contentDescription = null,
                        tint = Color(0xFF031219),
                        modifier = Modifier.size(18.dp)
                    )
                },
                backgroundColor = NeonCyan,
                borderColor = NeonCyan,
                textColor = Color(0xFF031219),
                modifier = Modifier.weight(1f),
                onClick = onRecenterClick
            )
        }
    }
}

@Composable
private fun LiveRoutePill() {
    Row(
        modifier = Modifier
            .background(SuccessGreen.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
            .border(1.dp, SuccessGreen.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(SuccessGreen, CircleShape)
        )

        Spacer(modifier = Modifier.width(5.dp))

        Text(
            text = "LIVE ROUTE",
            color = SuccessGreen,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun TopSearchOverlay(
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SearchBar(
            modifier = Modifier.fillMaxWidth(),
            onClick = onSearchClick
        )
        Spacer(modifier = Modifier.height(12.dp))
        CategoryFilters()
    }
}

@Composable
private fun CategoryFilters() {
    val categories = listOf(
        MapCategory("All", NeonCyan),
        MapCategory("Food", AmberPin),
        MapCategory("Fuel", SuccessGreen),
        MapCategory("Health", SoftRed),
        MapCategory("ATM", Color(0xFF9BD7FF)),
        MapCategory("Parks", Color(0xFF74E58A))
    )
    var selectedCategory by remember { mutableStateOf(categories.first().label) }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(categories) { category ->
            val isSelected = category.label == selectedCategory
            val bgColor = if (isSelected) category.color.copy(alpha = 0.24f) else GlassPanelSoft
            val borderColor = if (isSelected) category.color.copy(alpha = 0.9f) else GlassBorder
            val textColor = if (isSelected) Color.White else TextPrimary

            Row(
                modifier = Modifier
                    .height(38.dp)
                    .background(color = bgColor, shape = RoundedCornerShape(19.dp))
                    .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(19.dp))
                    .clip(RoundedCornerShape(19.dp))
                    .clickable { selectedCategory = category.label }
                    .padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 9.dp else 7.dp)
                        .background(category.color, CircleShape)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = category.label,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

private data class MapCategory(
    val label: String,
    val color: Color
)

@Composable
private fun SearchBar(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(58.dp)
            .background(
                color = Color(0xE0111821),
                shape = RoundedCornerShape(29.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0x4000D9FF),
                shape = RoundedCornerShape(29.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
                tint = NeonCyan,
                modifier = Modifier.size(21.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Where to?",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            SearchActionIcon(isMuted = true) {
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = "Voice search coming soon",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .height(34.dp)
                    .background(Color(0x2200D9FF), RoundedCornerShape(17.dp))
                    .border(1.dp, Color(0x3300D9FF), RoundedCornerShape(17.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Live",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SearchActionIcon(
    isMuted: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(
                color = if (isMuted) Color(0x221D2A35) else Color(0x331D2A35),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun RoutePreviewSheet(
    selectedPlace: SearchResult,
    straightLineDistance: String,
    roadDistance: String,
    roadDuration: String,
    fromValue: String,
    onStartClick: () -> Unit,
    onChangeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val navBarPadding = with(density) {
        val bottomInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.rootWindowInsets
                ?.getInsets(AndroidWindowInsets.Type.navigationBars())
                ?.bottom ?: 0
        } else {
            @Suppress("DEPRECATION")
            view.rootWindowInsets?.systemWindowInsetBottom ?: 0
        }
        bottomInset.toDp()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = GlassPanel,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0x5500D9FF),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
            .padding(
                start = 18.dp,
                top = 14.dp,
                end = 18.dp,
                bottom = 16.dp + navBarPadding
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 42.dp, height = 4.dp)
                .background(
                    color = Color(0x4400D9FF),
                    shape = RoundedCornerShape(999.dp)
                )
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RouteBadge()

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Road route",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = if (roadDuration.isNotBlank()) "ETA: $roadDuration" else "Calculating road route...",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }

            if (roadDuration.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .background(Color(0x2200D9FF), RoundedCornerShape(18.dp))
                        .border(1.dp, Color(0x6600D9FF), RoundedCornerShape(18.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = roadDuration,
                        color = NeonCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        RoutePointRow(
            label = "From",
            value = fromValue.ifBlank { "Current Location" },
            accentColor = NeonCyan
        )

        Spacer(modifier = Modifier.height(10.dp))

        RoutePointRow(
            label = "To",
            value = selectedPlace.name,
            accentColor = AmberPin
        )

        Spacer(modifier = Modifier.height(14.dp))

        RouteMetricRow(
            straightLineDistance = straightLineDistance,
            roadDistance = roadDistance
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PlaceActionButton(
                label = "Start route",
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Route,
                        contentDescription = null,
                        tint = Color(0xFF031219),
                        modifier = Modifier.size(18.dp)
                    )
                },
                backgroundColor = NeonCyan,
                borderColor = NeonCyan,
                textColor = Color(0xFF031219),
                modifier = Modifier.weight(1f),
                onClick = onStartClick
            )

            PlaceActionButton(
                label = "Edit",
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.SwapVert,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                backgroundColor = Color(0x66131A24),
                borderColor = Color(0x22FFFFFF),
                textColor = TextPrimary,
                modifier = Modifier.weight(1f),
                onClick = onChangeClick
            )
        }
    }
}

@Composable
private fun RouteBadge() {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                color = Color(0x2200D9FF),
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = Color(0x8800D9FF),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Route,
            contentDescription = null,
            tint = NeonCyan,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun RoutePointRow(
    label: String,
    value: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0x66131A24),
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0x22FFFFFF),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    color = accentColor,
                    shape = CircleShape
                )
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = Color(0x99FFFFFF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = value,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RouteMetricRow(
    straightLineDistance: String,
    roadDistance: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RouteMetric(
            label = "Straight-line",
            value = straightLineDistance,
            modifier = Modifier.weight(1f)
        )

        RouteMetric(
            label = "Road route",
            value = roadDistance,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RouteMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(58.dp)
            .background(
                color = Color(0x66131A24),
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0x22FFFFFF),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Text(
            text = label,
            color = Color(0x99FFFFFF),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun SelectedPlaceSheetExpandedContent(
    selectedPlace: SearchResult,
    distanceValue: String,
    onDirectionsClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val navBarPadding = with(density) {
        val bottomInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.rootWindowInsets
                ?.getInsets(AndroidWindowInsets.Type.navigationBars())
                ?.bottom ?: 0
        } else {
            @Suppress("DEPRECATION")
            view.rootWindowInsets?.systemWindowInsetBottom ?: 0
        }
        bottomInset.toDp()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 18.dp,
                top = 0.dp,
                end = 18.dp,
                bottom = 16.dp + navBarPadding
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .background(
                    color = Color(0x22131A24),
                    shape = RoundedCornerShape(22.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color(0x33FFC857),
                    shape = RoundedCornerShape(22.dp)
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectedPlaceBadge(size = 64)

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedPlace.name,
                        color = TextPrimary,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(7.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlaceTypePill(text = selectedPlace.subtitle, color = AmberPin)
                        Spacer(modifier = Modifier.width(8.dp))
                        PlaceTypePill(text = selectedPlace.source, color = NeonCyan)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        PlaceInfoCard(
            title = "Distance",
            value = distanceValue.ifBlank { "Location needed" },
            accentColor = AmberPin
        )

        Spacer(modifier = Modifier.height(10.dp))

        PlaceInfoCard(
            title = "Coordinates",
            value = formatSelectedPlaceCoordinates(selectedPlace),
            accentColor = NeonCyan
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MiniActionChip(
                label = "Save",
                icon = { Icon(Icons.Outlined.FavoriteBorder, null, tint = TextPrimary, modifier = Modifier.size(17.dp)) },
                modifier = Modifier.weight(1f)
            )
            MiniActionChip(
                label = "Share",
                icon = { Icon(Icons.Outlined.Share, null, tint = TextPrimary, modifier = Modifier.size(17.dp)) },
                modifier = Modifier.weight(1f)
            )
            MiniActionChip(
                label = "Info",
                icon = { Icon(Icons.Outlined.Info, null, tint = TextPrimary, modifier = Modifier.size(17.dp)) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PlaceActionButton(
                label = "Directions",
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Directions,
                        contentDescription = null,
                        tint = Color(0xFF031219),
                        modifier = Modifier.size(18.dp)
                    )
                },
                backgroundColor = AmberPin,
                borderColor = AmberPin,
                textColor = Color(0xFF031219),
                modifier = Modifier.weight(1f),
                onClick = onDirectionsClick
            )

            PlaceActionButton(
                label = "Close",
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                backgroundColor = Color(0x66131A24),
                borderColor = Color(0x22FFFFFF),
                textColor = TextPrimary,
                modifier = Modifier.weight(1f),
                onClick = onClearClick
            )
        }
    }
}

@Composable
private fun PlaceTypePill(
    text: String,
    color: Color
) {
    Text(
        text = text.ifBlank { "Place" },
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp)
    )
}

@Composable
private fun PlaceInfoCard(
    title: String,
    value: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x66131A24), RoundedCornerShape(18.dp))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(accentColor, CircleShape)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color(0x99FFFFFF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MiniActionChip(
    label: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(42.dp)
            .background(Color(0x44131A24), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun SelectedPlaceBadge(size: Int = 48) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(
                color = Color(0x22FFC857),
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = Color(0x88FFC857),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size((size * 0.62f).dp)
                .background(
                    color = Color(0x33FFC857),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Place,
                contentDescription = null,
                tint = AmberPin,
                modifier = Modifier.size((size * 0.4f).dp)
            )
        }
    }
}

@Composable
private fun PlaceActionButton(
    label: String,
    icon: @Composable () -> Unit,
    backgroundColor: Color,
    borderColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(46.dp)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun MapControlsColumn(
    modifier: Modifier = Modifier,
    onCurrentLocationClick: () -> Unit,
    onZoomInClick: () -> Unit,
    onZoomOutClick: () -> Unit,
    onStyleSwitchClick: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .background(
                    color = GlassPanelSoft,
                    shape = RoundedCornerShape(26.dp)
                )
                .border(
                    width = 1.dp,
                    color = GlassBorder,
                    shape = RoundedCornerShape(26.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                    .clickable(onClick = onZoomInClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Zoom In",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(1.dp)
                    .background(Color(0x33FFFFFF))
                    .align(Alignment.CenterHorizontally)
            )
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
                    .clickable(onClick = onZoomOutClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Remove,
                    contentDescription = "Zoom Out",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        CircleIconButton(
            size = 52.dp,
            modifier = Modifier.clickable(onClick = onStyleSwitchClick)
        ) {
            Icon(
                imageVector = Icons.Outlined.Layers,
                contentDescription = "Map Style",
                tint = TextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        CircleIconButton(
            size = 52.dp,
            modifier = Modifier.clickable(onClick = onCurrentLocationClick)
        ) {
            Icon(
                imageVector = Icons.Outlined.MyLocation,
                contentDescription = "Current location",
                tint = NeonCyan,
                modifier = Modifier.size(23.dp)
            )
        }
    }
}

private fun formatSelectedPlaceCoordinates(selectedPlace: SearchResult): String {
    return String.format(
        Locale.US,
        "Lat %.5f - Lng %.5f",
        selectedPlace.latitude,
        selectedPlace.longitude
    )
}

@Composable
private fun CurrentLocationSheet(
    addressValue: String,
    coordinateValue: String,
    areaValue: String,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val navBarPadding = with(density) {
        val bottomInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.rootWindowInsets
                ?.getInsets(AndroidWindowInsets.Type.navigationBars())
                ?.bottom ?: 0
        } else {
            @Suppress("DEPRECATION")
            view.rootWindowInsets?.systemWindowInsetBottom ?: 0
        }
        bottomInset.toDp()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = GlassPanel,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
            .border(
                width = 1.dp,
                color = GlassBorder,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
            .padding(
                start = 18.dp,
                top = 14.dp,
                end = 18.dp,
                bottom = 16.dp + navBarPadding
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 42.dp, height = 4.dp)
                .background(
                    color = Color(0x33FFFFFF),
                    shape = RoundedCornerShape(999.dp)
                )
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CurrentLocationBadge()

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Current Location",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(7.dp))

                Text(
                    text = "Address",
                    color = Color(0x99FFFFFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = addressValue,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(7.dp))

                Text(
                    text = coordinateValue,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AreaPanel(
            value = areaValue,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CurrentLocationBadge() {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                color = Color(0x2200D9FF),
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = Color(0x6600D9FF),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    color = Color(0x3300D9FF),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = NeonCyan,
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = TextPrimary,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun AreaPanel(
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(58.dp)
            .background(
                color = Color(0x66131A24),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0x22FFFFFF),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Place,
            contentDescription = null,
            tint = AmberPin,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(
                text = "Area",
                color = Color(0x99FFFFFF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = value,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CircleIconButton(
    size: androidx.compose.ui.unit.Dp = 56.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                color = GlassPanelSoft,
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = GlassBorder,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
