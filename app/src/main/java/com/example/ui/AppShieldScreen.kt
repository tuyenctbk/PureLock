package com.example.ui

import android.util.LruCache
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.LockedAppEntity
import com.example.data.model.ScheduleRuleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

private object AppIconCache {
    private val cache = LruCache<String, ImageBitmap>(96)

    fun get(packageName: String): ImageBitmap? = synchronized(cache) { cache.get(packageName) }

    fun put(packageName: String, icon: ImageBitmap) = synchronized(cache) { cache.put(packageName, icon) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShieldScreen(
    viewModel: PureLockViewModel,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val allApps by viewModel.allApps.collectAsState()
    val activeCount = remember(allApps) { allApps.count { it.isLocked } }
    val scheduleRules by viewModel.scheduleRules.collectAsState()
    val isSyncingApps by viewModel.isSyncingApps.collectAsState()
    val haptic = LocalHapticFeedback.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") } // "ALL", "LOCKED", "UNLOCKED"
    var showInfoDialog by remember { mutableStateOf(false) }
    var showPermGuideDialog by remember { mutableStateOf(false) }
    var showSchedulesDialog by remember { mutableStateOf(false) }

    val categories = listOf("ALL", "FINANCIAL", "SOCIAL", "SYSTEM", "MEDIA", "GAMES", "OTHER")

    val filteredApps = remember(allApps, searchQuery, selectedCategory, selectedStatusFilter) {
        allApps.filter { app ->
            val matchesSearch = searchQuery.isEmpty() ||
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "ALL" || app.category.equals(selectedCategory, ignoreCase = true)
            val matchesStatus = when (selectedStatusFilter) {
                "LOCKED" -> app.isLocked
                "UNLOCKED" -> !app.isLocked
                else -> true
            }
            matchesSearch && matchesCategory && matchesStatus
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Minimal Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PureLockLogoEmblem(
                        size = 38.dp,
                        showGlowRing = false,
                        badgeBackground = MaterialTheme.colorScheme.primaryContainer
                    )
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.nav_shield),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.shield_locked_count, activeCount, allApps.size),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sync / Refresh Apps with device (clears uninstalled and detects newly installed)
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.syncInstalledApps { removed ->
                                val msg = if (removed > 0) context.getString(R.string.shield_synced_removed_toast, removed) else context.getString(R.string.shield_synced_toast)
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isSyncingApps,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("btn_sync_apps")
                    ) {
                        if (isSyncingApps) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = stringResource(R.string.shield_sync_apps),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Lock All quick action
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setAllAppsLockState(true)
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.shield_all_apps_locked_toast, allApps.size),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("btn_lock_all")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = stringResource(R.string.shield_lock_all),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Unlock All quick action
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setAllAppsLockState(false)
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.shield_all_apps_unlocked_toast),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("btn_unlock_all")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = stringResource(R.string.shield_unlock_all),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Custom Lock Schedules quick action
                    IconButton(
                        onClick = { showSchedulesDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (scheduleRules.any { it.isEnabled }) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .testTag("btn_shield_schedules")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = stringResource(R.string.shield_custom_schedule_title),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Permission Approval Guide quick action
                    IconButton(
                        onClick = { showPermGuideDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("btn_shield_perm_guide")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = stringResource(R.string.perm_guide_title),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Info / Hint button to hide complexity
                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("btn_shield_info")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.shield_details_title),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Animated Shield Coverage Progress Strip
            if (allApps.isNotEmpty()) {
                val coverageFraction by animateFloatAsState(
                    targetValue = (activeCount.toFloat() / allApps.size.toFloat()).coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
                    label = "ShieldCoverageFraction"
                )
                LinearProgressIndicator(
                    progress = { coverageFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // Core Privileges Inactive Warning Banner
            ShieldInactiveWarningBanner(
                onClickOpenGuide = { showPermGuideDialog = true }
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_app_search"),
                placeholder = { Text(stringResource(R.string.shield_search_apps_placeholder), style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear), tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            )

            // Status Filter Row (ALL, LOCKED, UNLOCKED)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedStatusFilter == "ALL",
                    onClick = { selectedStatusFilter = "ALL" },
                    label = { Text(stringResource(R.string.shield_filter_all), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.weight(1f).testTag("chip_status_all")
                )
                FilterChip(
                    selected = selectedStatusFilter == "LOCKED",
                    onClick = { selectedStatusFilter = "LOCKED" },
                    label = { Text(stringResource(R.string.shield_filter_locked_count, activeCount), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp)) },
                    modifier = Modifier.weight(1.15f).testTag("chip_status_locked")
                )
                FilterChip(
                    selected = selectedStatusFilter == "UNLOCKED",
                    onClick = { selectedStatusFilter = "UNLOCKED" },
                    label = { Text(stringResource(R.string.shield_filter_unlocked_count, allApps.size - activeCount), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(12.dp)) },
                    modifier = Modifier.weight(1.25f).testTag("chip_status_unlocked")
                )
            }

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                text = stringResource(appCategoryLabelRes(category)),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        leadingIcon = {
                            val icon = when (category) {
                                "FINANCIAL" -> Icons.Default.AccountBalance
                                "SOCIAL" -> Icons.AutoMirrored.Filled.Chat
                                "SYSTEM" -> Icons.Default.Build
                                "MEDIA" -> Icons.Default.PlayCircle
                                "GAMES" -> Icons.Default.SportsEsports
                                else -> Icons.Default.Apps
                            }
                            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedCategory == category,
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("chip_category_$category")
                    )
                }
            }

            // App List
            if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = stringResource(R.string.shield_no_apps_found),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(
                        items = filteredApps,
                        key = { it.packageName }
                    ) { app ->
                        Box(modifier = Modifier.animateItem()) {
                            AppShieldItemCard(
                                app = app,
                                onToggle = { viewModel.toggleAppLock(app.packageName, app.isLocked) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Protection Details Info Dialog (Hides complexity & details behind hint button)
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.shield_details_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text(
                            text = stringResource(R.string.shield_realtime_desc),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text(
                            text = stringResource(R.string.shield_offline_desc),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text(
                            text = stringResource(R.string.shield_decoy_desc),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    if (showPermGuideDialog) {
        PermissionApproveGuideDialog(
            onDismissRequest = { showPermGuideDialog = false }
        )
    }

    if (showSchedulesDialog) {
        CustomScheduleRulesDialog(
            apps = allApps,
            rules = scheduleRules,
            onAddRule = { rule -> viewModel.addScheduleRule(rule) },
            onToggleRule = { id, enabled -> viewModel.toggleScheduleRule(id, enabled) },
            onDeleteRule = { id -> viewModel.deleteScheduleRule(id) },
            onDismiss = { showSchedulesDialog = false }
        )
    }
}

@Composable
private fun AppShieldItemCard(
    app: LockedAppEntity,
    onToggle: () -> Unit
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current

    val appIcon by produceState<ImageBitmap?>(
        initialValue = AppIconCache.get(app.packageName),
        key1 = app.packageName
    ) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                try {
                    val drawable = context.packageManager.getApplicationIcon(app.packageName)
                    drawableToBitmap(drawable, maxSizePx = 96).asImageBitmap().also {
                        AppIconCache.put(app.packageName, it)
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    Surface(
        color = if (app.isLocked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (app.isLocked) primaryColor.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggle()
            }
            .testTag("card_app_${app.packageName}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Real Installed App Icon (cached off main thread) with category fallback
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (app.isLocked) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val loadedIcon = appIcon
                    if (loadedIcon != null) {
                        Image(
                            bitmap = loadedIcon,
                            contentDescription = app.appName,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        val categoryIcon = when (app.category) {
                            "FINANCIAL" -> Icons.Default.AccountBalance
                            "SOCIAL" -> Icons.AutoMirrored.Filled.Chat
                            "SYSTEM" -> Icons.Default.Build
                            "MEDIA" -> Icons.Default.PlayCircle
                            "GAMES" -> Icons.Default.SportsEsports
                            else -> Icons.Default.Apps
                        }
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = null,
                            tint = if (app.isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(appCategoryLabelRes(app.category)),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (app.isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedLockToggleIcon(
                    isLocked = app.isLocked,
                    onToggle = null
                )

                Switch(
                    checked = app.isLocked,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggle()
                    },
                    thumbContent = if (app.isLocked) {
                        { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp)) }
                    } else {
                        { Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(12.dp)) }
                    },
                    modifier = Modifier.testTag("switch_lock_${app.packageName}")
                )
            }
        }
    }
}

private fun appCategoryLabelRes(category: String): Int = when (category.uppercase()) {
    "ALL" -> R.string.vault_filter_all
    "FINANCIAL" -> R.string.app_cat_financial
    "SOCIAL" -> R.string.app_cat_social
    "SYSTEM" -> R.string.app_cat_system
    "MEDIA" -> R.string.app_cat_media
    "GAMES" -> R.string.app_cat_games
    else -> R.string.app_cat_other
}

@Composable
fun AnimatedLockToggleIcon(
    isLocked: Boolean,
    onToggle: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val transition = updateTransition(targetState = isLocked, label = "LockToggleTransition")

    val scale by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium) },
        label = "Scale"
    ) { locked -> if (locked) 1.05f else 0.95f }

    val containerColor by transition.animateColor(
        transitionSpec = { tween(durationMillis = 250) },
        label = "ContainerColor"
    ) { locked ->
        if (locked) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }

    val iconColor by transition.animateColor(
        transitionSpec = { tween(durationMillis = 250) },
        label = "IconColor"
    ) { locked ->
        if (locked) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(containerColor)
            .then(if (onToggle != null) Modifier.clickable { onToggle() } else Modifier)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isLocked,
            transitionSpec = {
                fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(150))
            },
            label = "LockIconContent"
        ) { locked ->
            Icon(
                imageVector = if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = if (locked) stringResource(R.string.shield_locked_state) else stringResource(R.string.shield_unlocked_state),
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomScheduleRulesDialog(
    apps: List<LockedAppEntity>,
    rules: List<ScheduleRuleEntity>,
    onAddRule: (ScheduleRuleEntity) -> Unit,
    onToggleRule: (Long, Boolean) -> Unit,
    onDeleteRule: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val lockedApps = remember(apps) { apps.filter { it.isLocked }.ifEmpty { apps } }
    var showAddForm by remember { mutableStateOf(false) }
    var selectedPackageName by remember { mutableStateOf(lockedApps.firstOrNull()?.packageName) }
    val selectedApp = remember(lockedApps, selectedPackageName) {
        lockedApps.firstOrNull { it.packageName == selectedPackageName } ?: lockedApps.firstOrNull()
    }
    var appDropdownExpanded by remember { mutableStateOf(false) }
    var startHour by remember { mutableIntStateOf(22) }
    var endHour by remember { mutableIntStateOf(7) }
    val allDayTokens = remember { listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN") }
    var selectedDays by remember { mutableStateOf(allDayTokens.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = {
            Text(
                text = stringResource(R.string.shield_custom_schedule_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.shield_schedule_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                if (showAddForm && lockedApps.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box {
                                OutlinedButton(
                                    onClick = { appDropdownExpanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = selectedApp?.appName ?: "",
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = appDropdownExpanded,
                                    onDismissRequest = { appDropdownExpanded = false }
                                ) {
                                    lockedApps.forEach { app ->
                                        DropdownMenuItem(
                                            text = { Text(app.appName) },
                                            onClick = {
                                                selectedPackageName = app.packageName
                                                appDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(stringResource(R.string.shield_start_time), style = MaterialTheme.typography.labelSmall)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { startHour = (startHour + 23) % 24 }, modifier = Modifier.size(30.dp)) {
                                            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                        Text(String.format(Locale.US, "%02d:00", startHour), fontWeight = FontWeight.Bold)
                                        IconButton(onClick = { startHour = (startHour + 1) % 24 }, modifier = Modifier.size(30.dp)) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(stringResource(R.string.shield_end_time), style = MaterialTheme.typography.labelSmall)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { endHour = (endHour + 23) % 24 }, modifier = Modifier.size(30.dp)) {
                                            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                        Text(String.format(Locale.US, "%02d:00", endHour), fontWeight = FontWeight.Bold)
                                        IconButton(onClick = { endHour = (endHour + 1) % 24 }, modifier = Modifier.size(30.dp)) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            Text(stringResource(R.string.shield_rule_days), style = MaterialTheme.typography.labelSmall)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(allDayTokens) { day ->
                                    val chosen = day in selectedDays
                                    FilterChip(
                                        selected = chosen,
                                        onClick = {
                                            selectedDays = if (chosen && selectedDays.size > 1) selectedDays - day else selectedDays + day
                                        },
                                        label = { Text(day, fontSize = 10.sp) }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { showAddForm = false }) {
                                    Text(stringResource(R.string.cancel))
                                }
                                Button(
                                    enabled = selectedApp != null && selectedDays.isNotEmpty(),
                                    onClick = {
                                        val target = selectedApp ?: return@Button
                                        onAddRule(
                                            ScheduleRuleEntity(
                                                packageName = target.packageName,
                                                appName = target.appName,
                                                startHour = startHour,
                                                startMinute = 0,
                                                endHour = endHour,
                                                endMinute = 0,
                                                isEnabled = true,
                                                daysString = allDayTokens.filter { it in selectedDays }.joinToString(",")
                                            )
                                        )
                                        showAddForm = false
                                    }
                                ) {
                                    Text(stringResource(R.string.save))
                                }
                            }
                        }
                    }
                } else if (lockedApps.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showAddForm = true },
                        modifier = Modifier.fillMaxWidth().testTag("btn_add_schedule_rule")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.shield_add_schedule))
                    }
                }

                if (rules.isNotEmpty()) {
                    rules.forEach { rule ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(rule.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            "%02d:%02d – %02d:%02d • %s",
                                            rule.startHour,
                                            rule.startMinute,
                                            rule.endHour,
                                            rule.endMinute,
                                            rule.daysString
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Switch(
                                    checked = rule.isEnabled,
                                    onCheckedChange = { onToggleRule(rule.id, it) }
                                )
                                IconButton(
                                    onClick = { onDeleteRule(rule.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = stringResource(R.string.shield_delete_schedule),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_close))
            }
        }
    )
}
