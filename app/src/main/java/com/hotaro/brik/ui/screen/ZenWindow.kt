package com.hotaro.brik.ui.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.hotaro.brik.ui.component.TomatoSectionHeader
import com.hotaro.brik.ui.component.TomatoSettingsItem
import com.hotaro.brik.ui.component.segmentedItemShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.hotaro.brik.R
import com.hotaro.brik.data.model.AppInfo
import com.hotaro.brik.ui.component.SetEmergencyKeyDialog
import com.hotaro.brik.ui.component.VerifyEmergencyKeyDialog
import com.hotaro.brik.util.ZenManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZenWindow(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val isZenActive by ZenManager.isZenActive.collectAsState()
    val isInfinite by ZenManager.isInfinite.collectAsState()
    val sessionEndTime by ZenManager.sessionEndTime.collectAsState()
    val allowedPackages by ZenManager.allowedPackages.collectAsState()
    val useKiosk by ZenManager.useKiosk.collectAsState()
    val hasKey by ZenManager.hasSecurityKey.collectAsState()
    val holdDurationSeconds by ZenManager.holdDurationSeconds.collectAsState()

    var showSetKeyDialog by remember { mutableStateOf(false) }
    var showVerifyKeyDialog by remember { mutableStateOf(false) }

    if (isZenActive) {
        ActiveZenDesktop(
            sessionEndTime = sessionEndTime,
            isInfinite = isInfinite,
            allowedPackages = allowedPackages,
            holdDurationSeconds = holdDurationSeconds,
            onExitRequested = {
                if (ZenManager.isSessionExpired()) {
                    ZenManager.endZenSession(context, activity)
                    Toast.makeText(context, "Zen session completed", Toast.LENGTH_SHORT).show()
                } else {
                    showVerifyKeyDialog = true
                }
            }
        )
    } else {
        InactiveZenConfigScreen(
            allowedPackages = allowedPackages,
            useKiosk = useKiosk,
            hasKey = hasKey,
            holdDurationSeconds = holdDurationSeconds,
            onBack = onBack,
            onSetKeyRequested = { showSetKeyDialog = true },
            onStartZen = { durationMinutes, infinite ->
                if (!ZenManager.hasKey(context)) {
                    showSetKeyDialog = true
                } else {
                    ZenManager.startZenSession(
                        context = context,
                        activity = activity,
                        isInfinite = infinite,
                        durationMinutes = durationMinutes,
                        allowedPackages = allowedPackages,
                        useKiosk = useKiosk
                    )
                    Toast.makeText(context, "Zen Mode activated", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showSetKeyDialog) {
        SetEmergencyKeyDialog(
            title = "Set Zen Security Key",
            subtitle = "Set a secure password or QR key. This key will be required whenever you wish to exit Zen Mode early.",
            onDismiss = { showSetKeyDialog = false },
            onSaveKey = { keyType, secret ->
                ZenManager.setSecurityKey(context, keyType, secret)
                Toast.makeText(context, "Security key configured", Toast.LENGTH_SHORT).show()
            },
            onKeySet = {
                showSetKeyDialog = false
            }
        )
    }

    if (showVerifyKeyDialog) {
        VerifyEmergencyKeyDialog(
            title = "Exit Zen Mode",
            subtitle = "Verify your security key to unlock and terminate the active Zen session.",
            keyTypeOverride = ZenManager.getKeyType(context),
            onVerifyCustom = { input ->
                ZenManager.verifySecurityKey(context, input)
            },
            onDismiss = { showVerifyKeyDialog = false },
            onVerified = {
                showVerifyKeyDialog = false
                ZenManager.endZenSession(context, activity)
                Toast.makeText(context, "Exited Zen Mode", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun InactiveZenConfigScreen(
    allowedPackages: Set<String>,
    useKiosk: Boolean,
    hasKey: Boolean,
    holdDurationSeconds: Int,
    onBack: () -> Unit,
    onSetKeyRequested: () -> Unit,
    onStartZen: (durationMinutes: Int, isInfinite: Boolean) -> Unit
) {
    val context = LocalContext.current
    val isDark = colorScheme.surface.luminance() < 0.5f
    val appBackground = if (isDark) Color.Black else colorScheme.surface

    var selectedDurationMinutes by remember { mutableIntStateOf(30) }
    var isInfiniteSelected by remember { mutableStateOf(false) }
    var showAppPickerSheet by remember { mutableStateOf(false) }

    var isDurationExpanded by remember { mutableStateOf(false) }
    val durationChevronRotation by animateFloatAsState(
        targetValue = if (isDurationExpanded) 90f else 0f,
        label = "durationChevron"
    )

    var isAllowedAppsExpanded by remember { mutableStateOf(false) }
    val appsChevronRotation by animateFloatAsState(
        targetValue = if (isAllowedAppsExpanded) 90f else 0f,
        label = "appsChevron"
    )

    var isHoldDurationExpanded by remember { mutableStateOf(false) }
    val holdChevronRotation by animateFloatAsState(
        targetValue = if (isHoldDurationExpanded) 90f else 0f,
        label = "holdChevron"
    )

    val pm = context.packageManager
    val launcherApps = remember(context) { loadLauncherApps(pm, context.packageName) }
    val selectedAppInfos = remember(allowedPackages, launcherApps) {
        launcherApps.filter { allowedPackages.contains(it.packageName) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Zen",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Minimalist digital detox and kiosk lockdown",
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
        },
        containerColor = appBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Absorb blank space clicks */ }
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .padding(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Card
                ElevatedCard(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = colorScheme.primaryContainer,
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.self_improvement_fill0),
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Focus Desktop",
                                style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Turn your phone into a distraction-free tool. Only permitted apps and a clock will remain accessible.",
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // --- SECTION 1: SESSION & TIMER ---
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    TomatoSectionHeader(title = "SESSION & TIMER")

                    // 1. Session Duration Item
                    TomatoSettingsItem(
                        title = "Session Duration",
                        subtitle = if (isInfiniteSelected) "Infinite lockdown • Key required" else "${selectedDurationMinutes} minutes • Tap to adjust",
                        leadingIcon = R.drawable.schedule_fill0,
                        iconContainerColor = colorScheme.primaryContainer,
                        iconTint = colorScheme.onPrimaryContainer,
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isInfiniteSelected) colorScheme.tertiaryContainer else colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = if (isInfiniteSelected) "Infinite" else "${selectedDurationMinutes}m",
                                        style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isInfiniteSelected) colorScheme.onTertiaryContainer else colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Icon(
                                    painter = painterResource(R.drawable.arrow_forward_big),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .graphicsLayer { rotationZ = durationChevronRotation },
                                    tint = colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        shape = if (isDurationExpanded) {
                            RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                        } else {
                            segmentedItemShape(0, 2)
                        },
                        onClick = { isDurationExpanded = !isDurationExpanded }
                    )

                    // Smooth expansion for Session Duration
                    AnimatedVisibility(
                        visible = isDurationExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            color = colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(colorScheme.surfaceContainerLowest)
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Infinite Lockdown",
                                            style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Session runs indefinitely without a timer; unlocked strictly with key",
                                            style = typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = isInfiniteSelected,
                                        onCheckedChange = { isInfiniteSelected = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = colorScheme.onPrimary,
                                            checkedTrackColor = colorScheme.primary
                                        )
                                    )
                                }

                                if (!isInfiniteSelected) {
                                    Spacer(Modifier.height(14.dp))
                                    Text(
                                        text = "Preset Durations",
                                        style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(8.dp))

                                    val presets = listOf(15, 30, 45, 60, 120, 240)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        presets.forEach { minutes ->
                                            val isSelected = selectedDurationMinutes == minutes
                                            val label = if (minutes >= 60) "${minutes / 60}h" else "${minutes}m"
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { selectedDurationMinutes = minutes },
                                                label = { Text(label, fontWeight = FontWeight.SemiBold) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = colorScheme.primaryContainer,
                                                    selectedLabelColor = colorScheme.onPrimaryContainer
                                                ),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = "Custom: ${selectedDurationMinutes} minutes",
                                        style = typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                    Slider(
                                        value = selectedDurationMinutes.toFloat(),
                                        onValueChange = { selectedDurationMinutes = it.toInt() },
                                        valueRange = 5f..480f,
                                        steps = 94
                                    )
                                }
                            }
                        }
                    }

                    // 2. Allowed Apps Item
                    TomatoSettingsItem(
                        title = "Allowed Apps",
                        subtitle = "${allowedPackages.size}/10 apps selected • Tap to expand",
                        leadingIcon = R.drawable.apps_fill0,
                        iconContainerColor = colorScheme.primaryContainer,
                        iconTint = colorScheme.onPrimaryContainer,
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${allowedPackages.size}/10",
                                        style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Icon(
                                    painter = painterResource(R.drawable.arrow_forward_big),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .graphicsLayer { rotationZ = appsChevronRotation },
                                    tint = colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        shape = if (isAllowedAppsExpanded) {
                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                        } else {
                            segmentedItemShape(1, 2)
                        },
                        onClick = { isAllowedAppsExpanded = !isAllowedAppsExpanded }
                    )

                    // Smooth expansion for Allowed Apps
                    AnimatedVisibility(
                        visible = isAllowedAppsExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            color = colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 22.dp, bottomEnd = 22.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Whitelisted Apps (${allowedPackages.size}/10)",
                                        style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onSurface
                                    )
                                    TextButton(onClick = { showAppPickerSheet = true }) {
                                        Text("Manage Apps", fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                if (selectedAppInfos.isEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = colorScheme.surfaceContainerLowest,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "No apps selected. Defaults to Phone & SMS.",
                                                style = typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant
                                            )
                                            Spacer(Modifier.height(6.dp))
                                            OutlinedButton(
                                                onClick = {
                                                    val defaults = ZenManager.getDefaultEssentialPackages(context)
                                                    ZenManager.setAllowedPackages(context, defaults)
                                                },
                                                shape = CircleShape
                                            ) {
                                                Text("Restore Phone & SMS")
                                            }
                                        }
                                    }
                                } else {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        selectedAppInfos.take(10).forEach { app ->
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = colorScheme.surfaceContainerLowest,
                                                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.3f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (app.icon != null) {
                                                        Image(
                                                            painter = rememberDrawablePainter(drawable = app.icon),
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .size(20.dp)
                                                                .clip(RoundedCornerShape(4.dp))
                                                        )
                                                        Spacer(Modifier.width(6.dp))
                                                    }
                                                    Text(
                                                        text = app.label,
                                                        style = typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                        color = colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- SECTION 2: LOCKDOWN & SECURITY ---
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    TomatoSectionHeader(title = "LOCKDOWN & SECURITY")

                    // 1. Kiosk Screen Pinning Item
                    TomatoSettingsItem(
                        title = "Kiosk Screen Pinning",
                        subtitle = "Pins screen & blocks system navigation until unlocked",
                        leadingIcon = R.drawable.screen_lock_portrait_fill0,
                        iconContainerColor = colorScheme.secondaryContainer,
                        iconTint = colorScheme.onSecondaryContainer,
                        trailingContent = {
                            Switch(
                                checked = useKiosk,
                                onCheckedChange = { ZenManager.setUseKiosk(context, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colorScheme.onPrimary,
                                    checkedTrackColor = colorScheme.primary
                                )
                            )
                        },
                        shape = segmentedItemShape(0, 2)
                    )

                    // 2. Security Key Protection Item
                    TomatoSettingsItem(
                        title = if (hasKey) "Security Key Active" else "Security Key Required",
                        subtitle = if (hasKey) "Guards early exit (${ZenManager.getKeyType(context).name})" else "Configure key to enable Zen Mode",
                        leadingIcon = if (hasKey) R.drawable.check_fill0 else R.drawable.shield_fill0,
                        iconContainerColor = if (hasKey) colorScheme.primaryContainer else colorScheme.errorContainer,
                        iconTint = if (hasKey) colorScheme.onPrimaryContainer else colorScheme.onErrorContainer,
                        trailingContent = {
                            Button(
                                onClick = onSetKeyRequested,
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                            ) {
                                Text(if (hasKey) "Change" else "Configure", style = typography.labelSmall)
                            }
                        },
                        shape = segmentedItemShape(1, 2),
                        onClick = onSetKeyRequested
                    )
                }

                // --- SECTION 3: PREFERENCES & EXTRAS ---
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    TomatoSectionHeader(title = "PREFERENCES & EXTRAS")

                    // Exit Button Hold Duration Item
                    TomatoSettingsItem(
                        title = "Exit Hold Duration",
                        subtitle = "${holdDurationSeconds}s continuous press • Tap to adjust",
                        leadingIcon = R.drawable.tune_fill0,
                        iconContainerColor = colorScheme.tertiaryContainer,
                        iconTint = colorScheme.onTertiaryContainer,
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${holdDurationSeconds}s",
                                        style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Icon(
                                    painter = painterResource(R.drawable.arrow_forward_big),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .graphicsLayer { rotationZ = holdChevronRotation },
                                    tint = colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        shape = if (isHoldDurationExpanded) {
                            RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                        } else {
                            segmentedItemShape(0, 1)
                        },
                        onClick = { isHoldDurationExpanded = !isHoldDurationExpanded }
                    )

                    // Smooth expansion for Hold Duration
                    AnimatedVisibility(
                        visible = isHoldDurationExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            color = colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 22.dp, bottomEnd = 22.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 14.dp)
                            ) {
                                Text(
                                    text = "Continuous press duration required to reveal remaining time & exit button on the Zen desktop. Hides after 2 seconds.",
                                    style = typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(12.dp))

                                val holdPresets = listOf(1, 2, 3, 4, 5)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    holdPresets.forEach { seconds ->
                                        val isSelected = holdDurationSeconds == seconds
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { ZenManager.setHoldDurationSeconds(context, seconds) },
                                            label = {
                                                Text(
                                                    text = "${seconds}s",
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = colorScheme.primaryContainer,
                                                selectedLabelColor = colorScheme.onPrimaryContainer
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating Pill Button: Lotus icon + "Enter Zen" in soft-edged rectangle at bottom right
            Surface(
                onClick = { onStartZen(selectedDurationMinutes, isInfiniteSelected) },
                shape = RoundedCornerShape(16.dp),
                color = colorScheme.primary,
                contentColor = colorScheme.onPrimary,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = 20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.self_improvement_fill0),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Enter Zen",
                        style = typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet for Managing Allowed Apps
    if (showAppPickerSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var searchQuery by remember { mutableStateOf("") }
        var tempAllowed by remember { mutableStateOf(allowedPackages) }

        val filteredApps = remember(launcherApps, searchQuery) {
            if (searchQuery.isBlank()) launcherApps else launcherApps.filter {
                it.label.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showAppPickerSheet = false },
            sheetState = sheetState,
            containerColor = colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Allowed Apps",
                            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "${tempAllowed.size}/10 apps selected",
                            style = typography.bodySmall,
                            color = if (tempAllowed.size >= 10) colorScheme.primary else colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            ZenManager.setAllowedPackages(context, tempAllowed.take(10).toSet())
                            showAppPickerSheet = false
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps...") },
                    leadingIcon = {
                        Icon(painter = painterResource(R.drawable.search_fill0), contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(painter = painterResource(R.drawable.close_fill0), contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val essentials = ZenManager.getDefaultEssentialPackages(context)
                            tempAllowed = essentials.take(10).toSet()
                        },
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Essentials (Phone/SMS)", style = typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = { tempAllowed = emptySet() },
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear All", style = typography.labelSmall)
                    }
                }

                Spacer(Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val isSelected = tempAllowed.contains(app.packageName)
                        Surface(
                            onClick = {
                                if (isSelected) {
                                    tempAllowed = tempAllowed - app.packageName
                                } else {
                                    if (tempAllowed.size >= 10) {
                                        Toast.makeText(context, "Maximum 10 apps allowed", Toast.LENGTH_SHORT).show()
                                    } else {
                                        tempAllowed = tempAllowed + app.packageName
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, if (isSelected) colorScheme.primary else colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (app.icon != null) {
                                    Image(
                                        painter = rememberDrawablePainter(drawable = app.icon),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.apps_fill0),
                                        contentDescription = null,
                                        tint = colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                Spacer(Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = typography.bodySmall,
                                        color = if (isSelected) colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        painter = painterResource(R.drawable.check_fill0),
                                        contentDescription = null,
                                        tint = colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveZenDesktop(
    sessionEndTime: Long,
    isInfinite: Boolean,
    allowedPackages: Set<String>,
    holdDurationSeconds: Int,
    onExitRequested: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // Intercept back key to prevent navigating back without unlock
    BackHandler(enabled = true) {
        Toast.makeText(context, "Zen Mode is active. Hold screen to reveal exit.", Toast.LENGTH_SHORT).show()
    }

    var currentTime by remember { mutableStateOf(Date()) }
    var remainingMillis by remember { mutableLongStateOf(0L) }
    var isControlsRevealed by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var hideJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(sessionEndTime, isInfinite) {
        while (true) {
            val now = System.currentTimeMillis()
            currentTime = Date(now)
            if (!isInfinite && sessionEndTime > 0L) {
                remainingMillis = (sessionEndTime - now).coerceAtLeast(0L)
            }
            delay(1000)
        }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }

    val launcherApps = remember(context) { loadLauncherApps(pm, context.packageName) }
    val permittedApps = remember(allowedPackages, launcherApps) {
        launcherApps.filter { allowedPackages.contains(it.packageName) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0D10))
            .pointerInput(holdDurationSeconds) {
                val holdMillis = (holdDurationSeconds.coerceAtLeast(1)) * 1000L
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Minimalist Clock & Revealed Status Pill
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Text(
                    text = timeFormat.format(currentTime),
                    fontSize = 76.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.SansSerif,
                    color = Color.White,
                    letterSpacing = (-2).sp
                )

                Text(
                    text = dateFormat.format(currentTime),
                    style = typography.titleMedium.copy(fontWeight = FontWeight.Normal),
                    color = Color(0xFF9E9E9E)
                )

                Spacer(Modifier.height(18.dp))

                androidx.compose.animation.AnimatedVisibility(
                    visible = isControlsRevealed,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isInfinite || remainingMillis > 0L) Color(0xFF1E2024) else colorScheme.primaryContainer,
                        border = BorderStroke(1.dp, Color(0xFF2A2C32))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (isInfinite) R.drawable.self_improvement_fill0
                                    else if (remainingMillis > 0L) R.drawable.schedule_fill0
                                    else R.drawable.check_fill0
                                ),
                                contentDescription = null,
                                tint = if (remainingMillis == 0L && !isInfinite) colorScheme.primary else Color(0xFFE0E0E0),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isInfinite) {
                                    "Infinite Focus Active"
                                } else if (remainingMillis > 0L) {
                                    val totalSeconds = remainingMillis / 1000
                                    val hours = totalSeconds / 3600
                                    val minutes = (totalSeconds % 3600) / 60
                                    val seconds = totalSeconds % 60
                                    if (hours > 0) {
                                        String.format(Locale.getDefault(), "%02d:%02d:%02d remaining", hours, minutes, seconds)
                                    } else {
                                        String.format(Locale.getDefault(), "%02d:%02d remaining", minutes, seconds)
                                    }
                                } else {
                                    "Session Finished"
                                },
                                style = typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = if (remainingMillis == 0L && !isInfinite) colorScheme.primary else Color(0xFFE0E0E0)
                            )
                        }
                    }
                }
            }

            // Center: Minimalist Text-Only Launcher (No icons)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                if (permittedApps.isEmpty()) {
                    Text(
                        text = "No apps whitelisted\nFocus on your environment",
                        style = typography.bodyMedium,
                        color = Color(0xFF757575),
                        textAlign = TextAlign.Center
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(0.85f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(permittedApps, key = { it.packageName }) { app ->
                            Text(
                                text = app.label,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp,
                                color = Color(0xFFE8E8EC),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                                        if (launchIntent != null) {
                                            context.startActivity(launchIntent)
                                        } else {
                                            Toast.makeText(context, "Cannot launch app", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(horizontal = 24.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            // Bottom: Revealed Exit Button & Hold Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                contentAlignment = Alignment.Center
            ) {
                if (holdProgress > 0f && !isControlsRevealed) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.35f)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF26282E)),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(holdProgress)
                                .fillMaxHeight()
                                .background(Color.White)
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = isControlsRevealed,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        onClick = {
                            hideJob?.cancel()
                            onExitRequested()
                        },
                        shape = CircleShape,
                        color = Color(0xFF1F2128),
                        border = BorderStroke(1.dp, Color(0xFF333640)),
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(48.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (!isInfinite && remainingMillis == 0L) R.drawable.check_fill0
                                    else R.drawable.key_fill0
                                ),
                                contentDescription = null,
                                tint = Color(0xFFCCCCCC),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (!isInfinite && remainingMillis == 0L) "Complete & Exit" else "Exit Zen Mode",
                                style = typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
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
