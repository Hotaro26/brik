package com.hotaro.brik.ui.component

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundedPullRefreshPage(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    containerColor: Color = Color.Unspecified,
    pageColor: Color = Color.Unspecified,
    pageShape: Shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    content: @Composable (PaddingValues) -> Unit
) {
    val state = rememberPullToRefreshState()
    val view = LocalView.current
    val density = LocalDensity.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val actualContainerColor = if (containerColor != Color.Unspecified) {
        containerColor
    } else if (isDark) {
        Color.Black
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val actualPageColor = if (pageColor != Color.Unspecified) {
        pageColor
    } else if (isDark) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surface
    }

    val thresholdDp = 80.dp
    val thresholdPx = with(density) { thresholdDp.toPx() }
    val maxPullPx = with(density) { 140.dp.toPx() }

    // Haptic feedback tick when passing threshold
    var hasVibratedThreshold by remember { mutableStateOf(false) }
    LaunchedEffect(state.distanceFraction) {
        if (state.distanceFraction >= 1.0f && !hasVibratedThreshold && !isRefreshing) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            hasVibratedThreshold = true
        } else if (state.distanceFraction < 0.2f) {
            hasVibratedThreshold = false
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .pullToRefresh(
                state = state,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh
            ),
        containerColor = actualContainerColor,
        topBar = topBar,
        floatingActionButton = floatingActionButton
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            // Loading indicator revealed behind the descending page
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .graphicsLayer {
                        val progress = state.distanceFraction.coerceIn(0f, 1f)
                        alpha = if (isRefreshing) 1f else progress
                        val scale = if (isRefreshing) 1f else 0.4f + (progress * 0.6f)
                        scaleX = scale
                        scaleY = scale
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(38.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.5.dp
                    )
                } else if (state.distanceFraction > 0.05f) {
                    CircularProgressIndicator(
                        progress = { state.distanceFraction.coerceIn(0f, 1f) },
                        modifier = Modifier.size(38.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.5.dp
                    )
                }
            }

            // Rounded Page Surface that detaches and slides downwards when pulled
            val pullDistance = (state.distanceFraction * thresholdPx).coerceIn(0f, maxPullPx)
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = pullDistance
                        clip = true
                        shape = pageShape
                    },
                shape = pageShape,
                color = actualPageColor
            ) {
                val subPadding = PaddingValues(
                    start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                    top = 0.dp,
                    end = innerPadding.calculateEndPadding(LayoutDirection.Ltr),
                    bottom = innerPadding.calculateBottomPadding()
                )
                content(subPadding)
            }
        }
    }
}
