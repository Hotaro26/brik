package com.hotaro.brik.ui.screen

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.hotaro.brik.R
import com.hotaro.brik.policy.BridgeType
import com.hotaro.brik.ui.viewmodel.SettingsViewModel
import com.hotaro.brik.ui.viewmodel.ToolsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = viewModel(),
    toolsViewModel: ToolsViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()

    val bridgeStatus by settingsViewModel.bridgeStatus.collectAsState()
    val suspendedPackages by settingsViewModel.suspendedPackages.collectAsState()
    val hiddenPackages by settingsViewModel.hiddenPackages.collectAsState()
    val isKioskActive by settingsViewModel.isKioskActive.collectAsState()
    val installedApps by toolsViewModel.installedApps.collectAsState()
    val bags by toolsViewModel.bags.collectAsState()

    var showBlockSheet by remember { mutableStateOf(false) }
    var showHideSheet by remember { mutableStateOf(false) }
    var showKioskSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        settingsViewModel.refreshStatus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Title & Active Bridge Banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Settings",
                    style = typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface
                )
                Text(
                    text = "Privileged Bridges & Enforcement",
                    style = typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = CircleShape,
                color = if (bridgeStatus.activeBridge != BridgeType.NONE) colorScheme.primaryContainer else colorScheme.surfaceContainerHighest
            ) {
                Text(
                    text = when (bridgeStatus.activeBridge) {
                        BridgeType.DEVICE_OWNER -> "Device Owner"
                        BridgeType.DHIZUKU -> "Dhizuku"
                        BridgeType.SHIZUKU -> "Shizuku"
                        BridgeType.ROOT -> "Root"
                        BridgeType.NONE -> "Unprivileged"
                    },
                    style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (bridgeStatus.activeBridge != BridgeType.NONE) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // --- SECTION 1: PRIVILEGED BRIDGES ---
        Text(
            text = "Privileged Bridges",
            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = colorScheme.primary
        )

        // Device Owner Card
        ElevatedCard(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (bridgeStatus.isDeviceOwner) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.shield_fill0),
                                contentDescription = null,
                                tint = if (bridgeStatus.isDeviceOwner) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Device Owner (DPM)",
                            style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = if (bridgeStatus.isDeviceOwner) "Active & Full Control" else "Not Provisioned",
                            style = typography.bodySmall,
                            color = if (bridgeStatus.isDeviceOwner) colorScheme.primary else colorScheme.error
                        )
                    }
                }

                if (!bridgeStatus.isDeviceOwner) {
                    Spacer(Modifier.height(12.dp))
                    val dpmCmd = "adb shell dpm set-device-owner com.hotaro.brik/.admin.BrikAdminReceiver"
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colorScheme.surfaceContainerLowest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dpmCmd,
                                style = typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", dpmCmd))
                                Toast.makeText(context, "Command copied!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.content_copy_fill0),
                                    contentDescription = "Copy",
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Dhizuku Card
        ElevatedCard(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (bridgeStatus.isDhizukuGranted) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.key_fill0),
                            contentDescription = null,
                            tint = if (bridgeStatus.isDhizukuGranted) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Dhizuku Bridge",
                        style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = if (bridgeStatus.isDhizukuGranted) "Granted & Operational" else if (bridgeStatus.isDhizukuInstalled) "Permission Needed" else "Not Installed",
                        style = typography.bodySmall,
                        color = if (bridgeStatus.isDhizukuGranted) colorScheme.primary else colorScheme.onSurfaceVariant
                    )
                }

                if (!bridgeStatus.isDhizukuGranted && bridgeStatus.isDhizukuInstalled) {
                    FilledTonalButton(onClick = { settingsViewModel.requestDhizukuPermission() }) {
                        Text("Grant")
                    }
                }
            }
        }

        // Shizuku Card
        ElevatedCard(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (bridgeStatus.isShizukuGranted) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.backpack_fill0),
                            contentDescription = null,
                            tint = if (bridgeStatus.isShizukuGranted) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Shizuku Bridge",
                        style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = if (bridgeStatus.isShizukuGranted) "Granted & Running" else if (bridgeStatus.isShizukuRunning) "Permission Needed" else "Service Stopped",
                        style = typography.bodySmall,
                        color = if (bridgeStatus.isShizukuGranted) colorScheme.primary else colorScheme.onSurfaceVariant
                    )
                }

                if (bridgeStatus.isShizukuRunning && !bridgeStatus.isShizukuGranted) {
                    FilledTonalButton(onClick = { settingsViewModel.requestShizukuPermission() }) {
                        Text("Grant")
                    }
                }
            }
        }

        // --- SECTION 2: NON-NEGOTIABLES ENFORCEMENT ---
        Text(
            text = "Policy Enforcement (Core)",
            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = colorScheme.primary
        )

        // 1. App Blocking (Suspension)
        ElevatedCard(
            onClick = { showBlockSheet = true },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.errorContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.block_fill0),
                            contentDescription = null,
                            tint = colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "App Blocking (Suspension)",
                        style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = if (suspendedPackages.isNotEmpty()) "${suspendedPackages.size} packages suspended" else "Suspend distracting apps or entire bags",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 2. App Hiding
        ElevatedCard(
            onClick = { showHideSheet = true },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.tertiaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.visibility_off_fill0),
                            contentDescription = null,
                            tint = colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "App Hiding",
                        style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = if (hiddenPackages.isNotEmpty()) "${hiddenPackages.size} apps hidden" else "Completely hide packages from system launcher",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 3. Lock / Kiosk Mode
        ElevatedCard(
            onClick = { showKioskSheet = true },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.secondaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.screen_lock_portrait_fill0),
                            contentDescription = null,
                            tint = colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Lock / Kiosk Mode",
                        style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = if (isKioskActive) "Kiosk Active" else "Pin app into single-task kiosk mode",
                        style = typography.bodySmall,
                        color = if (isKioskActive) colorScheme.primary else colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }

    // --- APP BLOCKING SHEET ---
    if (showBlockSheet) {
        val blockSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showBlockSheet = false },
            sheetState = blockSheetState,
            containerColor = colorScheme.surfaceContainerLowest,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "App Blocking",
                            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Suspend bags or individual apps",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showBlockSheet = false }) {
                        Icon(
                            painter = painterResource(R.drawable.close_fill0),
                            contentDescription = "Close",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Bags Quick-Action
                if (bags.isNotEmpty()) {
                    Text(
                        text = "Suspend by Bag",
                        style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    bags.forEach { bag ->
                        val isBagSuspended = bag.packageNames.isNotEmpty() && bag.packageNames.all { suspendedPackages.contains(it) }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = bag.name,
                                        style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${bag.packageNames.size} apps",
                                        style = typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }

                                Switch(
                                    checked = isBagSuspended,
                                    onCheckedChange = { suspend ->
                                        settingsViewModel.suspendPackages(
                                            bag.packageNames.toList(),
                                            suspend
                                        ) { ok, err ->
                                            if (!ok) {
                                                Toast.makeText(context, err ?: "Operation failed", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                Text(
                    text = "Individual Apps",
                    style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(installedApps, key = { it.packageName }) { app ->
                        val isSuspended = suspendedPackages.contains(app.packageName)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = rememberDrawablePainter(app.icon),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        style = typography.titleSmall,
                                        color = colorScheme.onSurface
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = typography.bodySmall.copy(fontSize = 11.sp),
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isSuspended,
                                    onCheckedChange = { suspend ->
                                        settingsViewModel.suspendPackages(
                                            listOf(app.packageName),
                                            suspend
                                        ) { ok, err ->
                                            if (!ok) {
                                                Toast.makeText(context, err ?: "Operation failed", Toast.LENGTH_SHORT).show()
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
    }

    // --- APP HIDING SHEET ---
    if (showHideSheet) {
        val hideSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showHideSheet = false },
            sheetState = hideSheetState,
            containerColor = colorScheme.surfaceContainerLowest,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "App Hiding",
                            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Hide apps completely from the system",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showHideSheet = false }) {
                        Icon(
                            painter = painterResource(R.drawable.close_fill0),
                            contentDescription = "Close",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(installedApps, key = { it.packageName }) { app ->
                        val isHidden = hiddenPackages.contains(app.packageName)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = rememberDrawablePainter(app.icon),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        style = typography.titleSmall,
                                        color = colorScheme.onSurface
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = typography.bodySmall.copy(fontSize = 11.sp),
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isHidden,
                                    onCheckedChange = { hide ->
                                        settingsViewModel.toggleAppHidden(
                                            app.packageName,
                                            hide
                                        ) { ok, err ->
                                            if (!ok) {
                                                Toast.makeText(context, err ?: "Operation failed", Toast.LENGTH_SHORT).show()
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
    }

    // --- LOCK / KIOSK SHEET ---
    if (showKioskSheet) {
        val kioskSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showKioskSheet = false },
            sheetState = kioskSheetState,
            containerColor = colorScheme.surfaceContainerLowest,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Lock / Kiosk Mode",
                            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Single-task kiosk mode controls",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showKioskSheet = false }) {
                        Icon(
                            painter = painterResource(R.drawable.close_fill0),
                            contentDescription = "Close",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "When Kiosk Mode is activated, the device is locked strictly into Brik or allowed apps, preventing home/back button exit without authorization.",
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(24.dp))

                if (isKioskActive) {
                    Button(
                        onClick = {
                            if (activity != null) {
                                val success = settingsViewModel.stopKioskMode(activity)
                                if (success) {
                                    Toast.makeText(context, "Kiosk Mode Stopped", Toast.LENGTH_SHORT).show()
                                    showKioskSheet = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("Exit Kiosk Mode")
                    }
                } else {
                    Button(
                        onClick = {
                            if (activity != null) {
                                val success = settingsViewModel.startKioskMode(activity)
                                if (success) {
                                    Toast.makeText(context, "Kiosk Mode Started", Toast.LENGTH_SHORT).show()
                                    showKioskSheet = false
                                } else {
                                    Toast.makeText(context, "Could not start kiosk. Ensure Device Owner or Dhizuku is active.", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("Start Kiosk Mode")
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
