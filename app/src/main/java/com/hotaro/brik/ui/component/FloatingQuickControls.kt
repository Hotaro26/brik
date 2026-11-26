package com.hotaro.brik.ui.component

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hotaro.brik.R
import com.hotaro.brik.util.AssistivePreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private fun executePrivilegedCommand(cmd: String): Boolean {
    return try {
        if (com.topjohnwu.superuser.Shell.isAppGrantedRoot() == true) {
            com.topjohnwu.superuser.Shell.cmd(cmd).exec().isSuccess
        } else if (rikka.shizuku.Shizuku.pingBinder() && rikka.shizuku.Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            val method = rikka.shizuku.Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
            process.waitFor() == 0
        } else {
            false
        }
    } catch (_: Throwable) {
        false
    }
}

private fun checkWifiApEnabled(context: Context): Boolean {
    return try {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return false
        val method = wm.javaClass.getDeclaredMethod("isWifiApEnabled")
        method.isAccessible = true
        method.invoke(wm) as? Boolean ?: false
    } catch (_: Exception) {
        try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return false
            val method = wm.javaClass.getDeclaredMethod("getWifiApState")
            method.isAccessible = true
            val state = method.invoke(wm) as? Int ?: 0
            state == 13
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * iPhone AssistiveTouch-style floating ball with Material 3 Expressive Quick Controls.
 * - Smooth container morphing between 56dp circle and expandable Quick Controls card.
 * - Anti-squeeze collapse physics (critically damped Spring.DampingRatioNoBouncy).
 * - Real-time state listeners and toggle actions for Wi-Fi, Bluetooth, Hotspot, Flashlight, Data, DND.
 * - Independent scrolling area preventing Bluetooth / Caller tile clipping.
 * - Movable card via top handle drag without interfering with vertical tile scrolling.
 * - Edge snap glide on release.
 */
@Composable
fun FloatingQuickControls(
    autoHideDelaySeconds: Int = 3,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val showWifi by AssistivePreferences.showWifi.collectAsState()
    val showData by AssistivePreferences.showData.collectAsState()
    val showHotspot by AssistivePreferences.showHotspot.collectAsState()
    val showFlashlight by AssistivePreferences.showFlashlight.collectAsState()
    val showBluetooth by AssistivePreferences.showBluetooth.collectAsState()
    val showDnd by AssistivePreferences.showDnd.collectAsState()
    val showPhone by AssistivePreferences.showPhone.collectAsState()
    val showNotifications by AssistivePreferences.showNotifications.collectAsState()
    val notifPreviewMode by AssistivePreferences.notifPreviewMode.collectAsState()

    var isExpanded by remember { mutableStateOf(false) }
    var isDraggingBall by remember { mutableStateOf(false) }
    var isDraggingCard by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Flashlight State
    var isFlashlightOn by remember { mutableStateOf(false) }
    val cameraManager = remember { context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager }
    val cameraId = remember {
        try {
            cameraManager?.cameraIdList?.firstOrNull()
        } catch (_: Exception) {
            null
        }
    }

    DisposableEffect(cameraManager, cameraId) {
        if (cameraManager == null || cameraId == null) {
            return@DisposableEffect onDispose {}
        }
        val torchCallback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(camId: String, enabled: Boolean) {
                if (camId == cameraId) {
                    isFlashlightOn = enabled
                }
            }
        }
        try {
            cameraManager.registerTorchCallback(torchCallback, null)
        } catch (_: Exception) {}
        onDispose {
            try {
                cameraManager.unregisterTorchCallback(torchCallback)
            } catch (_: Exception) {}
        }
    }

    // Wi-Fi State
    val wifiManager = remember { context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager }
    var isWifiEnabled by remember { mutableStateOf(wifiManager?.isWifiEnabled == true) }

    // Bluetooth State
    val bluetoothManager = remember { context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager }
    val bluetoothAdapter = remember(bluetoothManager) { bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter() }
    var isBtEnabled by remember {
        mutableStateOf(try { bluetoothAdapter?.isEnabled == true } catch (_: Exception) { false })
    }

    // Hotspot State
    var isHotspotEnabled by remember { mutableStateOf(checkWifiApEnabled(context)) }

    // Mobile Data State
    var isDataEnabled by remember { mutableStateOf(false) }

    // DND State
    val notificationManager = remember { context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager }
    var isDndOn by remember {
        mutableStateOf(
            try {
                val filter = notificationManager?.currentInterruptionFilter ?: android.app.NotificationManager.INTERRUPTION_FILTER_ALL
                filter != android.app.NotificationManager.INTERRUPTION_FILTER_ALL && filter != android.app.NotificationManager.INTERRUPTION_FILTER_UNKNOWN
            } catch (_: Exception) { false }
        )
    }

    val updateNetworkState = {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(activeNetwork)
            isDataEnabled = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        } catch (_: Exception) {}
        try {
            isWifiEnabled = wifiManager?.isWifiEnabled == true
        } catch (_: Exception) {}
        try {
            isBtEnabled = bluetoothAdapter?.isEnabled == true
        } catch (_: Exception) {}
        try {
            isHotspotEnabled = checkWifiApEnabled(context)
        } catch (_: Exception) {}
        try {
            val filter = notificationManager?.currentInterruptionFilter ?: android.app.NotificationManager.INTERRUPTION_FILTER_ALL
            isDndOn = filter != android.app.NotificationManager.INTERRUPTION_FILTER_ALL && filter != android.app.NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        } catch (_: Exception) {}
    }

    LaunchedEffect(isExpanded) {
        if (isExpanded) updateNetworkState()
    }

    DisposableEffect(context) {
        val filter = IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction("android.net.wifi.WIFI_AP_STATE_CHANGED")
            addAction(android.app.NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                        val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
                        isWifiEnabled = (state == WifiManager.WIFI_STATE_ENABLED)
                    }
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                        isBtEnabled = (state == BluetoothAdapter.STATE_ON)
                    }
                    "android.net.wifi.WIFI_AP_STATE_CHANGED" -> {
                        val state = intent.getIntExtra("wifi_state", 0)
                        isHotspotEnabled = (state == 13)
                    }
                    android.app.NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> {
                        val curFilter = notificationManager?.currentInterruptionFilter ?: android.app.NotificationManager.INTERRUPTION_FILTER_ALL
                        isDndOn = curFilter != android.app.NotificationManager.INTERRUPTION_FILTER_ALL && curFilter != android.app.NotificationManager.INTERRUPTION_FILTER_UNKNOWN
                    }
                }
            }
        }
        try {
            context.registerReceiver(receiver, filter)
        } catch (_: Exception) {}

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                isDataEnabled = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            }
            override fun onLost(network: Network) {
                updateNetworkState()
            }
        }
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm?.registerNetworkCallback(request, callback)
        } catch (_: Exception) {}

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
            try {
                cm?.unregisterNetworkCallback(callback)
            } catch (_: Exception) {}
        }
    }

    val toggleWifi: () -> Unit = {
        lastInteractionTime = System.currentTimeMillis()
        val target = !isWifiEnabled
        val shellCmd = if (target) "svc wifi enable || cmd wifi set-wifi-enabled enabled" else "svc wifi disable || cmd wifi set-wifi-enabled disabled"
        val shellSuccess = executePrivilegedCommand(shellCmd)
        if (shellSuccess) {
            isWifiEnabled = target
        } else {
            val direct = try {
                @Suppress("DEPRECATION")
                wifiManager?.setWifiEnabled(target) == true
            } catch (_: Exception) { false }
            if (direct) {
                isWifiEnabled = target
            } else {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        context.startActivity(Intent(Settings.Panel.ACTION_WIFI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } else {
                        context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                } catch (_: Exception) {
                    try {
                        context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (_: Exception) {
                        Toast.makeText(context, "Wi-Fi settings unavailable", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val toggleBluetooth: () -> Unit = {
        lastInteractionTime = System.currentTimeMillis()
        val target = !isBtEnabled
        val shellCmd = if (target) "svc bluetooth enable || cmd bluetooth_manager enable" else "svc bluetooth disable || cmd bluetooth_manager disable"
        val shellSuccess = executePrivilegedCommand(shellCmd)
        if (shellSuccess) {
            isBtEnabled = target
        } else {
            var direct = false
            if (bluetoothAdapter != null) {
                try {
                    direct = if (target) {
                        @Suppress("DEPRECATION")
                        bluetoothAdapter.enable()
                    } else {
                        @Suppress("DEPRECATION")
                        bluetoothAdapter.disable()
                    }
                } catch (_: Exception) {
                    direct = false
                }
            }
            if (direct) {
                isBtEnabled = target
            } else {
                try {
                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: Exception) {
                    Toast.makeText(context, "Bluetooth settings unavailable", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val toggleHotspot: () -> Unit = {
        lastInteractionTime = System.currentTimeMillis()
        val target = !isHotspotEnabled
        val shellCmd = if (target) {
            "svc wifi-tethering start || cmd connectivity start-tethering 0"
        } else {
            "svc wifi-tethering stop || cmd connectivity stop-tethering 0"
        }
        val shellSuccess = executePrivilegedCommand(shellCmd)
        if (shellSuccess) {
            isHotspotEnabled = target
        } else {
            val tetherIntents = listOf(
                Intent().setClassName("com.android.settings", "com.android.settings.TetherSettings"),
                Intent("android.settings.TETHER_SETTINGS"),
                Intent("android.settings.WIFI_TETHERING_SETTINGS"),
                Intent(Settings.ACTION_WIRELESS_SETTINGS)
            )
            var launched = false
            for (tIntent in tetherIntents) {
                try {
                    context.startActivity(tIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    launched = true
                    break
                } catch (_: Exception) {}
            }
            if (!launched) {
                Toast.makeText(context, "Hotspot settings unavailable", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val toggleFlashlight = {
        lastInteractionTime = System.currentTimeMillis()
        if (cameraManager != null && cameraId != null) {
            try {
                val newState = !isFlashlightOn
                cameraManager.setTorchMode(cameraId, newState)
                isFlashlightOn = newState
            } catch (_: Exception) {
                Toast.makeText(context, "Torch unavailable", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "No camera flash found", Toast.LENGTH_SHORT).show()
        }
    }

    BoxWithConstraints(
        modifier = modifier
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()

        val circleSizeDp = 56.dp
        val circleSizePx = with(density) { circleSizeDp.toPx() }
        val boxWidthDp = 290.dp
        val boxWidthPx = with(density) { boxWidthDp.toPx() }

        val activeTiles = remember(showWifi, showData, showHotspot, showFlashlight, showBluetooth, showDnd, showPhone, isWifiEnabled, isDataEnabled, isHotspotEnabled, isBtEnabled, isDndOn, isFlashlightOn) {
            val list = mutableListOf<QuickTileData>()
            if (showWifi) {
                list.add(
                    QuickTileData(
                        iconRes = R.drawable.ic_wifi,
                        title = "Wi-Fi",
                        statusText = if (isWifiEnabled) "On" else "Off",
                        isActive = isWifiEnabled,
                        activeColor = Color(0xFF2979FF),
                        onClick = toggleWifi
                    )
                )
            }
            if (showData) {
                list.add(
                    QuickTileData(
                        iconRes = R.drawable.ic_mobile_data,
                        title = "Mobile Data",
                        statusText = if (isDataEnabled) "Active" else "Off",
                        isActive = isDataEnabled,
                        activeColor = Color(0xFF00E676),
                        onClick = {
                            lastInteractionTime = System.currentTimeMillis()
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    context.startActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                } else {
                                    context.startActivity(Intent(Settings.ACTION_DATA_ROAMING_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                }
                            } catch (_: Exception) {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                } catch (_: Exception) {}
                            }
                        }
                    )
                )
            }
            if (showHotspot) {
                list.add(
                    QuickTileData(
                        iconRes = R.drawable.ic_hotspot,
                        title = "Hotspot",
                        statusText = if (isHotspotEnabled) "On" else "Off",
                        isActive = isHotspotEnabled,
                        activeColor = Color(0xFFFF9100),
                        onClick = toggleHotspot
                    )
                )
            }
            if (showFlashlight) {
                list.add(
                    QuickTileData(
                        iconRes = R.drawable.ic_flashlight,
                        title = "Flashlight",
                        statusText = if (isFlashlightOn) "Torch ON" else "Off",
                        isActive = isFlashlightOn,
                        activeColor = Color(0xFFFFD54F),
                        onClick = { toggleFlashlight() }
                    )
                )
            }
            if (showBluetooth) {
                list.add(
                    QuickTileData(
                        iconRes = R.drawable.ic_bluetooth,
                        title = "Bluetooth",
                        statusText = if (isBtEnabled) "On" else "Off",
                        isActive = isBtEnabled,
                        activeColor = Color(0xFF00B0FF),
                        onClick = toggleBluetooth
                    )
                )
            }
            if (showDnd) {
                list.add(
                    QuickTileData(
                        iconRes = R.drawable.ic_dnd,
                        title = "Do Not Disturb",
                        statusText = if (isDndOn) "Active" else "Off",
                        isActive = isDndOn,
                        activeColor = Color(0xFFE040FB),
                        onClick = {
                            lastInteractionTime = System.currentTimeMillis()
                            try {
                                if (notificationManager != null && notificationManager.isNotificationPolicyAccessGranted) {
                                    val cur = notificationManager.currentInterruptionFilter
                                    val target = if (cur == android.app.NotificationManager.INTERRUPTION_FILTER_ALL) {
                                        android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY
                                    } else {
                                        android.app.NotificationManager.INTERRUPTION_FILTER_ALL
                                    }
                                    notificationManager.setInterruptionFilter(target)
                                    isDndOn = (target != android.app.NotificationManager.INTERRUPTION_FILTER_ALL)
                                } else {
                                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                }
                            } catch (_: Exception) {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                } catch (_: Exception) {}
                            }
                        }
                    )
                )
            }
            if (showPhone) {
                list.add(
                    QuickTileData(
                        iconRes = R.drawable.call_fill0,
                        title = "Phone",
                        statusText = "Caller",
                        isActive = false,
                        activeColor = Color(0xFF00E676),
                        onClick = {
                            lastInteractionTime = System.currentTimeMillis()
                            try {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    val defaultDialer = (context.getSystemService(Context.TELECOM_SERVICE) as? android.telecom.TelecomManager)?.defaultDialerPackage
                                    if (defaultDialer != null) {
                                        val pmIntent = context.packageManager.getLaunchIntentForPackage(defaultDialer)
                                        if (pmIntent != null) context.startActivity(pmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                    }
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Phone caller unavailable", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                )
            }
            list
        }

        val tileRows = ((activeTiles.size + 1) / 2).coerceAtLeast(1)
        val notifHeight = if (showNotifications) (if (notifPreviewMode == "preview") 56.dp else 40.dp) else 0.dp
        val maxBoxHeightDp = ((screenHeightPx / density.density) - 100f).coerceIn(240f, 440f).dp
        val naturalHeightDp = (68.dp + (tileRows * 94).dp + notifHeight + 16.dp)
        val boxHeightDp = minOf(naturalHeightDp, maxBoxHeightDp)
        val boxHeightPx = with(density) { boxHeightDp.toPx() }

        val edgeMarginPx = with(density) { 12.dp.toPx() }
        val topLimitPx = with(density) { 48.dp.toPx() }
        val bottomLimitPx = (screenHeightPx - circleSizePx - with(density) { 48.dp.toPx() }).coerceAtLeast(topLimitPx)

        // Coordinates for Floating Ball
        val ballAnimX = remember(screenWidthPx) {
            Animatable((screenWidthPx - circleSizePx - edgeMarginPx).coerceAtLeast(edgeMarginPx))
        }
        val ballAnimY = remember(screenHeightPx) {
            Animatable((screenHeightPx * 0.62f).coerceIn(topLimitPx, bottomLimitPx))
        }

        // Coordinates for Movable Expanded Card
        val cardAnimX = remember(screenWidthPx) {
            Animatable(
                (screenWidthPx - boxWidthPx - with(density) { 16.dp.toPx() }).coerceIn(
                    with(density) { 16.dp.toPx() },
                    (screenWidthPx - boxWidthPx - with(density) { 16.dp.toPx() }).coerceAtLeast(16f)
                )
            )
        }
        val cardAnimY = remember(screenHeightPx) {
            Animatable((screenHeightPx * 0.55f).coerceIn(topLimitPx, (screenHeightPx - boxHeightPx - topLimitPx).coerceAtLeast(topLimitPx)))
        }

        // Auto-Hide (dim) timer when idle
        var isAutoHidden by remember { mutableStateOf(false) }
        LaunchedEffect(lastInteractionTime, isExpanded, isDraggingBall, autoHideDelaySeconds) {
            if (isExpanded || isDraggingBall || autoHideDelaySeconds <= 0) {
                isAutoHidden = false
                return@LaunchedEffect
            }
            isAutoHidden = false
            delay(autoHideDelaySeconds * 1000L)
            isAutoHidden = true
        }

        val ballAlpha by animateFloatAsState(
            targetValue = when {
                isExpanded || isDraggingBall -> 1f
                isAutoHidden -> 0.20f
                else -> 0.88f
            },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "ballAlpha"
        )

        // Smooth Container Morphing Dimensions & Position (Critically damped on collapse to completely eliminate squeezing)
        val morphSpecDp: androidx.compose.animation.core.AnimationSpec<androidx.compose.ui.unit.Dp> = if (isExpanded) {
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        }

        val morphSpecFloat: androidx.compose.animation.core.AnimationSpec<Float> = if (isExpanded) {
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        }

        val morphWidth by animateDpAsState(
            targetValue = if (isExpanded) boxWidthDp else circleSizeDp,
            animationSpec = morphSpecDp,
            label = "morphWidth"
        )

        val morphHeight by animateDpAsState(
            targetValue = if (isExpanded) boxHeightDp else circleSizeDp,
            animationSpec = morphSpecDp,
            label = "morphHeight"
        )

        val targetMorphX = if (isExpanded) cardAnimX.value else ballAnimX.value
        val targetMorphY = if (isExpanded) cardAnimY.value else ballAnimY.value

        val morphX by animateFloatAsState(
            targetValue = targetMorphX,
            animationSpec = morphSpecFloat,
            label = "morphX"
        )

        val morphY by animateFloatAsState(
            targetValue = targetMorphY,
            animationSpec = morphSpecFloat,
            label = "morphY"
        )

        // Backdrop to dismiss expanded card when tapping outside
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                        }
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isExpanded = false
                        lastInteractionTime = System.currentTimeMillis()
                    }
            )
        }

        // --- SINGLE CONTINUOUS MORPHING SURFACE ---
        Box(
            modifier = Modifier
                .offset { IntOffset(morphX.roundToInt(), morphY.roundToInt()) }
                .size(width = morphWidth, height = morphHeight)
                .alpha(if (isExpanded) 1f else ballAlpha)
                // Consume pointer down to prevent triggering root kiosk exit
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                    }
                }
                .pointerInput(isExpanded, screenWidthPx, screenHeightPx) {
                    if (!isExpanded) {
                        detectDragGestures(
                            onDragStart = {
                                isDraggingBall = true
                                lastInteractionTime = System.currentTimeMillis()
                            },
                            onDragEnd = {
                                isDraggingBall = false
                                lastInteractionTime = System.currentTimeMillis()
                                // Auto-move smoothly to nearest horizontal edge
                                val midX = ballAnimX.value + (circleSizePx / 2f)
                                val targetEdgeX = if (midX < screenWidthPx / 2f) {
                                    edgeMarginPx
                                } else {
                                    (screenWidthPx - circleSizePx - edgeMarginPx).coerceAtLeast(edgeMarginPx)
                                }
                                coroutineScope.launch {
                                    ballAnimX.animateTo(
                                        targetValue = targetEdgeX,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            },
                            onDragCancel = {
                                isDraggingBall = false
                                lastInteractionTime = System.currentTimeMillis()
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            lastInteractionTime = System.currentTimeMillis()
                            coroutineScope.launch {
                                ballAnimX.snapTo((ballAnimX.value + dragAmount.x).coerceIn(0f, screenWidthPx - circleSizePx))
                                ballAnimY.snapTo((ballAnimY.value + dragAmount.y).coerceIn(topLimitPx, bottomLimitPx))
                            }
                        }
                    }
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (!isExpanded) {
                        lastInteractionTime = System.currentTimeMillis()
                        val cardMargin = with(density) { 16.dp.toPx() }
                        val targetCardPosX = ballAnimX.value.coerceIn(
                            cardMargin,
                            (screenWidthPx - boxWidthPx - cardMargin).coerceAtLeast(cardMargin)
                        )
                        val targetCardPosY = ballAnimY.value.coerceIn(
                            topLimitPx,
                            (screenHeightPx - boxHeightPx - topLimitPx).coerceAtLeast(topLimitPx)
                        )
                        coroutineScope.launch {
                            cardAnimX.snapTo(targetCardPosX)
                            cardAnimY.snapTo(targetCardPosY)
                        }
                        isExpanded = true
                        updateNetworkState()
                    }
                }
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = if (isExpanded) Color(0xF71B1C22) else Color(0x99121316),
                border = BorderStroke(1.dp, if (isExpanded) Color(0x33FFFFFF) else Color(0x35FFFFFF)),
                shadowElevation = if (isExpanded) 18.dp else 8.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                Crossfade(
                    targetState = isExpanded,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "morphContent"
                ) { expanded ->
                    if (!expanded) {
                        // Concentric disc inside the 56dp circle
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0x30FFFFFF),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0x45FFFFFF),
                                        border = BorderStroke(0.8.dp, Color(0x55FFFFFF)),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (isFlashlightOn) Color(0xFFFFC107) else Color(0xFAFFFFFF),
                                                modifier = Modifier.size(13.dp)
                                            ) {}
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Expanded Quick Controls Bento Grid
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Top Drag Handle & Header Area (Card dragging is isolated to this header)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(screenWidthPx, screenHeightPx, boxWidthPx, boxHeightPx) {
                                        detectDragGestures(
                                            onDragStart = {
                                                isDraggingCard = true
                                                lastInteractionTime = System.currentTimeMillis()
                                            },
                                            onDragEnd = {
                                                isDraggingCard = false
                                                lastInteractionTime = System.currentTimeMillis()
                                            },
                                            onDragCancel = {
                                                isDraggingCard = false
                                                lastInteractionTime = System.currentTimeMillis()
                                            }
                                        ) { change, dragAmount ->
                                            change.consume()
                                            lastInteractionTime = System.currentTimeMillis()
                                            coroutineScope.launch {
                                                val cardMargin = with(density) { 12.dp.toPx() }
                                                cardAnimX.snapTo(
                                                    (cardAnimX.value + dragAmount.x).coerceIn(
                                                        cardMargin,
                                                        (screenWidthPx - boxWidthPx - cardMargin).coerceAtLeast(cardMargin)
                                                    )
                                                )
                                                cardAnimY.snapTo(
                                                    (cardAnimY.value + dragAmount.y).coerceIn(
                                                        topLimitPx,
                                                        (screenHeightPx - boxHeightPx - topLimitPx).coerceAtLeast(topLimitPx)
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0x35FFFFFF),
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(4.dp)
                                ) {}

                                Spacer(Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = colorScheme.primaryContainer.copy(alpha = 0.35f),
                                        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.4f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_assistive_touch),
                                                contentDescription = null,
                                                tint = colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "Quick Controls",
                                                style = typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0x22FFFFFF),
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                isExpanded = false
                                                lastInteractionTime = System.currentTimeMillis()
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                painter = painterResource(R.drawable.close_fill0),
                                                contentDescription = "Close",
                                                tint = Color(0xEEFFFFFF),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Freely Scrollable Quick Tiles Container
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .verticalScroll(rememberScrollState())
                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (showNotifications) {
                                    NotificationControlPill(
                                        previewMode = notifPreviewMode,
                                        onClick = {
                                            lastInteractionTime = System.currentTimeMillis()
                                            expandNotificationShade(context)
                                        }
                                    )
                                }

                                activeTiles.chunked(2).forEach { rowTiles ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        rowTiles.forEach { tile ->
                                            ExpressiveQuickTile(
                                                iconRes = tile.iconRes,
                                                title = tile.title,
                                                statusText = tile.statusText,
                                                isActive = tile.isActive,
                                                activeColor = tile.activeColor,
                                                modifier = Modifier.weight(1f),
                                                onClick = tile.onClick
                                            )
                                        }
                                        if (rowTiles.size == 1) {
                                            Spacer(Modifier.weight(1f))
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
}

private data class QuickTileData(
    val iconRes: Int,
    val title: String,
    val statusText: String,
    val isActive: Boolean,
    val activeColor: Color,
    val onClick: () -> Unit
)

@Composable
private fun NotificationControlPill(
    previewMode: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color(0x22FFFFFF),
        border = BorderStroke(1.dp, Color(0x33FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (previewMode == "preview") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0x33FFFFFF),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_notifications),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Notifications Muted",
                        style = typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Focus lockdown active • Tap to expand notification shade",
                        style = typography.bodySmall.copy(fontSize = 10.sp),
                        color = Color(0xBBFFFFFF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_notifications),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Notification Shade",
                        style = typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = Color(0x33FFFFFF)
                ) {
                    Text(
                        text = "Expand",
                        style = typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

private fun expandNotificationShade(context: Context) {
    try {
        val sbservice = context.getSystemService("statusbar")
        val statusbarManager = Class.forName("android.app.StatusBarManager")
        val showsb = statusbarManager.getMethod("expandNotificationsPanel")
        showsb.invoke(sbservice)
    } catch (_: Exception) {
        try {
            val sbservice = context.getSystemService("statusbar")
            val statusbarManager = Class.forName("android.app.StatusBarManager")
            val showsb = statusbarManager.getMethod("expand")
            showsb.invoke(sbservice)
        } catch (_: Exception) {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) {
                Toast.makeText(context, "Notification shade unavailable", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

/**
 * Material 3 Expressive Quick Tile component.
 */
@Composable
private fun ExpressiveQuickTile(
    iconRes: Int,
    title: String,
    statusText: String,
    isActive: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val containerBg by animateColorAsState(
        targetValue = if (isActive) activeColor.copy(alpha = 0.20f) else Color(0x1CFFFFFF),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tileContainerBg"
    )

    val iconBoxBg by animateColorAsState(
        targetValue = if (isActive) activeColor else Color(0x28FFFFFF),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "iconBoxBg"
    )

    val iconTint by animateColorAsState(
        targetValue = if (isActive) Color(0xFF101114) else Color(0xEEFFFFFF),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tileIconTint"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = containerBg,
        border = BorderStroke(
            width = 1.dp,
            color = if (isActive) activeColor.copy(alpha = 0.5f) else Color(0x1AFFFFFF)
        ),
        modifier = modifier.height(84.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top row: Squircle icon + status indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = iconBoxBg,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = title,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = if (isActive) activeColor.copy(alpha = 0.25f) else Color(0x1AFFFFFF)
                ) {
                    Text(
                        text = if (isActive) "ON" else "OFF",
                        style = typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 8.sp
                        ),
                        color = if (isActive) activeColor else Color(0x88FFFFFF),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Bottom row: Title & status
            Column {
                Text(
                    text = title,
                    style = typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = statusText,
                    style = typography.bodySmall.copy(
                        fontSize = 10.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (isActive) activeColor else Color(0x88FFFFFF),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
