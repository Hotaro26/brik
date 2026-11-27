package com.hotaro.brik.ui.screen

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import com.hotaro.brik.ui.component.BrikSwitch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hotaro.brik.R
import com.hotaro.brik.ui.component.PullRefreshIndicatorDispatcher
import com.hotaro.brik.ui.component.TomatoSectionHeader
import com.hotaro.brik.ui.component.TomatoSettingsItem
import com.hotaro.brik.ui.component.segmentedItemShape
import com.hotaro.brik.ui.theme.PullRefreshAnimStyle
import com.hotaro.brik.ui.theme.ThemePreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiscWindow(
    onBack: () -> Unit,
    onNavigateToAssistiveSettings: () -> Unit = {},
    onNavigateToKioskSettings: () -> Unit = {},
    onNavigateToSideloadLock: () -> Unit = {}
) {
    val context = LocalContext.current
    val use24Hour by ThemePreferences.use24HourClock.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Miscellaneous",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        letterSpacing = (-0.5).sp,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "General app-wide preferences",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                Surface(
                    shape = CircleShape,
                    color = colorScheme.secondaryContainer,
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(40.dp),
                    onClick = onBack
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_fill0),
                            contentDescription = "Back",
                            tint = colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            modifier = Modifier.statusBarsPadding()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Spacer(Modifier.height(10.dp))

            TomatoSectionHeader(title = "DATE & TIME")

            // Clock format card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colorScheme.secondaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.schedule_fill0),
                                contentDescription = null,
                                tint = colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Clock Format",
                            style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = if (use24Hour) "24-hour (14:30)" else "12-hour (2:30 PM)",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    BrikSwitch(
                        checked = use24Hour,
                        onCheckedChange = { ThemePreferences.setUse24HourClock(context, it) }
                    )
                }
            }

            // Format preview card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colorScheme.surfaceContainerLowest,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Preview",
                            style = typography.labelMedium,
                            color = colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(2.dp))
                        val previewTime = remember(use24Hour) {
                            if (use24Hour) "14:30 — 09:05" else "2:30 PM — 9:05 AM"
                        }
                        Text(
                            text = previewTime,
                            style = typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Used across routines, app limits, and time displays",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // SECTION 2: CONTROLS & KIOSK
            TomatoSectionHeader(title = "CONTROLS & KIOSK")

            TomatoSettingsItem(
                title = "Assistive Quick Controls",
                subtitle = "Floating ball, tile selection & notifications",
                leadingIcon = R.drawable.ic_assistive_touch,
                shape = segmentedItemShape(0, 3),
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.arrow_forward_big),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                },
                onClick = onNavigateToAssistiveSettings
            )

            TomatoSettingsItem(
                title = "Kiosk Launcher Settings",
                subtitle = "Incoming calls window, emergency & lockdown",
                leadingIcon = R.drawable.screen_lock_portrait_fill0,
                shape = segmentedItemShape(1, 3),
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.arrow_forward_big),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                },
                onClick = onNavigateToKioskSettings
            )

            TomatoSettingsItem(
                title = "Sideload & ADB Lock",
                subtitle = "Block APK installation, Play Store & debugging",
                leadingIcon = R.drawable.block_fill0,
                shape = segmentedItemShape(2, 3),
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.arrow_forward_big),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                },
                onClick = onNavigateToSideloadLock
            )

            Spacer(Modifier.height(18.dp))

            // SECTION 3: PULL-DOWN ANIMATION
            TomatoSectionHeader(title = "PULL-DOWN ANIMATION")

            val currentAnimStyle by ThemePreferences.pullRefreshAnim.collectAsState()
            var previewStyle by remember(currentAnimStyle) { mutableStateOf(currentAnimStyle) }

            // Live Animated Preview Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            PullRefreshIndicatorDispatcher(
                                style = previewStyle,
                                progress = 1f,
                                isRefreshing = true,
                                modifier = Modifier.size(36.dp),
                                color = colorScheme.primary
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = previewStyle.displayName,
                            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = when (previewStyle) {
                                PullRefreshAnimStyle.SMILEY -> "Playful smiling face with winking animation"
                                PullRefreshAnimStyle.CAT -> "Cute feline with twitching ears and whiskers"
                                PullRefreshAnimStyle.BUTTERFLY -> "Graceful fluttering butterfly wings"
                                PullRefreshAnimStyle.BOOKS -> "Open tome with flipping knowledge pages"
                                PullRefreshAnimStyle.LOTUS -> "Mindful meditator on a blooming lotus"
                                PullRefreshAnimStyle.RANDOM -> "Surprise random animation on every pull"
                            },
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Selector list of styles
            val styles = listOf(
                PullRefreshAnimStyle.SMILEY to "Smiley (Default)",
                PullRefreshAnimStyle.CAT to "Cat",
                PullRefreshAnimStyle.BUTTERFLY to "Butterfly",
                PullRefreshAnimStyle.BOOKS to "Books",
                PullRefreshAnimStyle.LOTUS to "Man in Lotus",
                PullRefreshAnimStyle.RANDOM to "Random Mixed"
            )

            styles.forEachIndexed { index, (style, label) ->
                val isSelected = currentAnimStyle == style
                Surface(
                    onClick = {
                        previewStyle = style
                        ThemePreferences.setPullRefreshAnim(context, style)
                    },
                    shape = segmentedItemShape(index, styles.size),
                    color = if (isSelected) colorScheme.primaryContainer.copy(alpha = 0.35f) else colorScheme.surfaceContainerHigh,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) colorScheme.primary.copy(alpha = 0.6f) else Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    PullRefreshIndicatorDispatcher(
                                        style = style,
                                        progress = 1f,
                                        isRefreshing = false,
                                        modifier = Modifier.size(22.dp),
                                        color = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = label,
                                    style = typography.titleSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                    ),
                                    color = if (isSelected) colorScheme.primary else colorScheme.onSurface
                                )
                                Text(
                                    text = if (style == PullRefreshAnimStyle.RANDOM) "Picks a different animation each time" else "Fixed pull animation",
                                    style = typography.bodySmall.copy(fontSize = 11.sp),
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                previewStyle = style
                                ThemePreferences.setPullRefreshAnim(context, style)
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colorScheme.primary
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}
