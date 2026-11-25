@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.hotaro.brik.ui.glass

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hotaro.brik.ui.component.FloatingNavItem
import com.hotaro.brik.ui.glass.floatingtabbar.FloatingTabBar
import com.hotaro.brik.ui.glass.floatingtabbar.FloatingTabBarDefaults
import com.hotaro.brik.ui.glass.floatingtabbar.FloatingTabBarInlineBehavior
import com.hotaro.brik.ui.glass.floatingtabbar.LocalTabBarBackdropFrozen
import com.hotaro.brik.ui.glass.floatingtabbar.rememberFloatingTabBarScrollConnection
import com.hotaro.brik.ui.glass.shapes.ContinuousRoundedRectangle

private val NavBarShape = ContinuousRoundedRectangle(percent = 50)
private val NavBarMinTabWidth = 56.dp

/**
 * Floating Liquid Glass navigation bar for Brik, imported from Mizan / QuranReader.
 *
 * Configured with FloatingTabBarInlineBehavior.Never so it does NOT hide or collapse
 * when scrolling downwards.
 */
@Composable
fun BrikGlassNavBar(
    items: List<FloatingNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navBarBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val glassConfig = LocalGlassEffectConfig.current
    val useGlass = glassConfig.isEnabledFor(GlassComponent.NAV_BAR) && isGlassAllowed()

    val backgroundColor = when {
        useGlass -> Color.Transparent
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val usesCustomTextColor = useGlass && glassConfig.textColor.isSpecified
    val rawTabTextColor = when {
        usesCustomTextColor -> glassConfig.textColor
        useGlass -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    val tabTextColor by animateColorAsState(
        targetValue = rawTabTextColor,
        label = "navBarTextColor",
    )
    val selectedContentColor = tabTextColor
    val unselectedContentColor = if (usesCustomTextColor) {
        tabTextColor.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val tabBarContentModifier = if (useGlass) {
        Modifier.liquidGlass(
            config = glassConfig,
            shape = NavBarShape,
            highlightAlpha = glassConfig.highlightOpacity,
            frozen = LocalTabBarBackdropFrozen.current,
        )
    } else Modifier

    // Configured with inlineBehavior = Never so it never hides or collapses on scroll down
    val scrollConnection = rememberFloatingTabBarScrollConnection(
        initialIsInline = false,
        inlineBehavior = FloatingTabBarInlineBehavior.Never
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = navBarBottomInset + 12.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints {
            val tabWidth = if (items.isEmpty()) {
                FloatingTabBarDefaults.TabWidth
            } else {
                (maxWidth / items.size).coerceIn(NavBarMinTabWidth, FloatingTabBarDefaults.TabWidth)
            }

            FloatingTabBar(
                selectedTabKey = selectedIndex,
                scrollConnection = scrollConnection,
                modifier = Modifier.wrapContentWidth(),
                tabBarContentModifier = tabBarContentModifier,
                colors = FloatingTabBarDefaults.colors(
                    backgroundColor = backgroundColor,
                    accessoryBackgroundColor = backgroundColor,
                ),
                sizes = FloatingTabBarDefaults.sizes(
                    tabBarContentPadding = PaddingValues(vertical = 4.dp, horizontal = 4.dp),
                    tabExpandedContentPadding = PaddingValues(vertical = 4.dp, horizontal = 6.dp),
                    tabInlineContentPadding = PaddingValues(8.dp),
                    tabWidth = tabWidth,
                ),
                backdrop = if (useGlass) LocalAppBackdrop.current else null,
                accentColor = tabTextColor,
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = selectedIndex == index
                    tab(
                        key = index,
                        title = {
                            val itemColor by animateColorAsState(
                                targetValue = if (isSelected) selectedContentColor else unselectedContentColor,
                                animationSpec = androidx.compose.animation.core.tween(durationMillis = 250),
                                label = "tabTitleColor_$index"
                            )
                            Text(
                                text = item.label,
                                color = itemColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 10.sp,
                            )
                        },
                        icon = {
                            val itemColor by animateColorAsState(
                                targetValue = if (isSelected) selectedContentColor else unselectedContentColor,
                                animationSpec = androidx.compose.animation.core.tween(durationMillis = 250),
                                label = "tabIconColor_$index"
                            )
                            Icon(
                                painter = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                                tint = itemColor,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        onClick = { onItemSelected(index) },
                    )
                }
            }
        }
    }
}
