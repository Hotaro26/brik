package com.hotaro.brik.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hotaro.brik.R
import com.hotaro.brik.ui.component.RoundedPullRefreshPage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesWindow(
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    RoundedPullRefreshPage(
        isRefreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                kotlinx.coroutines.delay(650)
                isRefreshing = false
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Routines",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Automated schedules & lock rules",
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
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = colorScheme.primaryContainer,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.schedule_fill0),
                            contentDescription = null,
                            tint = colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "No Routines Configured",
                    style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = colorScheme.onSurface
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Schedule automatic bag enforcement rules during specific hours or days.",
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}
