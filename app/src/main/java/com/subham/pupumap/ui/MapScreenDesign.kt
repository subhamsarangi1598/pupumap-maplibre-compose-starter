package com.subham.pupumap.ui

import android.os.Build
import android.view.WindowInsets as AndroidWindowInsets
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GlassPanel = Color(0xCC101820)
private val GlassPanelSoft = Color(0x99101820)
private val GlassBorder = Color(0x33FFFFFF)
private val TextPrimary = Color(0xF2FFFFFF)
private val TextSecondary = Color(0xB8FFFFFF)
private val NeonCyan = Color(0xFF00D9FF)
private val AmberPin = Color(0xFFFFC857)

@Composable
fun MapScreenDesign(
    addressValue: String = "Finding your exact place...",
    coordinateValue: String = "Waiting for GPS",
    areaValue: String = "Detecting area"
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 20.dp, start = 16.dp, end = 16.dp)
    ) {
        TopSearchOverlay(
            modifier = Modifier.align(Alignment.TopCenter)
        )

        CurrentLocationButton(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(bottom = 80.dp)
        )

        CurrentLocationSheet(
            addressValue = addressValue,
            coordinateValue = coordinateValue,
            areaValue = areaValue,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun TopSearchOverlay(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleIconButton {
            Icon(
                imageVector = Icons.Outlined.GridView,
                contentDescription = "Menu",
                tint = TextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        SearchBar(
            modifier = Modifier.weight(1f)
        )

        CircleIconButton {
            Icon(
                imageVector = Icons.Outlined.AccountCircle,
                contentDescription = "Profile",
                tint = TextPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun SearchBar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(56.dp)
            .background(
                color = GlassPanelSoft,
                shape = RoundedCornerShape(28.dp)
            )
            .border(
                width = 1.dp,
                color = GlassBorder,
                shape = RoundedCornerShape(28.dp)
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = "Search destination",
                color = TextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Outlined.Mic,
                contentDescription = "Voice search",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CurrentLocationButton(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        CircleIconButton(size = 52.dp) {
            Icon(
                imageVector = Icons.Outlined.MyLocation,
                contentDescription = "Current location",
                tint = NeonCyan,
                modifier = Modifier.size(23.dp)
            )
        }
    }
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
                shape = RoundedCornerShape(28.dp)
            )
            .border(
                width = 1.dp,
                color = GlassBorder,
                shape = RoundedCornerShape(28.dp)
            )
            .padding(
                start = 18.dp,
                top = 16.dp,
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

                Spacer(modifier = Modifier.height(4.dp))

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

                Spacer(modifier = Modifier.height(5.dp))

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
            modifier = Modifier.fillMaxWidth(),
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
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
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
