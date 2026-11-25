package com.hotaro.brik

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    if (currentSubScreen == "keys") {
        BackHandler { currentSubScreen = null }
        KeysWindow(onBack = { currentSubScreen = null })
        return
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
}
