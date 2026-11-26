package com.hotaro.brik.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun Modifier.holdToProgress(
    durationMillis: Long = 5000L,
    enabled: Boolean = true,
    onProgressUpdate: (Float) -> Unit,
    onCompleted: () -> Unit,
    onTap: () -> Unit
): Modifier = holdToProgress(
    durationMillis = durationMillis,
    enabled = enabled,
    onProgressUpdate = { progress, _ -> onProgressUpdate(progress) },
    onCompleted = onCompleted,
    onTap = onTap
)

@Composable
fun Modifier.holdToProgress(
    durationMillis: Long = 5000L,
    enabled: Boolean = true,
    onProgressUpdate: (progress: Float, isPressed: Boolean) -> Unit,
    onCompleted: () -> Unit,
    onTap: () -> Unit
): Modifier {
    val coroutineScope = rememberCoroutineScope()
    var animJob by remember { mutableStateOf<Job?>(null) }
    val progress = remember { Animatable(0f) }
    var isPressed by remember { mutableStateOf(false) }

    LaunchedEffect(progress.value, isPressed) {
        onProgressUpdate(progress.value, isPressed)
    }

    return this.pointerInput(enabled) {
        if (!enabled) {
            detectTapGestures(onTap = { onTap() })
            return@pointerInput
        }
        detectTapGestures(
            onPress = {
                var completed = false
                isPressed = true
                val startTime = System.currentTimeMillis()
                animJob = coroutineScope.launch {
                    progress.snapTo(0f)
                    progress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = durationMillis.toInt(), easing = LinearEasing)
                    )
                    completed = true
                    onCompleted()
                }
                val released = tryAwaitRelease()
                val pressDuration = System.currentTimeMillis() - startTime
                isPressed = false
                if (!completed) {
                    animJob?.cancel()
                    coroutineScope.launch {
                        progress.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                        )
                    }
                    if (released && pressDuration < 400) {
                        onTap()
                    }
                }
            }
        )
    }
}

fun Modifier.drawBorderProgress(
    progress: Float,
    shape: Shape,
    color: Color,
    strokeWidth: Dp = 3.5.dp
): Modifier = this.drawWithContent {
    drawContent()
    if (progress > 0.005f) {
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = when (outline) {
            is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
            is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
            is Outline.Generic -> outline.path
        }
        val pathMeasure = PathMeasure()
        pathMeasure.setPath(path, true)
        val length = pathMeasure.length
        val segmentPath = Path()
        pathMeasure.getSegment(0f, length * progress.coerceIn(0f, 1f), segmentPath, true)
        drawPath(
            path = segmentPath,
            color = color,
            style = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@Composable
fun rememberFloatingActionButtonExpanded(
    listState: LazyListState,
    initialExpanded: Boolean = true
): State<Boolean> {
    val expanded = remember { mutableStateOf(initialExpanded) }
    LaunchedEffect(listState) {
        var prevIndex = listState.firstVisibleItemIndex
        var prevOffset = listState.firstVisibleItemScrollOffset
        snapshotFlow { Pair(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) }
            .collect { (currIndex, currOffset) ->
                if (currIndex == 0 && currOffset <= 20) {
                    expanded.value = true
                    prevIndex = 0
                    prevOffset = currOffset
                } else if (currIndex > prevIndex) {
                    expanded.value = false
                    prevIndex = currIndex
                    prevOffset = currOffset
                } else if (currIndex < prevIndex) {
                    expanded.value = true
                    prevIndex = currIndex
                    prevOffset = currOffset
                } else {
                    val delta = currOffset - prevOffset
                    if (delta > 8) {
                        expanded.value = false
                        prevOffset = currOffset
                    } else if (delta < -8) {
                        expanded.value = true
                        prevOffset = currOffset
                    }
                }
            }
    }
    return expanded
}

@Composable
fun rememberScrollStateExpanded(
    scrollState: ScrollState,
    initialExpanded: Boolean = true
): State<Boolean> {
    val expanded = remember { mutableStateOf(initialExpanded) }
    LaunchedEffect(scrollState) {
        var prevValue = scrollState.value
        snapshotFlow { scrollState.value }
            .collect { currValue ->
                if (currValue <= 20) {
                    expanded.value = true
                    prevValue = currValue
                } else {
                    val delta = currValue - prevValue
                    if (delta > 8) {
                        expanded.value = false
                        prevValue = currValue
                    } else if (delta < -8) {
                        expanded.value = true
                        prevValue = currValue
                    }
                }
            }
    }
    return expanded
}
