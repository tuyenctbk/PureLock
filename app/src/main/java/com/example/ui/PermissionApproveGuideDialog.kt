package com.example.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.R
import com.example.util.PermissionGuideItem
import com.example.util.PermissionManager
import com.example.util.PermissionPriority

/**
 * Fullscreen / adaptive dialog displaying the step-by-step Permission Approval Guide.
 */
@Composable
fun PermissionApproveGuideDialog(
    onDismissRequest: () -> Unit,
    onTestLockRequested: (() -> Unit)? = null
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PureLockLogoEmblem(size = 32.dp, showGlowRing = false)
                        Column {
                            Text(
                                text = stringResource(R.string.perm_guide_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.perm_guide_diagnostics),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("btn_close_perm_guide")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.vault_close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Content
                PermissionApproveGuideContent(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    onTestLockRequested = onTestLockRequested
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Done Button
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_perm_guide_done"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.ok), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Reusable content composable containing live permission cards, expandable step instructions,
 * manufacturer-specific guidance, and test shield trigger.
 */
@Composable
fun PermissionApproveGuideContent(
    modifier: Modifier = Modifier,
    onTestLockRequested: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State for re-checking permissions on ON_RESUME
    var refreshTrigger by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val guideItems = remember(refreshTrigger) {
        PermissionManager.getGuideItems(context)
    }

    val areCoreGranted = remember(guideItems) {
        PermissionManager.areCorePermissionsGranted(context)
    }

    var expandedItemId by remember { mutableStateOf<String?>(if (!areCoreGranted) "accessibility" else null) }
    var showTipsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status Banner Card
        item {
            Surface(
                color = if (areCoreGranted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = if (areCoreGranted) Icons.Default.VerifiedUser else Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = if (areCoreGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(if (areCoreGranted) R.string.perm_guide_core_active else R.string.perm_guide_core_missing),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (areCoreGranted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = if (areCoreGranted)
                                stringResource(R.string.perm_guide_all_granted_congrats)
                            else
                                stringResource(R.string.perm_guide_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (areCoreGranted) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // Subtitle & Tips Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.perm_guide_instructions_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = { showTipsDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("btn_manufacturer_tips")
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.perm_guide_device_tips), fontSize = 12.sp)
                }
            }
        }

        // List of Permission Cards
        items(guideItems, key = { it.id }) { item ->
            PermissionCard(
                item = item,
                isExpanded = expandedItemId == item.id,
                onToggleExpand = {
                    expandedItemId = if (expandedItemId == item.id) null else item.id
                },
                onActionClicked = {
                    item.openAction(context)
                }
            )
        }

        // Test App Lock Button
        item {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = {
                    if (onTestLockRequested != null) {
                        onTestLockRequested()
                    } else {
                        Toast.makeText(context, context.getString(R.string.perm_guide_test_toast), Toast.LENGTH_SHORT).show()
                        val intent = Intent(context, LockOverlayActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            putExtra(LockOverlayActivity.EXTRA_LOCKED_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_test_lock_overlay"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.perm_guide_test_button), fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // Manufacturer Tips Dialog
    if (showTipsDialog) {
        AlertDialog(
            onDismissRequest = { showTipsDialog = false },
            icon = { Icon(Icons.Default.Smartphone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.perm_guide_device_tips_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DeviceTipRow("Samsung One UI", stringResource(R.string.perm_guide_tip_samsung))
                    DeviceTipRow("Xiaomi / MIUI", stringResource(R.string.perm_guide_tip_xiaomi))
                    DeviceTipRow("Google Pixel", stringResource(R.string.perm_guide_tip_pixel))
                }
            },
            confirmButton = {
                TextButton(onClick = { showTipsDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}

@Composable
private fun DeviceTipRow(brand: String, tip: String) {
    Column {
        Text(brand, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Text(tip, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PermissionCard(
    item: PermissionGuideItem,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onActionClicked: () -> Unit
) {
    val icon: ImageVector = when (item.id) {
        "accessibility" -> Icons.Default.AccessibilityNew
        "overlay" -> Icons.Default.Layers
        "usage" -> Icons.Default.Insights
        "camera" -> Icons.Default.CameraAlt
        else -> Icons.Default.Notifications
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (item.isGranted) Color(0xFF10B981).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (item.isGranted) Color(0xFF10B981).copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (item.isGranted) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(item.titleRes),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val badgeColor = when (item.priority) {
                                PermissionPriority.CORE_REQUIRED -> MaterialTheme.colorScheme.errorContainer
                                PermissionPriority.RECOMMENDED -> MaterialTheme.colorScheme.primaryContainer
                                PermissionPriority.FEATURE_OPTIONAL -> MaterialTheme.colorScheme.secondaryContainer
                            }
                            val badgeTextColor = when (item.priority) {
                                PermissionPriority.CORE_REQUIRED -> MaterialTheme.colorScheme.onErrorContainer
                                PermissionPriority.RECOMMENDED -> MaterialTheme.colorScheme.onPrimaryContainer
                                PermissionPriority.FEATURE_OPTIONAL -> MaterialTheme.colorScheme.onSecondaryContainer
                            }
                            val badgeTextRes = when (item.priority) {
                                PermissionPriority.CORE_REQUIRED -> R.string.perm_guide_badge_core
                                PermissionPriority.RECOMMENDED -> R.string.perm_guide_badge_recommended
                                PermissionPriority.FEATURE_OPTIONAL -> R.string.perm_guide_badge_optional
                            }
                            Surface(
                                color = badgeColor,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = stringResource(badgeTextRes),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeTextColor,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                // Status Badge & Action
                if (item.isGranted) {
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.perm_guide_status_granted),
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onActionClicked,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("btn_setup_${item.id}"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.perm_guide_status_pending), fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(item.descRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            // Expandable Step-by-Step Instructions
            AnimatedVisibility(visible = isExpanded && item.stepStrings.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item.stepStrings.forEach { stepRes ->
                        Text(
                            text = stringResource(stepRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )
                    }

                    if (!item.isGranted) {
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = onActionClicked,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(stringResource(R.string.perm_guide_open_settings), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Top warning banner displayed in AppShieldScreen when core permissions are missing.
 */
@Composable
fun ShieldInactiveWarningBanner(
    onClickOpenGuide: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshTrigger by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val areCoreGranted = remember(refreshTrigger) {
        PermissionManager.areCorePermissionsGranted(context)
    }

    AnimatedVisibility(
        visible = !areCoreGranted,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClickOpenGuide)
                .testTag("banner_shield_inactive")
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.perm_banner_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = stringResource(R.string.perm_banner_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
                }
                Button(
                    onClick = onClickOpenGuide,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(stringResource(R.string.perm_banner_action), fontSize = 11.sp)
                }
            }
        }
    }
}
