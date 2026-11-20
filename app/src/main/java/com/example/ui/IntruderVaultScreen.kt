package com.example.ui

import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.EncryptedVaultEntity
import com.example.data.model.IntruderSelfieEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntruderVaultScreen(
    viewModel: PureLockViewModel
) {
    val context = LocalContext.current
    val intruderSelfies by viewModel.intruderSelfies.collectAsState()
    val encryptedVaultItems by viewModel.encryptedVaultItems.collectAsState()
    val activeCopiedItemId by viewModel.activeCopiedItemId.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Intruder Photos, 1: Secret Notes
    var showVaultInfoDialog by remember { mutableStateOf(false) }
    var selectedSelfie by remember { mutableStateOf<IntruderSelfieEntity?>(null) }
    var showAddVaultDialog by remember { mutableStateOf(false) }
    var editingVaultItem by remember { mutableStateOf<EncryptedVaultEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<EncryptedVaultEntity?>(null) }
    var vaultSearchQuery by remember { mutableStateOf("") }
    var showClearAllSelfiesConfirm by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("MMM dd • HH:mm", Locale.getDefault()) }

    val filteredNotes = remember(encryptedVaultItems, vaultSearchQuery) {
        encryptedVaultItems.filter { item ->
            vaultSearchQuery.isEmpty() ||
                    item.title.contains(vaultSearchQuery, ignoreCase = true) ||
                    item.secretContent.contains(vaultSearchQuery, ignoreCase = true)
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
            // Header Bar
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
                        Text(
                            text = if (selectedTab == 0) stringResource(R.string.intruder_title) else stringResource(R.string.vault_dashboard_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (selectedTab == 0) stringResource(R.string.tab_intruder_photos, intruderSelfies.size) else stringResource(R.string.tab_secret_vault, encryptedVaultItems.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedTab == 0 && intruderSelfies.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearAllSelfiesConfirm = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .testTag("btn_clear_all_selfies")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = stringResource(R.string.cd_clear_all),
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (selectedTab == 1) {
                        IconButton(
                            onClick = { showAddVaultDialog = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .testTag("btn_add_secret")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.cd_add_secret),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { showVaultInfoDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("btn_vault_info")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.cd_vault_info),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Compact Tab Switcher (Photos vs Notes)
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.tab_intruder_photos, intruderSelfies.size), fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("tab_intruder_photos")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.tab_secret_vault, encryptedVaultItems.size), fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("tab_secret_notes")
                )
            }

            // Content Area with smooth animated tab transition
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "IntruderTabTransition",
                modifier = Modifier.weight(1f)
            ) { tabIndex ->
                if (tabIndex == 0) {
                    // Intruder Photos Tab
                    if (intruderSelfies.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyIntruderIllustration()
                        }
                    } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(intruderSelfies, key = { it.id }) { selfie ->
                            val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
                                initialValue = null,
                                key1 = selfie.photoData
                            ) {
                                value = withContext(Dispatchers.IO) {
                                    try {
                                        val bytes = Base64.decode(selfie.photoData, Base64.DEFAULT)
                                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                    } catch (_: Exception) {
                                        null
                                    }
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedSelfie = selfie }
                                    .testTag("card_selfie_${selfie.id}")
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                            .background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        val currentBitmap = bitmap
                                        if (currentBitmap != null) {
                                            Image(
                                                bitmap = currentBitmap,
                                                contentDescription = stringResource(R.string.snapshots),
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.BrokenImage,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = selfie.attemptedAppName.ifEmpty { stringResource(R.string.app_name) },
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = dateFormat.format(Date(selfie.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                } else {
                    // Secret Notes Tab
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = vaultSearchQuery,
                            onValueChange = { vaultSearchQuery = it },
                            modifier = Modifier.fillMaxWidth().testTag("input_vault_search"),
                            placeholder = { Text(stringResource(R.string.search_vault_placeholder), style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
                            trailingIcon = {
                                if (vaultSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { vaultSearchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear), tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (filteredNotes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (vaultSearchQuery.isNotEmpty()) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SearchOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.no_matching_secrets),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(R.string.no_secrets_match_query, vaultSearchQuery),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    EmptyVaultIllustration(
                                        onAddFirstSecret = { showAddVaultDialog = true }
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                        items(filteredNotes, key = { it.id }) { item ->
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { editingVaultItem = item }
                                    .testTag("item_vault_${item.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Key,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "••••••••••••",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                viewModel.copyVaultItemToClipboard(item)
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (activeCopiedItemId == item.id) Icons.Default.Check else Icons.Default.ContentCopy,
                                                contentDescription = stringResource(R.string.cd_copy_secret),
                                                tint = if (activeCopiedItemId == item.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { itemToDelete = item },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = stringResource(R.string.cd_delete),
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
    }

    // Vault Info Dialog
    if (showVaultInfoDialog) {
        AlertDialog(
            onDismissRequest = { showVaultInfoDialog = false },
            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.vault_security_details_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.vault_security_details_bullet1), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.vault_security_details_bullet2), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.vault_security_details_bullet3), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showVaultInfoDialog = false }) { Text(stringResource(R.string.ok)) }
            }
        )
    }

    // Inspect Selfie Dialog
    selectedSelfie?.let { selfie ->
        AlertDialog(
            onDismissRequest = { selectedSelfie = null },
            title = {
                Text(
                    text = stringResource(R.string.intruder_attempt_app, selfie.attemptedAppName.ifEmpty { stringResource(R.string.app_name) }),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
                        initialValue = null,
                        key1 = selfie.photoData
                    ) {
                        value = withContext(Dispatchers.IO) {
                            try {
                                val bytes = Base64.decode(selfie.photoData, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                            } catch (_: Exception) {
                                null
                            }
                        }
                    }

                    val detailBitmap = bitmap
                    if (detailBitmap != null) {
                        Image(
                            bitmap = detailBitmap,
                            contentDescription = stringResource(R.string.snapshots),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Text(
                        text = stringResource(R.string.intruder_attempt_at, dateFormat.format(Date(selfie.timestamp))),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedSelfie = null }) { Text(stringResource(R.string.cancel)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteIntruderSelfie(selfie.id)
                        selectedSelfie = null
                    }
                ) {
                    Text(stringResource(R.string.btn_delete_snapshot), color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    // Clear All Selfies Confirmation
    if (showClearAllSelfiesConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllSelfiesConfirm = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.clear_all_snapshots_title)) },
            text = { Text(stringResource(R.string.clear_all_snapshots_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllIntruders()
                        showClearAllSelfiesConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllSelfiesConfirm = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // Add / Edit Secret Note Dialog
    if (showAddVaultDialog || editingVaultItem != null) {
        val isEditing = editingVaultItem != null
        var title by remember(editingVaultItem?.id) { mutableStateOf(editingVaultItem?.title ?: "") }
        var secretContent by remember(editingVaultItem?.id) { mutableStateOf(editingVaultItem?.secretContent ?: "") }
        var isPasswordVisible by remember(editingVaultItem?.id) { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                showAddVaultDialog = false
                editingVaultItem = null
            },
            title = { Text(if (isEditing) stringResource(R.string.dialog_edit_secret) else stringResource(R.string.dialog_new_secret), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.field_title)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = secretContent,
                        onValueChange = { secretContent = it },
                        label = { Text(stringResource(R.string.field_secret_content)) },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val currentItem = editingVaultItem
                            if (isEditing && currentItem != null) {
                                viewModel.updateEncryptedVaultItem(
                                    currentItem.copy(
                                        title = title,
                                        secretContent = secretContent
                                    )
                                )
                            } else {
                                viewModel.saveEncryptedVaultItem(
                                    title = title,
                                    secretContent = secretContent,
                                    category = "NOTE"
                                )
                            }
                            showAddVaultDialog = false
                            editingVaultItem = null
                        }
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddVaultDialog = false
                    editingVaultItem = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Delete Vault Item Confirmation
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.delete_secret_dialog_title)) },
            text = { Text(stringResource(R.string.delete_secret_dialog_message, item.title)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEncryptedVaultItem(item.id)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
