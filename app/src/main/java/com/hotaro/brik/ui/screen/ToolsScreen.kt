package com.hotaro.brik.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.hotaro.brik.data.model.AppBag
import com.hotaro.brik.data.model.AppInfo
import com.hotaro.brik.data.model.KeyType
import com.hotaro.brik.policy.DnsManager
import com.hotaro.brik.policy.DnsMode
import com.hotaro.brik.ui.component.QrScannerView
import com.hotaro.brik.ui.component.RoundedPullRefreshPage
import com.hotaro.brik.ui.component.TomatoSectionHeader
import com.hotaro.brik.ui.component.TomatoSettingsItem
import com.hotaro.brik.ui.component.segmentedItemShape
import com.hotaro.brik.ui.viewmodel.KeyViewModel
import com.hotaro.brik.ui.viewmodel.ToolsViewModel
import com.hotaro.brik.util.QrUtils
import com.hotaro.brik.util.StealthUtils
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    modifier: Modifier = Modifier,
    onNavigateToAppManager: () -> Unit = {},
    onNavigateToBags: () -> Unit = {},
    onNavigateToRoutines: () -> Unit = {},
    onNavigateToModes: () -> Unit = {},
    onNavigateToZen: () -> Unit = {},
    onNavigateToGrayscale: () -> Unit = {},
    viewModel: ToolsViewModel = viewModel(),
    keyViewModel: KeyViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showDnsSheet by remember { mutableStateOf(false) }
    var showStealthSheet by remember { mutableStateOf(false) }
    var showToolsCustomizationSheet by remember { mutableStateOf(false) }

    val pullTimestamps = remember { mutableStateListOf<Long>() }

    val toolsPrefs = remember { context.getSharedPreferences("brik_tools_prefs", Context.MODE_PRIVATE) }
    var hiddenCardIds by remember {
        mutableStateOf(toolsPrefs.getStringSet("hidden_tool_cards", emptySet())?.toSet() ?: emptySet())
    }

    var dnsStatus by remember { mutableStateOf(DnsManager.getCurrentDns(context)) }
    var isAppHidden by remember { mutableStateOf(StealthUtils.isAppHidden(context)) }
    var dialerCode by remember { mutableStateOf(StealthUtils.getDialerCode(context)) }

    val bags by viewModel.bags.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val showSystemApps by viewModel.showSystemApps.collectAsState()
    val activeLocks by keyViewModel.activeLocks.collectAsState()

    val dnsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val stealthSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isRefreshing by remember { mutableStateOf(false) }

    RoundedPullRefreshPage(
        isRefreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                val now = System.currentTimeMillis()
                pullTimestamps.removeAll { now - it > 5000L }
                pullTimestamps.add(now)
                if (pullTimestamps.size >= 3) {
                    pullTimestamps.clear()
                    showToolsCustomizationSheet = true
                }

                isRefreshing = true
                viewModel.loadInstalledApps()
                dnsStatus = DnsManager.getCurrentDns(context)
                isAppHidden = StealthUtils.isAppHidden(context)
                dialerCode = StealthUtils.getDialerCode(context)
                kotlinx.coroutines.delay(650)
                isRefreshing = false
            }
        },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tools",
                    style = typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = colorScheme.onSurface
                )
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
            // --- SECTION 1: APPLICATIONS & BAGS ---
            val section1Cards = listOf("app_manager", "bags").filter { !hiddenCardIds.contains(it) }
            if (section1Cards.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    TomatoSectionHeader(title = "APPLICATIONS & BAGS")

                    section1Cards.forEachIndexed { index, cardId ->
                        when (cardId) {
                            "app_manager" -> {
                                val visibleAppsCount = if (showSystemApps) installedApps.size else installedApps.count { !it.isSystemApp }
                                TomatoSettingsItem(
                                    title = "App Manager",
                                    subtitle = if (installedApps.isNotEmpty()) "$visibleAppsCount apps available" else "Manage and inspect applications",
                                    leadingIcon = R.drawable.apps_fill0,
                                    iconContainerColor = colorScheme.primaryContainer,
                                    iconTint = colorScheme.primary,
                                    trailingContent = {
                                        Icon(
                                            painter = painterResource(R.drawable.arrow_forward_big),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = colorScheme.onSurfaceVariant
                                        )
                                    },
                                    shape = segmentedItemShape(index, section1Cards.size),
                                    onClick = onNavigateToAppManager
                                )
                            }
                            "bags" -> {
                                TomatoSettingsItem(
                                    title = "Bags",
                                    subtitle = if (bags.isNotEmpty()) "${bags.size} bags configured (${activeLocks.size} locked)" else "Pack apps into custom bundles",
                                    leadingIcon = R.drawable.backpack_fill0,
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
                                    shape = segmentedItemShape(index, section1Cards.size),
                                    onClick = onNavigateToBags
                                )
                            }
                        }
                    }
                }
            }

            // --- SECTION 2: TIME BLOCK ---
            val section2Cards = listOf("routines", "modes", "zen", "grayscale").filter { !hiddenCardIds.contains(it) }
            if (section2Cards.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    TomatoSectionHeader(title = "TIME BLOCK")

                    section2Cards.forEachIndexed { index, cardId ->
                        when (cardId) {
                            "routines" -> {
                                TomatoSettingsItem(
                                    title = "Automated Routines",
                                    subtitle = "Scheduled locks, intervals & smart triggers",
                                    leadingIcon = R.drawable.schedule_fill0,
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
                                    shape = segmentedItemShape(index, section2Cards.size),
                                    onClick = onNavigateToRoutines
                                )
                            }
                            "modes" -> {
                                TomatoSettingsItem(
                                    title = "Focus Modes",
                                    subtitle = "Deep work, study & sleep restriction profiles",
                                    leadingIcon = R.drawable.mode_fill0,
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
                                    shape = segmentedItemShape(index, section2Cards.size),
                                    onClick = onNavigateToModes
                                )
                            }
                            "zen" -> {
                                TomatoSettingsItem(
                                    title = "Zen",
                                    subtitle = "lockdown and deep digital detox",
                                    leadingIcon = R.drawable.self_improvement_fill0,
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
                                    shape = segmentedItemShape(index, section2Cards.size),
                                    onClick = onNavigateToZen
                                )
                            }
                            "grayscale" -> {
                                TomatoSettingsItem(
                                    title = "Grayscale",
                                    subtitle = "Monochrome screen filter & wind down display",
                                    leadingIcon = R.drawable.contrast_fill0,
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
                                    shape = segmentedItemShape(index, section2Cards.size),
                                    onClick = onNavigateToGrayscale
                                )
                            }
                        }
                    }
                }
            }

            // --- SECTION 3: PRIVACY & NETWORK ---
            val section3Cards = listOf("private_dns", "stealth").filter { !hiddenCardIds.contains(it) }
            if (section3Cards.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    TomatoSectionHeader(title = "PRIVACY & NETWORK")

                    section3Cards.forEachIndexed { index, cardId ->
                        when (cardId) {
                            "private_dns" -> {
                                val dnsSub = when (dnsStatus.mode) {
                                    DnsMode.OFF -> "Disabled (Standard ISP DNS)"
                                    DnsMode.OPPORTUNISTIC -> "Automatic (Opportunistic)"
                                    DnsMode.SPECIFIED_HOST -> dnsStatus.host ?: "Custom Host"
                                }
                                TomatoSettingsItem(
                                    title = "Private DNS (DoT)",
                                    subtitle = dnsSub,
                                    leadingIcon = R.drawable.dns_fill0,
                                    iconContainerColor = colorScheme.tertiaryContainer,
                                    iconTint = colorScheme.onTertiaryContainer,
                                    trailingContent = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (dnsStatus.mode == DnsMode.SPECIFIED_HOST) colorScheme.primaryContainer else if (dnsStatus.mode == DnsMode.OPPORTUNISTIC) colorScheme.secondaryContainer else colorScheme.surfaceContainerLowest
                                            ) {
                                                Text(
                                                    text = when (dnsStatus.mode) {
                                                        DnsMode.OFF -> "Off"
                                                        DnsMode.OPPORTUNISTIC -> "Auto"
                                                        DnsMode.SPECIFIED_HOST -> "Custom"
                                                    },
                                                    style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (dnsStatus.mode == DnsMode.SPECIFIED_HOST) colorScheme.onPrimaryContainer else if (dnsStatus.mode == DnsMode.OPPORTUNISTIC) colorScheme.onSecondaryContainer else colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Icon(
                                                painter = painterResource(R.drawable.arrow_forward_big),
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    shape = segmentedItemShape(index, section3Cards.size),
                                    onClick = {
                                        dnsStatus = DnsManager.getCurrentDns(context)
                                        showDnsSheet = true
                                    }
                                )
                            }
                            "stealth" -> {
                                TomatoSettingsItem(
                                    title = "Stealth Mode (App Concealment)",
                                    subtitle = if (isAppHidden) "Hidden • Dial *#*#$dialerCode#*#*" else "Visible • Dial code: *#*#$dialerCode#*#*",
                                    leadingIcon = R.drawable.stealth_fill0,
                                    iconContainerColor = if (isAppHidden) colorScheme.errorContainer else colorScheme.surfaceContainerLowest,
                                    iconTint = if (isAppHidden) colorScheme.onErrorContainer else colorScheme.primary,
                                    trailingContent = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (isAppHidden) colorScheme.errorContainer else colorScheme.surfaceContainerHighest
                                            ) {
                                                Text(
                                                    text = if (isAppHidden) "Hidden" else "Visible",
                                                    style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (isAppHidden) colorScheme.onErrorContainer else colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Icon(
                                                painter = painterResource(R.drawable.arrow_forward_big),
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    shape = segmentedItemShape(index, section3Cards.size),
                                    onClick = {
                                        isAppHidden = StealthUtils.isAppHidden(context)
                                        dialerCode = StealthUtils.getDialerCode(context)
                                        showStealthSheet = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(130.dp))
        }
    }

    // --- Private DNS Bottom Sheet ---
    if (showDnsSheet) {
        var selectedMode by remember { mutableStateOf(dnsStatus.mode) }
        var hostInput by remember { mutableStateOf(dnsStatus.host ?: "dns.adguard-dns.com") }

        ModalBottomSheet(
            onDismissRequest = { showDnsSheet = false },
            sheetState = dnsSheetState,
            containerColor = colorScheme.surfaceContainerLowest,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Private DNS",
                            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Route device traffic through secure encrypted DNS",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            dnsSheetState.hide()
                            showDnsSheet = false
                        }
                    }) {
                        Icon(painterResource(R.drawable.close_fill0), contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Mode Selector Segmented Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colorScheme.surfaceContainerHigh)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        DnsMode.OFF to "Off",
                        DnsMode.OPPORTUNISTIC to "Automatic",
                        DnsMode.SPECIFIED_HOST to "Custom DNS"
                    ).forEach { (m, label) ->
                        Surface(
                            onClick = { selectedMode = m },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedMode == m) colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    style = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (selectedMode == m) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                if (selectedMode == DnsMode.SPECIFIED_HOST) {
                    Text(
                        text = "Quick Presets",
                        style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(DnsManager.PRESETS) { preset ->
                            val isSelected = hostInput.trim().equals(preset.host, ignoreCase = true)
                            Surface(
                                onClick = { hostInput = preset.host },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh,
                                modifier = Modifier.height(36.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = preset.name,
                                        style = typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                        color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = hostInput,
                        onValueChange = { hostInput = it },
                        label = { Text("DNS Provider Hostname") },
                        placeholder = { Text("e.g. dns.adguard-dns.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Android will establish an encrypted TLS connection directly to this DNS provider host.",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                } else if (selectedMode == DnsMode.OPPORTUNISTIC) {
                    Text(
                        text = "Automatic mode will attempt to use encrypted DNS with your existing network's servers when available.",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Private DNS is turned off. Your ISP's standard unencrypted DNS will be used.",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        val (ok, msg) = DnsManager.setDns(context, selectedMode, hostInput)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        if (ok) {
                            dnsStatus = DnsManager.getCurrentDns(context)
                            coroutineScope.launch {
                                dnsSheetState.hide()
                                showDnsSheet = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Apply DNS Settings")
                }
            }
        }
    }

    // --- Stealth Mode Bottom Sheet ---
    if (showStealthSheet) {
        var tempCode by remember { mutableStateOf(dialerCode) }

        ModalBottomSheet(
            onDismissRequest = { showStealthSheet = false },
            sheetState = stealthSheetState,
            containerColor = colorScheme.surfaceContainerLowest,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Stealth Mode",
                            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Hide Brik icon & reopen via Phone dialer",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            stealthSheetState.hide()
                            showStealthSheet = false
                        }
                    }) {
                        Icon(painterResource(R.drawable.close_fill0), contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Toggle Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Hide Brik from Launcher",
                                style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = if (isAppHidden) "App icon is currently hidden" else "App icon is visible in launcher drawer",
                                style = typography.bodySmall,
                                color = if (isAppHidden) colorScheme.error else colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isAppHidden,
                            onCheckedChange = { hide ->
                                StealthUtils.setAppHidden(context, hide)
                                isAppHidden = StealthUtils.isAppHidden(context)
                                Toast.makeText(
                                    context,
                                    if (hide) "Brik icon hidden! Dial *#*#$dialerCode#*#* to open." else "Brik icon restored to launcher.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Dialer Code Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Dialer Secret Code",
                            style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "To open Brik while hidden, open your Phone dialer and type:",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colorScheme.primaryContainer,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "*#*#$dialerCode#*#*",
                                style = typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 2.sp
                                ),
                                color = colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = tempCode,
                            onValueChange = { input ->
                                tempCode = input.filter { it.isDigit() }
                            },
                            label = { Text("Change Code (digits only)") },
                            placeholder = { Text("e.g. 2745") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (tempCode.isNotBlank()) {
                                        StealthUtils.setDialerCode(context, tempCode)
                                        dialerCode = StealthUtils.getDialerCode(context)
                                        Toast.makeText(context, "Secret code saved to *#*#$dialerCode#*#*", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save Code")
                            }

                            Button(
                                onClick = {
                                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:*#*#$dialerCode#*#*")
                                    }
                                    try {
                                        context.startActivity(dialIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Dial *#*#$dialerCode#*#* in Phone app", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Test in Dialer")
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Tools Card Customization Bottom Sheet ---
    if (showToolsCustomizationSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val allCards = listOf(
            Triple("app_manager", "App Manager", R.drawable.apps_fill0),
            Triple("bags", "Bags", R.drawable.backpack_fill0),
            Triple("routines", "Automated Routines", R.drawable.schedule_fill0),
            Triple("modes", "Focus Modes", R.drawable.mode_fill0),
            Triple("zen", "Zen Mode", R.drawable.self_improvement_fill0),
            Triple("grayscale", "Grayscale", R.drawable.contrast_fill0),
            Triple("private_dns", "Private DNS", R.drawable.dns_fill0),
            Triple("stealth", "Stealth Mode", R.drawable.stealth_fill0)
        )

        ModalBottomSheet(
            onDismissRequest = { showToolsCustomizationSheet = false },
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Customize Tools",
                            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Choose which cards appear on your Tools screen",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        onClick = {
                            showToolsCustomizationSheet = false
                        },
                        shape = CircleShape,
                        color = colorScheme.primary
                    ) {
                        Text(
                            text = "Done",
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    allCards.forEachIndexed { index, (cardId, label, iconRes) ->
                        val isVisible = !hiddenCardIds.contains(cardId)
                        Surface(
                            shape = segmentedItemShape(index, allCards.size),
                            color = colorScheme.surfaceContainerLowest,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isVisible) colorScheme.primaryContainer else colorScheme.surfaceContainerHighest,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                painter = painterResource(iconRes),
                                                contentDescription = null,
                                                tint = if (isVisible) colorScheme.primary else colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Text(
                                        text = label,
                                        style = typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = colorScheme.onSurface
                                    )
                                }

                                Switch(
                                    checked = isVisible,
                                    onCheckedChange = { checked ->
                                        val newSet = if (checked) {
                                            hiddenCardIds - cardId
                                        } else {
                                            hiddenCardIds + cardId
                                        }
                                        hiddenCardIds = newSet
                                        toolsPrefs.edit().putStringSet("hidden_tool_cards", newSet).apply()
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

@Composable
fun AppListItem(
    app: AppInfo,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) colorScheme.primaryContainer.copy(alpha = 0.35f) else colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val painter = rememberDrawablePainter(drawable = app.icon)
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Spacer(Modifier.width(14.dp))

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
                    style = typography.bodySmall.copy(fontSize = 11.sp),
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = colorScheme.primary,
                    uncheckedColor = colorScheme.outline
                )
            )
        }
    }
}

@Composable
fun BagItemCard(
    bag: AppBag,
    isLocked: Boolean,
    viewModel: ToolsViewModel,
    onToggleLock: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.fillMaxWidth()
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
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                                color = colorScheme.onSurface
                            )
                            if (isLocked) {
                                Spacer(Modifier.width(6.dp))
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
                            }
                        }
                        Text(
                            text = "${bag.packageNames.size} apps packed",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = isLocked,
                        onCheckedChange = { onToggleLock(it) }
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = onDelete) {
                        Icon(
                            painter = painterResource(R.drawable.delete_fill0),
                            contentDescription = "Delete Bag",
                            tint = colorScheme.error
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Mini App Icons Preview in the Bag
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(bag.packageNames.toList()) { pkg ->
                    val appInfo = viewModel.getAppInfo(pkg)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colorScheme.surfaceContainerLowest,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (appInfo?.icon != null) {
                                Image(
                                    painter = rememberDrawablePainter(drawable = appInfo.icon),
                                    contentDescription = appInfo.label,
                                    modifier = Modifier.size(28.dp)
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
                }
            }
        }
    }
}
