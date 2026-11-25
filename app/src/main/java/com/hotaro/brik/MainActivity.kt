package com.hotaro.brik

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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.hotaro.brik.ui.screen.AppManagerWindow
import com.hotaro.brik.ui.screen.BagsWindow
import com.hotaro.brik.ui.screen.KeyScreen
import com.hotaro.brik.ui.screen.KeysWindow
import com.hotaro.brik.ui.screen.ModesWindow
import com.hotaro.brik.ui.screen.RoutinesWindow
import com.hotaro.brik.ui.screen.SettingsScreen
import com.hotaro.brik.ui.screen.ToolsScreen
import com.hotaro.brik.ui.theme.ThemePreferences
import com.hotaro.brik.ui.theme.BrikTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemePreferences.init(this)
        enableEdgeToEdge()
        setContent {
            BrikTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BrikMainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrikMainScreen() {
    var selectedIndex by remember { mutableIntStateOf(0) }
    var currentSubScreen by remember { mutableStateOf<String?>(null) }
    val animatedBackProgress = remember { Animatable(0f) }
    var isPredictiveBackActive by remember { mutableStateOf(false) }
    var backSwipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Synchronize and enforce anti-uninstall and suspension on active locks
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val keyRepo = com.hotaro.brik.data.repository.KeyRepository(context)
            val lockedPackages = keyRepo.activeLocks.value.flatMap { it.packageNames }.distinct()
            if (lockedPackages.isNotEmpty()) {
                com.hotaro.brik.policy.PolicyBridgeManager.suspendPackages(context, lockedPackages, true)
                com.hotaro.brik.policy.PolicyBridgeManager.setUninstallBlocked(context, lockedPackages, true)
            }
        }
    }

    // Tomato-styled Predictive Back gesture handler
    PredictiveBackHandler(enabled = currentSubScreen != null) { progress: Flow<BackEventCompat> ->
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
            currentSubScreen = null
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
            animatedBackProgress.snapTo(1f)
            currentSubScreen = screen
            animatedBackProgress.animateTo(0f, tween(durationMillis = 260))
        }
    }

    val onBackFromSubScreen: () -> Unit = {
        coroutineScope.launch {
            animatedBackProgress.animateTo(1f, tween(durationMillis = 200))
            currentSubScreen = null
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
            ) {
                Crossfade(
                    targetState = selectedIndex,
                    label = "tab_transition",
                    modifier = Modifier.fillMaxSize()
                ) { tab ->
                    when (tab) {
                        0 -> KeyScreen()
                        1 -> ToolsScreen(
                            onNavigateToAppManager = { onNavigateToSubScreen("app_manager") },
                            onNavigateToBags = { onNavigateToSubScreen("bags") },
                            onNavigateToRoutines = { onNavigateToSubScreen("routines") },
                            onNavigateToModes = { onNavigateToSubScreen("modes") }
                        )
                        2 -> SettingsScreen(onNavigateToKeys = { onNavigateToSubScreen("keys") })
                    }
                }

                BrikFloatingNavBar(
                    items = navItems,
                    selectedIndex = selectedIndex,
                    onItemSelected = { selectedIndex = it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // Top layer: Sub-screen (KeysWindow, AppManagerWindow, BagsWindow, RoutinesWindow, ModesWindow) with predictive slide transition
        if (isSubScreenVisible) {
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
            ) {
                when (currentSubScreen) {
                    "keys" -> KeysWindow(onBack = onBackFromSubScreen)
                    "app_manager" -> AppManagerWindow(onBack = onBackFromSubScreen)
                    "bags" -> BagsWindow(onBack = onBackFromSubScreen)
                    "routines" -> RoutinesWindow(onBack = onBackFromSubScreen)
                    "modes" -> ModesWindow(onBack = onBackFromSubScreen)
                }
            }
        }
    }
}
