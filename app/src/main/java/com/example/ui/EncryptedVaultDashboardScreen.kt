package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.EncryptedVaultEntity
import com.example.service.PasswordGeneratorService
import java.text.SimpleDateFormat
import java.util.*

enum class VaultFilterCategory(val labelRes: Int, val categoryKey: String, val icon: ImageVector) {
    ALL(R.string.vault_filter_all, "ALL", Icons.Default.Folder),
    PASSWORD(R.string.vault_filter_passwords, "PASSWORD", Icons.Default.Key),
    NOTE(R.string.vault_filter_notes, "NOTE", Icons.Default.Description),
    CARD(R.string.vault_filter_cards, "CARD", Icons.Default.CreditCard),
    CODE_2FA(R.string.vault_filter_2fa, "CODE_2FA", Icons.Default.QrCode),
    PIN(R.string.vault_filter_pins, "PIN", Icons.Default.Pin),
    FAVORITES(R.string.vault_filter_favorites, "FAVORITES", Icons.Default.Star),
    TRASH(R.string.vault_filter_trash, "TRASH", Icons.Default.DeleteOutline)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncryptedVaultDashboardScreen(
    viewModel: PureLockViewModel,
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val encryptedVaultItems by viewModel.encryptedVaultItems.collectAsState()
    val trashVaultItems by viewModel.trashVaultItems.collectAsState()
    val activeCopiedItemId by viewModel.activeCopiedItemId.collectAsState()
    val clipboardCountdown by viewModel.clipboardCountdown.collectAsState()
    val isSensitiveClipActive by viewModel.isSensitiveClipActive.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(VaultFilterCategory.ALL) }
    var revealedItemIds by remember { mutableStateOf(setOf<Long>()) }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<EncryptedVaultEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<EncryptedVaultEntity?>(null) }
    var showEmptyTrashDialog by remember { mutableStateOf(false) }

    // Filter items based on category and search query
    val displayedItems = remember(
        encryptedVaultItems,
        trashVaultItems,
        selectedCategory,
        searchQuery
    ) {
        val baseList = if (selectedCategory == VaultFilterCategory.TRASH) {
            trashVaultItems
        } else {
            when (selectedCategory) {
                VaultFilterCategory.ALL -> encryptedVaultItems
                VaultFilterCategory.FAVORITES -> encryptedVaultItems.filter { it.isFavorite }
                else -> encryptedVaultItems.filter { it.category.equals(selectedCategory.categoryKey, ignoreCase = true) }
            }
        }

        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter { item ->
                item.title.contains(searchQuery, ignoreCase = true) ||
                        item.username.contains(searchQuery, ignoreCase = true) ||
                        item.websiteOrApp.contains(searchQuery, ignoreCase = true) ||
                        item.notes.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            if (selectedCategory != VaultFilterCategory.TRASH) {
                FloatingActionButton(
                    onClick = {
                        editingItem = null
                        showAddEditDialog = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_add_vault_entry")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Encrypted Entry")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Bar
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                Text(
                                    text = stringResource(R.string.vault_dashboard_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.vault_dashboard_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (selectedCategory == VaultFilterCategory.TRASH && trashVaultItems.isNotEmpty()) {
                                TextButton(
                                    onClick = { showEmptyTrashDialog = true },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.vault_empty_trash_button), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Live Clipboard Auto-Clear Security Notification Banner
                item {
                    AnimatedVisibility(
                        visible = isSensitiveClipActive && clipboardCountdown > 0,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("banner_clipboard_security")
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
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$clipboardCountdown",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = stringResource(R.string.vault_clipboard_protected),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = stringResource(R.string.vault_clipboard_wiping_in, clipboardCountdown),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                val wipedToastMsg = stringResource(R.string.vault_clipboard_wiped_toast)
                                FilledTonalButton(
                                    onClick = {
                                        viewModel.clearClipboardNow()
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        Toast.makeText(context, wipedToastMsg, Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp).testTag("btn_clear_clipboard_now")
                                ) {
                                    Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.clear_clipboard_now), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Search Bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_search_vault"),
                        placeholder = { Text(stringResource(R.string.vault_search_placeholder), fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.outline
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }

                // Category Filter Chips
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(VaultFilterCategory.values()) { cat ->
                            val isSelected = selectedCategory == cat
                            val labelText = stringResource(cat.labelRes)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCategory = cat
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                label = {
                                    Text(
                                        text = when (cat) {
                                            VaultFilterCategory.ALL -> "$labelText (${encryptedVaultItems.size})"
                                            VaultFilterCategory.TRASH -> "$labelText (${trashVaultItems.size})"
                                            VaultFilterCategory.FAVORITES -> "$labelText (${encryptedVaultItems.count { it.isFavorite }})"
                                            else -> "$labelText (${encryptedVaultItems.count { it.category.equals(cat.categoryKey, ignoreCase = true) }})"
                                        },
                                        fontSize = 12.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("chip_category_${cat.name.lowercase()}")
                            )
                        }
                    }
                }

                // Empty State
                if (displayedItems.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (selectedCategory == VaultFilterCategory.TRASH) Icons.Default.DeleteOutline else Icons.Default.LockClock,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = if (selectedCategory == VaultFilterCategory.TRASH) {
                                        stringResource(R.string.vault_trash_empty_title)
                                    } else if (searchQuery.isNotEmpty()) {
                                        stringResource(R.string.vault_empty_no_match, searchQuery)
                                    } else {
                                        stringResource(R.string.vault_empty_in_category, stringResource(selectedCategory.labelRes))
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (selectedCategory == VaultFilterCategory.TRASH) {
                                        stringResource(R.string.vault_trash_empty_desc)
                                    } else {
                                        stringResource(R.string.vault_empty_category_desc)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // Encrypted Entries List
                    items(displayedItems, key = { it.id }) { item ->
                        val isRevealed = revealedItemIds.contains(item.id)
                        val isRecentlyCopied = activeCopiedItemId == item.id

                        VaultItemCard(
                            item = item,
                            isRevealed = isRevealed,
                            isRecentlyCopied = isRecentlyCopied,
                            isInTrash = selectedCategory == VaultFilterCategory.TRASH,
                            onToggleReveal = {
                                revealedItemIds = if (isRevealed) {
                                    revealedItemIds - item.id
                                } else {
                                    revealedItemIds + item.id
                                }
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onCopySecret = {
                                viewModel.copyVaultItemToClipboard(item)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onToggleFavorite = {
                                viewModel.toggleVaultItemFavorite(item.id, !item.isFavorite)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onTogglePin = {
                                viewModel.toggleVaultItemPin(item.id, !item.isPinned)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onEdit = {
                                editingItem = item
                                showAddEditDialog = true
                            },
                            onMoveToTrash = {
                                viewModel.moveVaultItemToTrash(item.id)
                                Toast.makeText(context, "\"${item.title}\" moved to Trash", Toast.LENGTH_SHORT).show()
                            },
                            onRestore = {
                                viewModel.restoreVaultItemFromTrash(item.id)
                                Toast.makeText(context, "\"${item.title}\" restored", Toast.LENGTH_SHORT).show()
                            },
                            onPermanentDelete = {
                                itemToDelete = item
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }

    // Add / Edit Entry Dialog Sheet
    if (showAddEditDialog) {
        val updatedToast = stringResource(R.string.vault_item_updated)
        val savedToast = stringResource(R.string.vault_item_saved_securely)
        AddEditVaultEntryDialog(
            initialItem = editingItem,
            onDismiss = { showAddEditDialog = false },
            onSave = { title, secret, category, username, website, notes, isPinned, isFavorite ->
                if (editingItem != null) {
                    viewModel.updateEncryptedVaultItem(
                        editingItem!!.copy(
                            title = title,
                            secretContent = secret,
                            category = category,
                            username = username,
                            websiteOrApp = website,
                            notes = notes,
                            isPinned = isPinned,
                            isFavorite = isFavorite,
                            updatedTimestamp = System.currentTimeMillis()
                        )
                    )
                    Toast.makeText(context, updatedToast, Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.saveEncryptedVaultItem(
                        title = title,
                        secretContent = secret,
                        category = category,
                        username = username,
                        websiteOrApp = website,
                        notes = notes,
                        isPinned = isPinned
                    )
                    Toast.makeText(context, savedToast, Toast.LENGTH_SHORT).show()
                }
                showAddEditDialog = false
            }
        )
    }

    // Permanent Delete Confirmation Dialog
    if (itemToDelete != null) {
        val deleteToast = stringResource(R.string.vault_item_deleted_permanently)
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.vault_delete_dialog_title)) },
            text = {
                Text(
                    stringResource(R.string.vault_delete_dialog_message, itemToDelete?.title ?: ""),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        itemToDelete?.let { viewModel.deleteEncryptedVaultItem(it.id) }
                        itemToDelete = null
                        Toast.makeText(context, deleteToast, Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.vault_delete_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Empty Trash Confirmation Dialog
    if (showEmptyTrashDialog) {
        val trashEmptiedToast = stringResource(R.string.vault_trash_emptied)
        AlertDialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.vault_empty_trash_dialog_title)) },
            text = {
                Text(
                    stringResource(R.string.vault_empty_trash_dialog_message, trashVaultItems.size),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.emptyTrashVault()
                        showEmptyTrashDialog = false
                        Toast.makeText(context, trashEmptiedToast, Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.vault_empty_trash_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

/**
 * Minimalist Encrypted Vault Item Card with Default Masking
 */
@Composable
fun VaultItemCard(
    item: EncryptedVaultEntity,
    isRevealed: Boolean,
    isRecentlyCopied: Boolean,
    isInTrash: Boolean,
    onToggleReveal: () -> Unit,
    onCopySecret: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onMoveToTrash: () -> Unit,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    val categoryIcon = when (item.category.uppercase()) {
        "PASSWORD" -> Icons.Default.Key
        "NOTE" -> Icons.Default.Description
        "CARD" -> Icons.Default.CreditCard
        "CODE_2FA" -> Icons.Default.QrCode
        "PIN" -> Icons.Default.Pin
        else -> Icons.Default.Lock
    }

    val categoryColor = when (item.category.uppercase()) {
        "PASSWORD" -> MaterialTheme.colorScheme.primary
        "NOTE" -> MaterialTheme.colorScheme.secondary
        "CARD" -> Color(0xFF10B981) // Emerald green
        "CODE_2FA" -> Color(0xFF8B5CF6) // Purple
        "PIN" -> Color(0xFFF59E0B) // Amber
        else -> MaterialTheme.colorScheme.tertiary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_vault_item_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (item.isPinned) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row (Icon, Title, Category Badge, Action Menu)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(categoryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (item.isPinned) {
                                Icon(
                                    Icons.Default.PushPin,
                                    contentDescription = "Pinned",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        if (item.username.isNotBlank()) {
                            Text(
                                text = item.username,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (item.websiteOrApp.isNotBlank()) {
                            Text(
                                text = item.websiteOrApp,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Top Actions (Favorite, Edit/More)
                if (!isInTrash) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(32.dp).testTag("btn_fav_${item.id}")
                        ) {
                            Icon(
                                imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorite",
                                tint = if (item.isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(32.dp).testTag("btn_edit_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = onMoveToTrash,
                            modifier = Modifier.size(32.dp).testTag("btn_trash_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Trash",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onRestore,
                            modifier = Modifier.size(32.dp).testTag("btn_restore_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestoreFromTrash,
                                contentDescription = "Restore",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onPermanentDelete,
                            modifier = Modifier.size(32.dp).testTag("btn_perm_del_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = "Delete Permanently",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Sensitive Secret Field (Masked by default)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isRevealed) item.secretContent else "••••••••••••••••",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isRevealed) FontWeight.Medium else FontWeight.Bold,
                            fontSize = if (isRevealed) 13.sp else 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = if (isRevealed) 0.sp else 2.sp,
                            maxLines = if (isRevealed) 4 else 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reveal / Mask toggle button
                        IconButton(
                            onClick = onToggleReveal,
                            modifier = Modifier.size(32.dp).testTag("btn_reveal_${item.id}")
                        ) {
                            Icon(
                                imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isRevealed) "Mask secret" else "Reveal secret",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Copy Button
                        IconButton(
                            onClick = onCopySecret,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isRecentlyCopied) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .testTag("btn_copy_${item.id}")
                        ) {
                            Icon(
                                imageVector = if (isRecentlyCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy to clipboard",
                                tint = if (isRecentlyCopied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Notes metadata snippet if present
            if (item.notes.isNotBlank() && isRevealed) {
                Text(
                    text = item.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

/**
 * Dialog for Adding or Editing an Encrypted Vault Entry
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditVaultEntryDialog(
    initialItem: EncryptedVaultEntity?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        secret: String,
        category: String,
        username: String,
        website: String,
        notes: String,
        isPinned: Boolean,
        isFavorite: Boolean
    ) -> Unit
) {
    var title by remember { mutableStateOf(initialItem?.title ?: "") }
    var secret by remember { mutableStateOf(initialItem?.secretContent ?: "") }
    var category by remember { mutableStateOf(initialItem?.category ?: "PASSWORD") }
    var username by remember { mutableStateOf(initialItem?.username ?: "") }
    var website by remember { mutableStateOf(initialItem?.websiteOrApp ?: "") }
    var notes by remember { mutableStateOf(initialItem?.notes ?: "") }
    var isPinned by remember { mutableStateOf(initialItem?.isPinned ?: false) }
    var isFavorite by remember { mutableStateOf(initialItem?.isFavorite ?: false) }

    var isSecretVisible by remember { mutableStateOf(false) }
    var showPasswordGenerator by remember { mutableStateOf(false) }

    val categories = listOf(
        Pair("PASSWORD", stringResource(R.string.category_password)),
        Pair("NOTE", stringResource(R.string.category_note)),
        Pair("CARD", stringResource(R.string.vault_filter_cards)),
        Pair("CODE_2FA", stringResource(R.string.vault_filter_2fa)),
        Pair("PIN", stringResource(R.string.category_pin))
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialItem != null) stringResource(R.string.vault_edit_title) else stringResource(R.string.vault_add_new_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Category Selector
                Text(stringResource(R.string.vault_field_category), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { (key, label) ->
                        FilterChip(
                            selected = category == key,
                            onClick = { category = key },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.vault_field_title_label)) },
                    placeholder = { Text(stringResource(R.string.vault_field_title_placeholder)) },
                    modifier = Modifier.fillMaxWidth().testTag("input_vault_title"),
                    singleLine = true
                )

                // Secret / Password Field
                OutlinedTextField(
                    value = secret,
                    onValueChange = { secret = it },
                    label = { Text(if (category == "NOTE") stringResource(R.string.vault_field_note_content_label) else stringResource(R.string.vault_field_secret_label)) },
                    modifier = Modifier.fillMaxWidth().testTag("input_vault_secret"),
                    visualTransformation = if (isSecretVisible || category == "NOTE") {
                        androidx.compose.ui.text.input.VisualTransformation.None
                    } else {
                        androidx.compose.ui.text.input.PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { isSecretVisible = !isSecretVisible }) {
                                Icon(
                                    imageVector = if (isSecretVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle visibility",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            if (category == "PASSWORD") {
                                IconButton(onClick = { showPasswordGenerator = !showPasswordGenerator }) {
                                    Icon(
                                        imageVector = Icons.Default.AutoFixHigh,
                                        contentDescription = "Generate Password",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    },
                    maxLines = if (category == "NOTE") 5 else 1
                )

                // Password Generator Quick Generator Strip
                if (showPasswordGenerator) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.vault_generator_helper), fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Button(
                                onClick = {
                                    secret = PasswordGeneratorService.generateSecurePassword(
                                        length = 20,
                                        includeUppercase = true,
                                        includeLowercase = true,
                                        includeDigits = true,
                                        includeSymbols = true
                                    )
                                    isSecretVisible = true
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(stringResource(R.string.vault_btn_generate), fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Username / Account
                if (category == "PASSWORD" || category == "CODE_2FA") {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(stringResource(R.string.vault_field_username_label)) },
                        placeholder = { Text(stringResource(R.string.vault_field_username_placeholder)) },
                        modifier = Modifier.fillMaxWidth().testTag("input_vault_username"),
                        singleLine = true
                    )
                }

                // Website / App URL
                if (category == "PASSWORD") {
                    OutlinedTextField(
                        value = website,
                        onValueChange = { website = it },
                        label = { Text(stringResource(R.string.vault_field_website_label)) },
                        placeholder = { Text(stringResource(R.string.vault_field_website_placeholder)) },
                        modifier = Modifier.fillMaxWidth().testTag("input_vault_website"),
                        singleLine = true
                    )
                }

                // Additional Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.vault_field_notes_label)) },
                    modifier = Modifier.fillMaxWidth().testTag("input_vault_notes"),
                    maxLines = 3
                )

                // Pin / Favorite toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Checkbox(checked = isPinned, onCheckedChange = { isPinned = it })
                        Text(stringResource(R.string.vault_field_pin_top), fontSize = 12.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Checkbox(checked = isFavorite, onCheckedChange = { isFavorite = it })
                        Text(stringResource(R.string.vault_field_favorite), fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && secret.isNotBlank()) {
                        onSave(title.trim(), secret.trim(), category, username.trim(), website.trim(), notes.trim(), isPinned, isFavorite)
                    }
                },
                enabled = title.isNotBlank() && secret.isNotBlank(),
                modifier = Modifier.testTag("btn_save_vault_item")
            ) {
                Text(stringResource(R.string.vault_btn_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
