package com.hotaro.brik.ui.screen

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.hotaro.brik.util.AssistivePreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistiveSettingsWindow(onBack: () -> Unit) {
    val context = LocalContext.current

    val enabled by AssistivePreferences.enabled.collectAsState()
    val autoHideSec by AssistivePreferences.autoHideSeconds.collectAsState()
    val showWifi by AssistivePreferences.showWifi.collectAsState()
    val showData by AssistivePreferences.showData.collectAsState()
    val showHotspot by AssistivePreferences.showHotspot.collectAsState()
    val showFlashlight by AssistivePreferences.showFlashlight.collectAsState()
    val showBluetooth by AssistivePreferences.showBluetooth.collectAsState()
    val showDnd by AssistivePreferences.showDnd.collectAsState()
    val showPhone by AssistivePreferences.showPhone.collectAsState()
    val showQuickSettings by AssistivePreferences.showQuickSettings.collectAsState()
    val showNotifications by AssistivePreferences.showNotifications.collectAsState()
    val notifPreviewMode by AssistivePreferences.notifPreviewMode.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Assistive Touch",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        letterSpacing = (-0.5).sp,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "Floating quick controls & toggles",
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

            // Master Toggle Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (enabled) colorScheme.primaryContainer else colorScheme.secondaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.ic_assistive_touch),
                                contentDescription = null,
                                tint = if (enabled) colorScheme.primary else colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Assistive Ball",
                            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = if (enabled) "Floating control ball active during kiosk" else "Disabled in all modes",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = enabled,
                        onCheckedChange = { AssistivePreferences.setEnabled(context, it) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // SECTION: AUTO-HIDE DELAY
            TomatoSectionHeader(title = "AUTO-HIDE INACTIVITY DELAY")

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
                        text = "Fades ball to faint outline when idle",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val intervals = listOf(
                            2 to "2 sec",
                            3 to "3 sec",
                            5 to "5 sec",
                            0 to "Always on"
                        )
                        intervals.forEach { (sec, label) ->
                            val isSelected = autoHideSec == sec
                            FilterChip(
                                selected = isSelected,
                                onClick = { AssistivePreferences.setAutoHideSeconds(context, sec) },
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

            Spacer(Modifier.height(16.dp))

            // SECTION: QUICK SETTINGS TILES
            TomatoSectionHeader(title = "TILES IN QUICK CONTROLS")

            val tiles = listOf(
                TileConfig(
                    title = "Wi-Fi",
                    subtitle = "Quick connectivity panel",
                    iconRes = R.drawable.ic_wifi,
                    checked = showWifi,
                    onToggle = { AssistivePreferences.setShowWifi(context, it) }
                ),
                TileConfig(
                    title = "Mobile Data",
                    subtitle = "Cellular connectivity shortcut",
                    iconRes = R.drawable.ic_mobile_data,
                    checked = showData,
                    onToggle = { AssistivePreferences.setShowData(context, it) }
                ),
                TileConfig(
                    title = "Hotspot",
                    subtitle = "Tethering & portable hotspot",
                    iconRes = R.drawable.ic_hotspot,
                    checked = showHotspot,
                    onToggle = { AssistivePreferences.setShowHotspot(context, it) }
                ),
                TileConfig(
                    title = "Flashlight",
                    subtitle = "Instant LED torch toggle",
                    iconRes = R.drawable.ic_flashlight,
                    checked = showFlashlight,
                    onToggle = { AssistivePreferences.setShowFlashlight(context, it) }
                ),
                TileConfig(
                    title = "Bluetooth",
                    subtitle = "Bluetooth device settings",
                    iconRes = R.drawable.ic_bluetooth,
                    checked = showBluetooth,
                    onToggle = { AssistivePreferences.setShowBluetooth(context, it) }
                ),
                TileConfig(
                    title = "Do Not Disturb",
                    subtitle = "Mute notifications & alerts",
                    iconRes = R.drawable.ic_dnd,
                    checked = showDnd,
                    onToggle = { AssistivePreferences.setShowDnd(context, it) }
                ),
                TileConfig(
                    title = "Phone",
                    subtitle = "Open phone caller app",
                    iconRes = R.drawable.call_fill0,
                    checked = showPhone,
                    onToggle = { AssistivePreferences.setShowPhone(context, it) }
                ),
                TileConfig(
                    title = "Quick Settings & Status Bar",
                    subtitle = "Allow pulling down quick settings & notifications from top",
                    iconRes = R.drawable.ic_status_bar_shade,
                    checked = showQuickSettings,
                    onToggle = { AssistivePreferences.setShowQuickSettings(context, it) }
                )
            )

            tiles.forEachIndexed { index, item ->
                Surface(
                    shape = segmentedItemShape(index, tiles.size),
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

                        Switch(
                            checked = item.checked,
                            onCheckedChange = item.onToggle
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // SECTION: NOTIFICATIONS
            TomatoSectionHeader(title = "NOTIFICATIONS IN QUICK CONTROLS")

            // Enable Notifications Switch
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colorScheme.surfaceContainerLow,
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
                            shape = RoundedCornerShape(12.dp),
                            color = colorScheme.secondaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_notifications),
                                    contentDescription = null,
                                    tint = colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Show Notifications Bar",
                                style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Displays notification summary inside the expanded card",
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = showNotifications,
                            onCheckedChange = { AssistivePreferences.setShowNotifications(context, it) }
                        )
                    }

                    if (showNotifications) {
                        Text(
                            text = "Notification Preview Style",
                            style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = notifPreviewMode == "badge",
                                onClick = { AssistivePreferences.setNotifPreviewMode(context, "badge") },
                                label = { Text("Badge Count", fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colorScheme.primaryContainer,
                                    selectedLabelColor = colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            FilterChip(
                                selected = notifPreviewMode == "preview",
                                onClick = { AssistivePreferences.setNotifPreviewMode(context, "preview") },
                                label = { Text("Banner Preview", fontWeight = FontWeight.SemiBold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colorScheme.primaryContainer,
                                    selectedLabelColor = colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                try {
                                    context.startActivity(
                                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                } catch (_: Exception) {
                                    context.startActivity(
                                        Intent(Settings.ACTION_SETTINGS)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = colorScheme.surfaceContainerHigh
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("System Notification Access Settings", fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

private data class TileConfig(
    val title: String,
    val subtitle: String,
    val iconRes: Int,
    val checked: Boolean,
    val onToggle: (Boolean) -> Unit
)
