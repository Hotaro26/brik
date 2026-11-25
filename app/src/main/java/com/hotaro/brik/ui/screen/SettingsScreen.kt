package com.hotaro.brik.ui.screen

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
import com.hotaro.brik.data.model.KeyType
import com.hotaro.brik.policy.BridgeType
import com.hotaro.brik.ui.component.QrScannerView
import com.hotaro.brik.ui.component.RoundedPullRefreshPage
import androidx.compose.runtime.mutableIntStateOf
import com.hotaro.brik.ui.component.BridgeSetupSheet
import com.hotaro.brik.ui.component.TomatoSectionHeader
import com.hotaro.brik.ui.component.TomatoSettingsItem
import com.hotaro.brik.ui.component.segmentedItemShape
import com.hotaro.brik.ui.theme.ThemePreferences
import com.hotaro.brik.ui.theme.NavBarStyle
import com.hotaro.brik.ui.viewmodel.SettingsViewModel
import com.hotaro.brik.ui.viewmodel.ToolsViewModel
import com.hotaro.brik.util.AppIconManager
import com.hotaro.brik.util.EmergencyManager
import com.hotaro.brik.util.QrUtils
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToKeys: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToEmergency: () -> Unit = {},
    settingsViewModel: SettingsViewModel = viewModel(),
    toolsViewModel: ToolsViewModel = viewModel(),
    keyViewModel: com.hotaro.brik.ui.viewmodel.KeyViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()

    val appVersion = remember(context) {
        try {
            val pInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val raw = pInfo.versionName ?: "1.0.3"
            if (raw.startsWith("v", ignoreCase = true)) raw else "v$raw"
        } catch (e: Exception) {
            "v1.0.3"
        }
    }

    val bridgeStatus by settingsViewModel.bridgeStatus.collectAsState()
    val suspendedPackages by settingsViewModel.suspendedPackages.collectAsState()
    val hiddenPackages by settingsViewModel.hiddenPackages.collectAsState()
    val isKioskActive by settingsViewModel.isKioskActive.collectAsState()
    val installedApps by toolsViewModel.installedApps.collectAsState()
    val bags by toolsViewModel.bags.collectAsState()
    val activeLocks by keyViewModel.activeLocks.collectAsState()
    val stats by keyViewModel.stats.collectAsState()

    var showBlockSheet by remember { mutableStateOf(false) }
    var showHideSheet by remember { mutableStateOf(false) }
    var showKioskSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showPaletteSheet by remember { mutableStateOf(false) }
    var showNavBarSheet by remember { mutableStateOf(false) }
    var showIconSheet by remember { mutableStateOf(false) }
    var showSetUninstallKeyDialog by remember { mutableStateOf(false) }
    var showVerifyUninstallKeyDialog by remember { mutableStateOf(false) }
    var showBridgeSetupSheet by remember { mutableStateOf(false) }
    var bridgeSetupInitialTab by remember { mutableIntStateOf(0) }
    var isDeviceOwnerExpanded by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        settingsViewModel.refreshStatus()
    }

    RoundedPullRefreshPage(
        isRefreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                settingsViewModel.refreshStatus()
                toolsViewModel.loadInstalledApps()
                kotlinx.coroutines.delay(650)
                isRefreshing = false
            }
        },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Settings",
                        style = typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = colorScheme.onSurface
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
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

        // --- SECTION: PRIVILEGED BRIDGES & KEYS (CONJOINED) ---
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            TomatoSectionHeader(title = "SECURITY & BRIDGES")

            val isDeviceOwner = bridgeStatus.isDeviceOwner
            val totalItems = if (isDeviceOwner) 2 else 4

            val chevronRotation by animateFloatAsState(
                targetValue = if (isDeviceOwner && isDeviceOwnerExpanded) 90f else 0f,
                label = "dpmChevron"
            )

            // 1. Device Owner (DPM)
            TomatoSettingsItem(
                title = "Device Owner (DPM)",
                subtitle = if (isDeviceOwner) {
                    if (isDeviceOwnerExpanded) "Tap to collapse" else "Active • Tap to manage or deactivate"
                } else {
                    "Not Provisioned (Tap to set up)"
                },
                leadingIcon = R.drawable.shield_fill0,
                iconContainerColor = if (isDeviceOwner) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                iconTint = if (isDeviceOwner) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                trailingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDeviceOwner) colorScheme.primaryContainer else colorScheme.errorContainer
                        ) {
                            Text(
                                text = if (isDeviceOwner) "Active" else "Not Set",
                                style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isDeviceOwner) colorScheme.onPrimaryContainer else colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        if (isDeviceOwner) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_forward_big),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .graphicsLayer { rotationZ = chevronRotation },
                                tint = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                shape = if (isDeviceOwner && isDeviceOwnerExpanded) {
                    RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                } else {
                    segmentedItemShape(0, totalItems)
                },
                onClick = {
                    if (isDeviceOwner) {
                        isDeviceOwnerExpanded = !isDeviceOwnerExpanded
                    } else {
                        bridgeSetupInitialTab = 0
                        showBridgeSetupSheet = true
                    }
                }
            )

            // Morphic slide out of Deactivate Device Owner card
            AnimatedVisibility(
                visible = isDeviceOwner && isDeviceOwnerExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 2.dp)) {
                    TomatoSettingsItem(
                        title = "Deactivate Device Owner",
                        subtitle = "Requires security key verification to allow uninstalling Brik",
                        leadingIcon = R.drawable.delete_fill0,
                        iconContainerColor = colorScheme.errorContainer,
                        iconTint = colorScheme.onErrorContainer,
                        trailingContent = {
                            Surface(
                                shape = CircleShape,
                                color = colorScheme.errorContainer
                            ) {
                                Text(
                                    text = "Revoke",
                                    style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        },
                        shape = RoundedCornerShape(4.dp),
                        onClick = {
                            val existing = keyViewModel.getDpmUninstallLock()
                            if (existing == null) {
                                showSetUninstallKeyDialog = true
                            } else {
                                showVerifyUninstallKeyDialog = true
                            }
                        }
                    )

                    Spacer(Modifier.height(2.dp))

                    val isEmergencyActive by EmergencyManager.isEmergencyModeActive.collectAsState()
                    TomatoSettingsItem(
                        title = "Emergency Mode",
                        subtitle = if (isEmergencyActive) "Active • Overrides and saved keys revealed" else "Inactive • Pull down 3x in Keys to trigger",
                        leadingIcon = R.drawable.shield_fill0,
                        iconContainerColor = if (isEmergencyActive) colorScheme.errorContainer else colorScheme.secondaryContainer,
                        iconTint = if (isEmergencyActive) colorScheme.onErrorContainer else colorScheme.onSecondaryContainer,
                        trailingContent = {
                            Surface(
                                shape = CircleShape,
                                color = if (isEmergencyActive) colorScheme.errorContainer else colorScheme.surfaceContainerHighest
                            ) {
                                Text(
                                    text = if (isEmergencyActive) "Active" else "Configure",
                                    style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isEmergencyActive) colorScheme.onErrorContainer else colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        },
                        shape = RoundedCornerShape(4.dp),
                        onClick = onNavigateToEmergency
                    )
                }
            }

            // If DPM is not active, display Dhizuku and Shizuku
            if (!isDeviceOwner) {
                // 2. Dhizuku Bridge
                TomatoSettingsItem(
                    title = "Dhizuku Bridge",
                    subtitle = if (bridgeStatus.isDhizukuGranted) "Granted & Operational" else if (bridgeStatus.isDhizukuInstalled) "Permission Needed" else "Not Installed",
                    leadingIcon = R.drawable.key_fill0,
                    iconContainerColor = if (bridgeStatus.isDhizukuGranted) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                    iconTint = if (bridgeStatus.isDhizukuGranted) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                    trailingContent = {
                        if (!bridgeStatus.isDhizukuGranted && bridgeStatus.isDhizukuInstalled) {
                            FilledTonalButton(
                                onClick = { settingsViewModel.requestDhizukuPermission() },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Grant", style = typography.labelSmall)
                            }
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = if (bridgeStatus.isDhizukuGranted) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest
                            ) {
                                Text(
                                    text = if (bridgeStatus.isDhizukuGranted) "Granted" else "Unavailable",
                                    style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (bridgeStatus.isDhizukuGranted) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    },
                    shape = segmentedItemShape(1, totalItems),
                    onClick = {
                        bridgeSetupInitialTab = 2
                        showBridgeSetupSheet = true
                    }
                )

                // 3. Shizuku Bridge
                TomatoSettingsItem(
                    title = "Shizuku Bridge",
                    subtitle = if (bridgeStatus.isShizukuGranted) "Granted & Running" else if (bridgeStatus.isShizukuRunning) "Permission Needed" else "Service Stopped",
                    leadingIcon = R.drawable.backpack_fill0,
                    iconContainerColor = if (bridgeStatus.isShizukuGranted) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                    iconTint = if (bridgeStatus.isShizukuGranted) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                    trailingContent = {
                        if (bridgeStatus.isShizukuRunning && !bridgeStatus.isShizukuGranted) {
                            FilledTonalButton(
                                onClick = { settingsViewModel.requestShizukuPermission() },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Grant", style = typography.labelSmall)
                            }
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = if (bridgeStatus.isShizukuGranted) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest
                            ) {
                                Text(
                                    text = if (bridgeStatus.isShizukuGranted) "Running" else "Stopped",
                                    style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (bridgeStatus.isShizukuGranted) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    },
                    shape = segmentedItemShape(2, totalItems),
                    onClick = {
                        bridgeSetupInitialTab = 1
                        showBridgeSetupSheet = true
                    }
                )
            }

            // 4. Keys & Lock Analytics
            val keysIndex = if (isDeviceOwner) 1 else 3
            TomatoSettingsItem(
                title = "Keys & Lock Analytics",
                subtitle = "${activeLocks.size} active bag keys • ${stats.totalLocks} sessions",
                leadingIcon = R.drawable.key_fill0,
                iconContainerColor = colorScheme.primaryContainer,
                iconTint = colorScheme.onPrimaryContainer,
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.arrow_forward_big),
                        contentDescription = "Open Keys",
                        modifier = Modifier.size(18.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                },
                shape = segmentedItemShape(keysIndex, totalItems),
                onClick = onNavigateToKeys
            )
        }

        // --- SECTION: APPEARANCE ---
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            TomatoSectionHeader(title = "APPEARANCE")

            val currentThemeMode by ThemePreferences.themeMode.collectAsState()
            val currentPalette by ThemePreferences.colorPalette.collectAsState()
            val currentAppIcon by ThemePreferences.appIcon.collectAsState()
            val currentNavBarStyle by ThemePreferences.navBarStyle.collectAsState()

            val currentIconOption = AppIconManager.icons.find { it.id == currentAppIcon } ?: AppIconManager.icons.first()

            val themeSubtitle = when (currentThemeMode) {
                1 -> "Light"
                2 -> "Dark (AMOLED)"
                else -> "System Default"
            }

            val paletteSubtitle = when (currentPalette) {
                "dynamic" -> "Dynamic (Material You)"
                "brik" -> "Brik Terracotta"
                "classic" -> "Classic Green"
                "cherry" -> "Cherry Red"
                "navy" -> "Navy Blue"
                "lavender" -> "Lavender"
                "pink" -> "Pink"
                "mocha" -> "Mocha"
                "catppuccin" -> "Catppuccin"
                "monochrome" -> "Monochrome"
                else -> "Dynamic (Material You)"
            }

            val navBarSubtitle = if (currentNavBarStyle == NavBarStyle.FLOATING) {
                "Floating (Default)"
            } else {
                "Classic Sticky"
            }

            // 1. Theme Mode
            TomatoSettingsItem(
                title = "Theme Mode",
                subtitle = themeSubtitle,
                leadingIcon = R.drawable.dark_mode_fill0,
                iconContainerColor = colorScheme.secondaryContainer,
                iconTint = colorScheme.onSecondaryContainer,
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.arrow_forward_big),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                },
                shape = segmentedItemShape(0, 4),
                onClick = { showThemeSheet = true }
            )

            // 2. Colour Scheme
            TomatoSettingsItem(
                title = "Colour Scheme",
                subtitle = paletteSubtitle,
                leadingIcon = R.drawable.palette_fill0,
                iconContainerColor = colorScheme.tertiaryContainer,
                iconTint = colorScheme.onTertiaryContainer,
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.arrow_forward_big),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                },
                shape = segmentedItemShape(1, 4),
                onClick = { showPaletteSheet = true }
            )

            // 3. Icons
            TomatoSettingsItem(
                title = "Icons",
                subtitle = currentIconOption.name,
                leadingIcon = R.drawable.apps_fill0,
                iconContainerColor = colorScheme.secondaryContainer,
                iconTint = colorScheme.onSecondaryContainer,
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.arrow_forward_big),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                },
                shape = segmentedItemShape(2, 4),
                onClick = { showIconSheet = true }
            )

            // 4. Navbar
            TomatoSettingsItem(
                title = "Navbar",
                subtitle = navBarSubtitle,
                leadingIcon = R.drawable.dock_bottom_fill0,
                iconContainerColor = colorScheme.primaryContainer,
                iconTint = colorScheme.onPrimaryContainer,
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.arrow_forward_big),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                },
                shape = segmentedItemShape(3, 4),
                onClick = { showNavBarSheet = true }
            )
        }

        // --- SECTION 3: ABOUT ---
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            TomatoSectionHeader(title = "ABOUT")

            TomatoSettingsItem(
                title = "About Brik",
                subtitle = "$appVersion • App info, GitHub repo & privacy policy",
                leadingIcon = R.drawable.ic_brik_monochrome,
                iconTint = colorScheme.primary,
                iconContainerColor = colorScheme.primaryContainer,
                iconSize = 28.dp,
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.arrow_forward_big),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = colorScheme.onSurfaceVariant
                    )
                },
                shape = segmentedItemShape(0, 1),
                onClick = onNavigateToAbout
            )
        }

        Spacer(Modifier.height(160.dp))
    }
}

    // --- Step 1: Set Uninstall Key Dialog ---
    if (showSetUninstallKeyDialog) {
        var selectedKeyType by remember { mutableStateOf(KeyType.PASSWORD) }
        var password by remember { mutableStateOf("") }
        var qrMode by remember { mutableStateOf(0) } // 0 = Random, 1 = Scan External
        val generatedQrToken = remember { UUID.randomUUID().toString() }
        var scannedQrToken by remember { mutableStateOf<String?>(null) }
        var showScannerDialog by remember { mutableStateOf(false) }

        val activeQrToken = if (qrMode == 0) generatedQrToken else scannedQrToken
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
            onDismissRequest = { showSetUninstallKeyDialog = false },
            title = {
                Text(
                    text = "Set Uninstall Security Key",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "To revoke Device Owner and uninstall Brik, you must set either a Password or QR Code key. This key will be tracked in your Keys section.",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    // Key Type Toggle
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
                                .height(52.dp)
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
                                .height(52.dp)
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
                            placeholder = { Text("Enter security password") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // QR Key Sub-mode Selector
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
                                        text = "Scan Any QR",
                                        style = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (qrMode == 1) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
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
                                        contentDescription = "Uninstall QR Key",
                                        modifier = Modifier
                                            .size(180.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(android.graphics.Color.WHITE.let { Color(it) })
                                            .padding(8.dp)
                                    )
                                }

                                Spacer(Modifier.height(10.dp))

                                Text(
                                    text = "Save or copy this random QR token. You will need to scan or paste it to revoke Device Owner.",
                                    style = typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )

                                Spacer(Modifier.height(8.dp))

                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Brik QR Token", generatedQrToken))
                                        Toast.makeText(context, "QR Token copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Copy Token Text", style = typography.labelSmall)
                                }
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (scannedQrToken == null) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = colorScheme.surfaceContainerLowest,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(18.dp),
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
                                                text = "Point camera at any physical QR code you have (card, book, packaging) to use it as your key.",
                                                style = typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
                                            contentDescription = "Scanned QR Key",
                                            modifier = Modifier
                                                .size(160.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(android.graphics.Color.WHITE.let { Color(it) })
                                                .padding(8.dp)
                                        )
                                    }

                                    Spacer(Modifier.height(10.dp))

                                    Surface(
                                        shape = CircleShape,
                                        color = colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "Physical QR Captured",
                                            style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                        )
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    Text(
                                        text = scannedQrToken!!,
                                        style = typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                        color = colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(Modifier.height(8.dp))

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
                                                Toast.makeText(context, "Scanned key copied!", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Copy Text", style = typography.labelSmall)
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
                    if (qrMode == 0) true else scannedQrToken != null
                }

                Button(
                    onClick = {
                        val secret = if (selectedKeyType == KeyType.PASSWORD) {
                            password.trim()
                        } else {
                            if (qrMode == 0) generatedQrToken else scannedQrToken!!
                        }
                        keyViewModel.setDpmUninstallLock(selectedKeyType, secret)
                        Toast.makeText(context, "Security key saved in Keys section!", Toast.LENGTH_SHORT).show()
                        showSetUninstallKeyDialog = false
                        showVerifyUninstallKeyDialog = true
                    },
                    enabled = isFormValid,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Key & Proceed")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetUninstallKeyDialog = false }) {
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
                                Toast.makeText(context, "QR Code scanned successfully!", Toast.LENGTH_SHORT).show()
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

    // --- Step 2: Verify Uninstall Key Dialog ---
    if (showVerifyUninstallKeyDialog) {
        val uninstallLock = keyViewModel.getDpmUninstallLock()
        var inputSecret by remember { mutableStateOf("") }
        var showCameraScanner by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {
                showVerifyUninstallKeyDialog = false
                showCameraScanner = false
            },
            title = {
                Text(
                    text = "Verify Key to Deactivate",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.error
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (uninstallLock?.keyType == KeyType.QR_CODE) {
                            "Scan your QR code key or enter the token to revoke Device Owner and uninstall Brik."
                        } else {
                            "Enter the password you set to revoke Device Owner and uninstall Brik."
                        },
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    if (uninstallLock?.keyType == KeyType.QR_CODE) {
                        if (showCameraScanner) {
                            QrScannerView(
                                onQrDetected = { scannedToken ->
                                    keyViewModel.verifyAndDeactivateDeviceOwner(context, scannedToken) { ok, err ->
                                        if (ok) {
                                            settingsViewModel.refreshStatus()
                                            Toast.makeText(context, "Device Owner deactivated! You can now uninstall Brik.", Toast.LENGTH_LONG).show()
                                            showVerifyUninstallKeyDialog = false
                                            showCameraScanner = false
                                        } else {
                                            errorMessage = err ?: "Invalid QR key"
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                            )
                        } else {
                            Button(
                                onClick = { showCameraScanner = true },
                                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.qr_code_fill0),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Scan with Camera")
                            }

                            Spacer(Modifier.height(12.dp))

                            Text("— or paste token text —", style = typography.labelSmall, color = colorScheme.onSurfaceVariant)

                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = inputSecret,
                                onValueChange = {
                                    inputSecret = it
                                    errorMessage = null
                                },
                                placeholder = { Text("Paste QR token") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = inputSecret,
                            onValueChange = {
                                inputSecret = it
                                errorMessage = null
                            },
                            placeholder = { Text("Enter password") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (errorMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = errorMessage!!,
                            style = typography.bodySmall,
                            color = colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                if (!showCameraScanner) {
                    Button(
                        onClick = {
                            keyViewModel.verifyAndDeactivateDeviceOwner(context, inputSecret.trim()) { ok, err ->
                                if (ok) {
                                    settingsViewModel.refreshStatus()
                                    Toast.makeText(context, "Device Owner deactivated! You can now uninstall Brik.", Toast.LENGTH_LONG).show()
                                    showVerifyUninstallKeyDialog = false
                                } else {
                                    errorMessage = err ?: "Incorrect password or key"
                                }
                            }
                        },
                        enabled = inputSecret.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Verify & Deactivate")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showVerifyUninstallKeyDialog = false
                    showCameraScanner = false
                }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
        )
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

    // --- NAVBAR STYLE BOTTOM SHEET ---
    if (showNavBarSheet) {
        val navBarSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showNavBarSheet = false },
            sheetState = navBarSheetState,
            containerColor = colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            val currentNavBarStyle by ThemePreferences.navBarStyle.collectAsState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 36.dp)
            ) {
                Text(
                    text = "Navbar Style",
                    style = typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Select your preferred navigation bar style",
                    style = typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val navBarOptions = listOf(
                    Triple("Floating Navbar", NavBarStyle.FLOATING, "Modern floating pill navigation bar (Default)"),
                    Triple("Classic Sticky Navbar", NavBarStyle.CLASSIC, "Traditional bottom docked navigation bar")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    navBarOptions.forEach { (name, style, desc) ->
                        val isSelected = currentNavBarStyle == style
                        Surface(
                            onClick = {
                                ThemePreferences.setNavBarStyle(context, style)
                                coroutineScope.launch {
                                    navBarSheetState.hide()
                                    showNavBarSheet = false
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        style = typography.titleMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface
                                    )
                                    Text(
                                        text = desc,
                                        style = typography.bodySmall,
                                        color = if (isSelected) colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                painter = painterResource(R.drawable.check_fill0),
                                                contentDescription = "Selected",
                                                tint = colorScheme.onPrimary,
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

    // --- APP ICONS BOTTOM SHEET ---
    if (showIconSheet) {
        val iconSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showIconSheet = false },
            sheetState = iconSheetState,
            containerColor = colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            val currentAppIcon by ThemePreferences.appIcon.collectAsState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 36.dp)
            ) {
                Text(
                    text = "App Icon",
                    style = typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Select your favorite launcher icon aesthetic",
                    style = typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppIconManager.icons.forEach { iconOption ->
                        val isSelected = currentAppIcon == iconOption.id
                        Surface(
                            onClick = {
                                AppIconManager.setAppIcon(context, iconOption)
                                coroutineScope.launch {
                                    iconSheetState.hide()
                                    showIconSheet = false
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Themed Brick Icon Preview Box
                                val isDynamic = iconOption.id == "dynamic"
                                val boxBg = if (isDynamic) colorScheme.surfaceContainerHighest else Color(iconOption.bgColor)
                                val boxTint = if (isDynamic) colorScheme.primary else Color(iconOption.primaryColor)

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = boxBg,
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_brik_monochrome),
                                            contentDescription = null,
                                            tint = boxTint,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = iconOption.name,
                                        style = typography.titleMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface
                                    )
                                    Text(
                                        text = iconOption.subtitle,
                                        style = typography.bodySmall,
                                        color = if (isSelected) colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isSelected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                painter = painterResource(R.drawable.check_fill0),
                                                contentDescription = "Selected",
                                                tint = colorScheme.onPrimary,
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

    // --- THEME MODE BOTTOM SHEET ---
    if (showThemeSheet) {
        val themeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showThemeSheet = false },
            sheetState = themeSheetState,
            containerColor = colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            val currentThemeMode by ThemePreferences.themeMode.collectAsState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 36.dp)
            ) {
                Text(
                    text = "Theme Mode",
                    style = typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Select your preferred appearance mode",
                    style = typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val themeOptions = listOf(
                    Triple("System Default", 0, "Follow system light/dark settings"),
                    Triple("Light", 1, "Crisp light appearance"),
                    Triple("Dark", 2, "AMOLED pitch black appearance")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    themeOptions.forEach { (name, mode, desc) ->
                        val isSelected = currentThemeMode == mode
                        Surface(
                            onClick = {
                                ThemePreferences.setThemeMode(context, mode)
                                coroutineScope.launch {
                                    themeSheetState.hide()
                                    showThemeSheet = false
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        style = typography.titleMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface
                                    )
                                    Text(
                                        text = desc,
                                        style = typography.bodySmall,
                                        color = if (isSelected) colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                painter = painterResource(R.drawable.check_fill0),
                                                contentDescription = "Selected",
                                                tint = colorScheme.onPrimary,
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

    // --- COLOUR SCHEME BOTTOM SHEET ---
    if (showPaletteSheet) {
        val paletteSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPaletteSheet = false },
            sheetState = paletteSheetState,
            containerColor = colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            val currentPalette by ThemePreferences.colorPalette.collectAsState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 36.dp)
            ) {
                Text(
                    text = "Colour Scheme",
                    style = typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Personalize colors across the UI",
                    style = typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val palettes = listOf(
                    Triple("Dynamic (Material You)", "dynamic", colorScheme.primary),
                    Triple("Brik Terracotta", "brik", Color(0xFFB5492A)),
                    Triple("Classic Green", "classic", Color(0xFF006C4C)),
                    Triple("Cherry Red", "cherry", Color(0xFF904A4E)),
                    Triple("Navy Blue", "navy", Color(0xFF2E6386)),
                    Triple("Lavender", "lavender", Color(0xFF6B587B)),
                    Triple("Pink", "pink", Color(0xFF8B4A62)),
                    Triple("Mocha", "mocha", Color(0xFF7B5841)),
                    Triple("Catppuccin", "catppuccin", Color(0xFF8839EF)),
                    Triple("Monochrome", "monochrome", Color(0xFF5E5E5E))
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(palettes) { (name, id, swatchColor) ->
                        val isSelected = currentPalette == id
                        Surface(
                            onClick = {
                                ThemePreferences.setColorPalette(context, id)
                                coroutineScope.launch {
                                    paletteSheetState.hide()
                                    showPaletteSheet = false
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = swatchColor,
                                    modifier = Modifier.size(36.dp)
                                ) {}

                                Spacer(Modifier.width(16.dp))

                                Text(
                                    text = name,
                                    style = typography.titleMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )

                                if (isSelected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                painter = painterResource(R.drawable.check_fill0),
                                                contentDescription = "Selected",
                                                tint = colorScheme.onPrimary,
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

    if (showBridgeSetupSheet) {
        BridgeSetupSheet(
            initialTab = bridgeSetupInitialTab,
            bridgeStatus = bridgeStatus,
            settingsViewModel = settingsViewModel,
            onDismiss = { showBridgeSetupSheet = false }
        )
    }
}
