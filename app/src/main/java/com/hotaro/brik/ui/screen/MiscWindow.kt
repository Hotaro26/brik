package com.hotaro.brik.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hotaro.brik.R
import com.hotaro.brik.ui.component.TomatoSectionHeader
import com.hotaro.brik.ui.theme.ThemePreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiscWindow(onBack: () -> Unit) {
    val context = LocalContext.current
    val use24Hour by ThemePreferences.use24HourClock.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Miscellaneous",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        letterSpacing = (-0.5).sp,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "General app-wide preferences",
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Spacer(Modifier.height(10.dp))

            TomatoSectionHeader(title = "DATE & TIME")

            // Clock format card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colorScheme.secondaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.schedule_fill0),
                                contentDescription = null,
                                tint = colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Clock Format",
                            style = typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = if (use24Hour) "24-hour (14:30)" else "12-hour (2:30 PM)",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = use24Hour,
                        onCheckedChange = { ThemePreferences.setUse24HourClock(context, it) }
                    )
                }
            }

            // Format preview card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colorScheme.surfaceContainerLowest,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Preview",
                            style = typography.labelMedium,
                            color = colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(2.dp))
                        val previewTime = remember(use24Hour) {
                            if (use24Hour) "14:30 — 09:05" else "2:30 PM — 9:05 AM"
                        }
                        Text(
                            text = previewTime,
                            style = typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Used across routines, app limits, and time displays",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}
