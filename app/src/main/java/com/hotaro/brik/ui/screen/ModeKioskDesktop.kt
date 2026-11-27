package com.hotaro.brik.ui.screen

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.BatteryManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import com.hotaro.brik.ui.component.RealFillBatteryIcon
import com.hotaro.brik.ui.component.queryBatteryStatus
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
    val bm = remember { context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager }
    val initialBattery = remember { queryBatteryStatus(context, bm) }
    var batteryLevel by remember { mutableIntStateOf(initialBattery.first) }
    var isCharging by remember { mutableStateOf(initialBattery.second) }

    LaunchedEffect(endsAt) {
        while (true) {
            val now = System.currentTimeMillis()
            currentTime = Date(now)
            if (endsAt > 0L) {
                remainingMillis = (endsAt - now).coerceAtLeast(0L)
            }
            try {
                val (cap, chg) = queryBatteryStatus(context, bm)
                batteryLevel = cap
                isCharging = chg
            } catch (_: Exception) {}
            delay(1000)
        }
    }

    val use24Hour by com.hotaro.brik.ui.theme.ThemePreferences.use24HourClock.collectAsState()
    val timeDigitsFormat = remember(use24Hour) { SimpleDateFormat(if (use24Hour) "HH:mm" else "h:mm", Locale.getDefault()) }
    val amPmFormat = remember(use24Hour) { if (use24Hour) null else SimpleDateFormat("a", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }

    val assistiveEnabled by com.hotaro.brik.util.AssistivePreferences.enabled.collectAsState()
    val assistiveAutoHide by com.hotaro.brik.util.AssistivePreferences.autoHideSeconds.collectAsState()
    val exitHoldSec by com.hotaro.brik.util.KioskPreferences.exitHoldSeconds.collectAsState()
    val allowEmergencyDialer by com.hotaro.brik.util.KioskPreferences.allowEmergencyDialer.collectAsState()
    val showBatteryIndicator by com.hotaro.brik.util.KioskPreferences.showBatteryIndicator.collectAsState()
    val appNamesAlignment by com.hotaro.brik.util.KioskPreferences.appNamesAlignment.collectAsState()

    DisposableEffect(context) {
        val batteryFilter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                intent?.let {
                    when (it.action) {
                        Intent.ACTION_POWER_CONNECTED -> {
                            isCharging = true
                        }
                        Intent.ACTION_POWER_DISCONNECTED -> {
                            isCharging = false
                        }
                        Intent.ACTION_BATTERY_CHANGED -> {
                            val (level, chg) = queryBatteryStatus(context, bm)
                            batteryLevel = level
                            isCharging = chg
                        }
                    }
                }
            }
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(batteryReceiver, batteryFilter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(batteryReceiver, batteryFilter)
            }
        } catch (_: Throwable) {
            try {
                context.registerReceiver(batteryReceiver, batteryFilter)
            } catch (_: Throwable) {}
        }
        onDispose {
            try {
                context.unregisterReceiver(batteryReceiver)
            } catch (_: Exception) {}
        }
    }

    val launcherApps = remember(context) { loadLauncherApps(pm, context.packageName) }
    val permittedApps = remember(mode.kioskAllowedPackages, launcherApps) {
        launcherApps.filter { mode.kioskAllowedPackages.contains(it.packageName) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0D10))
            .pointerInput(exitHoldSec) {
                val holdMillis = (exitHoldSec * 1000L).coerceAtLeast(1000L)
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)
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
                        if (event.changes.any { it.isConsumed }) {
                            job.cancel()
                            holdProgress = 0f
                            break
                        }
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
            Spacer(Modifier.height(24.dp))

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

            Spacer(Modifier.height(18.dp))

            // Clock & Date
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = timeDigitsFormat.format(currentTime),
                    style = typography.displayLarge.copy(
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-2).sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = Color.White
                )
                if (!use24Hour) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = amPmFormat?.format(currentTime)?.uppercase() ?: "",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = dateFormat.format(currentTime),
                style = typography.titleSmall.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                ),
                color = Color.White.copy(alpha = 0.5f)
            )

            if (showBatteryIndicator) {
                Spacer(Modifier.height(8.dp))
                val batteryColor = when {
                    isCharging -> colorScheme.primary
                    batteryLevel <= 15 -> Color(0xFFFF5252)
                    batteryLevel <= 20 -> Color(0xFFFFAB40)
                    else -> Color.White.copy(alpha = 0.85f)
                }
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RealFillBatteryIcon(
                        batteryLevel = batteryLevel,
                        isCharging = isCharging,
                        tint = batteryColor
                    )
                    Text(
                        text = "$batteryLevel%${if (isCharging) " • Charging" else ""}",
                        style = typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = batteryColor
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

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
                val (columnAlignment, textAlignment) = remember(appNamesAlignment) {
                    when (appNamesAlignment) {
                        "left" -> Alignment.Start to TextAlign.Start
                        "right" -> Alignment.End to TextAlign.End
                        else -> Alignment.CenterHorizontally to TextAlign.Center
                    }
                }

                var isAppListExpanded by remember { mutableStateOf(false) }
                val visibleApps = remember(permittedApps, isAppListExpanded) {
                    if (isAppListExpanded || permittedApps.size <= 5) permittedApps else permittedApps.take(5)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ),
                    horizontalAlignment = columnAlignment,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
                ) {
                    visibleApps.forEach { app ->
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
                                .padding(vertical = 6.dp),
                            textAlign = textAlignment
                        )
                    }

                    if (permittedApps.size > 5) {
                        val moreCount = permittedApps.size - 5
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { isAppListExpanded = !isAppListExpanded }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isAppListExpanded) "less" else "more ($moreCount)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Icon(
                                painter = painterResource(R.drawable.arrow_forward_big),
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(12.dp)
                                    .graphicsLayer { rotationZ = if (isAppListExpanded) -90f else 90f }
                            )
                        }
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
            enter = fadeIn(animationSpec = tween(350)),
            exit = fadeOut(animationSpec = tween(250)),
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
                            isControlsRevealed = false
                            hideJob?.cancel()
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

        if (mode.kioskEnableFloatingControls && assistiveEnabled) {
            com.hotaro.brik.ui.component.FloatingQuickControls(
                autoHideDelaySeconds = assistiveAutoHide,
                modifier = Modifier.fillMaxSize()
            )
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
