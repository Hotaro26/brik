package com.hotaro.brik.ui.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hotaro.brik.R
import com.hotaro.brik.data.model.AppInfo
import com.hotaro.brik.data.model.FocusMode
import com.hotaro.brik.data.model.KeyType
import com.hotaro.brik.ui.component.VerifyEmergencyKeyDialog
import com.hotaro.brik.util.QrUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ModeKioskDesktop(
    mode: FocusMode,
    sessionEndsAt: Long?,
    onExitSession: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var showVerifyKeyDialog by remember { mutableStateOf(false) }

    // Intercept back key to prevent navigating back without unlock
    BackHandler(enabled = true) {
        Toast.makeText(context, "Focus Mode kiosk is active. Hold screen to reveal exit.", Toast.LENGTH_SHORT).show()
    }

    var currentTime by remember { mutableStateOf(Date()) }
    var remainingMillis by remember { mutableLongStateOf(0L) }
    var isControlsRevealed by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var hideJob by remember { mutableStateOf<Job?>(null) }

    val endsAt = sessionEndsAt ?: 0L

    LaunchedEffect(endsAt) {
        while (true) {
            val now = System.currentTimeMillis()
            currentTime = Date(now)
            if (endsAt > 0L) {
                remainingMillis = (endsAt - now).coerceAtLeast(0L)
            }
            delay(1000)
        }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }

    val launcherApps = remember(context) { loadLauncherApps(pm, context.packageName) }
    val permittedApps = remember(mode.kioskAllowedPackages, launcherApps) {
        launcherApps.filter { mode.kioskAllowedPackages.contains(it.packageName) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0D10))
            .pointerInput(Unit) {
                val holdMillis = 3000L
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val start = System.currentTimeMillis()

                    val job = coroutineScope.launch {
                        while (isActive) {
                            val elapsed = System.currentTimeMillis() - start
                            val progress = (elapsed.toFloat() / holdMillis).coerceIn(0f, 1f)
                            holdProgress = progress
                            if (elapsed >= holdMillis) {
                                holdProgress = 0f
                                isControlsRevealed = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                hideJob?.cancel()
                                hideJob = coroutineScope.launch {
                                    delay(2000L)
                                    isControlsRevealed = false
                                }
                                break
                            }
                            delay(16L)
                        }
                    }

                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })

                    job.cancel()
                    holdProgress = 0f
                }
            }
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Main Minimalist Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(36.dp))

            // Profile Tag
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.mode_fill0),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = mode.name,
                        style = typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // Clock & Date
            Text(
                text = timeFormat.format(currentTime),
                style = typography.displayLarge.copy(
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-2).sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = Color.White
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = dateFormat.format(currentTime),
                style = typography.titleSmall.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                ),
                color = Color.White.copy(alpha = 0.5f)
            )

            Spacer(Modifier.height(44.dp))

            // Minimalist Text-Only App List (No icons)
            if (permittedApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pure Focus\nNo external apps permitted",
                        style = typography.bodyLarge.copy(
                            fontWeight = FontWeight.Light,
                            letterSpacing = 0.5.sp,
                            lineHeight = 24.sp
                        ),
                        color = Color.White.copy(alpha = 0.35f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(permittedApps, key = { it.packageName }) { app ->
                        Text(
                            text = app.label,
                            style = typography.titleMedium.copy(
                                fontWeight = FontWeight.Normal,
                                fontSize = 19.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                                    if (launchIntent != null) {
                                        try {
                                            context.startActivity(launchIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Cannot open app", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                .padding(vertical = 10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Subtle Hint at Bottom
            if (!isControlsRevealed && holdProgress == 0f) {
                Text(
                    text = "Press and hold anywhere to exit",
                    style = typography.labelSmall,
                    color = Color.White.copy(alpha = 0.2f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            } else {
                Spacer(Modifier.height(32.dp))
            }
        }

        // Circular Hold Progress Indicator (Centered)
        if (holdProgress > 0f) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { holdProgress },
                    color = Color.White,
                    strokeWidth = 3.dp,
                    trackColor = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.size(80.dp)
                )
            }
        }

        // Hold-to-Reveal Controls Overlay (Reveals for 2 seconds after 3s continuous press)
        AnimatedVisibility(
            visible = isControlsRevealed,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1E2028),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val remainingMinutes = remainingMillis / (1000 * 60)
                    val remainingSeconds = (remainingMillis / 1000) % 60
                    val timeRemainingStr = if (remainingMinutes > 0) {
                        "${remainingMinutes}m ${remainingSeconds}s remaining"
                    } else {
                        "${remainingSeconds}s remaining"
                    }

                    Text(
                        text = if (remainingMillis == 0L && endsAt > 0L) "Session Complete" else timeRemainingStr,
                        style = typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White
                    )

                    Spacer(Modifier.height(14.dp))

                    Surface(
                        onClick = {
                            if (remainingMillis == 0L && endsAt > 0L) {
                                onExitSession()
                            } else {
                                showVerifyKeyDialog = true
                            }
                        },
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (remainingMillis == 0L && endsAt > 0L) "Complete & Exit" else "Exit Focus Mode",
                                style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }

    if (showVerifyKeyDialog) {
        VerifyEmergencyKeyDialog(
            title = "Exit Focus Mode",
            subtitle = "Verify your security key to unlock and terminate the active kiosk session.",
            keyTypeOverride = mode.keyType,
            onVerifyCustom = { input ->
                val storedSecret = mode.keySecret
                if (storedSecret.isNullOrBlank()) {
                    true
                } else if (mode.keyType == KeyType.PASSWORD) {
                    storedSecret == input.trim()
                } else {
                    QrUtils.sha256(input.trim()).equals(storedSecret, ignoreCase = true)
                }
            },
            onDismiss = { showVerifyKeyDialog = false },
            onVerified = {
                showVerifyKeyDialog = false
                onExitSession()
            }
        )
    }
}

private fun loadLauncherApps(pm: PackageManager, currentPackageName: String): List<AppInfo> {
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    val resolveInfos = pm.queryIntentActivities(intent, 0)
    val seenPackages = mutableSetOf<String>()
    val result = mutableListOf<AppInfo>()

    for (info in resolveInfos) {
        val pkg = info.activityInfo.packageName
        if (pkg != currentPackageName && seenPackages.add(pkg)) {
            val label = info.loadLabel(pm).toString().ifBlank { pkg }
            val icon: Drawable? = try {
                info.loadIcon(pm)
            } catch (_: Exception) {
                null
            }
            result.add(
                AppInfo(
                    packageName = pkg,
                    label = label,
                    icon = icon,
                    isSystemApp = false
                )
            )
        }
    }
    return result.sortedBy { it.label.lowercase(Locale.getDefault()) }
}
