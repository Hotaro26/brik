package com.hotaro.brik.ui.screen

import android.widget.Toast
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
import com.hotaro.brik.ui.component.RoundedPullRefreshPage
import com.hotaro.brik.ui.viewmodel.ToolsViewModel
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

    var showManageDialog by remember { mutableStateOf(false) }
    var showCreateBagSheet by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var expandedPackage by remember { mutableStateOf<String?>(null) }

    RoundedPullRefreshPage(
        isRefreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                viewModel.loadInstalledApps()
                kotlinx.coroutines.delay(650)
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

                // Expressive Search & Filter Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Cylindrical Pill Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        modifier = Modifier.weight(1f),
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

                    // Visibility Toggle Pill
                    Surface(
                        onClick = { showManageDialog = true },
                        shape = CircleShape,
                        color = if (showSystemApps) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh,
                        contentColor = if (showSystemApps) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                        tonalElevation = if (showSystemApps) 4.dp else 0.dp,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(
                                    if (showSystemApps) R.drawable.visibility_fill0 else R.drawable.visibility_off_fill0
                                ),
                                contentDescription = "Manage app visibility",
                                modifier = Modifier.size(24.dp)
                            )
                            if (showSystemApps) {
                                Surface(
                                    shape = CircleShape,
                                    color = colorScheme.primary,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 10.dp, end = 10.dp)
                                        .size(8.dp)
                                ) {}
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

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

                            ExpressiveAppCard(
                                app = app,
                                isExpanded = isExpanded,
                                currentLimit = currentLimit,
                                onToggleExpand = {
                                    expandedPackage = if (isExpanded) null else app.packageName
                                },
                                onSaveLimit = { minutes, mode ->
                                    viewModel.setAppLimit(app.packageName, minutes, mode)
                                    Toast.makeText(context, "Limit saved for ${app.label}", Toast.LENGTH_SHORT).show()
                                },
                                onClearLimit = {
                                    viewModel.clearAppLimit(app.packageName)
                                    Toast.makeText(context, "Limit removed for ${app.label}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            // Floating Plus Icon at Bottom Right Corner to create bags
            FloatingActionButton(
                onClick = { showCreateBagSheet = true },
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 28.dp, end = 4.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.add_fill0),
                    contentDescription = "Create Bag",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    // --- Bottom Sheet: Create Bag with App Selection ---
    if (showCreateBagSheet) {
        CreateBagBottomSheet(
            installedApps = installedApps,
            onDismiss = { showCreateBagSheet = false },
            onSaveBag = { name, selectedPkgs ->
                val success = viewModel.createBagFromPackages(name, selectedPkgs)
                if (success) {
                    Toast.makeText(context, "Bag '$name' created with ${selectedPkgs.size} apps!", Toast.LENGTH_SHORT).show()
                    showCreateBagSheet = false
                }
            }
        )
    }

    // --- Manage Pop-up Dialog (System Apps Toggle) ---
    if (showManageDialog) {
        AlertDialog(
            onDismissRequest = { showManageDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.tune_fill0),
                                contentDescription = null,
                                tint = colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Text(
                        text = "App Visibility",
                        fontWeight = FontWeight.Bold,
                        style = typography.titleLarge,
                        color = colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Customize which categories of apps appear in the manager:",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Include System Apps",
                                    style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "Show pre-installed Android OS applications and OEM system packages",
                                    style = typography.bodySmall.copy(fontSize = 11.sp),
                                    color = colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Switch(
                                checked = showSystemApps,
                                onCheckedChange = { viewModel.setShowSystemApps(it) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showManageDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
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
    onToggleExpand: () -> Unit,
    onSaveLimit: (minutes: Int, mode: String) -> Unit,
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
                            text = app.packageName,
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
                                hours > 0 && mins > 0 -> "${hours}h ${mins}m"
                                hours > 0 -> "${hours}h"
                                else -> "${mins}m"
                            }
                            val modeLabel = if (currentLimit.blockMode == "hide") "Hide" else "Suspend"

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colorScheme.primaryContainer,
                                modifier = Modifier.padding(start = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.schedule_fill0),
                                        contentDescription = null,
                                        tint = colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        text = "$limitLabel • $modeLabel",
                                        style = typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        ),
                                        color = colorScheme.onPrimaryContainer
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

                    // Preset Chips
                    val presets = listOf(15, 30, 45, 60, 90, 120, 180, 240)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presets) { preset ->
                            val isSelected = limitMinutes == preset
                            val label = when {
                                preset >= 60 && preset % 60 == 0 -> "${preset / 60}h"
                                preset >= 60 -> "${preset / 60}h ${preset % 60}m"
                                else -> "${preset}m"
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { limitMinutes = preset },
                                label = {
                                    Text(
                                        text = label,
                                        style = typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colorScheme.primary,
                                    selectedLabelColor = colorScheme.onPrimary,
                                    containerColor = colorScheme.surfaceContainerHighest,
                                    labelColor = colorScheme.onSurface
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Slider for fine tuning (15m to 360m)
                    Slider(
                        value = limitMinutes.toFloat(),
                        onValueChange = { limitMinutes = (it / 15).toInt() * 15 },
                        valueRange = 15f..360f,
                        steps = 22,
                        colors = SliderDefaults.colors(
                            thumbColor = colorScheme.primary,
                            activeTrackColor = colorScheme.primary,
                            inactiveTrackColor = colorScheme.surfaceContainerHighest
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

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
                            onClick = { onSaveLimit(limitMinutes, blockMode) },
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

/**
 * Bottom Sheet to create bags with app selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBagBottomSheet(
    installedApps: List<AppInfo>,
    onDismiss: () -> Unit,
    onSaveBag: (name: String, selectedPackages: Set<String>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var bagName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPackages by remember { mutableStateOf<Set<String>>(emptySet()) }

    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            val q = searchQuery.trim().lowercase()
            installedApps.filter {
                it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "New App Bag",
                        style = typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "Pack selected apps into a bundle",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                if (selectedPackages.isNotEmpty()) {
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${selectedPackages.size} selected",
                            style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Bag Name Input
            OutlinedTextField(
                value = bagName,
                onValueChange = { bagName = it },
                label = { Text("Bag Name") },
                placeholder = { Text("e.g. Socials, Study, Games") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colorScheme.surfaceContainerLowest,
                    unfocusedContainerColor = colorScheme.surfaceContainerLowest
                )
            )

            Spacer(Modifier.height(10.dp))

            // Search Bar & Selection Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter apps...") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.search_fill0),
                            contentDescription = null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    painter = painterResource(R.drawable.close_fill0),
                                    contentDescription = null,
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = CircleShape,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colorScheme.surfaceContainerLowest,
                        unfocusedContainerColor = colorScheme.surfaceContainerLowest
                    )
                )

                if (selectedPackages.isNotEmpty()) {
                    TextButton(onClick = { selectedPackages = emptySet() }) {
                        Text("Clear", color = colorScheme.error, style = typography.labelMedium)
                    }
                } else {
                    TextButton(onClick = { selectedPackages = filteredApps.map { it.packageName }.toSet() }) {
                        Text("All", color = colorScheme.primary, style = typography.labelMedium)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // App list with checkboxes
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val isChecked = selectedPackages.contains(app.packageName)

                    Surface(
                        onClick = {
                            selectedPackages = if (isChecked) {
                                selectedPackages - app.packageName
                            } else {
                                selectedPackages + app.packageName
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isChecked) colorScheme.primaryContainer.copy(alpha = 0.45f) else colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colorScheme.surfaceContainerHighest,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (app.icon != null) {
                                        Image(
                                            painter = rememberDrawablePainter(drawable = app.icon),
                                            contentDescription = app.label,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(R.drawable.apps_fill0),
                                            contentDescription = null,
                                            tint = colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = app.label,
                                    style = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = app.packageName,
                                    style = typography.bodySmall.copy(fontSize = 10.sp),
                                    color = colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    selectedPackages = if (isChecked) {
                                        selectedPackages - app.packageName
                                    } else {
                                        selectedPackages + app.packageName
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = colorScheme.primary,
                                    uncheckedColor = colorScheme.outline.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Save Bag Button
            Button(
                onClick = { onSaveBag(bagName, selectedPackages) },
                enabled = bagName.isNotBlank() && selectedPackages.isNotEmpty(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = if (selectedPackages.isEmpty()) "Select apps to pack" else "Save Bag (${selectedPackages.size})",
                    style = typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
