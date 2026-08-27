package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.EncryptedVaultEntity
import com.example.data.model.IntruderSelfieEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class VaultSortOption {
    DATE_DESC,
    DATE_ASC,
    ALPHA_ASC,
    ALPHA_DESC,
    STRENGTH_DESC,
    STRENGTH_ASC
}

@Composable
fun IntruderVaultScreen(
    viewModel: PureLockViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val intruderSelfies by viewModel.intruderSelfies.collectAsState()
    val encryptedVaultItems by viewModel.encryptedVaultItems.collectAsState()
    val trashVaultItems by viewModel.trashVaultItems.collectAsState()
    val trashPurgeDays by viewModel.trashPurgeDays.collectAsState()
    val activeCopiedItemId by viewModel.activeCopiedItemId.collectAsState()
    val clipboardCountdown by viewModel.clipboardCountdown.collectAsState()

    var selectedTab by remember { mutableIntStateOf(1) }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var sortOption by remember { mutableStateOf(VaultSortOption.DATE_DESC) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }
    var showVaultInsightsDialog by remember { mutableStateOf(false) }
    var selectedSelfie by remember { mutableStateOf<IntruderSelfieEntity?>(null) }
    var showAddVaultDialog by remember { mutableStateOf(false) }
    var editingVaultItem by remember { mutableStateOf<EncryptedVaultEntity?>(null) }
    var noteToDelete by remember { mutableStateOf<EncryptedVaultEntity?>(null) }
    var trashToDelete by remember { mutableStateOf<EncryptedVaultEntity?>(null) }

    var showExportNotesDialog by remember { mutableStateOf(false) }
    var showImportNotesDialog by remember { mutableStateOf(false) }
    var exportPassphrase by remember { mutableStateOf("") }
    var importPassphrase by remember { mutableStateOf("") }
    var importPayloadInput by remember { mutableStateOf("") }
    var exportResultStatus by remember { mutableStateOf<String?>(null) }

    var vaultSearchQuery by remember { mutableStateOf("") }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header Title with Tab Statistics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (selectedTab) {
                            0 -> Icons.Default.CameraAlt
                            1 -> Icons.Default.Lock
                            else -> Icons.Default.Delete
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.secure_vault),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when (selectedTab) {
                            0 -> "${intruderSelfies.size} Snapshots Captured"
                            1 -> "${encryptedVaultItems.size} Encrypted Notes & Secrets"
                            else -> "${trashVaultItems.size} Trash Bin Items"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (selectedTab == 0 && intruderSelfies.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.clearAllIntruders() },
                    modifier = Modifier.testTag("btn_clear_intruders")
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Vault")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All")
                }
            } else if (selectedTab == 1) {
                Button(
                    onClick = { showAddVaultDialog = true },
                    modifier = Modifier.testTag("btn_add_vault_secret")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Note")
                }
            } else if (selectedTab == 2 && trashVaultItems.isNotEmpty()) {
                Button(
                    onClick = { viewModel.emptyTrashVault() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_empty_trash_bin")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Empty Trash")
                }
            }
        }

        // Vault Navigation Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).testTag("vault_tab_row")
        ) {
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(stringResource(R.string.secrets)) },
                icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                modifier = Modifier.testTag("tab_encrypted_secrets")
            )
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(stringResource(R.string.snapshots)) },
                icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                modifier = Modifier.testTag("tab_intruder_snapshots")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text(stringResource(R.string.trash_bin)) },
                icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                modifier = Modifier.testTag("tab_trash_bin")
            )
        }

        if (selectedTab == 1) {
            // Tab 1: SQLCipher & AES-256 Encrypted Secrets & Passwords Vault
            
            // Search Bar & Sort Overflow Menu Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = vaultSearchQuery,
                    onValueChange = { vaultSearchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_vault_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Vault") },
                    trailingIcon = {
                        if (vaultSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { vaultSearchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_search))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("input_vault_search")
                )

                // Overflow Sort Menu
                Box {
                    IconButton(
                        onClick = { isSortMenuExpanded = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .testTag("btn_vault_sort_overflow")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = stringResource(R.string.sort_notes_title),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    DropdownMenu(
                        expanded = isSortMenuExpanded,
                        onDismissRequest = { isSortMenuExpanded = false },
                        modifier = Modifier.testTag("menu_vault_sort_options")
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_date_desc)) },
                            onClick = {
                                sortOption = VaultSortOption.DATE_DESC
                                isSortMenuExpanded = false
                            },
                            leadingIcon = {
                                if (sortOption == VaultSortOption.DATE_DESC) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                } else {
                                    Icon(Icons.Default.Schedule, contentDescription = null)
                                }
                            },
                            modifier = Modifier.testTag("menu_item_sort_date_desc")
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_date_asc)) },
                            onClick = {
                                sortOption = VaultSortOption.DATE_ASC
                                isSortMenuExpanded = false
                            },
                            leadingIcon = {
                                if (sortOption == VaultSortOption.DATE_ASC) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                } else {
                                    Icon(Icons.Default.History, contentDescription = null)
                                }
                            },
                            modifier = Modifier.testTag("menu_item_sort_date_asc")
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_alphabetical_asc)) },
                            onClick = {
                                sortOption = VaultSortOption.ALPHA_ASC
                                isSortMenuExpanded = false
                            },
                            leadingIcon = {
                                if (sortOption == VaultSortOption.ALPHA_ASC) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                } else {
                                    Icon(Icons.Default.SortByAlpha, contentDescription = null)
                                }
                            },
                            modifier = Modifier.testTag("menu_item_sort_alpha_asc")
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_alphabetical_desc)) },
                            onClick = {
                                sortOption = VaultSortOption.ALPHA_DESC
                                isSortMenuExpanded = false
                            },
                            leadingIcon = {
                                if (sortOption == VaultSortOption.ALPHA_DESC) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                } else {
                                    Icon(Icons.Default.SortByAlpha, contentDescription = null)
                                }
                            },
                            modifier = Modifier.testTag("menu_item_sort_alpha_desc")
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_strength_desc)) },
                            onClick = {
                                sortOption = VaultSortOption.STRENGTH_DESC
                                isSortMenuExpanded = false
                            },
                            leadingIcon = {
                                if (sortOption == VaultSortOption.STRENGTH_DESC) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                } else {
                                    Icon(Icons.Default.Shield, contentDescription = null)
                                }
                            },
                            modifier = Modifier.testTag("menu_item_sort_strength_desc")
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_strength_asc)) },
                            onClick = {
                                sortOption = VaultSortOption.STRENGTH_ASC
                                isSortMenuExpanded = false
                            },
                            leadingIcon = {
                                if (sortOption == VaultSortOption.STRENGTH_ASC) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                } else {
                                    Icon(Icons.Default.Warning, contentDescription = null)
                                }
                            },
                            modifier = Modifier.testTag("menu_item_sort_strength_asc")
                        )
                    }
                }
            }

            // Category Filter Chip Bar
            val categoryDefinitions = listOf(
                "ALL" to stringResource(R.string.category_all_notes),
                "WORK" to stringResource(R.string.category_work),
                "PERSONAL" to stringResource(R.string.category_personal),
                "FINANCE" to stringResource(R.string.category_finance),
                "PASSWORD" to stringResource(R.string.category_password),
                "PIN" to stringResource(R.string.category_pin),
                "NOTE" to stringResource(R.string.category_note)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth().testTag("category_filter_chip_bar"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categoryDefinitions) { (key, label) ->
                    val isSelected = selectedCategory.equals(key, ignoreCase = true)
                    val count = if (key == "ALL") encryptedVaultItems.size
                    else encryptedVaultItems.count { it.category.equals(key, ignoreCase = true) }

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = key },
                        label = {
                            Text(
                                text = "$label ($count)",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            when (key) {
                                "ALL" -> Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                                "WORK" -> Icon(Icons.Default.BusinessCenter, contentDescription = null, modifier = Modifier.size(16.dp))
                                "PERSONAL" -> Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                "FINANCE" -> Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                                "PASSWORD" -> Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                "PIN" -> Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(16.dp))
                                else -> Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("chip_category_$key")
                    )
                }
            }

            // Quick Utility Actions (Export, Import, Insights)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        exportPassphrase = ""
                        exportResultStatus = null
                        showExportNotesDialog = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).dpadFocusable(shape = RoundedCornerShape(10.dp)).testTag("btn_export_notes_vault")
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.export_btn_label), fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        importPassphrase = ""
                        importPayloadInput = ""
                        showImportNotesDialog = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).dpadFocusable(shape = RoundedCornerShape(10.dp)).testTag("btn_import_notes_vault")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.import_btn_label), fontSize = 12.sp)
                }

                FilledTonalButton(
                    onClick = { showVaultInsightsDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.1f).dpadFocusable(shape = RoundedCornerShape(10.dp)).testTag("btn_vault_insights")
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.insights_btn_label), fontSize = 12.sp)
                }
            }

            // Filter & Sort Pipeline
            val passwordCalc = remember { com.example.service.PasswordGeneratorService() }
            val categoryFilteredItems = remember(encryptedVaultItems, selectedCategory) {
                if (selectedCategory.equals("ALL", ignoreCase = true)) encryptedVaultItems
                else encryptedVaultItems.filter { it.category.equals(selectedCategory, ignoreCase = true) }
            }

            val searchFilteredItems = remember(categoryFilteredItems, vaultSearchQuery) {
                if (vaultSearchQuery.isBlank()) categoryFilteredItems
                else categoryFilteredItems.filter {
                    it.title.contains(vaultSearchQuery, ignoreCase = true) ||
                    it.category.contains(vaultSearchQuery, ignoreCase = true) ||
                    it.secretContent.contains(vaultSearchQuery, ignoreCase = true)
                }
            }

            val finalVaultItems = remember(searchFilteredItems, sortOption) {
                when (sortOption) {
                    VaultSortOption.DATE_DESC -> searchFilteredItems.sortedByDescending { it.timestamp }
                    VaultSortOption.DATE_ASC -> searchFilteredItems.sortedBy { it.timestamp }
                    VaultSortOption.ALPHA_ASC -> searchFilteredItems.sortedBy { it.title.lowercase() }
                    VaultSortOption.ALPHA_DESC -> searchFilteredItems.sortedByDescending { it.title.lowercase() }
                    VaultSortOption.STRENGTH_DESC -> searchFilteredItems.sortedByDescending { passwordCalc.calculateEntropyBits(it.secretContent) }
                    VaultSortOption.STRENGTH_ASC -> searchFilteredItems.sortedBy { passwordCalc.calculateEntropyBits(it.secretContent) }
                }
            }

            if (finalVaultItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedCategory != "ALL") {
                        val label = categoryDefinitions.find { it.first.equals(selectedCategory, ignoreCase = true) }?.second ?: selectedCategory
                        EmptyCategoryIllustration(
                            categoryKey = selectedCategory,
                            categoryLabel = label,
                            onAddSecret = { showAddVaultDialog = true },
                            onClearCategory = {
                                vaultSearchQuery = ""
                                selectedCategory = "ALL"
                            }
                        )
                    } else if (vaultSearchQuery.isNotBlank()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = stringResource(R.string.no_matching_secrets),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.no_secrets_match_query, vaultSearchQuery),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(horizontal = 32.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            OutlinedButton(
                                onClick = { vaultSearchQuery = "" },
                                modifier = Modifier.dpadFocusable(shape = RoundedCornerShape(20.dp))
                            ) {
                                Text(stringResource(R.string.clear_search))
                            }
                        }
                    } else {
                        EmptyVaultIllustration(
                            onAddFirstSecret = { showAddVaultDialog = true }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = finalVaultItems,
                        key = { it.id }
                    ) { vaultItem ->
                        EncryptedSecretCard(
                            item = vaultItem,
                            isCopied = (activeCopiedItemId == vaultItem.id),
                            copyCountdown = clipboardCountdown,
                            onCopy = { viewModel.copyVaultItemToClipboard(vaultItem) },
                            onClearClipboard = { viewModel.clearClipboardNow() },
                            dateFormat = dateFormat,
                            onEdit = { editingVaultItem = vaultItem },
                            onDelete = { noteToDelete = vaultItem }
                        )
                    }
                }
            }
        } else if (selectedTab == 0) {
            // Tab 0: Intruder Snapshots
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.intruder_banner_info),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (intruderSelfies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyIntruderIllustration()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(
                        items = intruderSelfies,
                        key = { it.id }
                    ) { selfie ->
                        IntruderSelfieCard(
                            selfie = selfie,
                            dateFormat = dateFormat,
                            onClick = { selectedSelfie = selfie },
                            onDelete = { viewModel.deleteIntruderSelfie(selfie.id) }
                        )
                    }
                }
            }
        } else if (selectedTab == 2) {
            // Tab 2: Trash Bin for Soft-Deleted Secrets
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoDelete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.trash_banner_info, trashPurgeDays),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (trashVaultItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyTrashIllustration(trashPurgeDays = trashPurgeDays)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = trashVaultItems,
                        key = { it.id }
                    ) { trashItem ->
                        TrashSecretCard(
                            item = trashItem,
                            dateFormat = dateFormat,
                            onRestore = { viewModel.restoreVaultItemFromTrash(trashItem.id) },
                            onPermanentDelete = { trashToDelete = trashItem }
                        )
                    }
                }
            }
        }
    }

    // Custom AlertDialog for Note Deletion (Preventing Accidental Data Loss)
    if (noteToDelete != null) {
        val item = noteToDelete!!
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.delete_note_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.delete_note_message, item.title, trashPurgeDays),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.moveVaultItemToTrash(item.id)
                        Toast.makeText(context, "Note moved to Trash Bin", Toast.LENGTH_SHORT).show()
                        noteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_confirm_delete_note")
                ) {
                    Text(stringResource(R.string.delete_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { noteToDelete = null },
                    modifier = Modifier.testTag("btn_cancel_delete_note")
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Custom AlertDialog for Permanent Deletion from Trash Bin
    if (trashToDelete != null) {
        val item = trashToDelete!!
        AlertDialog(
            onDismissRequest = { trashToDelete = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.delete_note_permanent_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.delete_note_permanent_message, item.title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEncryptedVaultItem(item.id)
                        Toast.makeText(context, "Note permanently erased", Toast.LENGTH_SHORT).show()
                        trashToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_confirm_permanent_delete")
                ) {
                    Text(stringResource(R.string.delete_permanent_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { trashToDelete = null },
                    modifier = Modifier.testTag("btn_cancel_permanent_delete")
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Dialog for Adding New Encrypted Vault Secret / Note
    if (showAddVaultDialog) {
        var secretTitle by remember { mutableStateOf("") }
        var secretValue by remember { mutableStateOf("") }
        var secretCategory by remember { mutableStateOf("NOTE") }
        var passLength by remember { mutableFloatStateOf(16f) }
        var incUpper by remember { mutableStateOf(true) }
        var incLower by remember { mutableStateOf(true) }
        var incDigits by remember { mutableStateOf(true) }
        var incSymbols by remember { mutableStateOf(true) }
        val passwordService = remember { com.example.service.PasswordGeneratorService() }

        AlertDialog(
            onDismissRequest = { showAddVaultDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.add_secret_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = secretTitle,
                        onValueChange = { secretTitle = it },
                        label = { Text(stringResource(R.string.title_service_name)) },
                        placeholder = { Text(stringResource(R.string.title_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_secret_title")
                    )

                    OutlinedTextField(
                        value = secretValue,
                        onValueChange = { secretValue = it },
                        label = { Text(stringResource(R.string.secret_content_label)) },
                        placeholder = { Text(stringResource(R.string.secret_content_placeholder)) },
                        modifier = Modifier.fillMaxWidth().testTag("input_secret_value"),
                        maxLines = 4
                    )

                    // Real-Time Password Strength Indicator
                    PasswordStrengthIndicator(password = secretValue)

                    // Secure Password Generator Utility Component
                    PasswordGeneratorUtilityComponent(
                        onPasswordGenerated = { generated -> secretValue = generated }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "WORK" to stringResource(R.string.category_work),
                            "PERSONAL" to stringResource(R.string.category_personal),
                            "FINANCE" to stringResource(R.string.category_finance),
                            "PASSWORD" to stringResource(R.string.category_password),
                            "PIN" to stringResource(R.string.category_pin),
                            "NOTE" to stringResource(R.string.category_note)
                        ).chunked(3).forEach { rowChips ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                rowChips.forEach { (catKey, catLabel) ->
                                    FilterChip(
                                        selected = secretCategory == catKey,
                                        onClick = { secretCategory = catKey },
                                        label = { Text(catLabel, fontSize = 11.sp) },
                                        modifier = Modifier.testTag("chip_add_category_$catKey")
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (secretTitle.isNotBlank() && secretValue.isNotBlank()) {
                            viewModel.saveEncryptedVaultItem(secretTitle, secretValue, secretCategory)
                            showAddVaultDialog = false
                        }
                    },
                    modifier = Modifier.testTag("btn_save_secret")
                ) {
                    Text(stringResource(R.string.save_encrypted))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVaultDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Dialog for Editing Existing Encrypted Vault Secret / Note
    if (editingVaultItem != null) {
        val item = editingVaultItem!!
        var editTitle by remember { mutableStateOf(item.title) }
        var editValue by remember { mutableStateOf(item.secretContent) }
        var editCategory by remember { mutableStateOf(item.category) }

        AlertDialog(
            onDismissRequest = { editingVaultItem = null },
            title = {
                Text(
                    text = stringResource(R.string.edit_secret_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text(stringResource(R.string.title_service_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_edit_secret_title")
                    )

                    OutlinedTextField(
                        value = editValue,
                        onValueChange = { editValue = it },
                        label = { Text(stringResource(R.string.secret_content_label)) },
                        modifier = Modifier.fillMaxWidth().testTag("input_edit_secret_value"),
                        maxLines = 4
                    )

                    // Real-Time Password Strength Indicator for Edits
                    PasswordStrengthIndicator(password = editValue)

                    PasswordGeneratorUtilityComponent(
                        onPasswordGenerated = { generated -> editValue = generated }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "WORK" to stringResource(R.string.category_work),
                            "PERSONAL" to stringResource(R.string.category_personal),
                            "FINANCE" to stringResource(R.string.category_finance),
                            "PASSWORD" to stringResource(R.string.category_password),
                            "PIN" to stringResource(R.string.category_pin),
                            "NOTE" to stringResource(R.string.category_note)
                        ).chunked(3).forEach { rowChips ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                rowChips.forEach { (catKey, catLabel) ->
                                    FilterChip(
                                        selected = editCategory == catKey,
                                        onClick = { editCategory = catKey },
                                        label = { Text(catLabel, fontSize = 11.sp) },
                                        modifier = Modifier.testTag("chip_edit_category_$catKey")
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editTitle.isNotBlank() && editValue.isNotBlank()) {
                            viewModel.updateEncryptedVaultItem(
                                item.copy(
                                    title = editTitle,
                                    secretContent = editValue,
                                    category = editCategory,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                            editingVaultItem = null
                            Toast.makeText(context, "Note updated successfully", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("btn_save_edited_secret")
                ) {
                    Text(stringResource(R.string.save_encrypted))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingVaultItem = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Dialog for Exporting Encrypted Notes (.plk file)
    if (showExportNotesDialog) {
        AlertDialog(
            onDismissRequest = { showExportNotesDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.export_notes_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.export_notes_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = exportPassphrase,
                        onValueChange = { exportPassphrase = it },
                        label = { Text(stringResource(R.string.export_passphrase_label)) },
                        placeholder = { Text(stringResource(R.string.export_passphrase_hint)) },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("input_export_notes_passphrase")
                    )

                    if (exportResultStatus != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = exportResultStatus!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (exportPassphrase.isNotBlank()) {
                            coroutineScope.launch {
                                val file = viewModel.exportEncryptedNotesToFile(exportPassphrase)
                                if (file.exists()) {
                                    exportResultStatus = context.getString(R.string.export_file_success, file.name)
                                    Toast.makeText(context, "Export complete: ${file.name}", Toast.LENGTH_LONG).show()
                                } else {
                                    exportResultStatus = "Export completed."
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_export_notes_file")
                ) {
                    Text(stringResource(R.string.btn_export_notes_file))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportNotesDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Dialog for Importing Encrypted Notes (.plk file / payload)
    if (showImportNotesDialog) {
        AlertDialog(
            onDismissRequest = { showImportNotesDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.import_notes_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.import_notes_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = importPassphrase,
                        onValueChange = { importPassphrase = it },
                        label = { Text(stringResource(R.string.import_passphrase_label)) },
                        placeholder = { Text(stringResource(R.string.import_passphrase_hint)) },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("input_import_notes_passphrase")
                    )

                    OutlinedTextField(
                        value = importPayloadInput,
                        onValueChange = { importPayloadInput = it },
                        label = { Text("Encrypted Payload or Filename") },
                        placeholder = { Text("Paste JSON payload or enter filename (e.g. purelock_encrypted_notes.plk)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_import_notes_payload"),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importPassphrase.isNotBlank()) {
                            coroutineScope.launch {
                                val success = if (importPayloadInput.startsWith("{")) {
                                    viewModel.importEncryptedNotesJson(importPayloadInput, importPassphrase)
                                } else {
                                    val backupDir = context.getExternalFilesDir("backups") ?: context.filesDir
                                    val targetFile = if (importPayloadInput.isNotBlank()) {
                                        java.io.File(backupDir, importPayloadInput)
                                    } else {
                                        backupDir.listFiles { f -> f.extension == "plk" }?.maxByOrNull { it.lastModified() }
                                    }
                                    if (targetFile != null && targetFile.exists()) {
                                        viewModel.importEncryptedNotesFromFile(targetFile, importPassphrase)
                                    } else {
                                        false
                                    }
                                }

                                if (success) {
                                    Toast.makeText(context, context.getString(R.string.import_file_success), Toast.LENGTH_LONG).show()
                                    showImportNotesDialog = false
                                } else {
                                    Toast.makeText(context, context.getString(R.string.import_file_error), Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_import_notes_file")
                ) {
                    Text(stringResource(R.string.btn_import_notes_file))
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportNotesDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Vault Insights Dashboard Modal Dialog
    if (showVaultInsightsDialog) {
        VaultInsightsDialog(
            vaultItems = encryptedVaultItems,
            onDismiss = { showVaultInsightsDialog = false }
        )
    }

    // Detail Photo Modal Dialog
    if (selectedSelfie != null) {
        val selfie = selectedSelfie!!
        AlertDialog(
            onDismissRequest = { selectedSelfie = null },
            title = {
                Text(
                    text = "Intruder Attempt Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val bitmap = remember(selfie.photoData) {
                        try {
                            val decoded = Base64.decode(selfie.photoData, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Captured Intruder Photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Target App: ${selfie.attemptedAppName}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Failed Attempts: ${selfie.failedAttempts}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Captured: ${dateFormat.format(Date(selfie.timestamp))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedSelfie = null }) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteIntruderSelfie(selfie.id)
                        selectedSelfie = null
                    }
                ) {
                    Text("Delete Record", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}

@Composable
private fun IntruderSelfieCard(
    selfie: IntruderSelfieEntity,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val bitmap = remember(selfie.photoData) {
        try {
            val decoded = Base64.decode(selfie.photoData, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
        } catch (e: Exception) {
            null
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("card_intruder_${selfie.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Intruder Selfie",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(topStart = 0.dp, bottomEnd = 8.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = "${selfie.failedAttempts} Failed",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = selfie.attemptedAppName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = dateFormat.format(Date(selfie.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp).testTag("btn_delete_intruder_${selfie.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Selfie",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EncryptedSecretCard(
    item: EncryptedVaultEntity,
    isCopied: Boolean,
    copyCountdown: Int,
    onCopy: () -> Unit,
    onClearClipboard: () -> Unit,
    dateFormat: SimpleDateFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var isRevealed by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth().testTag("card_vault_secret_${item.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (item.category.uppercase()) {
                                "WORK" -> Icons.Default.BusinessCenter
                                "PERSONAL" -> Icons.Default.Person
                                "FINANCE" -> Icons.Default.AccountBalance
                                "PASSWORD" -> Icons.Default.Key
                                "PIN" -> Icons.Default.Pin
                                else -> Icons.Default.Description
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${item.category} • ${dateFormat.format(Date(item.timestamp))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("btn_edit_secret_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Secret",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("btn_delete_secret_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Secret",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRevealed) item.secretContent else "••••••••••••••••",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isRevealed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { isRevealed = !isRevealed },
                            modifier = Modifier.size(32.dp).testTag("btn_toggle_reveal_${item.id}")
                        ) {
                            Icon(
                                imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Visibility",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Copy to Clipboard Button using Centralized Auto-Clear Policy
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(32.dp).testTag("btn_copy_secret_${item.id}")
                        ) {
                            Icon(
                                imageVector = if (isCopied) Icons.Default.CheckCircle else Icons.Default.ContentCopy,
                                contentDescription = "Copy Secret to Clipboard",
                                tint = if (isCopied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Clipboard Auto-Clear Countdown Live Banner
            if (isCopied) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (copyCountdown > 0) {
                                stringResource(R.string.copied_clipboard_countdown, copyCountdown)
                            } else "Copied to clipboard",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(
                        onClick = onClearClipboard,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(24.dp).testTag("btn_clear_clipboard_${item.id}")
                    ) {
                        Text(stringResource(R.string.clear_clipboard_now), style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PasswordStrengthIndicator(
    password: String,
    modifier: Modifier = Modifier
) {
    if (password.isEmpty()) return

    val passwordService = remember { com.example.service.PasswordGeneratorService() }
    val entropyBits = remember(password) { passwordService.calculateEntropyBits(password) }
    val strength = remember(password) { passwordService.calculateStrength(password) }

    val progressFraction = remember(entropyBits) {
        (entropyBits / 100.0).coerceIn(0.05, 1.0).toFloat()
    }

    val (strengthLabel, progressColor) = when (strength) {
        com.example.service.PasswordStrength.WEAK -> stringResource(R.string.password_strength_weak) to MaterialTheme.colorScheme.error
        com.example.service.PasswordStrength.MEDIUM -> stringResource(R.string.password_strength_fair) to MaterialTheme.colorScheme.tertiary
        com.example.service.PasswordStrength.STRONG -> stringResource(R.string.password_strength_strong) to MaterialTheme.colorScheme.primary
        com.example.service.PasswordStrength.VERY_STRONG -> stringResource(R.string.password_strength_very_strong) to androidx.compose.ui.graphics.Color(0xFF00C853)
    }

    val hasUpper = remember(password) { password.any { it.isUpperCase() } }
    val hasLower = remember(password) { password.any { it.isLowerCase() } }
    val hasDigits = remember(password) { password.any { it.isDigit() } }
    val hasSymbols = remember(password) { password.any { !it.isLetterOrDigit() } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = progressColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.entropy_bits_format, entropyBits),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = strengthLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = progressColor
            )
        }

        LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = progressColor,
            trackColor = progressColor.copy(alpha = 0.2f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StrengthChip(label = stringResource(R.string.strength_rule_upper), active = hasUpper)
            StrengthChip(label = stringResource(R.string.strength_rule_lower), active = hasLower)
            StrengthChip(label = stringResource(R.string.strength_rule_digits), active = hasDigits)
            StrengthChip(label = stringResource(R.string.strength_rule_symbols), active = hasSymbols)
            StrengthChip(label = stringResource(R.string.strength_rule_length), active = password.length >= 12)
        }
    }
}

@Composable
private fun StrengthChip(label: String, active: Boolean) {
    Surface(
        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Text(
            text = (if (active) "✓ " else "") + label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun TrashSecretCard(
    item: EncryptedVaultEntity,
    dateFormat: SimpleDateFormat,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth().testTag("card_trash_secret_${item.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Deleted: ${dateFormat.format(Date(if (item.deletedTimestamp > 0) item.deletedTimestamp else item.timestamp))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onRestore,
                    modifier = Modifier.testTag("btn_restore_trash_${item.id}")
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.restore))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onPermanentDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_delete_permanently_${item.id}")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.delete_confirm))
                }
            }
        }
    }
}

@Composable
fun PasswordGeneratorUtilityComponent(
    onPasswordGenerated: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var length by remember { mutableFloatStateOf(16f) }
    var incUpper by remember { mutableStateOf(true) }
    var incLower by remember { mutableStateOf(true) }
    var incDigits by remember { mutableStateOf(true) }
    var incSymbols by remember { mutableStateOf(true) }
    var excludeAmbiguous by remember { mutableStateOf(false) }
    val generatorService = remember { com.example.service.PasswordGeneratorService() }

    fun doGenerate(): String {
        val config = com.example.service.PasswordGeneratorConfig(
            length = length.toInt(),
            includeUppercase = incUpper,
            includeLowercase = incLower,
            includeNumbers = incDigits,
            includeSymbols = incSymbols,
            excludeAmbiguous = excludeAmbiguous
        )
        return generatorService.generatePassword(config)
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.generator_toggle_expand),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quick Presets
                    Text(
                        text = "Quick Presets:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SuggestionChip(
                            onClick = {
                                length = 4f; incDigits = true; incUpper = false; incLower = false; incSymbols = false
                                onPasswordGenerated(doGenerate())
                            },
                            label = { Text(stringResource(R.string.generator_preset_pin4), fontSize = 10.sp) }
                        )
                        SuggestionChip(
                            onClick = {
                                length = 6f; incDigits = true; incUpper = false; incLower = false; incSymbols = false
                                onPasswordGenerated(doGenerate())
                            },
                            label = { Text(stringResource(R.string.generator_preset_pin6), fontSize = 10.sp) }
                        )
                        SuggestionChip(
                            onClick = {
                                length = 16f; incDigits = true; incUpper = true; incLower = true; incSymbols = true
                                onPasswordGenerated(doGenerate())
                            },
                            label = { Text(stringResource(R.string.generator_preset_standard), fontSize = 10.sp) }
                        )
                    }

                    Text(
                        text = stringResource(R.string.generator_length_label, length.toInt()),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = length,
                        onValueChange = { length = it },
                        valueRange = 4f..32f,
                        steps = 27,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Character Set Options
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            FilterChip(
                                selected = incUpper,
                                onClick = { incUpper = !incUpper },
                                label = { Text("A-Z", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = incLower,
                                onClick = { incLower = !incLower },
                                label = { Text("a-z", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = incDigits,
                                onClick = { incDigits = !incDigits },
                                label = { Text("0-9", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = incSymbols,
                                onClick = { incSymbols = !incSymbols },
                                label = { Text("!@#", fontSize = 10.sp) }
                            )
                        }

                        FilterChip(
                            selected = excludeAmbiguous,
                            onClick = { excludeAmbiguous = !excludeAmbiguous },
                            label = { Text(stringResource(R.string.generator_charset_no_ambiguous), fontSize = 10.sp) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Button(
                        onClick = { onPasswordGenerated(doGenerate()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_generate_password_apply")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.generator_btn_apply))
                    }
                }
            }
        }
    }
}
