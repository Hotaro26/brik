package com.hotaro.brik.ui.screen

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
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.hotaro.brik.data.model.FocusMode
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ModesWindow(
    onBack: () -> Unit,
    viewModel: TimeBlockViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusModes by viewModel.focusModes.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val bags by viewModel.bags.collectAsState()
    val savedKeys by viewModel.savedKeys.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()

    var isRefreshing by remember { mutableStateOf(false) }

    // Bottom Drawer Sheet state for minimal creation (Name + Profile Icon)
    var showCreateSheet by remember { mutableStateOf(false) }
    var sheetModeName by remember { mutableStateOf("") }
    var sheetSelectedIcon by remember { mutableStateOf("mode_fill0") }

    // Full Detail Configuration Window state with predictive back
    var configuringMode by remember { mutableStateOf<FocusMode?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    val animatedDetailProgress = remember { Animatable(0f) }
    var isPredictiveDetailActive by remember { mutableStateOf(false) }
    var backSwipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }
    val isDetailWindowVisible = configuringMode != null || isPredictiveDetailActive || animatedDetailProgress.value > 0f

    // Session control dialogs
    var modeToStart by remember { mutableStateOf<FocusMode?>(null) }
    var modeToStop by remember { mutableStateOf<FocusMode?>(null) }
    var modeToDelete by remember { mutableStateOf<FocusMode?>(null) }
    var expandedModeId by remember { mutableStateOf<String?>(null) }

    // Live countdown timer state
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(activeSession) {
        while (activeSession != null) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    // Predictive Back gesture handler for ModeDetailWindow
    PredictiveBackHandler(enabled = configuringMode != null) { progress: Flow<BackEventCompat> ->
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

    val openDetailWindow: (FocusMode) -> Unit = { mode ->
        coroutineScope.launch {
            animatedDetailProgress.snapTo(1f)
            configuringMode = mode
            animatedDetailProgress.animateTo(0f, tween(durationMillis = 260))
        }
    }

    val closeDetailWindow: (Boolean) -> Unit = { shouldSave ->
        coroutineScope.launch {
            if (shouldSave && configuringMode != null) {
                val m = configuringMode!!
                if (m.name.isNotBlank() && !m.keySecret.isNullOrBlank()) {
                    viewModel.saveFocusMode(m)
                    Toast.makeText(context, "Saved '${m.name}'", Toast.LENGTH_SHORT).show()
                }
            }
            showDiscardDialog = false
            animatedDetailProgress.animateTo(1f, tween(durationMillis = 180))
            configuringMode = null
            animatedDetailProgress.snapTo(0f)
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
        val p = animatedDetailProgress.value

        // Bottom layer: Main Focus Modes list
        val mainOffsetX = if (isDetailWindowVisible) (-screenWidthPx / 4f) * (1f - p) else 0f
        val mainAlpha = if (isDetailWindowVisible) 0.75f + (p * 0.25f) else 1f

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
                            sheetModeName = ""
                            sheetSelectedIcon = "mode_fill0"
                            showCreateSheet = true
                        },
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary,
                        shape = CircleShape
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.add_fill0),
                            contentDescription = "New Focus Mode"
                        )
                    }
                },
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "Focus Modes",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "On-demand timed app suspension & DNS blocking",
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
                    // Active Mode Banner
                    if (activeSession != null && activeSession!!.type == TimeBlockType.FOCUS_MODE) {
                        val session = activeSession!!
                        val remainingMs = (session.endsAt ?: 0L) - currentTime
                        val remainingFormatted = if (remainingMs > 0) {
                            val mins = (remainingMs / 1000) / 60
                            val secs = (remainingMs / 1000) % 60
                            String.format(Locale.US, "%02d:%02d remaining", mins, secs)
                        } else {
                            "Ending session..."
                        }

                        item {
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = CircleShape,
                                                color = colorScheme.primary,
                                                modifier = Modifier.size(12.dp)
                                            ) {}
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                text = "ACTIVE SESSION: ${session.title.uppercase()}",
                                                style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = colorScheme.onPrimaryContainer
                                            )
                                        }

                                        Text(
                                            text = remainingFormatted,
                                            style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = colorScheme.primary
                                        )
                                    }

                                    Spacer(Modifier.height(10.dp))

                                    Text(
                                        text = "${session.packagesSuspended.size} apps suspended • DNS ${if (session.dnsBlocked) "filtered" else "normal"}",
                                        style = typography.bodySmall,
                                        color = colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )

                                    Spacer(Modifier.height(12.dp))

                                    Button(
                                        onClick = {
                                            val running = focusModes.find { it.id == session.id }
                                            if (running != null) {
                                                modeToStop = running
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(painter = painterResource(R.drawable.key_fill0), contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Unlock & Stop Session Early", fontWeight = FontWeight.Bold)
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
                                            painter = painterResource(R.drawable.mode_fill0),
                                            contentDescription = null,
                                            tint = colorScheme.primary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "On-Demand Focus Profiles",
                                        style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Start customized focus profiles for deep work, study, or detox. Early termination strictly requires your key.",
                                        style = typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // SECTION 1: FOCUS PROFILES
                    item {
                        TomatoSectionHeader(title = "FOCUS PROFILES (${focusModes.size})")
                    }

                    if (focusModes.isEmpty()) {
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
                                        painter = painterResource(R.drawable.mode_fill0),
                                        contentDescription = null,
                                        tint = colorScheme.outline,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = "No Focus Modes Created",
                                        style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Tap '+' below to create customized focus profiles.",
                                        style = typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        items(focusModes, key = { it.id }) { mode ->
                            val index = focusModes.indexOf(mode)
                            val isRunning = mode.isRunning || (activeSession?.id == mode.id)
                            val isExpanded = expandedModeId == mode.id
                            val chevronRotation by animateFloatAsState(
                                targetValue = if (isExpanded) 90f else 0f,
                                label = "modeChevron_${mode.id}"
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                TomatoSettingsItem(
                                    title = mode.name,
                                    subtitle = "${mode.defaultDurationMinutes} min session • ${mode.targetBagIds.size} bags • DNS ${if (mode.enableDnsBlocking) "filtered" else "normal"}",
                                    leadingIcon = getModeIcon(mode.iconName),
                                    iconContainerColor = if (isRunning) colorScheme.primaryContainer else colorScheme.surfaceContainerHighest,
                                    iconTint = if (isRunning) colorScheme.primary else colorScheme.onSurfaceVariant,
                                    trailingContent = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (isRunning) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = colorScheme.primary
                                                ) {
                                                    Text(
                                                        text = "RUNNING",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colorScheme.onPrimary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }

                                                Button(
                                                    onClick = { modeToStop = mode },
                                                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text("Unlock", fontSize = 12.sp)
                                                }
                                            } else {
                                                Button(
                                                    onClick = { modeToStart = mode },
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text("Start", fontSize = 12.sp)
                                                }
                                            }

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
                                        segmentedItemShape(index, focusModes.size)
                                    },
                                    onClick = {
                                        expandedModeId = if (isExpanded) null else mode.id
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
                                            // Target bags summary
                                            val targetBagsNames = bags.filter { mode.targetBagIds.contains(it.id) }.map { it.name }
                                            val targetsText = when {
                                                targetBagsNames.isNotEmpty() && mode.targetPackageNames.isNotEmpty() ->
                                                    "${targetBagsNames.joinToString()} + ${mode.targetPackageNames.size} apps"
                                                targetBagsNames.isNotEmpty() -> targetBagsNames.joinToString(", ")
                                                mode.targetPackageNames.isNotEmpty() -> "${mode.targetPackageNames.size} apps"
                                                else -> "No bags or apps assigned"
                                            }
                                            Text(
                                                text = "Blocks: $targetsText",
                                                style = typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant
                                            )

                                            if (mode.enableDnsBlocking) {
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    text = "DNS Filter: ${mode.dnsHost}",
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
                                                    onClick = { openDetailWindow(mode) },
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
                                                    onClick = { modeToDelete = mode },
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
                                Spacer(Modifier.height(2.dp))
                            }
                        }
                    }
                }
            }
        }

        // Top layer: ModeDetailWindow with predictive slide transition
        if (isDetailWindowVisible && configuringMode != null) {
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
                ModeDetailWindow(
                    mode = configuringMode!!,
                    bags = bags,
                    savedKeys = savedKeys,
                    installedApps = installedApps,
                    onBack = { showDiscardDialog = true },
                    onSave = { updated ->
                        if (updated.name.isBlank()) {
                            Toast.makeText(context, "Please enter a profile name", Toast.LENGTH_SHORT).show()
                        } else if (updated.keySecret.isNullOrBlank()) {
                            Toast.makeText(context, "A protection key is required", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.saveFocusMode(updated)
                            Toast.makeText(context, "Focus Mode '${updated.name}' saved!", Toast.LENGTH_SHORT).show()
                            coroutineScope.launch {
                                animatedDetailProgress.animateTo(1f, tween(durationMillis = 180))
                                configuringMode = null
                                animatedDetailProgress.snapTo(0f)
                            }
                        }
                    }
                )
            }
        }
    }

    // --- MINIMAL BOTTOM DRAWER SHEET FOR FOCUS MODE CREATION ---
    if (showCreateSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val icons = listOf(
            "mode_fill0" to R.drawable.mode_fill0,
            "schedule_fill0" to R.drawable.schedule_fill0,
            "dark_mode_fill0" to R.drawable.dark_mode_fill0,
            "backpack_fill0" to R.drawable.backpack_fill0,
            "shield_fill0" to R.drawable.shield_fill0,
            "palette_fill0" to R.drawable.palette_fill0
        )

        ModalBottomSheet(
            onDismissRequest = { showCreateSheet = false },
            sheetState = sheetState,
            containerColor = colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New Focus Mode",
                        style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface
                    )

                    Button(
                        onClick = {
                            val newMode = FocusMode(
                                id = UUID.randomUUID().toString(),
                                name = sheetModeName.trim(),
                                iconName = sheetSelectedIcon,
                                defaultDurationMinutes = 45,
                                keyType = KeyType.PASSWORD,
                                keySecret = ""
                            )
                            showCreateSheet = false
                            openDetailWindow(newMode)
                        },
                        enabled = sheetModeName.isNotBlank(),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                    ) {
                        Text("Next", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(18.dp))

                OutlinedTextField(
                    value = sheetModeName,
                    onValueChange = { sheetModeName = it },
                    label = { Text("Profile Name *") },
                    placeholder = { Text("e.g., Deep Focus, Reading, Gaming Ban") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(18.dp))

                Text("Profile Icon:", style = typography.labelMedium, color = colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    icons.forEach { (name, res) ->
                        val isSelected = sheetSelectedIcon == name
                        Surface(
                            onClick = { sheetSelectedIcon = name },
                            shape = CircleShape,
                            color = if (isSelected) colorScheme.primary else colorScheme.surfaceContainerLowest,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(res),
                                    contentDescription = null,
                                    tint = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
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
            text = { Text("You have unsaved changes. Do you want to save this focus mode before exiting or discard?") },
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

    // --- Start Focus Mode Dialog ---
    if (modeToStart != null) {
        val mode = modeToStart!!
        var durationInput by remember { mutableIntStateOf(mode.defaultDurationMinutes) }

        AlertDialog(
            onDismissRequest = { modeToStart = null },
            title = {
                Text(
                    text = "Start '${mode.name}'",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Once started, early unlocking will require verification of your assigned key.",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(14.dp))

                    Text("Duration:", style = typography.labelMedium, color = colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 30, 45, 60, 90).forEach { mins ->
                            val isSelected = durationInput == mins
                            Surface(
                                onClick = { durationInput = mins },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${mins}m",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    val targetBagsNames = bags.filter { mode.targetBagIds.contains(it.id) }.map { it.name }
                    val targetsText = when {
                        targetBagsNames.isNotEmpty() && mode.targetPackageNames.isNotEmpty() ->
                            "${targetBagsNames.joinToString()} + ${mode.targetPackageNames.size} apps"
                        targetBagsNames.isNotEmpty() -> targetBagsNames.joinToString(", ")
                        mode.targetPackageNames.isNotEmpty() -> "${mode.targetPackageNames.size} apps"
                        else -> "None"
                    }
                    Text("Targets: $targetsText", style = typography.bodySmall, color = colorScheme.onSurfaceVariant)
                    if (mode.enableDnsBlocking) {
                        Text("DNS Filter: ${mode.dnsHost}", style = typography.bodySmall, color = colorScheme.secondary)
                    }
                    Text("Key Type: ${mode.keyType.name}", style = typography.bodySmall, color = colorScheme.outline)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.startFocusMode(mode.id, durationInput) { success, err ->
                            if (success) {
                                Toast.makeText(context, "Focus Mode '${mode.name}' started!", Toast.LENGTH_SHORT).show()
                                modeToStart = null
                            } else {
                                Toast.makeText(context, err ?: "Failed to start", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Start Focus")
                }
            },
            dismissButton = {
                TextButton(onClick = { modeToStart = null }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
        )
    }

    // --- Stop / Unlock Focus Mode Dialog ---
    if (modeToStop != null) {
        val mode = modeToStop!!
        var enteredSecret by remember { mutableStateOf("") }
        var showScanner by remember { mutableStateOf(false) }

        if (showScanner) {
            AlertDialog(
                onDismissRequest = { showScanner = false },
                title = { Text("Scan Unlock Key", fontWeight = FontWeight.Bold) },
                text = {
                    Box(modifier = Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(16.dp))) {
                        QrScannerView(
                            onQrDetected = { detected ->
                                showScanner = false
                                viewModel.stopFocusMode(mode.id, detected) { success, err ->
                                    if (success) {
                                        Toast.makeText(context, "'${mode.name}' unlocked!", Toast.LENGTH_SHORT).show()
                                        modeToStop = null
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
                onDismissRequest = { modeToStop = null },
                title = {
                    Text(
                        text = "Unlock '${mode.name}'?",
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.error
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "To terminate or stop this active focus session early, enter or scan the assigned key.",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(14.dp))

                        if (mode.keyType == KeyType.QR_CODE) {
                            Button(
                                onClick = { showScanner = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(painter = painterResource(R.drawable.qr_code_scanner_fill0), contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Scan QR Code Key")
                            }

                            Spacer(Modifier.height(10.dp))

                            OutlinedTextField(
                                value = enteredSecret,
                                onValueChange = { enteredSecret = it },
                                label = { Text("Or Paste QR Token") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            OutlinedTextField(
                                value = enteredSecret,
                                onValueChange = { enteredSecret = it },
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
                            viewModel.stopFocusMode(mode.id, enteredSecret) { success, err ->
                                if (success) {
                                    Toast.makeText(context, "'${mode.name}' unlocked!", Toast.LENGTH_SHORT).show()
                                    modeToStop = null
                                } else {
                                    Toast.makeText(context, err ?: "Incorrect key", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = enteredSecret.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Verify & Unlock")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { modeToStop = null }) { Text("Cancel") }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = colorScheme.surfaceContainerHigh
            )
        }
    }

    // --- Delete Focus Mode Dialog ---
    if (modeToDelete != null) {
        val mode = modeToDelete!!
        var enteredSecret by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { modeToDelete = null },
            title = {
                Text(
                    text = "Delete '${mode.name}'?",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.error
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (mode.isRunning) "This mode is currently running. Key is required to abort and delete." else "Are you sure you want to delete this focus profile?",
                        style = typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )

                    if (mode.isRunning) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = enteredSecret,
                            onValueChange = { enteredSecret = it },
                            label = { Text("Mode Key") },
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
                        viewModel.deleteFocusMode(mode.id, enteredSecret) { ok, err ->
                            if (ok) {
                                Toast.makeText(context, "Deleted '${mode.name}'", Toast.LENGTH_SHORT).show()
                                modeToDelete = null
                            } else {
                                Toast.makeText(context, err ?: "Failed to delete", Toast.LENGTH_SHORT).show()
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
                TextButton(onClick = { modeToDelete = null }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
        )
    }
}

/**
 * Dedicated Full Screen Window for Configuring a Focus Mode (Extra configuration: Duration, Target Bags, DNS, Key)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ModeDetailWindow(
    mode: FocusMode,
    bags: List<AppBag>,
    savedKeys: List<SavedKey>,
    installedApps: List<AppInfo>,
    onBack: () -> Unit,
    onSave: (FocusMode) -> Unit
) {
    val context = LocalContext.current
    var nameInput by remember { mutableStateOf(mode.name) }
    var selectedIcon by remember { mutableStateOf(mode.iconName) }
    var durationMinutes by remember { mutableStateOf(mode.defaultDurationMinutes) }
    var selectedBags by remember { mutableStateOf(mode.targetBagIds.toSet()) }
    var selectedPackages by remember { mutableStateOf(mode.targetPackageNames.toSet()) }
    var showBagsSheet by remember { mutableStateOf(false) }
    var showCustomAppsSheet by remember { mutableStateOf(false) }

    var enableDns by remember { mutableStateOf(mode.enableDnsBlocking) }
    var selectedDnsHost by remember { mutableStateOf(mode.dnsHost) }

    var selectedKeyType by remember { mutableStateOf(mode.keyType) }
    var passwordSecret by remember { mutableStateOf(if (mode.keyType == KeyType.PASSWORD) mode.keySecret ?: "" else "") }
    var qrSecret by remember { mutableStateOf(if (mode.keyType == KeyType.QR_CODE) mode.keySecret ?: "" else "") }
    var selectedSavedKeyId by remember { mutableStateOf(mode.savedKeyId) }
    var showSavedKeysSheet by remember { mutableStateOf(false) }
    var showScanKeySheet by remember { mutableStateOf(false) }

    val currentSecret = if (selectedKeyType == KeyType.PASSWORD) passwordSecret.trim() else qrSecret.trim()

    var isProfileExpanded by remember { mutableStateOf(false) }
    var isDurationExpanded by remember { mutableStateOf(false) }
    var isTargetsExpanded by remember { mutableStateOf(false) }
    var isKeyExpanded by remember { mutableStateOf(false) }

    var isKioskMode by remember { mutableStateOf(mode.enableKioskMode) }
    var kioskPackages by remember { mutableStateOf(mode.kioskAllowedPackages.toSet()) }
    var disableSystemGestures by remember { mutableStateOf(mode.kioskDisableSystemGestures) }
    var enableFloatingControls by remember { mutableStateOf(mode.kioskEnableFloatingControls) }
    var floatingAutoHideSeconds by remember { mutableIntStateOf(mode.kioskFloatingAutoHideSeconds) }
    var showKioskAppPickerSheet by remember { mutableStateOf(false) }

    var isKioskAppsExpanded by remember { mutableStateOf(false) }
    var isKioskPolicyExpanded by remember { mutableStateOf(false) }

    val profileChevronRotation by animateFloatAsState(if (isProfileExpanded) 90f else 0f, label = "profileChevron")
    val durationChevronRotation by animateFloatAsState(if (isDurationExpanded) 90f else 0f, label = "durationChevron")
    val targetsChevronRotation by animateFloatAsState(if (isTargetsExpanded) 90f else 0f, label = "targetsChevron")
    val kioskAppsChevronRotation by animateFloatAsState(if (isKioskAppsExpanded) 90f else 0f, label = "kioskAppsChevron")
    val kioskPolicyChevronRotation by animateFloatAsState(if (isKioskPolicyExpanded) 90f else 0f, label = "kioskPolicyChevron")
    val keyChevronRotation by animateFloatAsState(if (isKeyExpanded) 90f else 0f, label = "keyChevron")

    val icons = listOf(
        "mode_fill0" to R.drawable.mode_fill0,
        "schedule_fill0" to R.drawable.schedule_fill0,
        "dark_mode_fill0" to R.drawable.dark_mode_fill0,
        "backpack_fill0" to R.drawable.backpack_fill0,
        "shield_fill0" to R.drawable.shield_fill0,
        "palette_fill0" to R.drawable.palette_fill0
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = nameInput.ifBlank { "Configure Mode" },
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
                            val updated = mode.copy(
                                name = nameInput.trim(),
                                iconName = selectedIcon,
                                defaultDurationMinutes = durationMinutes,
                                targetBagIds = if (!isKioskMode) selectedBags.toList() else emptyList(),
                                targetPackageNames = if (!isKioskMode) selectedPackages.toList() else emptyList(),
                                enableDnsBlocking = enableDns,
                                dnsHost = selectedDnsHost,
                                keyType = selectedKeyType,
                                keySecret = currentSecret,
                                savedKeyId = selectedSavedKeyId,
                                enableKioskMode = isKioskMode,
                                kioskAllowedPackages = if (isKioskMode) kioskPackages.toList() else emptyList(),
                                kioskDisableSystemGestures = disableSystemGestures,
                                kioskEnableFloatingControls = enableFloatingControls,
                                kioskFloatingAutoHideSeconds = floatingAutoHideSeconds
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
            // --- SECTION 1: PROFILE & IDENTITY ---
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    TomatoSectionHeader(title = "PROFILE & IDENTITY")

                    val iconRes = icons.find { it.first == selectedIcon }?.second ?: R.drawable.mode_fill0
                    TomatoSettingsItem(
                        title = "Profile Identification",
                        subtitle = nameInput.ifBlank { "Tap to configure name & icon" },
                        leadingIcon = iconRes,
                        iconContainerColor = colorScheme.primaryContainer,
                        iconTint = colorScheme.onPrimaryContainer,
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = colorScheme.surfaceContainerLowest,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(iconRes),
                                            contentDescription = null,
                                            tint = colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Icon(
                                    painter = painterResource(R.drawable.arrow_forward_big),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .graphicsLayer { rotationZ = profileChevronRotation },
                                    tint = colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        shape = if (isProfileExpanded) {
                            RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                        } else {
                            segmentedItemShape(0, 1)
                        },
                        onClick = { isProfileExpanded = !isProfileExpanded }
                    )

                    AnimatedVisibility(
                        visible = isProfileExpanded,
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
                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    label = { Text("Mode Name *") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(Modifier.height(14.dp))

                                Text("Profile Icon:", style = typography.labelSmall, color = colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(6.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    icons.forEach { (name, res) ->
                                        val isSelected = selectedIcon == name
                                        Surface(
                                            onClick = { selectedIcon = name },
                                            shape = CircleShape,
                                            color = if (isSelected) colorScheme.primary else colorScheme.surfaceContainerLowest,
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    painter = painterResource(res),
                                                    contentDescription = null,
                                                    tint = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(22.dp)
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

            // --- SECTION 2: SESSION DURATION ---
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    TomatoSectionHeader(title = "SESSION DURATION")

                    TomatoSettingsItem(
                        title = "Default Duration",
                        subtitle = "${durationMinutes} minutes • Tap to adjust",
                        leadingIcon = R.drawable.schedule_fill0,
                        iconContainerColor = colorScheme.secondaryContainer,
                        iconTint = colorScheme.onSecondaryContainer,
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
                                        text = "${durationMinutes}m",
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
                                        .graphicsLayer { rotationZ = durationChevronRotation },
                                    tint = colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        shape = if (isDurationExpanded) {
                            RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                        } else {
                            segmentedItemShape(0, 1)
                        },
                        onClick = { isDurationExpanded = !isDurationExpanded }
                    )

                    AnimatedVisibility(
                        visible = isDurationExpanded,
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
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(25, 45, 60, 90).forEach { mins ->
                                        val isSelected = durationMinutes == mins
                                        Surface(
                                            onClick = { durationMinutes = mins },
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                                            modifier = Modifier.weight(1f).height(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("${mins}m", fontWeight = FontWeight.Bold, color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface)
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = durationMinutes.toString(),
                                    onValueChange = { durationMinutes = it.toIntOrNull() ?: 1 },
                                    label = { Text("Custom Minutes") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // --- SECTION 3: FOCUS STRATEGY (MUTUALLY EXCLUSIVE) ---
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TomatoSectionHeader(title = "FOCUS STRATEGY")

                    // Mutually Exclusive Selector: Block Targets vs Minimalist Kiosk Launcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(colorScheme.surfaceContainerHigh)
                            .padding(4.dp)
                    ) {
                        Surface(
                            onClick = { isKioskMode = false },
                            shape = CircleShape,
                            color = if (!isKioskMode) colorScheme.primary else Color.Transparent,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Block Targets",
                                    style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (!isKioskMode) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            onClick = { isKioskMode = true },
                            shape = CircleShape,
                            color = if (isKioskMode) colorScheme.primary else Color.Transparent,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Minimalist Kiosk",
                                    style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isKioskMode) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (!isKioskMode) {
                        // Targets to Block Configuration
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            val targetsSummary = when {
                                selectedBags.isNotEmpty() && selectedPackages.isNotEmpty() -> "${selectedBags.size} bags • ${selectedPackages.size} apps"
                                selectedBags.isNotEmpty() -> "${selectedBags.size} bags"
                                selectedPackages.isNotEmpty() -> "${selectedPackages.size} apps"
                                else -> "No targets selected"
                            }

                            TomatoSettingsItem(
                                title = "Blocked Apps & Bags",
                                subtitle = "$targetsSummary • Tap to select",
                                leadingIcon = R.drawable.backpack_fill0,
                                iconContainerColor = colorScheme.tertiaryContainer,
                                iconTint = colorScheme.onTertiaryContainer,
                                trailingContent = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (selectedBags.isNotEmpty() || selectedPackages.isNotEmpty()) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest
                                        ) {
                                            Text(
                                                text = "${selectedBags.size + selectedPackages.size}",
                                                style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (selectedBags.isNotEmpty() || selectedPackages.isNotEmpty()) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                        Icon(
                                            painter = painterResource(R.drawable.arrow_forward_big),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .graphicsLayer { rotationZ = targetsChevronRotation },
                                            tint = colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                shape = if (isTargetsExpanded) {
                                    RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                                } else {
                                    segmentedItemShape(0, 1)
                                },
                                onClick = { isTargetsExpanded = !isTargetsExpanded }
                            )

                            AnimatedVisibility(
                                visible = isTargetsExpanded,
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
                                        Text("Choose bags or individual apps to suspend during this focus mode", style = typography.bodySmall, color = colorScheme.onSurfaceVariant)
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
                        }
                    } else {
                        // Minimalist Kiosk Launcher Configuration
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            // 1. Permitted Apps
                            TomatoSettingsItem(
                                title = "Permitted Kiosk Apps",
                                subtitle = if (kioskPackages.isEmpty()) "Pure Focus • No apps allowed" else "${kioskPackages.size} apps permitted (max 10) • Tap to configure",
                                leadingIcon = R.drawable.apps_fill0,
                                iconContainerColor = colorScheme.tertiaryContainer,
                                iconTint = colorScheme.onTertiaryContainer,
                                trailingContent = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (kioskPackages.isNotEmpty()) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest
                                        ) {
                                            Text(
                                                text = "${kioskPackages.size}/10",
                                                style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (kioskPackages.isNotEmpty()) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                        Icon(
                                            painter = painterResource(R.drawable.arrow_forward_big),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .graphicsLayer { rotationZ = kioskAppsChevronRotation },
                                            tint = colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                shape = if (isKioskAppsExpanded) {
                                    RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                                } else {
                                    segmentedItemShape(0, 3)
                                },
                                onClick = { isKioskAppsExpanded = !isKioskAppsExpanded }
                            )

                            AnimatedVisibility(
                                visible = isKioskAppsExpanded,
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
                                        Text(
                                            text = "Allowed apps will appear in the distraction-free minimal text launcher during focus mode.",
                                            style = typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Selected Apps (${kioskPackages.size}/10)",
                                                style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = colorScheme.onSurface
                                            )

                                            Button(
                                                onClick = { showKioskAppPickerSheet = true },
                                                shape = CircleShape
                                            ) {
                                                Text("Manage Apps", fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Spacer(Modifier.height(10.dp))

                                        if (kioskPackages.isEmpty()) {
                                            Text(
                                                text = "No apps selected. Focus mode will run in Pure Focus mode without external apps.",
                                                style = typography.bodySmall,
                                                color = colorScheme.outline
                                            )
                                        } else {
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                installedApps.filter { kioskPackages.contains(it.packageName) }.forEach { app ->
                                                    InputChip(
                                                        selected = true,
                                                        onClick = { kioskPackages = kioskPackages - app.packageName },
                                                        label = { Text(app.label) },
                                                        leadingIcon = {
                                                            Icon(painter = painterResource(R.drawable.apps_fill0), contentDescription = null, modifier = Modifier.size(14.dp))
                                                        },
                                                        trailingIcon = {
                                                            Icon(painter = painterResource(R.drawable.close_fill0), contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. Pinning & Gestures Flexibility
                            TomatoSettingsItem(
                                title = "Pinning & Gesture Policy",
                                subtitle = if (disableSystemGestures) {
                                    "Strict Kiosk • Gestures & panels disabled"
                                } else {
                                    "Flexible Pinning • Gestures & panels enabled"
                                },
                                leadingIcon = R.drawable.shield_fill0,
                                iconContainerColor = colorScheme.primaryContainer,
                                iconTint = colorScheme.onPrimaryContainer,
                                trailingContent = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = colorScheme.surfaceContainerLowest
                                        ) {
                                            Text(
                                                text = if (disableSystemGestures) "Strict" else "Flexible",
                                                style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                        Icon(
                                            painter = painterResource(R.drawable.arrow_forward_big),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .graphicsLayer { rotationZ = kioskPolicyChevronRotation },
                                            tint = colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                shape = if (isKioskPolicyExpanded) {
                                    RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                                } else {
                                    segmentedItemShape(1, 3)
                                },
                                onClick = { isKioskPolicyExpanded = !isKioskPolicyExpanded }
                            )

                            AnimatedVisibility(
                                visible = isKioskPolicyExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Surface(
                                    color = colorScheme.surfaceContainerHigh,
                                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Option 1: Strict Kiosk
                                        Surface(
                                            onClick = { disableSystemGestures = true },
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (disableSystemGestures) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                                            border = BorderStroke(1.dp, if (disableSystemGestures) colorScheme.primary else colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Strict Kiosk (Gestures & Panels Disabled)",
                                                        style = typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                        color = if (disableSystemGestures) colorScheme.onPrimaryContainer else colorScheme.onSurface
                                                    )
                                                    Spacer(Modifier.height(2.dp))
                                                    Text(
                                                        text = "Pin window and completely disable navigation gestures, status bar, and notification shade.",
                                                        style = typography.bodySmall,
                                                        color = colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                if (disableSystemGestures) {
                                                    Spacer(Modifier.width(8.dp))
                                                    Icon(
                                                        painter = painterResource(R.drawable.check_fill0),
                                                        contentDescription = null,
                                                        tint = colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Option 2: Flexible Pinning
                                        Surface(
                                            onClick = { disableSystemGestures = false },
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (!disableSystemGestures) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                                            border = BorderStroke(1.dp, if (!disableSystemGestures) colorScheme.primary else colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Flexible Pinning (Gestures & Panels Enabled)",
                                                        style = typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                        color = if (!disableSystemGestures) colorScheme.onPrimaryContainer else colorScheme.onSurface
                                                    )
                                                    Spacer(Modifier.height(2.dp))
                                                    Text(
                                                        text = "Pins to single launcher window but keeps system navigation gestures and panels active.",
                                                        style = typography.bodySmall,
                                                        color = colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                if (!disableSystemGestures) {
                                                    Spacer(Modifier.width(8.dp))
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

                            // 3. Assistive Quick Controls
                            Column(modifier = Modifier.fillMaxWidth()) {
                                TomatoSettingsItem(
                                    title = "Assistive Quick Controls",
                                    subtitle = "Floating ball for Data, Wi-Fi, Hotspot & Torch",
                                    leadingIcon = R.drawable.ic_assistive_touch,
                                    iconContainerColor = colorScheme.secondaryContainer,
                                    iconTint = colorScheme.onSecondaryContainer,
                                    trailingContent = {
                                        Switch(
                                            checked = enableFloatingControls,
                                            onCheckedChange = { enableFloatingControls = it }
                                        )
                                    },
                                    shape = if (enableFloatingControls) {
                                        RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                                    } else {
                                        segmentedItemShape(2, 3)
                                    },
                                    onClick = { enableFloatingControls = !enableFloatingControls }
                                )

                                if (enableFloatingControls) {
                                    Surface(
                                        shape = RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp),
                                        color = colorScheme.surfaceContainerHigh,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Auto-hide after",
                                                style = typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                color = colorScheme.onSurfaceVariant
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                listOf(2 to "2s", 3 to "3s", 5 to "5s", 0 to "Off").forEach { (sec, label) ->
                                                    val isSelected = floatingAutoHideSeconds == sec
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                                                        border = BorderStroke(
                                                            1.dp,
                                                            if (isSelected) colorScheme.primary else colorScheme.outlineVariant.copy(alpha = 0.4f)
                                                        ),
                                                        modifier = Modifier.clickable {
                                                            floatingAutoHideSeconds = sec
                                                        }
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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
                }
            }

            // --- SECTION 4: LOCKDOWN & NETWORK ---
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    TomatoSectionHeader(title = "LOCKDOWN & NETWORK")

                    // 1. DNS Filtering
                    TomatoSettingsItem(
                        title = "Private DNS Blocking",
                        subtitle = if (enableDns) "Enforced • ${selectedDnsHost}" else "Disabled",
                        leadingIcon = R.drawable.shield_fill0,
                        iconContainerColor = colorScheme.primaryContainer,
                        iconTint = colorScheme.onPrimaryContainer,
                        trailingContent = {
                            Switch(checked = enableDns, onCheckedChange = { enableDns = it })
                        },
                        shape = segmentedItemShape(0, 2),
                        onClick = { enableDns = !enableDns }
                    )

                    if (enableDns) {
                        Surface(
                            color = colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
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

                    // 2. Strict Protection Key
                    TomatoSettingsItem(
                        title = "Protection Key",
                        subtitle = if (currentSecret.isNotBlank()) "Key configured (${selectedKeyType.name.lowercase().replaceFirstChar { it.uppercase() }})" else "Required to lock session • Tap to configure",
                        leadingIcon = R.drawable.key_fill0,
                        iconContainerColor = if (currentSecret.isNotBlank()) colorScheme.primaryContainer else colorScheme.surfaceContainerHighest,
                        iconTint = if (currentSecret.isNotBlank()) colorScheme.primary else colorScheme.onSurfaceVariant,
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (currentSecret.isNotBlank()) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest
                                ) {
                                    Text(
                                        text = if (currentSecret.isNotBlank()) "Ready" else "Required",
                                        style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (currentSecret.isNotBlank()) colorScheme.onPrimaryContainer else colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Icon(
                                    painter = painterResource(R.drawable.arrow_forward_big),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .graphicsLayer { rotationZ = keyChevronRotation },
                                    tint = colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        shape = if (isKeyExpanded) {
                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                        } else {
                            segmentedItemShape(1, 2)
                        },
                        onClick = { isKeyExpanded = !isKeyExpanded }
                    )

                    AnimatedVisibility(
                        visible = isKeyExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            color = colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 22.dp, bottomEnd = 22.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                                Text("Required to stop or unlock this focus session early", style = typography.bodySmall, color = colorScheme.onSurfaceVariant)
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
        }
    }

    // --- BOTTOM SHEET: SELECT TARGET BAGS ---
    if (showBagsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBagsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Target Bags", style = typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("Select bags to block during this focus mode", style = typography.bodySmall, color = colorScheme.onSurfaceVariant)
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
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp)
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
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Saved Keys", style = typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("Select a key to protect this focus mode", style = typography.bodySmall, color = colorScheme.onSurfaceVariant)
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
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp)
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

    // --- BOTTOM SHEET: SELECT KIOSK ALLOWED APPS ---
    if (showKioskAppPickerSheet) {
        var kioskSearch by remember { mutableStateOf("") }
        val filteredApps = remember(installedApps, kioskSearch) {
            if (kioskSearch.isBlank()) installedApps else installedApps.filter { it.label.contains(kioskSearch, ignoreCase = true) || it.packageName.contains(kioskSearch, ignoreCase = true) }
        }

        ModalBottomSheet(
            onDismissRequest = { showKioskAppPickerSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Kiosk Allowed Apps", style = typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("${kioskPackages.size}/10 apps selected", style = typography.bodySmall, color = colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { showKioskAppPickerSheet = false },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = kioskSearch,
                    onValueChange = { kioskSearch = it },
                    placeholder = { Text("Search apps...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val isSelected = kioskPackages.contains(app.packageName)
                        Surface(
                            onClick = {
                                if (isSelected) {
                                    kioskPackages = kioskPackages - app.packageName
                                } else {
                                    if (kioskPackages.size < 10) {
                                        kioskPackages = kioskPackages + app.packageName
                                    } else {
                                        Toast.makeText(context, "Maximum 10 apps allowed in kiosk", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, if (isSelected) colorScheme.primary else colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (app.icon != null) {
                                    Image(
                                        painter = rememberDrawablePainter(app.icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp)
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.apps_fill0),
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(app.label, style = typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface)
                                    Text(app.packageName, style = typography.bodySmall.copy(fontSize = 11.sp), color = colorScheme.onSurfaceVariant)
                                }
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            if (kioskPackages.size < 10) {
                                                kioskPackages = kioskPackages + app.packageName
                                            } else {
                                                Toast.makeText(context, "Maximum 10 apps allowed in kiosk", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            kioskPackages = kioskPackages - app.packageName
                                        }
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

private fun getModeIcon(iconName: String): Int {
    return when (iconName) {
        "schedule_fill0" -> R.drawable.schedule_fill0
        "dark_mode_fill0" -> R.drawable.dark_mode_fill0
        "backpack_fill0" -> R.drawable.backpack_fill0
        "shield_fill0" -> R.drawable.shield_fill0
        "palette_fill0" -> R.drawable.palette_fill0
        else -> R.drawable.mode_fill0
    }
}
