package com.subham.pupumap.ui

import android.os.Build
import android.view.WindowInsets as AndroidWindowInsets
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subham.pupumap.model.SearchResult
import com.subham.pupumap.network.RetrofitClient
import kotlinx.coroutines.delay
import java.util.Locale

private val SearchBackground = Color(0xE603080D)
private val SearchPanel = Color(0xE0111821)
private val SearchPanelSoft = Color(0x99101820)
private val SearchBorder = Color(0x4000D9FF)
private val SearchBorderSoft = Color(0x22FFFFFF)
private val SearchTextPrimary = Color(0xF2FFFFFF)
private val SearchTextSecondary = Color(0xB8FFFFFF)
private val SearchTextMuted = Color(0x8AFFFFFF)
private val SearchCyan = Color(0xFF00D9FF)
private val SearchAmber = Color(0xFFFFC857)

@Composable
fun SearchOverlay(
    onClose: () -> Unit,
    onResultSelected: (SearchResult) -> Unit,
    currentLatitude: Double? = null,
    currentLongitude: Double? = null,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
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

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    LaunchedEffect(query, currentLatitude, currentLongitude) {
        val cleanQuery = query.trim()
        errorMessage = null

        if (cleanQuery.length < 2) {
            results = emptyList()
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        delay(250)

        try {
            results = RetrofitClient.api.searchPlaces(
                query = cleanQuery,
                latitude = currentLatitude,
                longitude = currentLongitude
            )
        } catch (e: Exception) {
            results = emptyList()
            errorMessage = "Search unavailable"
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SearchBackground)
            .padding(top = statusBarPadding + 14.dp, start = 14.dp, end = 14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SearchInputBar(
                query = query,
                onQueryChange = { query = it },
                onClose = onClose,
                focusRequester = focusRequester
            )

            Spacer(modifier = Modifier.height(14.dp))

            QuickSearchChips(
                onChipClick = { query = it }
            )

            Spacer(modifier = Modifier.height(18.dp))

            SearchResultsPanel(
                query = query,
                results = results,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onResultSelected = onResultSelected
            )
        }
    }
}

@Composable
private fun SearchInputBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    focusRequester: FocusRequester
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(
                color = SearchPanel,
                shape = RoundedCornerShape(29.dp)
            )
            .border(
                width = 1.dp,
                color = SearchBorder,
                shape = RoundedCornerShape(29.dp)
            )
            .padding(start = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SearchRoundButton(onClick = onClose) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Close search",
                tint = SearchTextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = SearchTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                cursorBrush = SolidColor(SearchCyan),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            if (query.isBlank()) {
                Text(
                    text = "Search destination",
                    color = SearchTextSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (query.isNotBlank()) {
            SearchRoundButton(onClick = { onQueryChange("") }) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Clear search",
                    tint = SearchTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            SearchRoundButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = "Voice search",
                    tint = SearchTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickSearchChips(onChipClick: (String) -> Unit) {
    val chips = listOf("Hospitals", "Fuel", "Food", "ATM")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        chips.forEach { label ->
            QuickChip(
                label = label,
                onClick = { onChipClick(label) }
            )
        }
    }
}

@Composable
private fun QuickChip(
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .background(
                color = SearchPanelSoft,
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = SearchBorderSoft,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(
                    color = Color(0x2200D9FF),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label.first().toString(),
                color = SearchCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = label,
            color = SearchTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SearchResultsPanel(
    query: String,
    results: List<SearchResult>,
    isLoading: Boolean,
    errorMessage: String?,
    onResultSelected: (SearchResult) -> Unit
) {
    val trimmedQuery = query.trim()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xBB101820),
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                color = SearchBorderSoft,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = when {
                trimmedQuery.length < 2 -> "Suggestions"
                isLoading -> "Searching map data"
                else -> "Results"
            },
            color = SearchTextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 16.dp, top = 6.dp, bottom = 4.dp)
        )

        when {
            trimmedQuery.length < 2 -> DefaultSuggestionRows()
            isLoading -> StatusRow(title = "Searching...", subtitle = "Checking places, roads, buildings and POIs")
            errorMessage != null -> StatusRow(title = errorMessage, subtitle = "Make sure backend is running")
            results.isEmpty() -> StatusRow(title = "No places found", subtitle = "Try a road, shop, area or landmark name")
            else -> results.forEach { result ->
                ResultRow(
                    result = result,
                    onClick = { onResultSelected(result) }
                )
            }
        }
    }
}

@Composable
private fun DefaultSuggestionRows() {
    val suggestions = listOf(
        SearchSuggestion("Current location", "Use your exact GPS position"),
        SearchSuggestion("Hospitals", "Search medical places"),
        SearchSuggestion("Fuel", "Find fuel stations"),
        SearchSuggestion("Popular places", "Explore places in your map data")
    )

    suggestions.forEach { suggestion ->
        SuggestionRow(suggestion = suggestion)
    }
}

@Composable
private fun SuggestionRow(suggestion: SearchSuggestion) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {}
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    color = Color(0x22FFC857),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (suggestion.title == "Current location") {
                    Icons.Outlined.Search
                } else {
                    Icons.Outlined.Place
                },
                contentDescription = null,
                tint = if (suggestion.title == "Current location") SearchCyan else SearchAmber,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.title,
                color = SearchTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = suggestion.subtitle,
                color = SearchTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ResultRow(
    result: SearchResult,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    color = Color(0x22FFC857),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Place,
                contentDescription = null,
                tint = SearchAmber,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.name,
                color = SearchTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = resultSubtitle(result),
                color = SearchTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StatusRow(
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    color = Color(0x2200D9FF),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = SearchCyan,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = SearchTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = subtitle,
                color = SearchTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SearchRoundButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(
                color = Color(0x331D2A35),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

private data class SearchSuggestion(
    val title: String,
    val subtitle: String
)

private fun resultSubtitle(result: SearchResult): String {
    val distance = result.distance_meters?.let { formatDistance(it) }
    return if (distance == null) {
        "${result.subtitle} - ${result.source}"
    } else {
        "${result.subtitle} - $distance"
    }
}

private fun formatDistance(distanceMeters: Int): String {
    return if (distanceMeters < 1000) {
        "$distanceMeters m away"
    } else {
        String.format(Locale.US, "%.1f km away", distanceMeters / 1000.0)
    }
}
