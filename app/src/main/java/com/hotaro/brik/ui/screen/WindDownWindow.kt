package com.hotaro.brik.ui.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.hotaro.brik.R
import com.hotaro.brik.data.model.AppInfo
import com.hotaro.brik.data.model.KeyType
import com.hotaro.brik.data.model.SavedKey
import com.hotaro.brik.ui.component.SetEmergencyKeyDialog
import com.hotaro.brik.util.WindDownManager
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WindDownWindow(
    onBack: () -> Unit,
    installedApps: List<AppInfo> = emptyList(),
    savedKeys: List<SavedKey> = emptyList()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val isDark = colorScheme.surface.luminance() < 0.5f
    val appBackground = if (isDark) Color.Black else colorScheme.surface

    val isEnabled by WindDownManager.isEnabled.collectAsState()
    val sleepHour by WindDownManager.sleepHour.collectAsState()
    val sleepMinute by WindDownManager.sleepMinute.collectAsState()
    val wakeHour by WindDownManager.wakeHour.collectAsState()
    val wakeMinute by WindDownManager.wakeMinute.collectAsState()
    val isNoAppsMode by WindDownManager.isNoAppsMode.collectAsState()
    val allowedPackages by WindDownManager.allowedPackages.collectAsState()
    val allowEmergencyUnlock by WindDownManager.allowEmergencyUnlock.collectAsState()
    val hasSecurityKey by WindDownManager.hasSecurityKey.collectAsState()
    val isWindDownActive by WindDownManager.isWindDownActive.collectAsState()

    var showSleepPicker by remember { mutableStateOf(false) }
    var showWakePicker by remember { mutableStateOf(false) }
    var showAppPickerSheet by remember { mutableStateOf(false) }
    var showSetKeyDialog by remember { mutableStateOf(false) }

    val pm = context.packageManager
    val launcherApps = remember(context, installedApps) {
        if (installedApps.isNotEmpty()) installedApps else loadLauncherApps(pm, context.packageName)
    }

    val selectedAppInfos = remember(allowedPackages, launcherApps) {
        launcherApps.filter { allowedPackages.contains(it.packageName) }
    }

    Scaffold(
        containerColor = appBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Wind Down",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Bedtime minimal launcher & lockdown",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_fill0),
                            contentDescription = "Back",
                            tint = colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = appBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Master Enable Card
            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isWindDownActive) {
                        colorScheme.primaryContainer.copy(alpha = 0.5f)
                    } else {
                        colorScheme.surfaceContainerHigh
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isWindDownActive) colorScheme.primary else colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.dark_mode_fill0),
                                    contentDescription = null,
                                    tint = if (isWindDownActive) colorScheme.onPrimary else colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Wind Down Routine",
                                style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = if (isWindDownActive) {
                                    "Active now • Bedtime lockdown engaged"
                                } else if (isEnabled) {
                                    "Enabled • Runs daily from ${String.format(Locale.getDefault(), "%02d:%02d", sleepHour, sleepMinute)} to ${String.format(Locale.getDefault(), "%02d:%02d", wakeHour, wakeMinute)}"
                                } else {
                                    "Disabled"
                                },
                                style = typography.bodySmall,
                                color = if (isWindDownActive) colorScheme.primary else colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Switch(
                        checked = isEnabled,
                        onCheckedChange = {
                            WindDownManager.setEnabled(context, it, activity)
                        }
                    )
                }
            }

            // Bedtime Schedule Card
            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.schedule_fill0),
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Bedtime Schedule",
                            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "During these hours, the device will switch to the minimalist kiosk night launcher.",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Bedtime Picker Tile
                        Surface(
                            onClick = { showSleepPicker = true },
                            shape = RoundedCornerShape(16.dp),
                            color = colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Bedtime (Sleep)",
                                    style = typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = String.format(Locale.getDefault(), "%02d:%02d", sleepHour, sleepMinute),
                                    style = typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = colorScheme.onSurface
                                )
                            }
                        }

                        // Wake Time Picker Tile
                        Surface(
                            onClick = { showWakePicker = true },
                            shape = RoundedCornerShape(16.dp),
                            color = colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Wake Time",
                                    style = typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = String.format(Locale.getDefault(), "%02d:%02d", wakeHour, wakeMinute),
                                    style = typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // App Access Policy Card ("apps or not" & "which apps to allow")
            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.apps_fill0),
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "App Access Policy",
                            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Choose whether you want complete isolation with no apps or allow up to 10 essential apps.",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(14.dp))

                    // Segmented Button: No Apps vs Allowed Apps
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(colorScheme.surfaceContainerLowest)
                            .padding(4.dp)
                    ) {
                        // Pure Rest (No Apps)
                        Surface(
                            onClick = { WindDownManager.setNoAppsMode(context, true) },
                            shape = CircleShape,
                            color = if (isNoAppsMode) colorScheme.primary else Color.Transparent,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Pure Rest (No Apps)",
                                    style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isNoAppsMode) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Allowed Apps
                        Surface(
                            onClick = { WindDownManager.setNoAppsMode(context, false) },
                            shape = CircleShape,
                            color = if (!isNoAppsMode) colorScheme.primary else Color.Transparent,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Allowed Apps",
                                    style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (!isNoAppsMode) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (!isNoAppsMode) {
                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Selected Apps (${allowedPackages.size}/10)",
                                style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )

                            TextButton(onClick = { showAppPickerSheet = true }) {
                                Text("Manage Apps", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (selectedAppInfos.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colorScheme.surfaceContainerLowest,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No apps configured for Wind Down",
                                        style = typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { showAppPickerSheet = true },
                                        shape = CircleShape
                                    ) {
                                        Text("Select Allowed Apps")
                                    }
                                }
                            }
                        } else {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                selectedAppInfos.forEach { app ->
                                    InputChip(
                                        selected = true,
                                        onClick = {
                                            // Clicking chip removes it from allowed packages
                                            val updated = allowedPackages - app.packageName
                                            WindDownManager.setAllowedPackages(context, updated)
                                        },
                                        label = {
                                            Text(
                                                text = app.label,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        leadingIcon = {
                                            if (app.icon != null) {
                                                Image(
                                                    painter = rememberDrawablePainter(app.icon),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            } else {
                                                Icon(
                                                    painter = painterResource(R.drawable.apps_fill0),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        },
                                        trailingIcon = {
                                            Icon(
                                                painter = painterResource(R.drawable.close_fill0),
                                                contentDescription = "Remove",
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Emergency Unlock & Security Key Card ("emergeny mode avaialble or not" & "keys")
            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.shield_fill0),
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Emergency & Security Keys",
                            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Configure how lockdown exits and key verifications are enforced during bedtime.",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    // Emergency Unlock Available Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Emergency Unlock Available",
                                style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = if (allowEmergencyUnlock) {
                                    "Emergency key unlock button is active during Wind Down."
                                } else {
                                    "Strict mode: Emergency button hidden, unlocking blocked until wake."
                                },
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Switch(
                            checked = allowEmergencyUnlock,
                            onCheckedChange = {
                                WindDownManager.setAllowEmergencyUnlock(context, it)
                            }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Security Key Tile
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colorScheme.surfaceContainerLowest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (hasSecurityKey) colorScheme.primaryContainer else colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(R.drawable.key_fill0),
                                            contentDescription = null,
                                            tint = if (hasSecurityKey) colorScheme.primary else colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Protection Key",
                                        style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (hasSecurityKey) {
                                            "Configured (${WindDownManager.getKeyType(context).name})"
                                        } else {
                                            "Using system emergency key"
                                        },
                                        style = typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = { showSetKeyDialog = true },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                            ) {
                                Text(if (hasSecurityKey) "Change" else "Configure")
                            }
                        }
                    }
                }
            }

            // Preview Mode Card
            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Test Launcher",
                        style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Preview your configured Wind Down desktop screen immediately without waiting for scheduled bedtime.",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            WindDownManager.startPreviewMode(context, activity)
                        },
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.self_improvement_fill0),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Preview Wind Down Desktop", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // --- TIME PICKERS ---
    if (showSleepPicker) {
        val sleepTimeState = rememberTimePickerState(
            initialHour = sleepHour,
            initialMinute = sleepMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showSleepPicker = false },
            title = { Text("Set Bedtime (Sleep Time)", fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = sleepTimeState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        WindDownManager.setSchedule(context, sleepTimeState.hour, sleepTimeState.minute, wakeHour, wakeMinute, activity)
                        showSleepPicker = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSleepPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showWakePicker) {
        val wakeTimeState = rememberTimePickerState(
            initialHour = wakeHour,
            initialMinute = wakeMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showWakePicker = false },
            title = { Text("Set Wake Time", fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = wakeTimeState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        WindDownManager.setSchedule(context, sleepHour, sleepMinute, wakeTimeState.hour, wakeTimeState.minute, activity)
                        showWakePicker = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWakePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- MANAGE ALLOWED APPS BOTTOM SHEET (MAX 10 APPS) ---
    if (showAppPickerSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var searchQuery by remember { mutableStateOf("") }
        var tempAllowed by remember { mutableStateOf(allowedPackages) }

        val filteredApps = remember(launcherApps, searchQuery) {
            if (searchQuery.isBlank()) launcherApps else launcherApps.filter {
                it.label.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showAppPickerSheet = false },
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
                    Column {
                        Text(
                            text = "Allowed Apps",
                            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "${tempAllowed.size}/10 apps selected",
                            style = typography.bodySmall,
                            color = if (tempAllowed.size >= 10) colorScheme.primary else colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            WindDownManager.setAllowedPackages(context, tempAllowed.take(10).toSet())
                            showAppPickerSheet = false
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps...") },
                    leadingIcon = {
                        Icon(painter = painterResource(R.drawable.search_fill0), contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(painter = painterResource(R.drawable.close_fill0), contentDescription = "Clear")
                            }
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val isSelected = tempAllowed.contains(app.packageName)
                        Surface(
                            onClick = {
                                if (isSelected) {
                                    tempAllowed = tempAllowed - app.packageName
                                } else {
                                    if (tempAllowed.size >= 10) {
                                        Toast.makeText(context, "Maximum 10 apps allowed in Wind Down", Toast.LENGTH_SHORT).show()
                                    } else {
                                        tempAllowed = tempAllowed + app.packageName
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            if (tempAllowed.size >= 10) {
                                                Toast.makeText(context, "Maximum 10 apps allowed in Wind Down", Toast.LENGTH_SHORT).show()
                                            } else {
                                                tempAllowed = tempAllowed + app.packageName
                                            }
                                        } else {
                                            tempAllowed = tempAllowed - app.packageName
                                        }
                                    }
                                )
                                Spacer(Modifier.width(10.dp))
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
                                        modifier = Modifier.size(36.dp),
                                        tint = colorScheme.primary
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        style = typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = typography.labelSmall,
                                        color = colorScheme.onSurfaceVariant,
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

    // --- SECURITY KEY CONFIGURATION DIALOG ---
    if (showSetKeyDialog) {
        SetEmergencyKeyDialog(
            savedKeys = savedKeys,
            title = "Set Wind Down Security Key",
            subtitle = "Set a secure key for Wind Down. This key will be required to emergency unlock during bedtime hours.",
            onDismiss = { showSetKeyDialog = false },
            onSaveKey = { keyType, secret ->
                WindDownManager.setSecurityKey(context, keyType, secret)
                Toast.makeText(context, "Wind Down security key configured", Toast.LENGTH_SHORT).show()
            },
            onKeySet = {
                showSetKeyDialog = false
            }
        )
    }
}

private fun loadLauncherApps(pm: PackageManager, currentPackageName: String): List<AppInfo> {
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    val resolveInfos = pm.queryIntentActivities(intent, 0)
    val seenPackages = mutableSetOf<String>()
    val result = mutableListOf<AppInfo>()

    for (info in resolveInfos) {
        val pkg = info.activityInfo.packageName
        if (pkg != currentPackageName && seenPackages.add(pkg)) {
            val label = info.loadLabel(pm).toString().ifBlank { pkg }
            val icon: Drawable? = try {
                info.loadIcon(pm)
            } catch (_: Exception) {
                null
            }
            result.add(
                AppInfo(
                    packageName = pkg,
                    label = label,
                    icon = icon,
                    isSystemApp = false
                )
            )
        }
    }

    return result.sortedBy { it.label.lowercase(Locale.getDefault()) }
}
