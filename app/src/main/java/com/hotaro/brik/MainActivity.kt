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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hotaro.brik.ui.component.BrikFloatingNavBar
import com.hotaro.brik.ui.component.FloatingNavItem
import com.hotaro.brik.ui.screen.KeyScreen
import com.hotaro.brik.ui.screen.KeysWindow
import com.hotaro.brik.ui.screen.SettingsScreen
import com.hotaro.brik.ui.screen.ToolsScreen
import com.hotaro.brik.ui.theme.BrikTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BrikTheme {
                BrikMainScreen()
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

    // Tomato-styled Predictive Back gesture handler
    PredictiveBackHandler(enabled = currentSubScreen == "keys") { progress: Flow<BackEventCompat> ->
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

    val onNavigateToKeys: () -> Unit = {
        coroutineScope.launch {
            animatedBackProgress.snapTo(1f)
            currentSubScreen = "keys"
            animatedBackProgress.animateTo(0f, tween(durationMillis = 260))
        }
    }

    val onBackFromKeys: () -> Unit = {
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
            selectedIcon = painterResource(R.drawable.backpack_fill0),
            unselectedIcon = painterResource(R.drawable.backpack_fill0)
        ),
        FloatingNavItem(
            label = "Settings",
            selectedIcon = painterResource(R.drawable.settings_filled),
            unselectedIcon = painterResource(R.drawable.settings)
        )
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val p = animatedBackProgress.value
        val isSubScreenVisible = currentSubScreen == "keys" || isPredictiveBackActive || p > 0f

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
                .graphicsLayer {
                    translationX = mainOffsetX
                    alpha = mainAlpha
                }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_brik),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Brik",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        ),
                        modifier = Modifier.statusBarsPadding()
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                ) {
                    Crossfade(
                        targetState = selectedIndex,
                        label = "tab_transition",
                        modifier = Modifier.fillMaxSize()
                    ) { tab ->
                        when (tab) {
                            0 -> KeyScreen()
                            1 -> ToolsScreen()
                            2 -> SettingsScreen(onNavigateToKeys = onNavigateToKeys)
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
        }

        // Top layer: Sub-screen (KeysWindow) with Tomato-matched predictive slide transition
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
                KeysWindow(onBack = onBackFromKeys)
            }
        }
    }
}
