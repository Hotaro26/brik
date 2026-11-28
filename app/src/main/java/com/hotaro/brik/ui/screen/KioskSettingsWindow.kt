package com.hotaro.brik.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import com.hotaro.brik.ui.component.BrikSwitch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hotaro.brik.R
import com.hotaro.brik.ui.component.TomatoSectionHeader
import com.hotaro.brik.ui.component.segmentedItemShape
import com.hotaro.brik.util.KioskPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KioskSettingsWindow(onBack: () -> Unit) {
    val context = LocalContext.current

    val allowIncomingCallsWindow by KioskPreferences.allowIncomingCallsWindow.collectAsState()
    val allowEmergencyDialer by KioskPreferences.allowEmergencyDialer.collectAsState()
    val autoResumeAfterCall by KioskPreferences.autoResumeAfterCall.collectAsState()
    val allowControlPanelsStatusBar by KioskPreferences.allowControlPanelsStatusBar.collectAsState()
    val blockAdbInKiosk by KioskPreferences.blockAdbInKiosk.collectAsState()
    val showBatteryIndicator by KioskPreferences.showBatteryIndicator.collectAsState()
    val appNamesAlignment by KioskPreferences.appNamesAlignment.collectAsState()
    val exitHoldSeconds by KioskPreferences.exitHoldSeconds.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Kiosk Mode",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        letterSpacing = (-0.5).sp,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "Calls window & lockdown behaviors",
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Spacer(Modifier.height(10.dp))

            // SECTION 1: PHONE & CALL HANDLING
            TomatoSectionHeader(title = "PHONE & CALLS")

            val callItems = listOf(
                KioskToggleItem(
                    title = "Incoming Calls Window",
                    subtitle = "Allow incoming call overlay window without breaking kiosk lockdown",
                    iconRes = R.drawable.call_fill0,
                    checked = allowIncomingCallsWindow,
                    onToggle = { KioskPreferences.setAllowIncomingCallsWindow(context, it) }
                ),
                KioskToggleItem(
                    title = "Auto-Resume Kiosk Session",
                    subtitle = "Re-lock into focus mode immediately when call concludes",
                    iconRes = R.drawable.mode_fill0,
                    checked = autoResumeAfterCall,
                    onToggle = { KioskPreferences.setAutoResumeAfterCall(context, it) }
                )
            )

            callItems.forEachIndexed { index, item ->
                Surface(
                    shape = segmentedItemShape(index, callItems.size),
                    color = colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (item.checked) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(item.iconRes),
                                    contentDescription = item.title,
                                    tint = if (item.checked) colorScheme.primary else colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = item.subtitle,
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }

                        BrikSwitch(
                            checked = item.checked,
                            onCheckedChange = item.onToggle
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // SECTION 2: SYSTEM LOCKDOWN & GESTURES
            TomatoSectionHeader(title = "SYSTEM LOCKDOWN & NAVIGATION")

            val lockdownItems = listOf(
                KioskToggleItem(
                    title = "Status Bar",
                    subtitle = "Show status bar info (time, battery, network) during kiosk mode",
                    iconRes = R.drawable.ic_status_bar_shade,
                    checked = allowControlPanelsStatusBar,
                    onToggle = {
                        KioskPreferences.setAllowControlPanelsStatusBar(context, it)
                        com.hotaro.brik.policy.PolicyBridgeManager.setLockTaskFeatures(
                            context = context,
                            disableSystemGestures = true,
                            lockdownNotificationShade = !it
                        )
                    }
                ),
                KioskToggleItem(
                    title = "Block ADB in Kiosk & Zen",
                    subtitle = "Disables ADB while Kiosk or Zen Mode is active (Reset & Revoke stays available in-app)",
                    iconRes = R.drawable.block_fill0,
                    checked = blockAdbInKiosk,
                    onToggle = { KioskPreferences.setBlockAdbInKiosk(context, it) }
                )
            )

            lockdownItems.forEachIndexed { index, item ->
                Surface(
                    shape = segmentedItemShape(index, lockdownItems.size),
                    color = colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (item.checked) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(item.iconRes),
                                    contentDescription = item.title,
                                    tint = if (item.checked) colorScheme.primary else colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = item.subtitle,
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }

                        BrikSwitch(
                            checked = item.checked,
                            onCheckedChange = item.onToggle
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // SECTION 3: DISPLAY & BATTERY
            TomatoSectionHeader(title = "DISPLAY & BATTERY")

            val displayItems = listOf(
                KioskToggleItem(
                    title = "Show Battery Percentage",
                    subtitle = "Display battery icon and percentage below date and time",
                    iconRes = R.drawable.ic_battery_std,
                    checked = showBatteryIndicator,
                    onToggle = { KioskPreferences.setShowBatteryIndicator(context, it) }
                )
            )

            displayItems.forEachIndexed { index, item ->
                Surface(
                    shape = segmentedItemShape(index, displayItems.size),
                    color = colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (item.checked) colorScheme.primaryContainer else colorScheme.surfaceContainerLowest,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(item.iconRes),
                                    contentDescription = item.title,
                                    tint = if (item.checked) colorScheme.primary else colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = item.subtitle,
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }

                        BrikSwitch(
                            checked = item.checked,
                            onCheckedChange = item.onToggle
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.dock_bottom_fill0),
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "App Names Row Alignment",
                                style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Align app names left, center, or right (default: center)",
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "left" to "Left",
                            "center" to "Center",
                            "right" to "Right"
                        ).forEach { (alignKey, label) ->
                            val isSelected = appNamesAlignment == alignKey
                            FilterChip(
                                selected = isSelected,
                                onClick = { KioskPreferences.setAppNamesAlignment(context, alignKey) },
                                label = {
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colorScheme.primary,
                                    selectedLabelColor = colorScheme.onPrimary,
                                    containerColor = colorScheme.surfaceContainerLowest,
                                    labelColor = colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // SECTION 3: EXIT TIMING
            TomatoSectionHeader(title = "EXIT PRESS DURATION")

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Hold time required on kiosk background to reveal the exit button",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val holdOptions = listOf(
                            1 to "1 sec",
                            2 to "2 sec",
                            3 to "3 sec",
                            5 to "5 sec"
                        )
                        holdOptions.forEach { (sec, label) ->
                            val isSelected = exitHoldSeconds == sec
                            FilterChip(
                                selected = isSelected,
                                onClick = { KioskPreferences.setExitHoldSeconds(context, sec) },
                                label = { Text(text = label, fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colorScheme.primaryContainer,
                                    selectedLabelColor = colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

private data class KioskToggleItem(
    val title: String,
    val subtitle: String,
    val iconRes: Int,
    val checked: Boolean,
    val onToggle: (Boolean) -> Unit
)
