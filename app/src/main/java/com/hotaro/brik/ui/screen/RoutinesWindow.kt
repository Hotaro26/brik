package com.hotaro.brik.ui.screen

import android.app.Activity
import android.widget.Toast
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.hotaro.brik.R
import com.hotaro.brik.data.model.AppBag
import com.hotaro.brik.data.model.AppInfo
import com.hotaro.brik.data.model.AutomatedRoutine
import com.hotaro.brik.data.model.KeyType
import com.hotaro.brik.data.model.SavedKey
import com.hotaro.brik.data.model.TimeBlockType
import com.hotaro.brik.policy.DnsManager
import com.hotaro.brik.ui.component.QrScannerView
import com.hotaro.brik.ui.component.RoundedPullRefreshPage
import com.hotaro.brik.ui.component.TomatoSectionHeader
import com.hotaro.brik.ui.component.TomatoSettingsItem
import com.hotaro.brik.ui.component.segmentedItemShape
import com.hotaro.brik.ui.viewmodel.TimeBlockViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import com.hotaro.brik.util.WindDownManager
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoutinesWindow(
    onBack: () -> Unit,
    viewModel: TimeBlockViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val routines by viewModel.routines.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val bags by viewModel.bags.collectAsState()
    val savedKeys by viewModel.savedKeys.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()

    val isWindDownEnabled by WindDownManager.isEnabled.collectAsState()
    val windDownSleepH by WindDownManager.sleepHour.collectAsState()
    val windDownSleepM by WindDownManager.sleepMinute.collectAsState()
    val windDownWakeH by WindDownManager.wakeHour.collectAsState()
    val windDownWakeM by WindDownManager.wakeMinute.collectAsState()
    val isWindDownNoApps by WindDownManager.isNoAppsMode.collectAsState()
    val windDownAllowedApps by WindDownManager.allowedPackages.collectAsState()
    val isWindDownActive by WindDownManager.isWindDownActive.collectAsState()

    var isWindDownOpen by remember { mutableStateOf(false) }
    val animatedWindDownProgress = remember { Animatable(0f) }
    var isPredictiveWindDownActive by remember { mutableStateOf(false) }
    val isWindDownVisible = isWindDownOpen || isPredictiveWindDownActive || animatedWindDownProgress.value > 0f

    var isRefreshing by remember { mutableStateOf(false) }

    // Bottom Drawer Sheet state for minimal creation
    var showCreateSheet by remember { mutableStateOf(false) }
    var sheetRoutineName by remember { mutableStateOf("") }
    var sheetDaysOfWeek by remember { mutableStateOf(setOf(2, 3, 4, 5, 6)) } // Mon-Fri
    var sheetStartHour by remember { mutableStateOf(9) }
    var sheetStartMinute by remember { mutableStateOf(0) }
    var sheetEndHour by remember { mutableStateOf(17) }
    var sheetEndMinute by remember { mutableStateOf(0) }
    var showSheetStartTimePicker by remember { mutableStateOf(false) }
    var showSheetEndTimePicker by remember { mutableStateOf(false) }

    // Full Detail Configuration Window state with predictive gesture
    var configuringRoutine by remember { mutableStateOf<AutomatedRoutine?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    val animatedDetailProgress = remember { Animatable(0f) }
    var isPredictiveDetailActive by remember { mutableStateOf(false) }
    var backSwipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }
    val isDetailWindowVisible = configuringRoutine != null || isPredictiveDetailActive || animatedDetailProgress.value > 0f

    // Toggle / Delete verification dialog states
    var routineToToggle by remember { mutableStateOf<AutomatedRoutine?>(null) }
    var routineToDelete by remember { mutableStateOf<AutomatedRoutine?>(null) }
    var expandedRoutineId by remember { mutableStateOf<String?>(null) }

    // Predictive Back gesture handler for RoutineDetailWindow
    PredictiveBackHandler(enabled = configuringRoutine != null) { progress: Flow<BackEventCompat> ->
        isPredictiveDetailActive = true
        try {
            progress.collect { backEvent ->
                backSwipeEdge = backEvent.swipeEdge
                animatedDetailProgress.snapTo(backEvent.progress)
            }
            // Finished swipe: display save/discard confirmation dialog
            showDiscardDialog = true
        } catch (e: CancellationException) {
            // Cancelled swipe: spring back smoothly
            animatedDetailProgress.animateTo(
                targetValue = 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
        } finally {
            isPredictiveDetailActive = false
        }
    }

    val openDetailWindow: (AutomatedRoutine) -> Unit = { routine ->
        coroutineScope.launch {
            animatedDetailProgress.snapTo(1f)
            configuringRoutine = routine
            animatedDetailProgress.animateTo(0f, tween(durationMillis = 260))
        }
    }

    val closeDetailWindow: (Boolean) -> Unit = { shouldSave ->
        coroutineScope.launch {
            if (shouldSave && configuringRoutine != null) {
                val r = configuringRoutine!!
                if (r.name.isNotBlank() && !r.keySecret.isNullOrBlank()) {
                    viewModel.saveRoutine(r)
                    Toast.makeText(context, "Saved '${r.name}'", Toast.LENGTH_SHORT).show()
                }
            }
            showDiscardDialog = false
            animatedDetailProgress.animateTo(1f, tween(durationMillis = 180))
            configuringRoutine = null
            animatedDetailProgress.snapTo(0f)
        }
    }

    // Predictive Back gesture handler for WindDownWindow
    PredictiveBackHandler(enabled = isWindDownOpen && configuringRoutine == null) { progress: Flow<BackEventCompat> ->
        isPredictiveWindDownActive = true
        try {
            progress.collect { backEvent ->
                animatedWindDownProgress.snapTo(backEvent.progress)
            }
            animatedWindDownProgress.animateTo(1f, tween(durationMillis = 180))
            isWindDownOpen = false
        } catch (e: CancellationException) {
            animatedWindDownProgress.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
        } finally {
            isPredictiveWindDownActive = false
            animatedWindDownProgress.snapTo(0f)
        }
    }

    val openWindDownWindow = {
        coroutineScope.launch {
            animatedWindDownProgress.snapTo(1f)
            isWindDownOpen = true
            animatedWindDownProgress.animateTo(0f, tween(durationMillis = 260))
        }
    }

    val closeWindDownWindow = {
        coroutineScope.launch {
            animatedWindDownProgress.animateTo(1f, tween(durationMillis = 180))
            isWindDownOpen = false
            animatedWindDownProgress.snapTo(0f)
        }
    }

    val isDark = colorScheme.surface.luminance() < 0.5f
    val appBackground = if (isDark) Color.Black else colorScheme.surface

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(appBackground)
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val isAnySubWindowVisible = isDetailWindowVisible || isWindDownVisible
        val pSub = if (isDetailWindowVisible) animatedDetailProgress.value else animatedWindDownProgress.value

        // Bottom layer: Main Routines list (shifts and dims slightly when sub-window is open)
        val mainOffsetX = if (isAnySubWindowVisible) (-screenWidthPx / 4f) * (1f - pSub) else 0f
        val mainAlpha = if (isAnySubWindowVisible) 0.75f + (pSub * 0.25f) else 1f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = mainOffsetX
                    alpha = mainAlpha
                }
        ) {
            RoundedPullRefreshPage(
                isRefreshing = isRefreshing,
                onRefresh = {
                    coroutineScope.launch {
                        isRefreshing = true
                        viewModel.syncActiveBlocks()
                        delay(650)
                        isRefreshing = false
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            sheetRoutineName = ""
                            sheetDaysOfWeek = setOf(2, 3, 4, 5, 6)
                            sheetStartHour = 9
                            sheetStartMinute = 0
                            sheetEndHour = 17
                            sheetEndMinute = 0
                            showCreateSheet = true
                        },
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary,
                        shape = CircleShape
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.add_fill0),
                            contentDescription = "New Routine"
                        )
                    }
                },
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "Routines",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "Automated recurring schedules & bedtime blocks",
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
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Active Routine Banner
                    if (activeSession != null && activeSession!!.type == TimeBlockType.ROUTINE) {
                        val session = activeSession!!
                        item {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = colorScheme.tertiaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(10.dp)
                                    ) {}

                                    Spacer(Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "ROUTINE ACTIVE: ${session.title.uppercase()}",
                                            style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = colorScheme.onTertiaryContainer
                                        )
                                        Text(
                                            text = "${session.packagesSuspended.size} apps suspended • Key required to toggle",
                                            style = typography.bodySmall,
                                            color = colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Explanatory Hero Card
                    item {
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
                                            painter = painterResource(R.drawable.schedule_fill0),
                                            contentDescription = null,
                                            tint = colorScheme.primary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Automated Schedules",
                                        style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Automated routines enforce strict distraction-free time blocks across your days without manual intervention.",
                                        style = typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // SECTION 1: BEDTIME & SCHEDULED REST
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            TomatoSectionHeader(title = "BEDTIME & SCHEDULED REST")

                            TomatoSettingsItem(
                                title = "Wind Down Routine",
                                subtitle = if (isWindDownActive) {
                                    "Active now • Bedtime lockdown engaged"
                                } else if (isWindDownEnabled) {
                                    "Daily ${String.format(Locale.getDefault(), "%02d:%02d", windDownSleepH, windDownSleepM)} - ${String.format(Locale.getDefault(), "%02d:%02d", windDownWakeH, windDownWakeM)} • ${if (isWindDownNoApps) "Pure Rest" else "${windDownAllowedApps.size} apps"}"
                                } else {
                                    "Off • Bedtime minimal kiosk lockdown"
                                },
                                leadingIcon = R.drawable.dark_mode_fill0,
                                iconContainerColor = if (isWindDownActive) colorScheme.primaryContainer else colorScheme.secondaryContainer,
                                iconTint = if (isWindDownActive) colorScheme.primary else colorScheme.onSecondaryContainer,
                                trailingContent = {
                                    Icon(
                                        painter = painterResource(R.drawable.arrow_forward_big),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = colorScheme.onSurfaceVariant
                                    )
                                },
                                shape = segmentedItemShape(0, 1),
                                onClick = { openWindDownWindow() }
                            )
                        }
                    }

                    // SECTION 2: RECURRING ROUTINES
                    item {
                        TomatoSectionHeader(title = "RECURRING ROUTINES (${routines.size})")
                    }

                    if (routines.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.schedule_fill0),
                                        contentDescription = null,
                                        tint = colorScheme.outline,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = "No Routines Created",
                                        style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Tap '+' below to schedule recurring study, work, or bedtime blocks.",
                                        style = typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        items(routines, key = { it.id }) { routine ->
                            val index = routines.indexOf(routine)
                            val isCurrentlyActive = routine.isActiveNow || (activeSession?.id == routine.id)
                            val isExpanded = expandedRoutineId == routine.id
                            val chevronRotation by animateFloatAsState(
                                targetValue = if (isExpanded) 90f else 0f,
                                label = "routineChevron_${routine.id}"
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                TomatoSettingsItem(
                                    title = routine.name,
                                    subtitle = formatRoutineTime(routine.startHour, routine.startMinute, routine.endHour, routine.endMinute) + " • ${routine.daysOfWeek.size} days",
                                    leadingIcon = R.drawable.schedule_fill0,
                                    iconContainerColor = if (routine.isEnabled) colorScheme.primaryContainer else colorScheme.surfaceContainerHighest,
                                    iconTint = if (routine.isEnabled) colorScheme.primary else colorScheme.onSurfaceVariant,
                                    trailingContent = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (isCurrentlyActive) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = colorScheme.primary
                                                ) {
                                                    Text(
                                                        text = "ACTIVE",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colorScheme.onPrimary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Switch(
                                                checked = routine.isEnabled,
                                                onCheckedChange = { routineToToggle = routine }
                                            )

                                            Icon(
                                                painter = painterResource(R.drawable.arrow_forward_big),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .graphicsLayer { rotationZ = chevronRotation },
                                                tint = colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    shape = if (isExpanded) {
                                        RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                                    } else {
                                        segmentedItemShape(index, routines.size)
                                    },
                                    onClick = {
                                        expandedRoutineId = if (isExpanded) null else routine.id
                                    }
                                )

                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Surface(
                                        color = colorScheme.surfaceContainerHigh,
                                        shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp, topStart = 4.dp, topEnd = 4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 20.dp, vertical = 14.dp)
                                        ) {
                                            // Days of week indicator
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                val dayLetters = listOf("S", "M", "T", "W", "T", "F", "S")
                                                (1..7).forEach { dayNum ->
                                                    val isDayActive = routine.daysOfWeek.contains(dayNum)
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = if (isDayActive) colorScheme.primary else colorScheme.surfaceContainerLowest,
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = dayLetters[dayNum - 1],
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isDayActive) colorScheme.onPrimary else colorScheme.outline
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(Modifier.height(12.dp))

                                            // Target bags summary
                                            val targetBagsNames = bags.filter { routine.targetBagIds.contains(it.id) }.map { it.name }
                                            val targetsText = when {
                                                targetBagsNames.isNotEmpty() && routine.targetPackageNames.isNotEmpty() ->
                                                    "${targetBagsNames.joinToString()} + ${routine.targetPackageNames.size} apps"
                                                targetBagsNames.isNotEmpty() -> targetBagsNames.joinToString(", ")
                                                routine.targetPackageNames.isNotEmpty() -> "${routine.targetPackageNames.size} apps"
                                                else -> "No bags or apps assigned"
                                            }
                                            Text(
                                                text = "Blocks: $targetsText",
                                                style = typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant
                                            )

                                            if (routine.enableDnsBlocking) {
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    text = "DNS Filter: ${routine.dnsHost}",
                                                    style = typography.bodySmall,
                                                    color = colorScheme.secondary
                                                )
                                            }

                                            Spacer(Modifier.height(14.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Button(
                                                    onClick = { openDetailWindow(routine) },
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.tune_fill0),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Configure", fontWeight = FontWeight.Bold)
                                                }

                                                OutlinedButton(
                                                    onClick = { routineToDelete = routine },
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        contentColor = colorScheme.error
                                                    )
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.delete_fill0),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Delete")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Top layer: RoutineDetailWindow with predictive slide transition
        if (isDetailWindowVisible && configuringRoutine != null) {
            val p = animatedDetailProgress.value
            val detailOffsetX = screenWidthPx * p
            val detailScale = 1f - (p * 0.05f)
            val detailCorners = (p * 24).dp
            val detailAlpha = 1f - (p * 0.15f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = detailOffsetX
                        scaleX = detailScale
                        scaleY = detailScale
                        alpha = detailAlpha
                        clip = p > 0f
                        shape = RoundedCornerShape(detailCorners)
                        shadowElevation = if (p > 0f) 24.dp.toPx() else 0f
                    }
            ) {
                RoutineDetailWindow(
                    routine = configuringRoutine!!,
                    bags = bags,
                    savedKeys = savedKeys,
                    installedApps = installedApps,
                    onBack = { showDiscardDialog = true },
                    onSave = { updated ->
                        if (updated.name.isBlank()) {
                            Toast.makeText(context, "Please enter a routine name", Toast.LENGTH_SHORT).show()
                        } else if (updated.keySecret.isNullOrBlank()) {
                            Toast.makeText(context, "A protection key is required", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.saveRoutine(updated)
                            Toast.makeText(context, "Routine '${updated.name}' saved!", Toast.LENGTH_SHORT).show()
                            coroutineScope.launch {
                                animatedDetailProgress.animateTo(1f, tween(durationMillis = 180))
                                configuringRoutine = null
                                animatedDetailProgress.snapTo(0f)
                            }
                        }
                    }
                )
            }
        }

        // Top layer: WindDownWindow with predictive slide transition
        if (isWindDownVisible) {
            val p = animatedWindDownProgress.value
            val wdOffsetX = screenWidthPx * p
            val wdScale = 1f - (p * 0.05f)
            val wdCorners = (p * 24).dp
            val wdAlpha = 1f - (p * 0.15f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = wdOffsetX
                        scaleX = wdScale
                        scaleY = wdScale
                        alpha = wdAlpha
                        clip = p > 0f
                        shape = RoundedCornerShape(wdCorners)
                        shadowElevation = if (p > 0f) 24.dp.toPx() else 0f
                    }
            ) {
                WindDownWindow(
                    onBack = { closeWindDownWindow() },
                    installedApps = installedApps,
                    savedKeys = savedKeys
                )
            }
        }
    }

    // --- MINIMAL BOTTOM DRAWER SHEET FOR ROUTINE CREATION ---
    if (showCreateSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showCreateSheet = false },
            sheetState = sheetState,
            containerColor = colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New Routine",
                        style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface
                    )

                    Button(
                        onClick = {
                            val newRoutine = AutomatedRoutine(
                                id = UUID.randomUUID().toString(),
                                name = sheetRoutineName.trim(),
                                daysOfWeek = sheetDaysOfWeek.toList().sorted(),
                                startHour = sheetStartHour,
                                startMinute = sheetStartMinute,
                                endHour = sheetEndHour,
                                endMinute = sheetEndMinute,
                                keyType = KeyType.PASSWORD,
                                keySecret = ""
                            )
                            showCreateSheet = false
                            openDetailWindow(newRoutine)
                        },
                        enabled = sheetRoutineName.isNotBlank() && sheetDaysOfWeek.isNotEmpty(),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                    ) {
                        Text("Next", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(18.dp))

                OutlinedTextField(
                    value = sheetRoutineName,
                    onValueChange = { sheetRoutineName = it },
                    label = { Text("Routine Name *") },
                    placeholder = { Text("e.g., Work Hours, Sleep Routine") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                Text("Active Days of Week:", style = typography.labelMedium, color = colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    (1..7).forEach { dayNum ->
                        val isSelected = sheetDaysOfWeek.contains(dayNum)
                        Surface(
                            onClick = {
                                sheetDaysOfWeek = if (isSelected) sheetDaysOfWeek - dayNum else sheetDaysOfWeek + dayNum
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) colorScheme.primary else colorScheme.surfaceContainerLowest,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = dayLabels[dayNum - 1].take(2),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Time Schedule (24-hour):", style = typography.labelMedium, color = colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = String.format(Locale.US, "%02d:%02d", sheetStartHour, sheetStartMinute),
                        onValueChange = { input ->
                            val parts = input.split(":")
                            if (parts.size == 2) {
                                parts[0].toIntOrNull()?.let { if (it in 0..23) sheetStartHour = it }
                                parts[1].toIntOrNull()?.let { if (it in 0..59) sheetStartMinute = it }
                            }
                        },
                        label = { Text("Start (HH:MM)") },
                        trailingIcon = {
                            Surface(
                                shape = CircleShape,
                                color = colorScheme.primaryContainer,
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(34.dp),
                                onClick = { showSheetStartTimePicker = true }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.schedule_fill0),
                                        contentDescription = "Pick start time",
                                        tint = colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = String.format(Locale.US, "%02d:%02d", sheetEndHour, sheetEndMinute),
                        onValueChange = { input ->
                            val parts = input.split(":")
                            if (parts.size == 2) {
                                parts[0].toIntOrNull()?.let { if (it in 0..23) sheetEndHour = it }
                                parts[1].toIntOrNull()?.let { if (it in 0..59) sheetEndMinute = it }
                            }
                        },
                        label = { Text("End (HH:MM)") },
                        trailingIcon = {
                            Surface(
                                shape = CircleShape,
                                color = colorScheme.primaryContainer,
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(34.dp),
                                onClick = { showSheetEndTimePicker = true }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.schedule_fill0),
                                        contentDescription = "Pick end time",
                                        tint = colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (showSheetStartTimePicker) {
                    BrikTimePickerDialog(
                        title = "Select Start Time",
                        initialHour = sheetStartHour,
                        initialMinute = sheetStartMinute,
                        onConfirm = { h, m ->
                            sheetStartHour = h
                            sheetStartMinute = m
                        },
                        onDismiss = { showSheetStartTimePicker = false }
                    )
                }

                if (showSheetEndTimePicker) {
                    BrikTimePickerDialog(
                        title = "Select End Time",
                        initialHour = sheetEndHour,
                        initialMinute = sheetEndMinute,
                        onConfirm = { h, m ->
                            sheetEndHour = h
                            sheetEndMinute = m
                        },
                        onDismiss = { showSheetEndTimePicker = false }
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Summary: ${formatRoutineTime(sheetStartHour, sheetStartMinute, sheetEndHour, sheetEndMinute)}",
                    style = typography.bodySmall,
                    color = colorScheme.primary
                )
            }
        }
    }

    // --- Discard / Save Confirmation Dialog ---
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = {
                showDiscardDialog = false
                coroutineScope.launch {
                    animatedDetailProgress.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                }
            },
            title = { Text("Save or Discard?", fontWeight = FontWeight.Bold) },
            text = { Text("You have unsaved routine configuration. Do you want to save before exiting or discard?") },
            confirmButton = {
                Button(
                    onClick = { closeDetailWindow(true) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { closeDetailWindow(false) }) {
                        Text("Discard", color = colorScheme.error)
                    }
                    TextButton(onClick = {
                        showDiscardDialog = false
                        coroutineScope.launch {
                            animatedDetailProgress.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                        }
                    }) {
                        Text("Cancel")
                    }
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
        )
    }

    // --- Key Verification Dialog to Toggle Routine (Enable / Disable) ---
    if (routineToToggle != null) {
        val routine = routineToToggle!!
        var enteredKey by remember { mutableStateOf("") }
        var showScanner by remember { mutableStateOf(false) }

        if (showScanner) {
            AlertDialog(
                onDismissRequest = { showScanner = false },
                title = { Text("Scan Routine Key", fontWeight = FontWeight.Bold) },
                text = {
                    Box(modifier = Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(16.dp))) {
                        QrScannerView(
                            onQrDetected = { detected ->
                                showScanner = false
                                viewModel.toggleRoutine(routine.id, detected) { ok, err ->
                                    if (ok) {
                                        Toast.makeText(context, "Routine status updated!", Toast.LENGTH_SHORT).show()
                                        routineToToggle = null
                                    } else {
                                        Toast.makeText(context, err ?: "Incorrect key", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showScanner = false }) { Text("Cancel") }
                },
                shape = RoundedCornerShape(24.dp)
            )
        } else {
            AlertDialog(
                onDismissRequest = { routineToToggle = null },
                title = {
                    Text(
                        text = if (routine.isEnabled) "Disable '${routine.name}'?" else "Enable '${routine.name}'?",
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "All routines require their configured key to toggle on or off.",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(14.dp))

                        if (routine.keyType == KeyType.QR_CODE) {
                            Button(
                                onClick = { showScanner = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(painter = painterResource(R.drawable.qr_code_scanner_fill0), contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Scan Key QR Code")
                            }

                            Spacer(Modifier.height(10.dp))

                            OutlinedTextField(
                                value = enteredKey,
                                onValueChange = { enteredKey = it },
                                label = { Text("Or Paste QR Token") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            OutlinedTextField(
                                value = enteredKey,
                                onValueChange = { enteredKey = it },
                                label = { Text("Secret Password") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.toggleRoutine(routine.id, enteredKey) { ok, err ->
                                if (ok) {
                                    Toast.makeText(context, "Routine updated!", Toast.LENGTH_SHORT).show()
                                    routineToToggle = null
                                } else {
                                    Toast.makeText(context, err ?: "Incorrect key", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = enteredKey.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Verify & Continue")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { routineToToggle = null }) { Text("Cancel") }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = colorScheme.surfaceContainerHigh
            )
        }
    }

    // --- Delete Routine Dialog ---
    if (routineToDelete != null) {
        val routine = routineToDelete!!
        var enteredSecret by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { routineToDelete = null },
            title = {
                Text(
                    text = "Delete '${routine.name}'?",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.error
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Are you sure you want to delete this routine? Key verification is required.",
                        style = typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = enteredSecret,
                        onValueChange = { enteredSecret = it },
                        label = { Text("Routine Key") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRoutine(routine.id, enteredSecret) { ok, err ->
                            if (ok) {
                                Toast.makeText(context, "Deleted routine!", Toast.LENGTH_SHORT).show()
                                routineToDelete = null
                            } else {
                                Toast.makeText(context, err ?: "Incorrect key", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { routineToDelete = null }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrikTimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(timePickerState.hour, timePickerState.minute)
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirm", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = {
            Text(text = title, fontWeight = FontWeight.Bold)
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

/**
 * Dedicated Full Screen Window for Configuring a Routine (Advanced items: Password/Key, Target Bags, DNS)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RoutineDetailWindow(
    routine: AutomatedRoutine,
    bags: List<AppBag>,
    savedKeys: List<SavedKey>,
    installedApps: List<AppInfo>,
    onBack: () -> Unit,
    onSave: (AutomatedRoutine) -> Unit
) {
    val context = LocalContext.current
    var nameInput by remember { mutableStateOf(routine.name) }
    var selectedDays by remember { mutableStateOf(routine.daysOfWeek.toSet()) }
    var startHour by remember { mutableStateOf(routine.startHour) }
    var startMinute by remember { mutableStateOf(routine.startMinute) }
    var endHour by remember { mutableStateOf(routine.endHour) }
    var endMinute by remember { mutableStateOf(routine.endMinute) }
    var showDetailStartTimePicker by remember { mutableStateOf(false) }
    var showDetailEndTimePicker by remember { mutableStateOf(false) }

    var selectedBags by remember { mutableStateOf(routine.targetBagIds.toSet()) }
    var selectedPackages by remember { mutableStateOf(routine.targetPackageNames.toSet()) }
    var showBagsSheet by remember { mutableStateOf(false) }
    var showCustomAppsSheet by remember { mutableStateOf(false) }

    var enableDns by remember { mutableStateOf(routine.enableDnsBlocking) }
    var selectedDnsHost by remember { mutableStateOf(routine.dnsHost) }

    var selectedKeyType by remember { mutableStateOf(routine.keyType) }
    var passwordSecret by remember { mutableStateOf(if (routine.keyType == KeyType.PASSWORD) routine.keySecret ?: "" else "") }
    var qrSecret by remember { mutableStateOf(if (routine.keyType == KeyType.QR_CODE) routine.keySecret ?: "" else "") }
    var selectedSavedKeyId by remember { mutableStateOf(routine.savedKeyId) }
    var showSavedKeysSheet by remember { mutableStateOf(false) }
    var showScanKeySheet by remember { mutableStateOf(false) }

    val currentSecret = if (selectedKeyType == KeyType.PASSWORD) passwordSecret.trim() else qrSecret.trim()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = nameInput.ifBlank { "Configure Routine" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                actions = {
                    Button(
                        onClick = {
                            val updated = routine.copy(
                                name = nameInput.trim(),
                                daysOfWeek = selectedDays.toList().sorted(),
                                startHour = startHour,
                                startMinute = startMinute,
                                endHour = endHour,
                                endMinute = endMinute,
                                targetBagIds = selectedBags.toList(),
                                targetPackageNames = selectedPackages.toList(),
                                enableDnsBlocking = enableDns,
                                dnsHost = selectedDnsHost,
                                keyType = selectedKeyType,
                                keySecret = currentSecret,
                                savedKeyId = selectedSavedKeyId
                            )
                            onSave(updated)
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        containerColor = colorScheme.surface
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Schedule Info Card
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Schedule Details", style = typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface)
                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Routine Name *") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(14.dp))

                        Text("Active Days of Week:", style = typography.labelSmall, color = colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                            (1..7).forEach { dayNum ->
                                val isSelected = selectedDays.contains(dayNum)
                                Surface(
                                    onClick = {
                                        selectedDays = if (isSelected) selectedDays - dayNum else selectedDays + dayNum
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) colorScheme.primary else colorScheme.surfaceContainerLowest,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = dayLabels[dayNum - 1].take(2),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = String.format(Locale.US, "%02d:%02d", startHour, startMinute),
                                onValueChange = { input ->
                                    val parts = input.split(":")
                                    if (parts.size == 2) {
                                        parts[0].toIntOrNull()?.let { if (it in 0..23) startHour = it }
                                        parts[1].toIntOrNull()?.let { if (it in 0..59) startMinute = it }
                                    }
                                },
                                label = { Text("Start (HH:MM)") },
                                trailingIcon = {
                                    Surface(
                                        shape = CircleShape,
                                        color = colorScheme.primaryContainer,
                                        modifier = Modifier
                                            .padding(end = 4.dp)
                                            .size(34.dp),
                                        onClick = { showDetailStartTimePicker = true }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                painter = painterResource(R.drawable.schedule_fill0),
                                                contentDescription = "Pick start time",
                                                tint = colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = String.format(Locale.US, "%02d:%02d", endHour, endMinute),
                                onValueChange = { input ->
                                    val parts = input.split(":")
                                    if (parts.size == 2) {
                                        parts[0].toIntOrNull()?.let { if (it in 0..23) endHour = it }
                                        parts[1].toIntOrNull()?.let { if (it in 0..59) endMinute = it }
                                    }
                                },
                                label = { Text("End (HH:MM)") },
                                trailingIcon = {
                                    Surface(
                                        shape = CircleShape,
                                        color = colorScheme.primaryContainer,
                                        modifier = Modifier
                                            .padding(end = 4.dp)
                                            .size(34.dp),
                                        onClick = { showDetailEndTimePicker = true }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                painter = painterResource(R.drawable.schedule_fill0),
                                                contentDescription = "Pick end time",
                                                tint = colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (showDetailStartTimePicker) {
                            BrikTimePickerDialog(
                                title = "Select Start Time",
                                initialHour = startHour,
                                initialMinute = startMinute,
                                onConfirm = { h, m ->
                                    startHour = h
                                    startMinute = m
                                },
                                onDismiss = { showDetailStartTimePicker = false }
                            )
                        }

                        if (showDetailEndTimePicker) {
                            BrikTimePickerDialog(
                                title = "Select End Time",
                                initialHour = endHour,
                                initialMinute = endMinute,
                                onConfirm = { h, m ->
                                    endHour = h
                                    endMinute = m
                                },
                                onDismiss = { showDetailEndTimePicker = false }
                            )
                        }
                    }
                }
            }

            // Target to Block Card (Two pills: Bags and Custom)
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Targets to Block", style = typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface)
                        Text("Choose bags or individual apps to suspend during routine", style = typography.bodySmall, color = colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))

                        // Two Pills: Bags and Custom
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Bags Pill
                            Surface(
                                onClick = { showBagsSheet = true },
                                shape = CircleShape,
                                color = if (selectedBags.isNotEmpty()) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                                border = BorderStroke(1.dp, if (selectedBags.isNotEmpty()) colorScheme.primary else colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.backpack_fill0),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (selectedBags.isNotEmpty()) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (selectedBags.isNotEmpty()) "Bags (${selectedBags.size})" else "Bags",
                                        style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (selectedBags.isNotEmpty()) colorScheme.onPrimaryContainer else colorScheme.onSurface
                                    )
                                }
                            }

                            // Custom Pill
                            Surface(
                                onClick = { showCustomAppsSheet = true },
                                shape = CircleShape,
                                color = if (selectedPackages.isNotEmpty()) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                                border = BorderStroke(1.dp, if (selectedPackages.isNotEmpty()) colorScheme.primary else colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.apps_fill0),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (selectedPackages.isNotEmpty()) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (selectedPackages.isNotEmpty()) "Custom (${selectedPackages.size})" else "Custom",
                                        style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (selectedPackages.isNotEmpty()) colorScheme.onPrimaryContainer else colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Summary badges preview
                        if (selectedBags.isEmpty() && selectedPackages.isEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Text("No targets selected yet. Tap Bags or Custom to choose apps to block.", style = typography.bodySmall, color = colorScheme.outline)
                        } else {
                            Spacer(Modifier.height(12.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                bags.filter { selectedBags.contains(it.id) }.forEach { b ->
                                    InputChip(
                                        selected = true,
                                        onClick = { selectedBags = selectedBags - b.id },
                                        label = { Text(b.name) },
                                        leadingIcon = {
                                            Icon(painter = painterResource(R.drawable.backpack_fill0), contentDescription = null, modifier = Modifier.size(14.dp))
                                        },
                                        trailingIcon = {
                                            Icon(painter = painterResource(R.drawable.close_fill0), contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                        }
                                    )
                                }

                                installedApps.filter { selectedPackages.contains(it.packageName) }.take(6).forEach { app ->
                                    InputChip(
                                        selected = true,
                                        onClick = { selectedPackages = selectedPackages - app.packageName },
                                        label = { Text(app.label) },
                                        leadingIcon = {
                                            Icon(painter = painterResource(R.drawable.apps_fill0), contentDescription = null, modifier = Modifier.size(14.dp))
                                        },
                                        trailingIcon = {
                                            Icon(painter = painterResource(R.drawable.close_fill0), contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                        }
                                    )
                                }

                                val remainingCustom = selectedPackages.size - installedApps.filter { selectedPackages.contains(it.packageName) }.take(6).size
                                if (remainingCustom > 0) {
                                    SuggestionChip(
                                        onClick = { showCustomAppsSheet = true },
                                        label = { Text("+$remainingCustom more") }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Private DNS Blocking Card
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Private DNS Blocking", style = typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Text("Filter domains during routine schedule", style = typography.bodySmall, color = colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = enableDns, onCheckedChange = { enableDns = it })
                        }

                        if (enableDns) {
                            Spacer(Modifier.height(12.dp))
                            Text("Select DNS Resolver:", style = typography.labelSmall, color = colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            DnsManager.PRESETS.take(4).forEach { preset ->
                                val isSelected = selectedDnsHost == preset.host
                                Surface(
                                    onClick = { selectedDnsHost = preset.host },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                ) {
                                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(preset.name, style = typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                            Text(preset.host, style = typography.bodySmall.copy(fontSize = 11.sp), color = colorScheme.outline)
                                        }
                                        if (isSelected) {
                                            Icon(painter = painterResource(R.drawable.check_fill0), contentDescription = null, tint = colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Routine Protection Key Card
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Routine Protection Key *", style = typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = colorScheme.primary)
                        Text("Strict key required to toggle, modify, or delete this routine", style = typography.bodySmall, color = colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                onClick = { selectedKeyType = KeyType.PASSWORD },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedKeyType == KeyType.PASSWORD) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) { Text("Password", style = typography.labelMedium.copy(fontWeight = FontWeight.Bold)) }
                            }
                            Surface(
                                onClick = { selectedKeyType = KeyType.QR_CODE },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedKeyType == KeyType.QR_CODE) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) { Text("QR Key", style = typography.labelMedium.copy(fontWeight = FontWeight.Bold)) }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        if (selectedKeyType == KeyType.PASSWORD) {
                            OutlinedTextField(
                                value = passwordSecret,
                                onValueChange = { passwordSecret = it },
                                label = { Text("Protection Password *") },
                                placeholder = { Text("Enter secret password") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            // QR Key Section with Two Pills: Choose from Saved Keys or Scan New Key
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    onClick = { showSavedKeysSheet = true },
                                    shape = CircleShape,
                                    color = if (selectedSavedKeyId != null) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                                    border = BorderStroke(1.dp, if (selectedSavedKeyId != null) colorScheme.primary else colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.weight(1f).height(42.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(painter = painterResource(R.drawable.key_fill0), contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Saved Keys", style = typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                }

                                Surface(
                                    onClick = { showScanKeySheet = true },
                                    shape = CircleShape,
                                    color = if (selectedSavedKeyId == null && qrSecret.isNotBlank()) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                                    border = BorderStroke(1.dp, if (selectedSavedKeyId == null && qrSecret.isNotBlank()) colorScheme.primary else colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.weight(1f).height(42.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(painter = painterResource(R.drawable.qr_code_scanner_fill0), contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Scan New Key", style = typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // Selected Key Preview Status
                            if (qrSecret.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(painter = painterResource(R.drawable.qr_code_fill0), contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(24.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            val keyName = savedKeys.find { it.id == selectedSavedKeyId }?.name
                                            if (keyName != null) {
                                                Text("Saved Key: $keyName", style = typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface)
                                            } else {
                                                Text("Scanned QR Key", style = typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = colorScheme.onSurface)
                                            }
                                            Text(
                                                text = if (qrSecret.length > 14) "${qrSecret.take(6)}...${qrSecret.takeLast(6)}" else qrSecret,
                                                style = typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                                color = colorScheme.outline
                                            )
                                        }
                                        IconButton(onClick = {
                                            qrSecret = ""
                                            selectedSavedKeyId = null
                                        }) {
                                            Icon(painter = painterResource(R.drawable.close_fill0), contentDescription = "Clear Key", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "No key selected. Choose a saved key or scan a new QR code.",
                                    style = typography.bodySmall,
                                    color = colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- BOTTOM SHEET: SELECT TARGET BAGS ---
    if (showBagsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBagsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Target Bags", style = typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("Select bags to block during this routine", style = typography.bodySmall, color = colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { showBagsSheet = false },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (bags.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text("No bags found in Tools > Bags.", style = typography.bodyMedium, color = colorScheme.outline)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(bags, key = { it.id }) { bag ->
                            val isSelected = selectedBags.contains(bag.id)
                            Surface(
                                onClick = {
                                    selectedBags = if (isSelected) selectedBags - bag.id else selectedBags + bag.id
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                                border = BorderStroke(1.dp, if (isSelected) colorScheme.primary else colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.backpack_fill0),
                                        contentDescription = null,
                                        tint = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(bag.name, style = typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface)
                                        Text("${bag.packageNames.size} apps included", style = typography.bodySmall, color = colorScheme.onSurfaceVariant)
                                    }
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selectedBags = if (checked) selectedBags + bag.id else selectedBags - bag.id
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- BOTTOM SHEET: SELECT CUSTOM APPS ---
    if (showCustomAppsSheet) {
        var customSearch by remember { mutableStateOf("") }
        val filteredApps = remember(installedApps, customSearch) {
            if (customSearch.isBlank()) installedApps else installedApps.filter { it.label.contains(customSearch, ignoreCase = true) || it.packageName.contains(customSearch, ignoreCase = true) }
        }

        ModalBottomSheet(
            onDismissRequest = { showCustomAppsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Custom Apps", style = typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("${selectedPackages.size} apps selected to block", style = typography.bodySmall, color = colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { showCustomAppsSheet = false },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = customSearch,
                    onValueChange = { customSearch = it },
                    placeholder = { Text("Search installed apps...") },
                    leadingIcon = { Icon(painter = painterResource(R.drawable.search_fill0), contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = if (customSearch.isNotEmpty()) {
                        { IconButton(onClick = { customSearch = "" }) { Icon(painter = painterResource(R.drawable.close_fill0), contentDescription = "Clear", modifier = Modifier.size(18.dp)) } }
                    } else null,
                    singleLine = true,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val isSelected = selectedPackages.contains(app.packageName)
                        Surface(
                            onClick = {
                                selectedPackages = if (isSelected) selectedPackages - app.packageName else selectedPackages + app.packageName
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, if (isSelected) colorScheme.primary else colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (app.icon != null) {
                                    Image(
                                        painter = rememberDrawablePainter(drawable = app.icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
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
                                    Text(app.label, style = typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(app.packageName, style = typography.bodySmall.copy(fontSize = 10.sp), color = colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedPackages = if (checked) selectedPackages + app.packageName else selectedPackages - app.packageName
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- BOTTOM SHEET: CHOOSE FROM SAVED KEYS ---
    if (showSavedKeysSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSavedKeysSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Saved Keys", style = typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("Select a key to protect this routine", style = typography.bodySmall, color = colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { showSavedKeysSheet = false },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (savedKeys.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text("No saved keys found. You can scan or add keys in Keys & Analytics.", style = typography.bodyMedium, color = colorScheme.outline)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savedKeys, key = { it.id }) { sk ->
                            val isSelected = selectedSavedKeyId == sk.id || qrSecret == sk.token
                            Surface(
                                onClick = {
                                    selectedSavedKeyId = sk.id
                                    qrSecret = sk.token
                                    showSavedKeysSheet = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                                border = BorderStroke(1.dp, if (isSelected) colorScheme.primary else colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(painter = painterResource(R.drawable.qr_code_fill0), contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(sk.name, style = typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface)
                                        if (sk.description.isNotBlank()) {
                                            Text(sk.description, style = typography.bodySmall, color = colorScheme.onSurfaceVariant)
                                        }
                                        Text(
                                            text = if (sk.token.length > 16) "${sk.token.take(8)}...${sk.token.takeLast(6)}" else sk.token,
                                            style = typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                            color = colorScheme.outline
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(painter = painterResource(R.drawable.check_fill0), contentDescription = null, tint = colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- BOTTOM SHEET: SCAN NEW KEY ---
    if (showScanKeySheet) {
        ModalBottomSheet(
            onDismissRequest = { showScanKeySheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Scan Key QR Code", style = typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("Point camera at any QR code to use as key", style = typography.bodySmall, color = colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { showScanKeySheet = false }) {
                        Icon(painter = painterResource(R.drawable.close_fill0), contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    QrScannerView(
                        onQrDetected = { detected ->
                            qrSecret = detected
                            selectedSavedKeyId = null
                            showScanKeySheet = false
                            Toast.makeText(context, "Key scanned successfully!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

private fun formatRoutineTime(sh: Int, sm: Int, eh: Int, em: Int): String {
    fun fmt(h: Int, m: Int): String {
        val ampm = if (h >= 12) "PM" else "AM"
        val hour12 = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return String.format(Locale.US, "%02d:%02d %s", hour12, m, ampm)
    }
    return "${fmt(sh, sm)} - ${fmt(eh, em)}"
}
