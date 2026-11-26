package com.hotaro.brik.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.hotaro.brik.ui.component.rememberFloatingActionButtonExpanded
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.hotaro.brik.R
import com.hotaro.brik.data.model.AppBag
import com.hotaro.brik.data.model.AppInfo
import com.hotaro.brik.data.model.BagAction
import com.hotaro.brik.data.model.KeyType
import com.hotaro.brik.data.model.SavedKey
import com.hotaro.brik.ui.component.QrScannerView
import com.hotaro.brik.ui.component.RoundedPullRefreshPage
import com.hotaro.brik.ui.viewmodel.KeyViewModel
import com.hotaro.brik.ui.viewmodel.ToolsViewModel
import com.hotaro.brik.util.EmergencyManager
import com.hotaro.brik.util.QrUtils
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BagsWindow(
    onBack: () -> Unit,
    viewModel: ToolsViewModel = viewModel(),
    keyViewModel: KeyViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val bags by viewModel.bags.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()

    var isRefreshing by remember { mutableStateOf(false) }

    // Selected bag for the detail bottom sheet
    var selectedBagForSheet by remember { mutableStateOf<AppBag?>(null) }
    // Bag being edited (name, apps, key)
    var editingBag by remember { mutableStateOf<AppBag?>(null) }
    // Bag to lock dialog state
    var bagToLock by remember { mutableStateOf<AppBag?>(null) }
    // Bag to delete confirmation state
    var bagToDelete by remember { mutableStateOf<AppBag?>(null) }
    // Attempted to edit a running bag
    var bagLockedEditAttempt by remember { mutableStateOf<AppBag?>(null) }
    var showCreateBagSheet by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val isFabExpanded by rememberFloatingActionButtonExpanded(listState)

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
                            text = "Bags",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "${bags.size} configured bundles • Tap a bag to inspect or lock",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Spacer(Modifier.height(10.dp))

                if (bags.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colorScheme.surfaceContainerHighest,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.backpack_fill0),
                                        contentDescription = null,
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "No Bags Configured",
                                style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Tap the + Add button below to pack apps into a custom bundle.",
                                style = typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(bags, key = { it.id }) { bag ->
                            val isLocked = keyViewModel.isBagLocked(bag.id)
                            SwipeableBagItemCard(
                                bag = bag,
                                isLocked = isLocked,
                                viewModel = viewModel,
                                onClick = { selectedBagForSheet = bag },
                                onSwipeDelete = {
                                    if (isLocked) {
                                        bagLockedEditAttempt = bag
                                    } else {
                                        bagToDelete = bag
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Floating Plus Icon as rectangle with round edges and "Add" word written beside it
            ExtendedFloatingActionButton(
                onClick = { showCreateBagSheet = true },
                expanded = isFabExpanded,
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.add_fill0),
                        contentDescription = "Add Bag",
                        modifier = Modifier.size(22.dp)
                    )
                },
                text = {
                    Text(
                        text = "Add",
                        fontWeight = FontWeight.Bold,
                        style = typography.labelLarge
                    )
                },
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 28.dp, end = 4.dp)
            )
        }
    }

    // --- CREATE BAG WIZARD BOTTOM SHEET ---
    if (showCreateBagSheet) {
        CreateBagWizardSheet(
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

    // --- BAG DETAIL BOTTOM SHEET (Card clicked) ---
    if (selectedBagForSheet != null) {
        val activeBag = bags.find { it.id == selectedBagForSheet!!.id } ?: selectedBagForSheet!!
        val isLocked = keyViewModel.isBagLocked(activeBag.id)
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { selectedBagForSheet = null },
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
                // Header Row with Bag Name, Toggle Switch Pill, and Edit Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeBag.name,
                            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${activeBag.packageNames.size} apps bundled",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isEmergencyActive by EmergencyManager.isEmergencyModeActive.collectAsState()
                        if (isLocked && isEmergencyActive) {
                            Surface(
                                shape = CircleShape,
                                color = colorScheme.errorContainer,
                                modifier = Modifier.height(38.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.shield_fill0),
                                        contentDescription = null,
                                        tint = colorScheme.onErrorContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "EMERGENCY",
                                        style = typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 9.sp),
                                        color = colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        // Toggle Switch Pill at top
                        Surface(
                            shape = CircleShape,
                            color = if (isLocked) colorScheme.errorContainer else colorScheme.surfaceContainerHighest,
                            border = BorderStroke(1.dp, if (isLocked) colorScheme.error else colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Icon(
                                    painter = painterResource(if (isLocked) R.drawable.block_fill0 else R.drawable.backpack_fill0),
                                    contentDescription = null,
                                    tint = if (isLocked) colorScheme.error else colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (isLocked) "Locked" else "Lock",
                                    style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isLocked) colorScheme.error else colorScheme.onSurface
                                )
                                Spacer(Modifier.width(4.dp))
                                Switch(
                                    checked = isLocked,
                                    onCheckedChange = { shouldLock ->
                                        if (shouldLock) {
                                            bagToLock = activeBag
                                        } else {
                                            Toast.makeText(context, "Go to Key window to unlock '${activeBag.name}'", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.scale(0.75f)
                                )
                            }
                        }

                        // Edit Icon to add/remove apps, change name, etc.
                        Surface(
                            onClick = {
                                if (isLocked) {
                                    bagLockedEditAttempt = activeBag
                                } else {
                                    editingBag = activeBag
                                }
                            },
                            shape = CircleShape,
                            color = colorScheme.secondaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.edit_fill0),
                                    contentDescription = "Edit Bag",
                                    tint = colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Segmented Buttons for Activation Mode (Suspend vs Hide)
                Text(
                    text = "Activation Mode",
                    style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(colorScheme.surfaceContainerLowest)
                        .padding(4.dp)
                ) {
                    val isSuspend = activeBag.actionType == BagAction.SUSPEND

                    Surface(
                        onClick = {
                            if (isLocked) {
                                bagLockedEditAttempt = activeBag
                            } else if (activeBag.actionType != BagAction.SUSPEND) {
                                val updated = activeBag.copy(actionType = BagAction.SUSPEND)
                                viewModel.updateBag(updated)
                                Toast.makeText(context, "Mode set to Suspend (apps greyed out)", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = CircleShape,
                        color = if (isSuspend) colorScheme.primaryContainer else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.block_fill0),
                                contentDescription = null,
                                tint = if (isSuspend) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Suspend",
                                style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSuspend) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        onClick = {
                            if (isLocked) {
                                bagLockedEditAttempt = activeBag
                            } else if (activeBag.actionType != BagAction.HIDE) {
                                val updated = activeBag.copy(actionType = BagAction.HIDE)
                                viewModel.updateBag(updated)
                                Toast.makeText(context, "Mode set to Hide (apps hidden from launcher)", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = CircleShape,
                        color = if (!isSuspend) colorScheme.primaryContainer else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.visibility_off_fill0),
                                contentDescription = null,
                                tint = if (!isSuspend) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Hide",
                                style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (!isSuspend) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Apps in this Bag:",
                    style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(10.dp))

                // List of all selected apps in this bag
                if (activeBag.packageNames.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No apps in this bag. Tap the edit icon above to add apps.",
                            style = typography.bodySmall,
                            color = colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(activeBag.packageNames.toList(), key = { it }) { pkg ->
                            val appInfo = viewModel.getAppInfo(pkg)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = colorScheme.surfaceContainerLowest,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (appInfo?.icon != null) {
                                        Image(
                                            painter = rememberDrawablePainter(drawable = appInfo.icon),
                                            contentDescription = appInfo.label,
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(R.drawable.apps_fill0),
                                            contentDescription = null,
                                            tint = colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(38.dp)
                                        )
                                    }

                                    Spacer(Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = appInfo?.label ?: pkg,
                                            style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = pkg,
                                            style = typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                            color = colorScheme.outline,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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

    // --- EDIT BAG BOTTOM SHEET (Change name, add/remove apps) ---
    if (editingBag != null) {
        val targetBag = editingBag!!
        if (keyViewModel.isBagLocked(targetBag.id)) {
            bagLockedEditAttempt = targetBag
            editingBag = null
        }
        var editedName by remember(targetBag.id) { mutableStateOf(targetBag.name) }
        var editedPackages by remember(targetBag.id) { mutableStateOf(targetBag.packageNames.toSet()) }
        var editedActionType by remember(targetBag.id) { mutableStateOf(targetBag.actionType) }
        var appSearchQuery by remember { mutableStateOf("") }
        var showSystemAppsInEdit by remember { mutableStateOf(false) }
        val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        val filteredApps = remember(installedApps, appSearchQuery, showSystemAppsInEdit) {
            val baseList = if (showSystemAppsInEdit) installedApps else installedApps.filter { !it.isSystemApp }
            if (appSearchQuery.isBlank()) {
                baseList
            } else {
                val q = appSearchQuery.trim().lowercase()
                baseList.filter {
                    it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { editingBag = null },
            sheetState = editSheetState,
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
                        Text(
                            text = "Edit Bag",
                            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "${editedPackages.size} apps selected",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            if (editedName.isNotBlank()) {
                                val updated = targetBag.copy(
                                    name = editedName.trim(),
                                    packageNames = editedPackages,
                                    actionType = editedActionType
                                )
                                viewModel.updateBag(updated)
                                if (selectedBagForSheet?.id == updated.id) {
                                    selectedBagForSheet = updated
                                }
                                Toast.makeText(context, "Bag '${updated.name}' updated!", Toast.LENGTH_SHORT).show()
                                editingBag = null
                            }
                        },
                        enabled = editedName.isNotBlank() && editedPackages.isNotEmpty(),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Bag Name *") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(14.dp))

                // Segmented Buttons in Edit sheet
                Text(
                    text = "Activation Mode",
                    style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(colorScheme.surfaceContainerLowest)
                        .padding(4.dp)
                ) {
                    val isSuspend = editedActionType == BagAction.SUSPEND

                    Surface(
                        onClick = { editedActionType = BagAction.SUSPEND },
                        shape = CircleShape,
                        color = if (isSuspend) colorScheme.primaryContainer else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.block_fill0),
                                contentDescription = null,
                                tint = if (isSuspend) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Suspend",
                                style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSuspend) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        onClick = { editedActionType = BagAction.HIDE },
                        shape = CircleShape,
                        color = if (!isSuspend) colorScheme.primaryContainer else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.visibility_off_fill0),
                                contentDescription = null,
                                tint = if (!isSuspend) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Hide",
                                style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (!isSuspend) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.surfaceContainerLowest,
                        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.search_fill0),
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (appSearchQuery.isEmpty()) {
                                    Text(
                                        text = "Search apps...",
                                        style = typography.bodyMedium,
                                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                BasicTextField(
                                    value = appSearchQuery,
                                    onValueChange = { appSearchQuery = it },
                                    textStyle = typography.bodyMedium.copy(color = colorScheme.onSurface),
                                    singleLine = true,
                                    cursorBrush = SolidColor(colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (appSearchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { appSearchQuery = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.close_fill0),
                                        contentDescription = "Clear",
                                        tint = colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = if (showSystemAppsInEdit) colorScheme.primaryContainer else colorScheme.surfaceContainerHighest,
                        border = BorderStroke(1.dp, if (showSystemAppsInEdit) colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent),
                        modifier = Modifier.size(42.dp),
                        onClick = { showSystemAppsInEdit = !showSystemAppsInEdit }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(if (showSystemAppsInEdit) R.drawable.visibility_fill0 else R.drawable.visibility_off_fill0),
                                contentDescription = if (showSystemAppsInEdit) "Hide System Apps" else "Show System Apps",
                                tint = if (showSystemAppsInEdit) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val isSelected = editedPackages.contains(app.packageName)
                        Surface(
                            onClick = {
                                editedPackages = if (isSelected) editedPackages - app.packageName else editedPackages + app.packageName
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, if (isSelected) colorScheme.primary else colorScheme.outlineVariant.copy(alpha = 0.4f)),
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
                                        style = typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = typography.bodySmall.copy(fontSize = 10.sp),
                                        color = colorScheme.outline,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        editedPackages = if (checked) editedPackages + app.packageName else editedPackages - app.packageName
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- SWIPE DELETE CONFIRMATION DIALOG ---
    if (bagToDelete != null) {
        val bag = bagToDelete!!
        AlertDialog(
            onDismissRequest = { bagToDelete = null },
            title = {
                Text(
                    text = "Delete '${bag.name}'?",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.error
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this bag? This will remove the bag bundle. Installed applications will remain on your device.",
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBag(bag.id)
                        if (selectedBagForSheet?.id == bag.id) {
                            selectedBagForSheet = null
                        }
                        Toast.makeText(context, "Deleted '${bag.name}'", Toast.LENGTH_SHORT).show()
                        bagToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { bagToDelete = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
        )
    }

    // --- RUNNING BAG EDIT PROTECTION DIALOG ---
    if (bagLockedEditAttempt != null) {
        val lockedBag = bagLockedEditAttempt!!
        AlertDialog(
            onDismissRequest = { bagLockedEditAttempt = null },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.block_fill0),
                    contentDescription = null,
                    tint = colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Bag is Running",
                    style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "'${lockedBag.name}' is currently active and locked. Adding apps, removing apps, or changing the activation mode is protected. Please unlock or disable this bag in the Key tab first using its key.",
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        bagLockedEditAttempt = null
                        selectedBagForSheet = null
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Go to Key Tab")
                }
            },
            dismissButton = {
                TextButton(onClick = { bagLockedEditAttempt = null }) {
                    Text("Dismiss")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
        )
    }

    // --- LOCK BAG DIALOG ---
    if (bagToLock != null) {
        val bag = bagToLock!!
        val savedKeys by keyViewModel.savedKeys.collectAsState()
        var selectedKeyType by remember { mutableStateOf(KeyType.PASSWORD) }
        var password by remember { mutableStateOf("") }
        var qrMode by remember { mutableStateOf(0) } // 0 = Random QR, 1 = Scan External QR, 2 = Saved Key
        val generatedQrToken = remember { UUID.randomUUID().toString() }
        var scannedQrToken by remember { mutableStateOf<String?>(null) }
        var selectedSavedKey by remember { mutableStateOf<SavedKey?>(null) }
        var showScannerDialog by remember { mutableStateOf(false) }

        if (qrMode == 2 && selectedSavedKey == null && savedKeys.isNotEmpty()) {
            selectedSavedKey = savedKeys.first()
        }

        val activeQrToken = if (qrMode == 0) generatedQrToken else if (qrMode == 1) scannedQrToken else selectedSavedKey?.token
        val qrBitmap = remember(activeQrToken) {
            if (activeQrToken != null) {
                try {
                    QrUtils.encodeToBitmap(activeQrToken, 320)
                } catch (e: Exception) {
                    null
                }
            } else null
        }

        AlertDialog(
            onDismissRequest = { bagToLock = null },
            title = {
                Text(
                    text = "Lock '${bag.name}'",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Choose a lock key to suspend apps in this bag:",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = { selectedKeyType = KeyType.PASSWORD },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selectedKeyType == KeyType.PASSWORD) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.password_fill0),
                                    contentDescription = null,
                                    tint = if (selectedKeyType == KeyType.PASSWORD) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Password",
                                    style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (selectedKeyType == KeyType.PASSWORD) colorScheme.onPrimaryContainer else colorScheme.onSurface
                                )
                            }
                        }

                        Surface(
                            onClick = { selectedKeyType = KeyType.QR_CODE },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selectedKeyType == KeyType.QR_CODE) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.qr_code_fill0),
                                    contentDescription = null,
                                    tint = if (selectedKeyType == KeyType.QR_CODE) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "QR Key",
                                    style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (selectedKeyType == KeyType.QR_CODE) colorScheme.onPrimaryContainer else colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (selectedKeyType == KeyType.PASSWORD) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("Enter secret password") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colorScheme.surfaceContainerLowest)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                onClick = { qrMode = 0 },
                                shape = RoundedCornerShape(10.dp),
                                color = if (qrMode == 0) colorScheme.primaryContainer else Color.Transparent,
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Random QR",
                                        style = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (qrMode == 0) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                onClick = { qrMode = 1 },
                                shape = RoundedCornerShape(10.dp),
                                color = if (qrMode == 1) colorScheme.primaryContainer else Color.Transparent,
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Scan Physical",
                                        style = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (qrMode == 1) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                onClick = { qrMode = 2 },
                                shape = RoundedCornerShape(10.dp),
                                color = if (qrMode == 2) colorScheme.primaryContainer else Color.Transparent,
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Saved Key",
                                        style = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (qrMode == 2) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        if (qrMode == 0) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (qrBitmap != null) {
                                    Image(
                                        bitmap = qrBitmap.asImageBitmap(),
                                        contentDescription = "QR Code Key",
                                        modifier = Modifier
                                            .size(160.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(android.graphics.Color.WHITE.let { Color(it) })
                                            .padding(6.dp)
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    text = "Save or copy this random QR key to unlock later.",
                                    style = typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                TextButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Brik QR Token", generatedQrToken))
                                    Toast.makeText(context, "Token copied!", Toast.LENGTH_SHORT).show()
                                }) {
                                    Text("Copy Token Text", style = typography.labelSmall)
                                }
                            }
                        } else if (qrMode == 1) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (scannedQrToken == null) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = colorScheme.surfaceContainerLowest,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.qr_code_fill0),
                                                contentDescription = null,
                                                tint = colorScheme.primary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                text = "Scan Physical QR Code",
                                                style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Scan any QR code from a book, card, or packaging to use as lock key.",
                                                style = typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                            )
                                            Button(
                                                onClick = { showScannerDialog = true },
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text("Start Camera Scanner")
                                            }
                                        }
                                    }
                                } else {
                                    if (qrBitmap != null) {
                                        Image(
                                            bitmap = qrBitmap.asImageBitmap(),
                                            contentDescription = "Scanned Physical QR",
                                            modifier = Modifier
                                                .size(150.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(android.graphics.Color.WHITE.let { Color(it) })
                                                .padding(6.dp)
                                        )
                                    }

                                    Spacer(Modifier.height(6.dp))

                                    Text(
                                        text = scannedQrToken!!,
                                        style = typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                        color = colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(Modifier.height(6.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { showScannerDialog = true },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Scan Again", style = typography.labelSmall)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Brik QR Token", scannedQrToken))
                                                Toast.makeText(context, "Key copied!", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Copy Text", style = typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        } else {
                            if (savedKeys.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = colorScheme.surfaceContainerLowest,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "No Saved Keys",
                                            style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Go to Keys & Analytics and tap '+' to scan and save your physical keys first.",
                                            style = typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Select a saved key for quick locking:",
                                        style = typography.labelSmall,
                                        color = colorScheme.onSurfaceVariant
                                    )

                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(savedKeys, key = { it.id }) { key ->
                                            val isSelected = selectedSavedKey?.id == key.id
                                            Surface(
                                                onClick = { selectedSavedKey = key },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.qr_code_fill0),
                                                        contentDescription = null,
                                                        tint = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(Modifier.width(10.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = key.name,
                                                            style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                            color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        if (key.description.isNotBlank()) {
                                                            Text(
                                                                text = key.description,
                                                                style = typography.bodySmall.copy(fontSize = 11.sp),
                                                                color = if (isSelected) colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else colorScheme.onSurfaceVariant,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                    if (isSelected) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.check_fill0),
                                                            contentDescription = "Selected",
                                                            tint = colorScheme.onPrimaryContainer,
                                                            modifier = Modifier.size(18.dp)
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
            },
            confirmButton = {
                val isFormValid = if (selectedKeyType == KeyType.PASSWORD) {
                    password.isNotBlank()
                } else {
                    if (qrMode == 0) true else if (qrMode == 1) scannedQrToken != null else selectedSavedKey != null
                }

                Button(
                    onClick = {
                        val secret = if (selectedKeyType == KeyType.PASSWORD) {
                            password
                        } else {
                            if (qrMode == 0) generatedQrToken else if (qrMode == 1) scannedQrToken!! else selectedSavedKey!!.token
                        }
                        keyViewModel.lockBag(context, bag, selectedKeyType, secret) { success, err ->
                            if (success) {
                                Toast.makeText(context, "'${bag.name}' locked and suspended!", Toast.LENGTH_SHORT).show()
                                bagToLock = null
                            } else {
                                Toast.makeText(context, err ?: "Failed to lock", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = isFormValid,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (selectedKeyType == KeyType.QR_CODE) "Lock Bag with QR" else "Lock Bag")
                }
            },
            dismissButton = {
                TextButton(onClick = { bagToLock = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
        )

        // Nested Scanner Dialog for Scanning Any External QR
        if (showScannerDialog) {
            AlertDialog(
                onDismissRequest = { showScannerDialog = false },
                title = {
                    Text("Scan Any QR Code", fontWeight = FontWeight.Bold)
                },
                text = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        QrScannerView(
                            onQrDetected = { detected ->
                                scannedQrToken = detected
                                showScannerDialog = false
                                Toast.makeText(context, "QR code scanned!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showScannerDialog = false }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

/**
 * Minimal swipeable card for a Bag.
 * Displays only name and top 3 apps at max.
 * Swiping from left or right reveals red background with delete bin icon and triggers confirmation dialog.
 */
@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableBagItemCard(
    bag: AppBag,
    isLocked: Boolean,
    viewModel: ToolsViewModel,
    onClick: () -> Unit,
    onSwipeDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.StartToEnd || dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onSwipeDelete()
                false
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) {
                Alignment.CenterStart
            } else {
                Alignment.CenterEnd
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colorScheme.error)
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment
            ) {
                Icon(
                    painter = painterResource(R.drawable.delete_fill0),
                    contentDescription = "Delete",
                    tint = colorScheme.onError,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isLocked) colorScheme.errorContainer else colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(if (isLocked) R.drawable.block_fill0 else R.drawable.backpack_fill0),
                                    contentDescription = null,
                                    tint = if (isLocked) colorScheme.onErrorContainer else colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = bag.name,
                                    style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isLocked) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = colorScheme.error
                                    ) {
                                        Text(
                                            text = "LOCKED",
                                            style = typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 9.sp),
                                            color = colorScheme.onError,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    val isEmergencyActive by EmergencyManager.isEmergencyModeActive.collectAsState()
                                    if (isEmergencyActive) {
                                        Spacer(Modifier.width(6.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = colorScheme.errorContainer
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.shield_fill0),
                                                    contentDescription = null,
                                                    tint = colorScheme.onErrorContainer,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Spacer(Modifier.width(3.dp))
                                                Text(
                                                    text = "EMERGENCY",
                                                    style = typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 9.sp),
                                                    color = colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Text(
                                text = "${bag.packageNames.size} apps bundled • ${if (bag.actionType == BagAction.HIDE) "Hide" else "Suspend"}",
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Top 3 apps at max
                val top3Packages = bag.packageNames.take(3).toList()
                if (top3Packages.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        top3Packages.forEach { pkg ->
                            val appInfo = viewModel.getAppInfo(pkg)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colorScheme.surfaceContainerLowest,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (appInfo?.icon != null) {
                                        Image(
                                            painter = rememberDrawablePainter(drawable = appInfo.icon),
                                            contentDescription = appInfo.label,
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(R.drawable.apps_fill0),
                                            contentDescription = null,
                                            tint = colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (bag.packageNames.size > 3) {
                            val remaining = bag.packageNames.size - 3
                            Surface(
                                shape = CircleShape,
                                color = colorScheme.surfaceContainerLowest,
                                modifier = Modifier.height(28.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = "+$remaining more",
                                        style = typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = colorScheme.onSurfaceVariant
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


/**
 * Bottom Sheet Wizard to create bags:
 * Step 1: Apps selection with search & select all/clear.
 * Step 2: Name bags prompt with suggestions & save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBagWizardSheet(
    installedApps: List<AppInfo>,
    onDismiss: () -> Unit,
    onSaveBag: (name: String, selectedPackages: Set<String>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentStep by remember { mutableIntStateOf(0) }
    var bagName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPackages by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showSystemApps by remember { mutableStateOf(false) }

    val filteredApps = remember(installedApps, searchQuery, showSystemApps) {
        val baseList = if (showSystemApps) installedApps else installedApps.filter { !it.isSystemApp }
        if (searchQuery.isBlank()) {
            baseList
        } else {
            val q = searchQuery.trim().lowercase()
            baseList.filter {
                it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 16.dp)
        ) {
            if (currentStep == 0) {
                // STEP 1: Apps Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Select Apps",
                            style = typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Step 1 of 2 • Choose apps for this bundle",
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

                // Search & Filter Row
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
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        shape = CircleShape,
                        color = if (showSystemApps) colorScheme.primaryContainer else colorScheme.surfaceContainerHighest,
                        border = BorderStroke(1.dp, if (showSystemApps) colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent),
                        modifier = Modifier.size(42.dp),
                        onClick = { showSystemApps = !showSystemApps }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(if (showSystemApps) R.drawable.visibility_fill0 else R.drawable.visibility_off_fill0),
                                contentDescription = if (showSystemApps) "Hide System Apps" else "Show System Apps",
                                tint = if (showSystemApps) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (selectedPackages.isNotEmpty()) {
                        TextButton(onClick = { selectedPackages = emptySet() }) {
                            Text("Clear", color = colorScheme.error, style = typography.labelMedium)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Apps List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val isChecked = selectedPackages.contains(app.packageName)
                        Surface(
                            onClick = {
                                selectedPackages = if (isChecked) selectedPackages - app.packageName else selectedPackages + app.packageName
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
                                        selectedPackages = if (isChecked) selectedPackages - app.packageName else selectedPackages + app.packageName
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = { currentStep = 1 },
                    enabled = selectedPackages.isNotEmpty(),
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
                        text = if (selectedPackages.isEmpty()) "Select apps to continue" else "Next (${selectedPackages.size} apps)",
                        style = typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            } else {
                // STEP 2: Name Bags Prompt
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.secondaryContainer,
                        modifier = Modifier.size(36.dp),
                        onClick = { currentStep = 0 }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back_fill0),
                                contentDescription = "Back to Apps",
                                tint = colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Name your Bag",
                            style = typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Step 2 of 2 • ${selectedPackages.size} apps selected",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = bagName,
                    onValueChange = { bagName = it },
                    label = { Text("Bag Name") },
                    placeholder = { Text("e.g. Socials, Study, Games") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Suggestions",
                    style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))

                val suggestions = listOf("Socials", "Games", "Study", "Work", "Entertainment", "News")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(suggestions) { suggestion ->
                        Surface(
                            onClick = { bagName = suggestion },
                            shape = RoundedCornerShape(10.dp),
                            color = if (bagName == suggestion) colorScheme.primaryContainer else colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = suggestion,
                                style = typography.labelSmall.copy(
                                    fontWeight = if (bagName == suggestion) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (bagName == suggestion) colorScheme.onPrimaryContainer else colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

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
                        text = "Save Bag",
                        style = typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
