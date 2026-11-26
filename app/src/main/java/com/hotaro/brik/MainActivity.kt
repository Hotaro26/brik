package com.hotaro.brik

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hotaro.brik.ui.component.BrikFloatingNavBar
import com.hotaro.brik.ui.component.FloatingNavItem
import com.hotaro.brik.ui.glass.BrikGlassNavBar
import com.hotaro.brik.ui.glass.LocalAppBackdrop
import com.hotaro.brik.ui.glass.isGlassAllowed
import com.hotaro.brik.ui.glass.backdrop.backdrops.layerBackdrop
import com.hotaro.brik.ui.glass.backdrop.backdrops.rememberLayerBackdrop
import com.hotaro.brik.ui.theme.NavBarStyle
import com.hotaro.brik.ui.screen.AppManagerWindow
import com.hotaro.brik.ui.screen.BagsWindow
import com.hotaro.brik.ui.screen.KeyScreen
import com.hotaro.brik.ui.screen.KeysWindow
import com.hotaro.brik.ui.screen.ModesWindow
import com.hotaro.brik.ui.screen.RoutinesWindow
import com.hotaro.brik.ui.screen.SettingsScreen
import com.hotaro.brik.ui.screen.ToolsScreen
import com.hotaro.brik.util.StealthUtils
import com.hotaro.brik.ui.screen.ZenWindow
import com.hotaro.brik.ui.screen.GrayscaleWindow
import com.hotaro.brik.ui.screen.AboutWindow
import com.hotaro.brik.ui.screen.EmergencyModeWindow
import com.hotaro.brik.ui.screen.MiscWindow
import com.hotaro.brik.ui.screen.AssistiveSettingsWindow
import com.hotaro.brik.ui.screen.KioskSettingsWindow
import com.hotaro.brik.util.EmergencyManager
import com.hotaro.brik.ui.theme.ThemePreferences
import com.hotaro.brik.ui.theme.BrikTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemePreferences.init(this)
        EmergencyManager.init(this)

        // Ensure active launcher alias is enabled if stealth mode is inactive
        if (!StealthUtils.isAppHidden(this)) {
            try {
                val pm = packageManager
                val activeAlias = StealthUtils.getActiveAliasComponent(this)
                val state = pm.getComponentEnabledSetting(activeAlias)
                if (state != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    pm.setComponentEnabledSetting(
                        activeAlias,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        enableEdgeToEdge()
        setContent {
            BrikTheme {
                val isWindDownActive by com.hotaro.brik.util.WindDownManager.isWindDownActive.collectAsState()
                val isZenActive by com.hotaro.brik.util.ZenManager.isZenActive.collectAsState()
                val allowControlPanelsStatusBar by com.hotaro.brik.util.KioskPreferences.allowControlPanelsStatusBar.collectAsState()
                val allowSystemGestures by com.hotaro.brik.util.KioskPreferences.allowSystemGestures.collectAsState()
                val timeBlockRepo = remember { com.hotaro.brik.data.repository.TimeBlockRepository.getInstance(applicationContext) }
                val activeSession by timeBlockRepo.activeSession.collectAsState()
                val focusModes by timeBlockRepo.focusModes.collectAsState()
                val activeKioskFocusMode = remember(activeSession, focusModes) {
                    if (activeSession?.type == com.hotaro.brik.data.model.TimeBlockType.FOCUS_MODE) {
                        focusModes.find { it.id == activeSession?.id && it.isRunning && it.enableKioskMode }
                    } else null
                }

                androidx.compose.runtime.LaunchedEffect(isWindDownActive, isZenActive, activeKioskFocusMode, allowControlPanelsStatusBar, allowSystemGestures) {
                    if (activeKioskFocusMode != null) {
                        try {
                            val defaultDialer = (getSystemService(android.content.Context.TELECOM_SERVICE) as? android.telecom.TelecomManager)?.defaultDialerPackage
                            val systemPackages = listOfNotNull(
                                packageName,
                                "com.android.settings",
                                defaultDialer,
                                "com.google.android.dialer",
                                "com.android.dialer",
                                "com.samsung.android.dialer"
                            )
                            val packagesToAllow = (activeKioskFocusMode.kioskAllowedPackages + systemPackages).distinct()
                            com.hotaro.brik.policy.PolicyBridgeManager.setLockTaskPackages(this@MainActivity, packagesToAllow)
                            val disableGestures = !allowSystemGestures || activeKioskFocusMode.kioskDisableSystemGestures
                            com.hotaro.brik.policy.PolicyBridgeManager.setLockTaskFeatures(
                                this@MainActivity,
                                disableSystemGestures = disableGestures,
                                lockdownNotificationShade = !allowControlPanelsStatusBar
                            )
                            startLockTask()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else if (!isWindDownActive && !isZenActive) {
                        try {
                            stopLockTask()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isWindDownActive) {
                        com.hotaro.brik.ui.screen.WindDownDesktop()
                    } else if (activeKioskFocusMode != null) {
                        com.hotaro.brik.ui.screen.ModeKioskDesktop(
                            mode = activeKioskFocusMode,
                            sessionEndsAt = activeKioskFocusMode.sessionEndsAt,
                            onExitSession = {
                                timeBlockRepo.stopFocusMode(activeKioskFocusMode.id, activeKioskFocusMode.keySecret ?: "") { success, _ ->
                                    if (success) {
                                        try {
                                            stopLockTask()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                        android.widget.Toast.makeText(this@MainActivity, "Exited Focus Mode", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    } else {
                        BrikMainScreen()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrikMainScreen() {
    val isZenActive by com.hotaro.brik.util.ZenManager.isZenActive.collectAsState()
    var selectedIndex by remember { mutableIntStateOf(0) }
    val subScreenHistory = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    var currentSubScreen by remember { mutableStateOf<String?>(if (com.hotaro.brik.util.ZenManager.isZenActive.value) "zen" else null) }
    val animatedBackProgress = remember { Animatable(0f) }
    var isPredictiveBackActive by remember { mutableStateOf(false) }
    var backSwipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    androidx.compose.runtime.LaunchedEffect(isZenActive) {
        if (isZenActive) {
            currentSubScreen = "zen"
        }
    }

    // Synchronize and enforce anti-uninstall and suspension on active locks
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val keyRepo = com.hotaro.brik.data.repository.KeyRepository(context)
            val lockedPackages = keyRepo.activeLocks.value.flatMap { it.packageNames }.distinct()
            if (lockedPackages.isNotEmpty()) {
                com.hotaro.brik.policy.PolicyBridgeManager.suspendPackages(context, lockedPackages, true)
                com.hotaro.brik.policy.PolicyBridgeManager.setUninstallBlocked(context, lockedPackages, true)
            }
            com.hotaro.brik.data.repository.TimeBlockRepository(context).syncActiveBlocks(context)
        }
    }

    // Tomato-styled Predictive Back gesture handler (disabled during active Zen session)
    PredictiveBackHandler(enabled = currentSubScreen != null && !(currentSubScreen == "zen" && isZenActive)) { progress: Flow<BackEventCompat> ->
        isPredictiveBackActive = true
        try {
            progress.collect { backEvent ->
                backSwipeEdge = backEvent.swipeEdge
                animatedBackProgress.snapTo(backEvent.progress)
            }
            // Swipe finished: animate smoothly off-screen
            animatedBackProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 180)
            )
            if (subScreenHistory.isNotEmpty()) {
                currentSubScreen = subScreenHistory.removeAt(subScreenHistory.lastIndex)
            } else {
                currentSubScreen = null
            }
        } catch (e: CancellationException) {
            // Swipe cancelled: spring smoothly back into place
            animatedBackProgress.animateTo(
                targetValue = 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
        } finally {
            isPredictiveBackActive = false
            animatedBackProgress.snapTo(0f)
        }
    }

    val onNavigateToSubScreen: (String) -> Unit = { screen ->
        coroutineScope.launch {
            currentSubScreen?.let { subScreenHistory.add(it) }
            animatedBackProgress.snapTo(1f)
            currentSubScreen = screen
            animatedBackProgress.animateTo(0f, tween(durationMillis = 260))
        }
    }

    val onBackFromSubScreen: () -> Unit = {
        coroutineScope.launch {
            animatedBackProgress.animateTo(1f, tween(durationMillis = 200))
            if (subScreenHistory.isNotEmpty()) {
                currentSubScreen = subScreenHistory.removeAt(subScreenHistory.lastIndex)
            } else {
                currentSubScreen = null
            }
            animatedBackProgress.snapTo(0f)
        }
    }

    val navItems = listOf(
        FloatingNavItem(
            label = "Key",
            selectedIcon = painterResource(R.drawable.key_fill0),
            unselectedIcon = painterResource(R.drawable.key_fill0)
        ),
        FloatingNavItem(
            label = "Tools",
            selectedIcon = painterResource(R.drawable.tools_fill0),
            unselectedIcon = painterResource(R.drawable.tools_outlined)
        ),
        FloatingNavItem(
            label = "Settings",
            selectedIcon = painterResource(R.drawable.settings_filled),
            unselectedIcon = painterResource(R.drawable.settings)
        )
    )

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val appBackground = if (isDark) Color.Black else MaterialTheme.colorScheme.secondaryContainer

    val appBackdrop = rememberLayerBackdrop()
    val navBarStyle by ThemePreferences.navBarStyle.collectAsState()

    CompositionLocalProvider(
        LocalAppBackdrop provides appBackdrop
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(appBackground)
        ) {
            val screenWidthPx = constraints.maxWidth.toFloat()
            val p = animatedBackProgress.value
            val isSubScreenVisible = currentSubScreen != null || isPredictiveBackActive || p > 0f

            // Bottom layer: Main Scaffold (Tomato spec: shifts by -screenWidth/4 and dims to 0.75 when sub-screen is active)
            val mainOffsetX = if (isSubScreenVisible) {
                (-screenWidthPx / 4f) * (1f - p)
            } else {
                0f
            }
            val mainAlpha = if (isSubScreenVisible) {
                0.75f + (p * 0.25f)
            } else {
                1f
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(appBackground)
                    .graphicsLayer {
                        translationX = mainOffsetX
                        alpha = mainAlpha
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .then(
                            if (navBarStyle == NavBarStyle.GLASS && isGlassAllowed()) {
                                Modifier.layerBackdrop(appBackdrop)
                            } else Modifier
                        )
                ) {
                    Crossfade(
                        targetState = selectedIndex,
                        label = "tab_transition",
                        modifier = Modifier.fillMaxSize()
                    ) { tab ->
                        when (tab) {
                            0 -> KeyScreen(
                                onNavigateToZen = { onNavigateToSubScreen("zen") }
                            )
                            1 -> ToolsScreen(
                                onNavigateToAppManager = { onNavigateToSubScreen("app_manager") },
                                onNavigateToBags = { onNavigateToSubScreen("bags") },
                                onNavigateToRoutines = { onNavigateToSubScreen("routines") },
                                onNavigateToModes = { onNavigateToSubScreen("modes") },
                                onNavigateToZen = { onNavigateToSubScreen("zen") },
                                onNavigateToGrayscale = { onNavigateToSubScreen("grayscale") }
                            )
                            2 -> SettingsScreen(
                                onNavigateToKeys = { onNavigateToSubScreen("keys") },
                                onNavigateToAbout = { onNavigateToSubScreen("about") },
                                onNavigateToEmergency = { onNavigateToSubScreen("emergency") },
                                onNavigateToMisc = { onNavigateToSubScreen("misc") }
                            )
                        }
                    }
                }

                when (navBarStyle) {
                    NavBarStyle.FLOATING -> {
                        BrikFloatingNavBar(
                            items = navItems,
                            selectedIndex = selectedIndex,
                            onItemSelected = { selectedIndex = it },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                    NavBarStyle.GLASS -> {
                        BrikGlassNavBar(
                            items = navItems,
                            selectedIndex = selectedIndex,
                            onItemSelected = { selectedIndex = it },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                    NavBarStyle.CLASSIC -> {
                        NavigationBar(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
                            containerColor = if (isDark) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                            tonalElevation = 4.dp
                        ) {
                            navItems.forEachIndexed { index, item ->
                                val isSelected = selectedIndex == index
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { selectedIndex = index },
                                    icon = {
                                        Icon(
                                            painter = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = item.label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }

            if (isSubScreenVisible) {
                // Scrim touch barrier: completely intercepts clicks so they cannot reach the bottom layer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* Absorb any touches */ }
                )
            }

            // Background layer for nested sub-screen (e.g., MiscWindow behind AssistiveSettingsWindow)
            val previousSubScreen = subScreenHistory.lastOrNull()
            if (isSubScreenVisible && previousSubScreen != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = (-screenWidthPx / 4f) * (1f - p)
                            alpha = 0.75f + (p * 0.25f)
                        }
                        .background(appBackground)
                ) {
                    RenderSubScreen(
                        screen = previousSubScreen,
                        isZenActive = isZenActive,
                        onBack = onBackFromSubScreen,
                        onNavigate = onNavigateToSubScreen
                    )
                }
            }

            // Top layer: Sub-screen with predictive slide transition
            if (isSubScreenVisible && currentSubScreen != null) {
                val keyOffsetX = screenWidthPx * p
                val keyScale = 1f - (p * 0.05f)
                val keyCorners = (p * 24).dp
                val keyAlpha = 1f - (p * 0.15f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = keyOffsetX
                            scaleX = keyScale
                            scaleY = keyScale
                            alpha = keyAlpha
                            clip = p > 0f
                            shape = RoundedCornerShape(keyCorners)
                            shadowElevation = if (p > 0f) 24.dp.toPx() else 0f
                        }
                        .background(appBackground)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* Absorb blank space clicks */ }
                ) {
                    RenderSubScreen(
                        screen = currentSubScreen!!,
                        isZenActive = isZenActive,
                        onBack = onBackFromSubScreen,
                        onNavigate = onNavigateToSubScreen
                    )
                }
            }
        }
    }
}

@Composable
private fun RenderSubScreen(
    screen: String,
    isZenActive: Boolean,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    when (screen) {
        "keys" -> KeysWindow(onBack = onBack)
        "app_manager" -> AppManagerWindow(onBack = onBack)
        "bags" -> BagsWindow(onBack = onBack)
        "routines" -> RoutinesWindow(onBack = onBack)
        "modes" -> ModesWindow(onBack = onBack)
        "zen" -> ZenWindow(onBack = { if (!isZenActive) onBack() })
        "grayscale" -> GrayscaleWindow(onBack = onBack)
        "about" -> AboutWindow(onBack = onBack)
        "emergency" -> EmergencyModeWindow(onBack = onBack)
        "misc" -> MiscWindow(
            onBack = onBack,
            onNavigateToAssistiveSettings = { onNavigate("assistive_settings") },
            onNavigateToKioskSettings = { onNavigate("kiosk_settings") }
        )
        "assistive_settings" -> AssistiveSettingsWindow(onBack = onBack)
        "kiosk_settings" -> KioskSettingsWindow(onBack = onBack)
    }
}
