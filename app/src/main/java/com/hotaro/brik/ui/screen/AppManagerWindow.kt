package com.hotaro.brik.ui.screen

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hotaro.brik.util.CommonKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.hotaro.brik.data.model.AppInfo
import com.hotaro.brik.data.repository.AppLimit
import com.hotaro.brik.data.repository.KeyRepository
import com.hotaro.brik.ui.component.BrikSwitch
import com.hotaro.brik.ui.component.RoundedPullRefreshPage
import com.hotaro.brik.ui.component.SetEmergencyKeyDialog
import com.hotaro.brik.ui.component.VerifyEmergencyKeyDialog
import com.hotaro.brik.ui.viewmodel.ToolsViewModel
import com.hotaro.brik.util.AppLimitManager
import kotlinx.coroutines.launch

/**
 * Material 3 Expressive App Manager Window.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppManagerWindow(
    onBack: () -> Unit,
    viewModel: ToolsViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val installedApps by viewModel.installedApps.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showSystemApps by viewModel.showSystemApps.collectAsState()
    val isLoadingApps by viewModel.isLoadingApps.collectAsState()
    val appLimits by viewModel.appLimits.collectAsState()

    var isRefreshing by remember { mutableStateOf(false) }
    var expandedPackage by remember { mutableStateOf<String?>(null) }

    val isKeyGuardEnabled by AppLimitManager.isKeyGuardEnabled.collectAsState()
    val hasSecurityKey by AppLimitManager.hasSecurityKey.collectAsState()
    val isUsingCommonKey = remember(context) {
        CommonKeyManager.isFollowingAndReady(context, AppLimitManager.SYSTEM_KEY_ID)
    }
    val keyRepository = remember(context) { KeyRepository(context) }
    val savedKeys by keyRepository.savedKeys.collectAsState()

    var hasUsagePermission by remember { mutableStateOf(AppLimitManager.hasUsageStatsPermission(context)) }
    var showUsagePermissionDialog by remember { mutableStateOf(!hasUsagePermission) }
    var usageMinutesMap by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = AppLimitManager.hasUsageStatsPermission(context)
                hasUsagePermission = granted
                if (granted) {
                    showUsagePermissionDialog = false
                    AppLimitManager.checkAndEnforceLimits(context, force = true)
                    coroutineScope.launch {
                        usageMinutesMap = withContext(Dispatchers.IO) {
                            AppLimitManager.getUsageMapToday(context)
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasUsagePermission) {
        if (hasUsagePermission) {
            AppLimitManager.checkAndEnforceLimits(context, force = true)
            usageMinutesMap = withContext(Dispatchers.IO) {
                AppLimitManager.getUsageMapToday(context)
            }
        }
    }

    var showKeyLockSheet by remember { mutableStateOf(false) }
    var showSetKeyDialog by remember { mutableStateOf(false) }
    var showVerifyKeyDialog by remember { mutableStateOf(false) }
    var showVerifyBeforeChangeKeyDialog by remember { mutableStateOf(false) }
    var pendingGuardedAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val performGuardedAction: (() -> Unit) -> Unit = { action ->
        if (isKeyGuardEnabled && hasSecurityKey) {
            pendingGuardedAction = action
            showVerifyKeyDialog = true
        } else {
            action()
        }
    }

    RoundedPullRefreshPage(
        isRefreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                val nextState = !showSystemApps
                viewModel.setShowSystemApps(nextState)
                Toast.makeText(
                    context,
                    if (nextState) "System apps revealed" else "System apps hidden",
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.loadInstalledApps()
                kotlinx.coroutines.delay(450)
                isRefreshing = false
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "App Manager",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            letterSpacing = (-0.5).sp,
                            color = colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${filteredApps.size} apps available",
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                            val limitsCount = appLimits.size
                            if (limitsCount > 0) {
                                Text(
                                    text = " • $limitsCount limit${if (limitsCount > 1) "s" else ""}",
                                    style = typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.primary
                                )
                            }
                        }
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
                actions = {
                    if (expandedPackage != null) {
                        TextButton(onClick = { expandedPackage = null }) {
                            Text(
                                text = "Collapse",
                                color = colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = if (isKeyGuardEnabled && hasSecurityKey) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(40.dp),
                        onClick = { showKeyLockSheet = true }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(
                                    if (isKeyGuardEnabled && hasSecurityKey) R.drawable.shield_fill0 else R.drawable.key_fill0
                                ),
                                contentDescription = "App Manager Key Lock",
                                tint = if (isKeyGuardEnabled && hasSecurityKey) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(Modifier.height(10.dp))

                // Expressive Search Field (Full Width - Pull down to show/hide system apps)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Search apps...",
                            style = typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.search_fill0),
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(
                                    painter = painterResource(R.drawable.close_fill0),
                                    contentDescription = "Clear search",
                                    tint = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = colorScheme.surfaceContainerLow,
                        focusedBorderColor = colorScheme.primary,
                        unfocusedBorderColor = colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                )

                Spacer(Modifier.height(14.dp))

                if (!hasUsagePermission) {
                    Surface(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = colorScheme.errorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Text(
                                    text = "Usage Access Required",
                                    style = typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Grant Usage Access so Brik can read app screen time and enforce daily limits.",
                                    style = typography.bodySmall,
                                    color = colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                )
                            }
                            Text(
                                text = "Grant",
                                style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // App list
                if (isLoadingApps) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colorScheme.primary, strokeWidth = 3.dp)
                    }
                } else if (filteredApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.apps_fill0),
                                        contentDescription = null,
                                        tint = colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "No apps found",
                                style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (!showSystemApps) "System apps are hidden. Tap Manage to include them." else "Try searching with a different keyword.",
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            val isExpanded = expandedPackage == app.packageName
                            val currentLimit = appLimits[app.packageName]
                            val usedMins = usageMinutesMap[app.packageName] ?: 0

                            ExpressiveAppCard(
                                app = app,
                                isExpanded = isExpanded,
                                currentLimit = currentLimit,
                                usedMinutesToday = usedMins,
                                onToggleExpand = {
                                    expandedPackage = if (isExpanded) null else app.packageName
                                },
                                onToggleRestrict = { mode, restrict ->
                                    if (!restrict) {
                                        performGuardedAction {
                                            viewModel.toggleAppRestrict(app.packageName, mode, false) { ok, err ->
                                                Toast.makeText(
                                                    context,
                                                    if (ok) "Unrestricted ${app.label}" else (err ?: "Failed to unrestrict ${app.label}"),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    } else {
                                        viewModel.toggleAppRestrict(app.packageName, mode, true) { ok, err ->
                                            Toast.makeText(
                                                context,
                                                if (ok) "${if (mode == "hide") "Hidden" else "Locked"} ${app.label}" else (err ?: "Failed to restrict ${app.label}"),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                },
                                onSaveLimit = { minutes, mode, restricted ->
                                    val isRelaxing = currentLimit != null && (
                                        (currentLimit.isRestricted && !restricted) ||
                                            (minutes > currentLimit.dailyLimitMinutes && currentLimit.dailyLimitMinutes == 0)
                                    )
                                    val doSave: () -> Unit = {
                                        viewModel.setAppLimit(app.packageName, minutes, mode, restricted) { ok, err ->
                                            if (ok) {
                                                Toast.makeText(context, "Settings saved for ${app.label}", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, err ?: "Saved, but bridge failed to enforce", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    if (isRelaxing) performGuardedAction(doSave) else doSave()
                                },
                                onClearLimit = {
                                    performGuardedAction {
                                        viewModel.clearAppLimit(app.packageName) { ok, err ->
                                            Toast.makeText(
                                                context,
                                                if (ok) "Limit removed for ${app.label}" else (err ?: "Limit removed, bridge unrestrict failed"),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showKeyLockSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showKeyLockSheet = false },
            sheetState = sheetState,
            containerColor = colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.key_fill0),
                                contentDescription = null,
                                tint = colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "App Manager Key Lock",
                            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Optional lock required to disable or modify app restrictions",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Require Key Lock",
                                style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = if (isKeyGuardEnabled && hasSecurityKey) "Active • Key required to turn off restrictions"
                                else if (!hasSecurityKey) "Configure a key below to enable protection"
                                else "Off • Changes allowed freely",
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        BrikSwitch(
                            checked = isKeyGuardEnabled && hasSecurityKey,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    if (hasSecurityKey) {
                                        AppLimitManager.setKeyGuardEnabled(context, true)
                                    } else {
                                        showSetKeyDialog = true
                                    }
                                } else {
                                    performGuardedAction {
                                        AppLimitManager.setKeyGuardEnabled(context, false)
                                        Toast.makeText(context, "App Manager Key Lock disabled", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }

                if (isUsingCommonKey) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.key_fill0),
                                contentDescription = null,
                                tint = colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Common Key configured for this. Manage or toggle it in Keys & Analytics.",
                                style = typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = colorScheme.onSecondaryContainer
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            if (hasSecurityKey) {
                                showVerifyBeforeChangeKeyDialog = true
                            } else {
                                showSetKeyDialog = true
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (hasSecurityKey) "Change Security Key" else "Configure Security Key")
                    }

                    if (hasSecurityKey) {
                        OutlinedButton(
                            onClick = {
                                performGuardedAction {
                                    AppLimitManager.clearSecurityKey(context)
                                    Toast.makeText(context, "App Manager key removed", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Remove Security Key")
                        }
                    }
                }
            }
        }
    }

    if (showUsagePermissionDialog && !hasUsagePermission) {
        AlertDialog(
            onDismissRequest = { showUsagePermissionDialog = false },
            title = {
                Text(
                    text = "Usage Access Needed",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "To track each app's daily screen time and block apps when they exceed their limit, please grant Usage Access to Brik."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUsagePermissionDialog = false
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                ) {
                    Text("Grant Access")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUsagePermissionDialog = false }) {
                    Text("Later")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
        )
    }

    if (showSetKeyDialog) {
        SetEmergencyKeyDialog(
            savedKeys = savedKeys,
            title = "App Manager Security Key",
            subtitle = "Set a password or QR key required to turn off app restrictions or change App Manager settings.",
            onDismiss = { showSetKeyDialog = false },
            onSaveKey = { keyType, secret ->
                AppLimitManager.setSecurityKey(context, keyType, secret)
                AppLimitManager.setKeyGuardEnabled(context, true)
                Toast.makeText(context, "App Manager Key Lock configured", Toast.LENGTH_SHORT).show()
            },
            onKeySet = { showSetKeyDialog = false }
        )
    }

    if (showVerifyBeforeChangeKeyDialog) {
        VerifyEmergencyKeyDialog(
            title = "Verify Current Key",
            subtitle = "Enter or scan your current App Manager key to change it.",
            keyTypeOverride = AppLimitManager.getKeyType(context),
            onVerifyCustom = { input -> AppLimitManager.verifySecurityKey(context, input) },
            onDismiss = { showVerifyBeforeChangeKeyDialog = false },
            onVerified = {
                showVerifyBeforeChangeKeyDialog = false
                showSetKeyDialog = true
            }
        )
    }

    if (showVerifyKeyDialog) {
        VerifyEmergencyKeyDialog(
            title = "App Manager Locked",
            subtitle = "Verify your security key to modify or disable app restrictions.",
            keyTypeOverride = AppLimitManager.getKeyType(context),
            onVerifyCustom = { input -> AppLimitManager.verifySecurityKey(context, input) },
            onDismiss = {
                showVerifyKeyDialog = false
                pendingGuardedAction = null
            },
            onVerified = {
                showVerifyKeyDialog = false
                pendingGuardedAction?.invoke()
                pendingGuardedAction = null
            }
        )
    }
}

/**
 * Material 3 Expressive App Card with Expandable Limit Configuration.
 */
@Composable
fun ExpressiveAppCard(
    app: AppInfo,
    isExpanded: Boolean,
    currentLimit: AppLimit?,
    usedMinutesToday: Int = 0,
    onToggleExpand: () -> Unit,
    onToggleRestrict: (mode: String, restrict: Boolean) -> Unit,
    onSaveLimit: (minutes: Int, mode: String, isRestricted: Boolean) -> Unit,
    onClearLimit: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (isExpanded) {
            colorScheme.surfaceContainerHigh
        } else if (currentLimit != null) {
            colorScheme.surfaceContainer
        } else {
            colorScheme.surfaceContainerLow
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cardContainerColor"
    )

    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "arrowRotation"
    )

    var limitMinutes by remember(currentLimit, isExpanded) {
        mutableIntStateOf(currentLimit?.dailyLimitMinutes ?: 60)
    }
    var blockMode by remember(currentLimit, isExpanded) {
        mutableStateOf(currentLimit?.blockMode ?: "suspend")
    }
    val isRestricted = currentLimit?.isRestricted == true

    Surface(
        onClick = onToggleExpand,
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Main App Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Icon with Squircle Container
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (app.icon != null) {
                            Image(
                                painter = rememberDrawablePainter(drawable = app.icon),
                                contentDescription = app.label,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.apps_fill0),
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = app.label,
                            style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (app.isSystemApp) {
                            Surface(
                                shape = CircleShape,
                                color = colorScheme.surfaceContainerHighest
                            ) {
                                Text(
                                    text = "System",
                                    style = typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 9.sp
                                    ),
                                    color = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (usedMinutesToday > 0) "${usedMinutesToday}m today • ${app.packageName}" else app.packageName,
                            style = typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        // Active Limit Badge when collapsed
                        if (currentLimit != null && !isExpanded) {
                            val hours = currentLimit.dailyLimitMinutes / 60
                            val mins = currentLimit.dailyLimitMinutes % 60
                            val limitLabel = when {
                                currentLimit.isRestricted -> "Restricted"
                                hours > 0 && mins > 0 -> "${hours}h ${mins}m"
                                hours > 0 -> "${hours}h"
                                else -> "${mins}m"
                            }
                            val modeLabel = if (currentLimit.blockMode == "hide") "Hide" else "Suspend"

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (currentLimit.isRestricted) colorScheme.errorContainer else colorScheme.primaryContainer,
                                modifier = Modifier.padding(start = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            if (currentLimit.isRestricted) R.drawable.block_fill0 else R.drawable.schedule_fill0
                                        ),
                                        contentDescription = null,
                                        tint = if (currentLimit.isRestricted) colorScheme.onErrorContainer else colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        text = "$limitLabel • $modeLabel",
                                        style = typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        ),
                                        color = if (currentLimit.isRestricted) colorScheme.onErrorContainer else colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Expand indicator arrow
                Surface(
                    shape = CircleShape,
                    color = if (isExpanded) colorScheme.primaryContainer else Color.Transparent,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_forward_big),
                            contentDescription = if (isExpanded) "Collapse" else "Expand to set limits",
                            tint = if (isExpanded) colorScheme.primary else colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(16.dp)
                                .graphicsLayer { rotationZ = arrowRotation }
                        )
                    }
                }
            }

            // Expanded Limit Configuration Panel
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    HorizontalDivider(
                        color = colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 1.dp
                    )

                    Spacer(Modifier.height(12.dp))

                    // Immediate App Restrict Toggle (Lock/Suspend or Hide right now)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isRestricted) colorScheme.errorContainer.copy(alpha = 0.45f) else colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "App Restrict",
                                    style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = if (isRestricted) {
                                        "Active • Currently ${if (blockMode == "hide") "hidden from launcher" else "locked & suspended"}"
                                    } else {
                                        "Immediately ${if (blockMode == "hide") "hide" else "suspend"} this app"
                                    },
                                    style = typography.bodySmall.copy(fontSize = 11.sp),
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            BrikSwitch(
                                checked = isRestricted,
                                onCheckedChange = { checked ->
                                    onToggleRestrict(blockMode, checked)
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Title & Current Duration Display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val cardContext = LocalContext.current
                        val usedTodayMins = remember(isExpanded) {
                            if (isExpanded) com.hotaro.brik.util.AppLimitManager.getUsageMinutesToday(cardContext, app.packageName) else 0
                        }

                        Column {
                            Text(
                                text = "Daily Time Limit",
                                style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = if (usedTodayMins > 0) "Used today: ${usedTodayMins}m • Action triggers on expiry" else "Action triggers after usage expires",
                                style = typography.bodySmall.copy(fontSize = 11.sp),
                                color = colorScheme.onSurfaceVariant
                            )
                        }

                        val h = limitMinutes / 60
                        val m = limitMinutes % 60
                        val durationStr = when {
                            h > 0 && m > 0 -> "${h}h ${m}m"
                            h > 0 -> "${h}h"
                            else -> "${m}m"
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colorScheme.primaryContainer
                        ) {
                            Text(
                                text = durationStr,
                                style = typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Custom Limit Input Field Placeholder
                    var customLimitText by remember(limitMinutes) {
                        mutableStateOf(if (limitMinutes > 0) limitMinutes.toString() else "")
                    }

                    OutlinedTextField(
                        value = customLimitText,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() }.take(4)
                            customLimitText = filtered
                            val parsed = filtered.toIntOrNull() ?: 0
                            limitMinutes = parsed.coerceIn(0, 1440)
                        },
                        placeholder = {
                            Text(
                                "Enter custom limit (e.g. 45)",
                                style = typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        trailingIcon = {
                            Text(
                                text = "minutes",
                                style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.primary,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.schedule_fill0),
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colorScheme.surfaceContainerHighest,
                            unfocusedContainerColor = colorScheme.surfaceContainerHigh,
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    // Block Mode Selection: Suspension vs Hiding
                    Text(
                        text = "Block Mode",
                        style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "Choose enforcement method when time limit is reached",
                        style = typography.bodySmall.copy(fontSize = 11.sp),
                        color = colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Option 1: Suspend
                        val isSuspend = blockMode == "suspend"
                        Surface(
                            onClick = { blockMode = "suspend" },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSuspend) colorScheme.primaryContainer else colorScheme.surfaceContainerHighest,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.block_fill0),
                                        contentDescription = null,
                                        tint = if (isSuspend) colorScheme.primary else colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Suspension",
                                        style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSuspend) colorScheme.onPrimaryContainer else colorScheme.onSurface
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Gray out and freeze application package",
                                    style = typography.bodySmall.copy(fontSize = 10.sp),
                                    color = if (isSuspend) colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Option 2: Hiding
                        val isHide = blockMode == "hide"
                        Surface(
                            onClick = { blockMode = "hide" },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isHide) colorScheme.primaryContainer else colorScheme.surfaceContainerHighest,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.visibility_off_fill0),
                                        contentDescription = null,
                                        tint = if (isHide) colorScheme.primary else colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Hiding",
                                        style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isHide) colorScheme.onPrimaryContainer else colorScheme.onSurface
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Completely hide application from launcher",
                                    style = typography.bodySmall.copy(fontSize = 10.sp),
                                    color = if (isHide) colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                        Spacer(Modifier.height(14.dp))

                    // Action Buttons Row: Remove Limit & Save Limit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentLimit != null) {
                            OutlinedButton(
                                onClick = onClearLimit,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = colorScheme.error
                                )
                            ) {
                                Text("Remove Limit")
                            }
                            Spacer(Modifier.width(8.dp))
                        }

                        Button(
                            onClick = { onSaveLimit(limitMinutes, blockMode, isRestricted) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.primary,
                                contentColor = colorScheme.onPrimary
                            )
                        ) {
                            Text(if (currentLimit != null) "Update Limit" else "Save Limit")
                        }
                    }
                }
            }
        }
    }
}
