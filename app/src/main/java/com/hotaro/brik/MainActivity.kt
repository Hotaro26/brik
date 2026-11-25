package com.hotaro.brik

import android.os.Bundle
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    var backProgress by remember { mutableFloatStateOf(0f) }
    var backSwipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }
    var isPredictiveBackActive by remember { mutableStateOf(false) }

    // Predictive back gesture handler for sub-screens
    PredictiveBackHandler(enabled = currentSubScreen == "keys") { progress: Flow<BackEventCompat> ->
        isPredictiveBackActive = true
        try {
            progress.collect { backEvent ->
                backProgress = backEvent.progress
                backSwipeEdge = backEvent.swipeEdge
            }
            currentSubScreen = null
        } catch (e: CancellationException) {
            // Cancelled: user dragged back to edge
        } finally {
            isPredictiveBackActive = false
            backProgress = 0f
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.ic_brik),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
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
                        2 -> SettingsScreen(onNavigateToKeys = { currentSubScreen = "keys" })
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

        // Sub-screen (KeysWindow) with native predictive back gesture animation
        if (currentSubScreen == "keys" || isPredictiveBackActive) {
            val scale = 1f - (backProgress * 0.12f)
            val translationX = if (backSwipeEdge == BackEventCompat.EDGE_LEFT) {
                (backProgress * 80).dp
            } else {
                -(backProgress * 80).dp
            }
            val cornerRadius = (backProgress * 32).dp
            val alpha = 1f - (backProgress * 0.25f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.translationX = translationX.toPx()
                        this.alpha = alpha
                        this.clip = backProgress > 0f
                        this.shape = RoundedCornerShape(cornerRadius)
                        this.shadowElevation = if (backProgress > 0f) 24.dp.toPx() else 0f
                    }
            ) {
                KeysWindow(onBack = { currentSubScreen = null })
            }
        }
    }
}
