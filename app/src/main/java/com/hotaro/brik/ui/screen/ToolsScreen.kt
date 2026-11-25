package com.hotaro.brik.ui.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import com.hotaro.brik.ui.viewmodel.ToolsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    modifier: Modifier = Modifier,
    viewModel: ToolsViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showAppManagerSheet by remember { mutableStateOf(false) }
    var showBagsSheet by remember { mutableStateOf(false) }
    var showCreateBagDialog by remember { mutableStateOf(false) }

    val bags by viewModel.bags.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val isLoadingApps by viewModel.isLoadingApps.collectAsState()
    val selectedPackages by viewModel.selectedPackages.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val appManagerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val bagsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Tools",
            style = typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = colorScheme.onSurface
        )

        // Section 1: App Manager Card
        ElevatedCard(
            onClick = { showAppManagerSheet = true },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colorScheme.primaryContainer,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.apps_fill0),
                            contentDescription = null,
                            tint = colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "App Manager",
                        style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = if (installedApps.isNotEmpty()) "${installedApps.size} apps available" else "Manage and inspect applications",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Section 2: Bags Card
        ElevatedCard(
            onClick = { showBagsSheet = true },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colorScheme.secondaryContainer,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.backpack_fill0),
                            contentDescription = null,
                            tint = colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Bags",
                        style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = if (bags.isNotEmpty()) "${bags.size} bags configured" else "Pack apps into custom bundles",
                        style = typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(130.dp))
    }

    // --- App Manager Bottom Sheet ---
    if (showAppManagerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAppManagerSheet = false },
            sheetState = appManagerSheetState,
            containerColor = colorScheme.surfaceContainerLowest,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Box(modifier = Modifier.fillMaxHeight(0.85f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "App Manager",
                                style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = if (selectedPackages.isNotEmpty()) "${selectedPackages.size} selected" else "${installedApps.size} apps installed",
                                style = typography.bodyMedium,
                                color = colorScheme.primary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedPackages.isNotEmpty()) {
                                TextButton(onClick = { viewModel.clearSelection() }) {
                                    Text("Clear", color = colorScheme.error)
                                }
                            }
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    appManagerSheetState.hide()
                                    showAppManagerSheet = false
                                }
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.close_fill0),
                                    contentDescription = "Close",
                                    tint = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search apps...") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.search_fill0),
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                    Icon(
                                        painter = painterResource(R.drawable.close_fill0),
                                        contentDescription = "Clear search",
                                        tint = colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = colorScheme.surfaceContainerLow,
                            focusedBorderColor = colorScheme.primary,
                            unfocusedBorderColor = colorScheme.outlineVariant
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    // Apps List or Loading State
                    if (isLoadingApps) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = colorScheme.primary)
                        }
                    } else if (filteredApps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No apps found",
                                style = typography.bodyLarge,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredApps, key = { it.packageName }) { app ->
                                AppListItem(
                                    app = app,
                                    isSelected = selectedPackages.contains(app.packageName),
                                    onToggle = { viewModel.toggleAppSelection(app.packageName) }
                                )
                            }
                        }
                    }
                }

                // Floating Action Button with Plus Icon to make a Bag
                FloatingActionButton(
                    onClick = {
                        if (selectedPackages.isEmpty()) {
                            Toast.makeText(context, "Select at least 1 app to create a bag", Toast.LENGTH_SHORT).show()
                        } else {
                            showCreateBagDialog = true
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp),
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.add_fill0),
                            contentDescription = "Create Bag"
                        )
                        AnimatedVisibility(visible = selectedPackages.isNotEmpty()) {
                            Text(
                                text = "Pack Bag (${selectedPackages.size})",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Bags Bottom Sheet ---
    if (showBagsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBagsSheet = false },
            sheetState = bagsSheetState,
            containerColor = colorScheme.surfaceContainerLowest,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Bags",
                            style = typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "${bags.size} configured bundles",
                            style = typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = {
                        coroutineScope.launch {
                            bagsSheetState.hide()
                            showBagsSheet = false
                        }
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.close_fill0),
                            contentDescription = "Close",
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (bags.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colorScheme.surfaceContainerHighest,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.backpack_fill0),
                                        contentDescription = null,
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "No Bags Yet",
                                style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Open App Manager, select your favorite apps, and tap '+' to pack them into a bag.",
                                style = typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(bags, key = { it.id }) { bag ->
                            BagItemCard(
                                bag = bag,
                                viewModel = viewModel,
                                onDelete = { viewModel.deleteBag(bag.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Create Bag Name Dialog ---
    if (showCreateBagDialog) {
        var bagNameInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateBagDialog = false },
            title = {
                Text(
                    text = "Name your Bag",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "Pack ${selectedPackages.size} selected app${if (selectedPackages.size > 1) "s" else ""} into a custom bundle.",
                        style = typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = bagNameInput,
                        onValueChange = { bagNameInput = it },
                        placeholder = { Text("e.g. Socials, Study, Work") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bagNameInput.isNotBlank()) {
                            val success = viewModel.createBag(bagNameInput)
                            if (success) {
                                Toast.makeText(context, "Bag '$bagNameInput' created!", Toast.LENGTH_SHORT).show()
                                showCreateBagDialog = false
                            }
                        }
                    },
                    enabled = bagNameInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Bag")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateBagDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh
        )
    }
}

@Composable
fun AppListItem(
    app: AppInfo,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) colorScheme.primaryContainer.copy(alpha = 0.35f) else colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            val painter = rememberDrawablePainter(drawable = app.icon)
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Spacer(Modifier.width(14.dp))

            // Label & Package
            Column(Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    style = typography.bodySmall.copy(fontSize = 11.sp),
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = colorScheme.primary,
                    uncheckedColor = colorScheme.outline
                )
            )
        }
    }
}

@Composable
fun BagItemCard(
    bag: AppBag,
    viewModel: ToolsViewModel,
    onDelete: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.backpack_fill0),
                                contentDescription = null,
                                tint = colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text(
                            text = bag.name,
                            style = typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "${bag.packageNames.size} apps packed",
                            style = typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        painter = painterResource(R.drawable.delete_fill0),
                        contentDescription = "Delete Bag",
                        tint = colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Mini App Icons Preview in the Bag
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(bag.packageNames.toList()) { pkg ->
                    val appInfo = viewModel.getAppInfo(pkg)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colorScheme.surfaceContainerLowest,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (appInfo?.icon != null) {
                                Image(
                                    painter = rememberDrawablePainter(drawable = appInfo.icon),
                                    contentDescription = appInfo.label,
                                    modifier = Modifier.size(28.dp)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.apps_fill0),
                                    contentDescription = null,
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
