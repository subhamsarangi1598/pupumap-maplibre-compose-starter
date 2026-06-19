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
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Search
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
    onSearchResultSelected: (SearchResult) -> Unit = {},
    onClearSelectedPlace: () -> Unit = {},
    onDirectionsClick: () -> Unit = {},
    onChangeRouteClick: () -> Unit = {},
    onStartRouteClick: () -> Unit = {},
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
            TopSearchOverlay(
                modifier = Modifier.align(Alignment.TopCenter),
                onSearchClick = { isSearchOpen = true }
            )

            MapControlsColumn(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(bottom = 120.dp),
                onCurrentLocationClick = onCurrentLocationClick,
                onZoomInClick = onZoomInClick,
                onZoomOutClick = onZoomOutClick,
                onStyleSwitchClick = onStyleSwitchClick
            )

            when {
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

        if (selectedPlace != null && !isRoutePreviewOpen) {
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

        if (isSearchOpen) {
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
    val categories = listOf("All", "Food", "Parks", "Museums", "Shopping", "Hotels")
    var selectedCategory by remember { mutableStateOf("All") }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(categories) { category ->
            val isSelected = category == selectedCategory
            val bgColor = if (isSelected) Color(0xFF0288D1) else GlassPanelSoft
            val borderColor = if (isSelected) Color(0xFF81D4FA) else GlassBorder
            val textColor = if (isSelected) Color.White else TextPrimary

            Box(
                modifier = Modifier
                    .background(color = bgColor, shape = RoundedCornerShape(16.dp))
                    .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { selectedCategory = category }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

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

            SearchActionIcon {
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = "Voice search",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            SearchActionIcon {
                Icon(
                    imageVector = Icons.Outlined.AccountCircle,
                    contentDescription = "Profile",
                    tint = TextPrimary,
                    modifier = Modifier.size(23.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchActionIcon(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(
                color = Color(0x331D2A35),
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
                    text = "Route Preview",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
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
                label = "Start",
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
                label = "Change",
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
            fontSize = 12.sp,
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
        // Image Placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(
                    color = Color(0x33FFFFFF),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Place,
                contentDescription = null,
                tint = Color(0x88FFFFFF),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Cover Image",
                color = Color(0x88FFFFFF),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 70.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            SelectedPlaceBadge()

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedPlace.name,
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${selectedPlace.subtitle} - ${selectedPlace.source}",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Directions,
                        contentDescription = null,
                        tint = AmberPin,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = distanceValue,
                        color = AmberPin,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = formatSelectedPlaceCoordinates(selectedPlace),
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "A beautiful and prominent location found in your search results. Come visit and explore the surroundings.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

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
private fun SelectedPlaceBadge() {
    Box(
        modifier = Modifier
            .size(48.dp)
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
                .size(30.dp)
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
                modifier = Modifier.size(19.dp)
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
