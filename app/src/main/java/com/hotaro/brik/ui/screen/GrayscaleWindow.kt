package com.hotaro.brik.ui.screen

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.hotaro.brik.R
import com.hotaro.brik.data.model.AppInfo
import com.hotaro.brik.data.model.KeyType
import com.hotaro.brik.data.model.SavedKey
import com.hotaro.brik.service.BrikAccessibilityService
import com.hotaro.brik.ui.component.SetEmergencyKeyDialog
import com.hotaro.brik.ui.component.TomatoSectionHeader
import com.hotaro.brik.ui.component.TomatoSettingsItem
import com.hotaro.brik.ui.component.VerifyEmergencyKeyDialog
import com.hotaro.brik.ui.component.segmentedItemShape
import com.hotaro.brik.util.GrayscaleManager
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GrayscaleWindow(
    onBack: () -> Unit,
    savedKeys: List<SavedKey> = emptyList()
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val isDark = colorScheme.surface.luminance() < 0.5f
    val appBackground = if (isDark) Color.Black else colorScheme.surface

    val isSystemwideEnabled by GrayscaleManager.isSystemwideEnabled.collectAsState()
    val systemwideExceptions by GrayscaleManager.systemwideExceptions.collectAsState()
    val isAppSpecificEnabled by GrayscaleManager.isAppSpecificEnabled.collectAsState()
    val appSpecificApps by GrayscaleManager.appSpecificApps.collectAsState()
    val isKeyGuardEnabled by GrayscaleManager.isKeyGuardEnabled.collectAsState()
    val hasSecurityKey by GrayscaleManager.hasSecurityKey.collectAsState()

    var isAccessibilityServiceEnabled by remember { mutableStateOf(BrikAccessibilityService.isEnabled(context)) }
    var hasWritePermission by remember { mutableStateOf(GrayscaleManager.hasPermission(context)) }

    // Expandable card states
    var isSystemwideExpanded by remember { mutableStateOf(false) }
    val systemwideChevronRotation by animateFloatAsState(
        targetValue = if (isSystemwideExpanded) 90f else 0f,
        label = "systemwideChevron"
    )

    var isAppSpecificExpanded by remember { mutableStateOf(false) }
    val appSpecificChevronRotation by animateFloatAsState(
        targetValue = if (isAppSpecificExpanded) 90f else 0f,
        label = "appSpecificChevron"
    )

    var isKeyGuardExpanded by remember { mutableStateOf(false) }
    val keyGuardChevronRotation by animateFloatAsState(
        targetValue = if (isKeyGuardExpanded) 90f else 0f,
        label = "keyGuardChevron"
    )

    var showSystemwideSheet by remember { mutableStateOf(false) }
    var showAppSpecificSheet by remember { mutableStateOf(false) }
    var showSetKeyDialog by remember { mutableStateOf(false) }
    var showVerifyKeyDialog by remember { mutableStateOf(false) }
    var showVerifyBeforeChangeKeyDialog by remember { mutableStateOf(false) }
    var pendingGuardedAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val keyRepository = remember(context) { com.hotaro.brik.data.repository.KeyRepository(context) }
    val repoSavedKeys by keyRepository.savedKeys.collectAsState()
    val effectiveSavedKeys = if (savedKeys.isNotEmpty()) savedKeys else repoSavedKeys

    val launcherApps = remember(context) { loadLauncherApps(pm, context.packageName) }

    val exceptedAppInfos = remember(systemwideExceptions, launcherApps) {
        launcherApps.filter { systemwideExceptions.contains(it.packageName) }
    }
    val specificAppInfos = remember(appSpecificApps, launcherApps) {
        launcherApps.filter { appSpecificApps.contains(it.packageName) }
    }

    // Refresh accessibility service status & write permission
    LaunchedEffect(Unit) {
        isAccessibilityServiceEnabled = BrikAccessibilityService.isEnabled(context)
        hasWritePermission = GrayscaleManager.hasPermission(context)
    }

    // Guard actions with Key Guard
    val performGuardedAction: (() -> Unit) -> Unit = { action ->
        if (isKeyGuardEnabled && hasSecurityKey) {
            pendingGuardedAction = action
            showVerifyKeyDialog = true
        } else {
            action()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Grayscale",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Monochrome screen display & app color filters",
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
        },
        containerColor = appBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Absorb empty area taps */ }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Explanatory Card
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
                                painter = painterResource(R.drawable.contrast_fill0),
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Monochrome Screen Filter",
                            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Suppress vibrant colors to minimize mindless scrolling and reduce dopamine feedback loops.",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Accessibility Service Banner (if disabled)
            if (!isAccessibilityServiceEnabled) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colorScheme.errorContainer.copy(alpha = 0.65f),
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
                            Icon(
                                painter = painterResource(R.drawable.shield_fill0),
                                contentDescription = null,
                                tint = colorScheme.onErrorContainer,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Accessibility Service Inactive",
                                    style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Required for dynamic app targeting and exception filtering.",
                                    style = typography.bodySmall,
                                    color = colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        Button(
                            onClick = {
                                val silentlyEnabled = BrikAccessibilityService.tryEnableSilently(context)
                                if (silentlyEnabled) {
                                    isAccessibilityServiceEnabled = true
                                    Toast.makeText(context, "Accessibility service enabled", Toast.LENGTH_SHORT).show()
                                } else {
                                    runCatching {
                                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        })
                                    }
                                }
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.onErrorContainer,
                                contentColor = colorScheme.errorContainer
                            )
                        ) {
                            Text("Enable", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Permission Banner (if WRITE_SECURE_SETTINGS not granted)
            if (!hasWritePermission) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colorScheme.errorContainer.copy(alpha = 0.65f),
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
                            Icon(
                                painter = painterResource(R.drawable.contrast_fill0),
                                contentDescription = null,
                                tint = colorScheme.onErrorContainer,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Permission Required",
                                    style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "WRITE_SECURE_SETTINGS is needed to apply grayscale.",
                                    style = typography.bodySmall,
                                    color = colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        Button(
                            onClick = {
                                val granted = GrayscaleManager.tryGrantPermissionViaPrivileged(context)
                                if (granted) {
                                    hasWritePermission = true
                                    Toast.makeText(context, "Permission granted", Toast.LENGTH_SHORT).show()
                                } else {
                                    val cmd = "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("ADB Command", cmd))
                                    Toast.makeText(context, "Command copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.onErrorContainer,
                                contentColor = colorScheme.errorContainer
                            )
                        ) {
                            Text("Grant", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // --- SECTION 1: SYSTEM DISPLAY FILTER ---
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TomatoSectionHeader(title = "SYSTEM DISPLAY FILTER")

                TomatoSettingsItem(
                    title = "System-wide Grayscale",
                    subtitle = if (isSystemwideEnabled) "Active • Monochrome filter applied" else "Inactive • Normal screen colors",
                    leadingIcon = R.drawable.contrast_fill0,
                    iconContainerColor = if (isSystemwideEnabled) colorScheme.primaryContainer else colorScheme.surfaceContainerHighest,
                    iconTint = if (isSystemwideEnabled) colorScheme.primary else colorScheme.onSurfaceVariant,
                    trailingContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Switch(
                                checked = isSystemwideEnabled,
                                onCheckedChange = { targetState ->
                                    if (targetState && !hasWritePermission) {
                                        val granted = GrayscaleManager.tryGrantPermissionViaPrivileged(context)
                                        if (granted) {
                                            hasWritePermission = true
                                        } else {
                                            Toast.makeText(context, "Grant WRITE_SECURE_SETTINGS first", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    if (!targetState && isKeyGuardEnabled) {
                                        performGuardedAction {
                                            GrayscaleManager.setSystemwideEnabled(context, false)
                                        }
                                    } else {
                                        GrayscaleManager.setSystemwideEnabled(context, targetState)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colorScheme.onPrimary,
                                    checkedTrackColor = colorScheme.primary
                                )
                            )
                            Icon(
                                painter = painterResource(R.drawable.arrow_forward_big),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .graphicsLayer { rotationZ = systemwideChevronRotation },
                                tint = colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    shape = if (isSystemwideExpanded) {
                        RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                    } else {
                        segmentedItemShape(0, 1)
                    },
                    onClick = { isSystemwideExpanded = !isSystemwideExpanded }
                )

                AnimatedVisibility(
                    visible = isSystemwideExpanded,
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
                            Text(
                                text = "Applies monochrome color suppression across the whole operating system, keeping selected apps in full color.",
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (systemwideExceptions.isEmpty()) "No Exceptions" else "${systemwideExceptions.size} Excepted Apps (Color)",
                                    style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onSurface
                                )

                                TextButton(
                                    onClick = {
                                        performGuardedAction {
                                            showSystemwideSheet = true
                                        }
                                    }
                                ) {
                                    Text("Choose Exceptions", fontWeight = FontWeight.Bold)
                                }
                            }

                            if (exceptedAppInfos.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    exceptedAppInfos.forEach { app ->
                                        InputChip(
                                            selected = true,
                                            onClick = {
                                                performGuardedAction {
                                                    GrayscaleManager.setSystemwideExceptions(
                                                        context,
                                                        systemwideExceptions - app.packageName
                                                    )
                                                }
                                            },
                                            label = {
                                                Text(text = app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            },
                                            leadingIcon = {
                                                if (app.icon != null) {
                                                    Image(
                                                        painter = rememberDrawablePainter(app.icon),
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
                            } else {
                                Text(
                                    text = "All applications will display in grayscale without color exemptions.",
                                    style = typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // --- SECTION 2: APP COLOR PROFILES ---
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TomatoSectionHeader(title = "APP COLOR PROFILES")

                TomatoSettingsItem(
                    title = "App-Specific Grayscale",
                    subtitle = if (isAppSpecificEnabled) "Active • Grayscale on ${appSpecificApps.size} apps" else "Inactive • Normal screen colors",
                    leadingIcon = R.drawable.apps_fill0,
                    iconContainerColor = if (isAppSpecificEnabled) colorScheme.primaryContainer else colorScheme.surfaceContainerHighest,
                    iconTint = if (isAppSpecificEnabled) colorScheme.primary else colorScheme.onSurfaceVariant,
                    trailingContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Switch(
                                checked = isAppSpecificEnabled,
                                onCheckedChange = { targetState ->
                                    if (targetState && !hasWritePermission) {
                                        val granted = GrayscaleManager.tryGrantPermissionViaPrivileged(context)
                                        if (granted) {
                                            hasWritePermission = true
                                        } else {
                                            Toast.makeText(context, "Grant WRITE_SECURE_SETTINGS first", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    if (!targetState && isKeyGuardEnabled) {
                                        performGuardedAction {
                                            GrayscaleManager.setAppSpecificEnabled(context, false)
                                        }
                                    } else {
                                        GrayscaleManager.setAppSpecificEnabled(context, targetState)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colorScheme.onPrimary,
                                    checkedTrackColor = colorScheme.primary
                                )
                            )
                            Icon(
                                painter = painterResource(R.drawable.arrow_forward_big),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .graphicsLayer { rotationZ = appSpecificChevronRotation },
                                tint = colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    shape = if (isAppSpecificExpanded) {
                        RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                    } else {
                        segmentedItemShape(0, 1)
                    },
                    onClick = { isAppSpecificExpanded = !isAppSpecificExpanded }
                )

                AnimatedVisibility(
                    visible = isAppSpecificExpanded,
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
                            Text(
                                text = "Keeps your device in vibrant color normally, but automatically switches to grayscale when designated distraction apps are opened.",
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (appSpecificApps.isEmpty()) "No Designated Apps" else "${appSpecificApps.size} Grayscale Apps",
                                    style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onSurface
                                )

                                TextButton(
                                    onClick = {
                                        performGuardedAction {
                                            showAppSpecificSheet = true
                                        }
                                    }
                                ) {
                                    Text("Choose Apps", fontWeight = FontWeight.Bold)
                                }
                            }

                            if (specificAppInfos.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    specificAppInfos.forEach { app ->
                                        InputChip(
                                            selected = true,
                                            onClick = {
                                                performGuardedAction {
                                                    GrayscaleManager.setAppSpecificApps(
                                                        context,
                                                        appSpecificApps - app.packageName
                                                    )
                                                }
                                            },
                                            label = {
                                                Text(text = app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            },
                                            leadingIcon = {
                                                if (app.icon != null) {
                                                    Image(
                                                        painter = rememberDrawablePainter(app.icon),
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
                            } else {
                                Text(
                                    text = "No apps designated yet. Tap Choose Apps to select apps for monochrome focus.",
                                    style = typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // --- SECTION 3: SECURITY & PROTECTION ---
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TomatoSectionHeader(title = "SECURITY & PROTECTION")

                TomatoSettingsItem(
                    title = "Key Guard Protection",
                    subtitle = if (isKeyGuardEnabled) "Guarded • Key required to modify or turn off" else "Unguarded • Free to modify",
                    leadingIcon = R.drawable.key_fill0,
                    iconContainerColor = if (isKeyGuardEnabled) colorScheme.primaryContainer else colorScheme.surfaceContainerHighest,
                    iconTint = if (isKeyGuardEnabled) colorScheme.primary else colorScheme.onSurfaceVariant,
                    trailingContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Switch(
                                checked = isKeyGuardEnabled,
                                onCheckedChange = { targetState ->
                                    if (targetState) {
                                        if (!hasSecurityKey) {
                                            showSetKeyDialog = true
                                        } else {
                                            GrayscaleManager.setKeyGuardEnabled(context, true)
                                            Toast.makeText(context, "Key guard protection enabled", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        performGuardedAction {
                                            GrayscaleManager.setKeyGuardEnabled(context, false)
                                            Toast.makeText(context, "Key guard protection disabled", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colorScheme.onPrimary,
                                    checkedTrackColor = colorScheme.primary
                                )
                            )
                            Icon(
                                painter = painterResource(R.drawable.arrow_forward_big),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(16.dp)
                                    .graphicsLayer { rotationZ = keyGuardChevronRotation },
                                tint = colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    shape = if (isKeyGuardExpanded) {
                        RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                    } else {
                        segmentedItemShape(0, 1)
                    },
                    onClick = { isKeyGuardExpanded = !isKeyGuardExpanded }
                )

                AnimatedVisibility(
                    visible = isKeyGuardExpanded,
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
                            Text(
                                text = "Prevents turning off Grayscale or modifying app lists without verifying a designated security key.",
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(14.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colorScheme.surfaceContainerLowest)
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Grayscale Security Key",
                                        style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onSurface
                                    )
                                    val grayKeyName = if (hasSecurityKey) {
                                        val qrToken = context.getSharedPreferences("brik_grayscale_preferences", Context.MODE_PRIVATE)
                                            .getString("pref_grayscale_qr_token", null)
                                        if (!qrToken.isNullOrBlank()) {
                                            effectiveSavedKeys.find { it.token == qrToken }?.name ?: "QR Key"
                                        } else {
                                            if (GrayscaleManager.getKeyType(context) == KeyType.PASSWORD) "Password" else "QR Key"
                                        }
                                    } else {
                                        "Uses System Emergency Key"
                                    }
                                    Text(
                                        text = if (hasSecurityKey) "Configured • $grayKeyName" else "Uses System Emergency Key",
                                        style = typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (hasSecurityKey) {
                                            showVerifyBeforeChangeKeyDialog = true
                                        } else {
                                            showSetKeyDialog = true
                                        }
                                    },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                                ) {
                                    Text(if (hasSecurityKey) "Change" else "Configure")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // --- BOTTOM SHEET 1: SYSTEMWIDE EXCEPTIONS ---
    if (showSystemwideSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var searchQuery by remember { mutableStateOf("") }
        var tempSelected by remember { mutableStateOf(systemwideExceptions) }
        var tempEnabled by remember { mutableStateOf(isSystemwideEnabled) }
        var showSystemAppsInExceptions by remember { mutableStateOf(false) }

        val filteredApps = remember(launcherApps, searchQuery, showSystemAppsInExceptions) {
            val baseList = if (showSystemAppsInExceptions) launcherApps else launcherApps.filter { !it.isSystemApp }
            if (searchQuery.isBlank()) baseList else baseList.filter {
                it.label.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showSystemwideSheet = false },
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Excepted Apps (Full Color)",
                            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "${tempSelected.size} apps selected to bypass grayscale",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Switch(
                            checked = tempEnabled,
                            onCheckedChange = { tempEnabled = it }
                        )

                        Surface(
                            onClick = {
                                GrayscaleManager.setSystemwideExceptions(context, tempSelected)
                                if (tempEnabled != isSystemwideEnabled) {
                                    GrayscaleManager.setSystemwideEnabled(context, tempEnabled)
                                }
                                showSystemwideSheet = false
                                Toast.makeText(context, "System-wide grayscale settings saved", Toast.LENGTH_SHORT).show()
                            },
                            shape = CircleShape,
                            color = colorScheme.primary
                        ) {
                            Text(
                                text = "OK",
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
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
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    Surface(
                        shape = CircleShape,
                        color = if (showSystemAppsInExceptions) colorScheme.primaryContainer else colorScheme.surfaceContainerHighest,
                        border = BorderStroke(1.dp, if (showSystemAppsInExceptions) colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent),
                        modifier = Modifier.size(42.dp),
                        onClick = { showSystemAppsInExceptions = !showSystemAppsInExceptions }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(if (showSystemAppsInExceptions) R.drawable.visibility_fill0 else R.drawable.visibility_off_fill0),
                                contentDescription = if (showSystemAppsInExceptions) "Hide System Apps" else "Show System Apps",
                                tint = if (showSystemAppsInExceptions) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val isSelected = tempSelected.contains(app.packageName)
                        Surface(
                            onClick = {
                                tempSelected = if (isSelected) tempSelected - app.packageName else tempSelected + app.packageName
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
                                        tempSelected = if (checked) tempSelected + app.packageName else tempSelected - app.packageName
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

    // --- BOTTOM SHEET 2: APP-SPECIFIC GRAYSCALE ---
    if (showAppSpecificSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var searchQuery by remember { mutableStateOf("") }
        var tempSelected by remember { mutableStateOf(appSpecificApps) }
        var tempEnabled by remember { mutableStateOf(isAppSpecificEnabled) }
        var showSystemAppsInSpecific by remember { mutableStateOf(false) }

        val filteredApps = remember(launcherApps, searchQuery, showSystemAppsInSpecific) {
            val baseList = if (showSystemAppsInSpecific) launcherApps else launcherApps.filter { !it.isSystemApp }
            if (searchQuery.isBlank()) baseList else baseList.filter {
                it.label.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showAppSpecificSheet = false },
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Designated Grayscale Apps",
                            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "${tempSelected.size} apps will display in monochrome",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Switch(
                            checked = tempEnabled,
                            onCheckedChange = { tempEnabled = it }
                        )

                        Surface(
                            onClick = {
                                GrayscaleManager.setAppSpecificApps(context, tempSelected)
                                if (tempEnabled != isAppSpecificEnabled) {
                                    GrayscaleManager.setAppSpecificEnabled(context, tempEnabled)
                                }
                                showAppSpecificSheet = false
                                Toast.makeText(context, "App-specific grayscale settings saved", Toast.LENGTH_SHORT).show()
                            },
                            shape = CircleShape,
                            color = colorScheme.primary
                        ) {
                            Text(
                                text = "OK",
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
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
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    Surface(
                        shape = CircleShape,
                        color = if (showSystemAppsInSpecific) colorScheme.primaryContainer else colorScheme.surfaceContainerHighest,
                        border = BorderStroke(1.dp, if (showSystemAppsInSpecific) colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent),
                        modifier = Modifier.size(42.dp),
                        onClick = { showSystemAppsInSpecific = !showSystemAppsInSpecific }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(if (showSystemAppsInSpecific) R.drawable.visibility_fill0 else R.drawable.visibility_off_fill0),
                                contentDescription = if (showSystemAppsInSpecific) "Hide System Apps" else "Show System Apps",
                                tint = if (showSystemAppsInSpecific) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val isSelected = tempSelected.contains(app.packageName)
                        Surface(
                            onClick = {
                                tempSelected = if (isSelected) tempSelected - app.packageName else tempSelected + app.packageName
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
                                        tempSelected = if (checked) tempSelected + app.packageName else tempSelected - app.packageName
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

    // --- VERIFY BEFORE CHANGE KEY DIALOG ---
    if (showVerifyBeforeChangeKeyDialog) {
        VerifyEmergencyKeyDialog(
            title = "Verify Previous Key",
            subtitle = if (GrayscaleManager.getKeyType(context) == KeyType.QR_CODE) {
                "Scan or enter your previous QR key to set up a new password or key."
            } else {
                "Enter your previous password to set up a new password or key."
            },
            keyTypeOverride = GrayscaleManager.getKeyType(context),
            onVerifyCustom = { input ->
                GrayscaleManager.verifySecurityKey(context, input)
            },
            onDismiss = { showVerifyBeforeChangeKeyDialog = false },
            onVerified = {
                showVerifyBeforeChangeKeyDialog = false
                showSetKeyDialog = true
            }
        )
    }

    // --- SECURITY KEY SETUP DIALOG ---
    if (showSetKeyDialog) {
        SetEmergencyKeyDialog(
            savedKeys = effectiveSavedKeys,
            title = "Set Grayscale Security Key",
            subtitle = "Set a secure password or QR key. This key will be required whenever you modify or turn off grayscale policies.",
            onDismiss = { showSetKeyDialog = false },
            onSaveKey = { keyType, secret ->
                GrayscaleManager.setSecurityKey(context, keyType, secret)
                GrayscaleManager.setKeyGuardEnabled(context, true)
                Toast.makeText(context, "Key guard configured and enabled", Toast.LENGTH_SHORT).show()
            },
            onKeySet = {
                showSetKeyDialog = false
            }
        )
    }

    // --- SECURITY KEY VERIFICATION DIALOG ---
    if (showVerifyKeyDialog) {
        VerifyEmergencyKeyDialog(
            title = "Grayscale Key Guard",
            subtitle = "Verify your security key to modify grayscale policies or disable color filters.",
            keyTypeOverride = GrayscaleManager.getKeyType(context),
            onVerifyCustom = { input ->
                GrayscaleManager.verifySecurityKey(context, input)
            },
            onDismiss = {
                showVerifyKeyDialog = false
                pendingGuardedAction = null
            },
            onVerified = {
                showVerifyKeyDialog = false
                val action = pendingGuardedAction
                pendingGuardedAction = null
                action?.invoke()
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
            val isSystem = (info.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (info.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            result.add(
                AppInfo(
                    packageName = pkg,
                    label = label,
                    icon = icon,
                    isSystemApp = isSystem
                )
            )
        }
    }

    return result.sortedBy { it.label.lowercase(Locale.getDefault()) }
}
