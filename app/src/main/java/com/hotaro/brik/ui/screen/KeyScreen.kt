package com.hotaro.brik.ui.screen

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.hotaro.brik.R
import com.hotaro.brik.data.model.AppBag
import com.hotaro.brik.data.model.AppInfo
import com.hotaro.brik.data.model.BagLock
import com.hotaro.brik.data.model.KeyType
import com.hotaro.brik.ui.component.BridgeSetupSheet
import com.hotaro.brik.ui.component.QrScannerView
import com.hotaro.brik.ui.component.RoundedPullRefreshPage
import com.hotaro.brik.ui.viewmodel.KeyViewModel
import com.hotaro.brik.ui.viewmodel.SettingsViewModel
import com.hotaro.brik.ui.viewmodel.ToolsViewModel
import com.hotaro.brik.util.ZenManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class HomeScreenMode {
    KEY,
    ZEN
}

/**
 * Creates a Material 3 Expressive Cookie shape (scalloped organic polygon)
 * using Catmull-Rom to Cubic Bézier curves.
 */
fun cookieShape(sides: Int = 9, indentFactor: Float = 0.86f): Shape = GenericShape { size, _ ->
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val outerR = minOf(centerX, centerY)
    val innerR = outerR * indentFactor
    val totalPoints = sides * 2
    val step = (2.0 * Math.PI / totalPoints).toFloat()

    val points = mutableListOf<Offset>()
    for (i in 0 until totalPoints) {
        val angle = i * step - (Math.PI / 2.0).toFloat()
        val r = if (i % 2 == 0) outerR else innerR
        points.add(Offset(centerX + r * kotlin.math.cos(angle), centerY + r * kotlin.math.sin(angle)))
    }

    moveTo(points[0].x, points[0].y)
    for (i in 0 until totalPoints) {
        val p0 = points[(i - 1 + totalPoints) % totalPoints]
        val p1 = points[i]
        val p2 = points[(i + 1) % totalPoints]
        val p3 = points[(i + 2) % totalPoints]

        val cp1X = p1.x + (p2.x - p0.x) / 6f
        val cp1Y = p1.y + (p2.y - p0.y) / 6f
        val cp2X = p2.x - (p3.x - p1.x) / 6f
        val cp2Y = p2.y - (p3.y - p1.y) / 6f

        cubicTo(cp1X, cp1Y, cp2X, cp2Y, p2.x, p2.y)
    }
    close()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyScreen(
    modifier: Modifier = Modifier,
    onNavigateToZen: () -> Unit = {},
    keyViewModel: KeyViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val homePrefs = remember { context.getSharedPreferences("brik_home_prefs", Context.MODE_PRIVATE) }
    var homeMode by remember {
        val saved = homePrefs.getString("home_screen_mode", HomeScreenMode.KEY.name)
        mutableStateOf(runCatching { HomeScreenMode.valueOf(saved!!) }.getOrDefault(HomeScreenMode.KEY))
    }

    var isUnlockPillVisible by remember { mutableStateOf(false) }
    var showUnlockSheet by remember { mutableStateOf(false) }
    var selectedLockForUnlock by remember { mutableStateOf<BagLock?>(null) }
    var passwordInput by remember { mutableStateOf("") }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showQrScannerDialog by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    val activeLocks by keyViewModel.activeLocks.collectAsState()
    val bridgeStatus by settingsViewModel.bridgeStatus.collectAsState()
    val isZenActive by ZenManager.isZenActive.collectAsState()
    var showBridgeSetupSheet by remember { mutableStateOf(false) }

    // Tap bounce scale
    val scale by animateFloatAsState(
        targetValue = if (isUnlockPillVisible) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "keyScale"
    )

    // Material 3 Expressive Cookie continuous slow rotation
    val infiniteTransition = rememberInfiniteTransition(label = "cookieRotationTransition")
    val cookieRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cookieRotation"
    )

    val isKeyMode = homeMode == HomeScreenMode.KEY

    RoundedPullRefreshPage(
        isRefreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                val newMode = if (homeMode == HomeScreenMode.KEY) HomeScreenMode.ZEN else HomeScreenMode.KEY
                homeMode = newMode
                homePrefs.edit().putString("home_screen_mode", newMode.name).apply()
                isUnlockPillVisible = false
                delay(550)
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
                    text = if (isKeyMode) "Key" else "Zen",
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
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .padding(bottom = 96.dp)
        ) {
            val contentMinHeight = maxHeight
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = contentMinHeight),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Crossfade(
                        targetState = homeMode,
                        label = "homeModeCrossfade"
                    ) { currentMode ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (currentMode == HomeScreenMode.KEY) {
                                // --- KEY MODE CONTENT ---
                                val isPrivileged = bridgeStatus.isDeviceOwner || bridgeStatus.isDhizukuGranted || bridgeStatus.isShizukuGranted
                                if (!isPrivileged) {
                                    Surface(
                                        onClick = { showBridgeSetupSheet = true },
                                        shape = RoundedCornerShape(16.dp),
                                        color = colorScheme.errorContainer.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 24.dp)
                                            .padding(bottom = 24.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = colorScheme.errorContainer,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.shield_fill0),
                                                        contentDescription = null,
                                                        tint = colorScheme.onErrorContainer,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Bridge Setup Needed",
                                                    style = typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                    color = colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Tap to set up using ADB, Shizuku, or Dhizuku",
                                                    style = typography.bodySmall,
                                                    color = colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Icon(
                                                painter = painterResource(R.drawable.arrow_forward_big),
                                                contentDescription = null,
                                                tint = colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                // Tappable Material 3 Expressive Cookie Shape with Slow Rotation
                                Surface(
                                    shape = cookieShape(sides = 9, indentFactor = 0.86f),
                                    color = if (isUnlockPillVisible) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh,
                                    shadowElevation = 8.dp,
                                    modifier = Modifier
                                        .scale(scale)
                                        .size(112.dp)
                                        .graphicsLayer { rotationZ = cookieRotation }
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            isUnlockPillVisible = !isUnlockPillVisible
                                        }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer { rotationZ = -cookieRotation } // Icon stays upright
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.key_fill0),
                                            contentDescription = "Key Icon",
                                            tint = if (isUnlockPillVisible) colorScheme.onPrimaryContainer else colorScheme.primary,
                                            modifier = Modifier.size(46.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(18.dp))

                                Text(
                                    text = "Key",
                                    style = typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onSurface
                                )

                                Text(
                                    text = if (activeLocks.isNotEmpty()) "${activeLocks.size} active locked bag${if (activeLocks.size > 1) "s" else ""}" else "Tap cookie to reveal unlock",
                                    style = typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp)
                                )

                                Spacer(Modifier.height(24.dp))

                                // Animated Unlock Pill
                                AnimatedVisibility(
                                    visible = isUnlockPillVisible,
                                    enter = slideInVertically(
                                        initialOffsetY = { -20 },
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    ) + expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Surface(
                                        onClick = { showUnlockSheet = true },
                                        shape = CircleShape,
                                        color = colorScheme.primary,
                                        shadowElevation = 8.dp,
                                        modifier = Modifier.height(52.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.password_fill0),
                                                contentDescription = null,
                                                tint = colorScheme.onPrimary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                text = "Unlock",
                                                style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }
                            } else {
                                // --- ZEN MODE CONTENT ---
                                Surface(
                                    shape = cookieShape(sides = 9, indentFactor = 0.86f),
                                    color = if (isZenActive) colorScheme.primary else colorScheme.surfaceContainerHigh,
                                    shadowElevation = 8.dp,
                                    modifier = Modifier
                                        .scale(scale)
                                        .size(112.dp)
                                        .graphicsLayer { rotationZ = cookieRotation }
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            onNavigateToZen()
                                        }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer { rotationZ = -cookieRotation } // Icon stays upright
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.self_improvement_fill0),
                                            contentDescription = "Zen Icon",
                                            tint = if (isZenActive) colorScheme.onPrimary else colorScheme.primary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(18.dp))

                                Text(
                                    text = "Zen Mode",
                                    style = typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onSurface
                                )

                                Text(
                                    text = if (isZenActive) "Active lockdown session • Tap to view" else "Minimalist digital detox • Tap to enter",
                                    style = typography.bodyMedium,
                                    color = if (isZenActive) colorScheme.primary else colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp)
                                )

                                Spacer(Modifier.height(24.dp))

                                Surface(
                                    onClick = onNavigateToZen,
                                    shape = CircleShape,
                                    color = if (isZenActive) colorScheme.primaryContainer else colorScheme.primary,
                                    shadowElevation = 8.dp,
                                    modifier = Modifier.height(52.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.self_improvement_fill0),
                                            contentDescription = null,
                                            tint = if (isZenActive) colorScheme.onPrimaryContainer else colorScheme.onPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = if (isZenActive) "View Zen Desktop" else "Enter Zen Mode",
                                            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isZenActive) colorScheme.onPrimaryContainer else colorScheme.onPrimary
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

    // --- Unlock Drawer (Bottom Sheet) ---
    if (showUnlockSheet) {
        val unlockSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showUnlockSheet = false },
            sheetState = unlockSheetState,
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
                            text = "Unlock Bags",
                            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = if (activeLocks.isNotEmpty()) "${activeLocks.size} locked bundles" else "No locked bags",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { showUnlockSheet = false }) {
                        Icon(
                            painter = painterResource(R.drawable.close_fill0),
                            contentDescription = "Close",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (activeLocks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.check_fill0),
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "All Bags Unlocked",
                                style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "There are no currently active locked bags to unlock.",
                                style = typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(activeLocks, key = { it.id }) { lock ->
                            ElevatedCard(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = colorScheme.surfaceContainerHigh
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = lock.bagName,
                                            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = colorScheme.onSurface
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (lock.keyType == KeyType.QR_CODE) colorScheme.tertiaryContainer else colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    text = if (lock.keyType == KeyType.QR_CODE) "QR Key" else "Password Key",
                                                    style = typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (lock.keyType == KeyType.QR_CODE) colorScheme.onTertiaryContainer else colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = "${lock.packageNames.size} apps",
                                                style = typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            selectedLockForUnlock = lock
                                            if (lock.keyType == KeyType.PASSWORD) {
                                                passwordInput = ""
                                                showPasswordDialog = true
                                            } else {
                                                showQrScannerDialog = true
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Unlock")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Password Input Dialog ---
    if (showPasswordDialog && selectedLockForUnlock != null) {
        val lock = selectedLockForUnlock!!
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = {
                Text(
                    text = "Unlock '${lock.bagName}'",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter the password you created when locking this bag.",
                        style = typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        placeholder = { Text("Password") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        keyViewModel.unlockBag(context, lock, passwordInput) { success, err ->
                            if (success) {
                                Toast.makeText(context, "'${lock.bagName}' unlocked successfully!", Toast.LENGTH_SHORT).show()
                                showPasswordDialog = false
                                selectedLockForUnlock = null
                            } else {
                                Toast.makeText(context, err ?: "Incorrect password", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    enabled = passwordInput.isNotBlank()
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
        )
    }

    // --- QR Scanner Dialog ---
    if (showQrScannerDialog && selectedLockForUnlock != null) {
        val lock = selectedLockForUnlock!!
        AlertDialog(
            onDismissRequest = { showQrScannerDialog = false },
            title = {
                Text(
                    text = "Scan QR Key for '${lock.bagName}'",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Point camera at the QR code generated when locking.",
                        style = typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    QrScannerView(
                        onQrDetected = { scannedToken ->
                            keyViewModel.unlockBag(context, lock, scannedToken) { success, err ->
                                if (success) {
                                    Toast.makeText(context, "'${lock.bagName}' unlocked via QR Key!", Toast.LENGTH_SHORT).show()
                                    showQrScannerDialog = false
                                    selectedLockForUnlock = null
                                } else {
                                    Toast.makeText(context, err ?: "Invalid QR Code", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showQrScannerDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
        )
    }

    if (showBridgeSetupSheet) {
        BridgeSetupSheet(
            initialTab = 0,
            bridgeStatus = bridgeStatus,
            settingsViewModel = settingsViewModel,
            onDismiss = { showBridgeSetupSheet = false }
        )
    }
}
