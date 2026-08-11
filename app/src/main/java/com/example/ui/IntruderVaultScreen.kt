package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IntruderSelfieEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun IntruderVaultScreen(
    viewModel: PureLockViewModel
) {
    val intruderSelfies by viewModel.intruderSelfies.collectAsState()
    val encryptedVaultItems by viewModel.encryptedVaultItems.collectAsState()
    val trashVaultItems by viewModel.trashVaultItems.collectAsState()
    val trashPurgeDays by viewModel.trashPurgeDays.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedSelfie by remember { mutableStateOf<IntruderSelfieEntity?>(null) }
    var showAddVaultDialog by remember { mutableStateOf(false) }
    var vaultSearchQuery by remember { mutableStateOf("") }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header Title with Clear All Action
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
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (selectedTab) {
                            0 -> Icons.Default.CameraAlt
                            1 -> Icons.Default.Key
                            else -> Icons.Default.Delete
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Column {
                    Text(
                        text = "PureLock Vault",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when (selectedTab) {
                            0 -> "${intruderSelfies.size} Snapshots Captured"
                            1 -> "${encryptedVaultItems.size} SQLCipher Encrypted Secrets"
                            else -> "${trashVaultItems.size} Soft-Deleted Items"
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
                    Text("Add Secret")
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
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Snapshots") },
                icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                modifier = Modifier.testTag("tab_intruder_snapshots")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Secrets") },
                icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                modifier = Modifier.testTag("tab_encrypted_secrets")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Trash Bin") },
                icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                modifier = Modifier.testTag("tab_trash_bin")
            )
        }

        if (selectedTab == 0) {
            // Privacy Banner
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
                        text = "Intruder photos are encrypted and stored exclusively inside app private memory. They never sync to clouds or gallery.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Grid Content
            if (intruderSelfies.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GppGood,
                            contentDescription = "Vault Secure",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Vault Secure — Zero Intruder Attempts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "If an unauthorized user attempts to open a locked app 3 times, their snapshot will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
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
        } else if (selectedTab == 1) {
            // Tab 1: SQLCipher Encrypted Secrets & Passwords Vault
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
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "256-bit SQLCipher Encrypted Vault. Secrets are saved locally in hardware-backed storage with zero cloud sync.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Search Bar for Vault Secrets
            OutlinedTextField(
                value = vaultSearchQuery,
                onValueChange = { vaultSearchQuery = it },
                placeholder = { Text("Search passwords, notes, titles...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Vault") },
                trailingIcon = {
                    if (vaultSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { vaultSearchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("input_vault_search")
            )

            val filteredVaultItems = remember(encryptedVaultItems, vaultSearchQuery) {
                if (vaultSearchQuery.isBlank()) encryptedVaultItems
                else encryptedVaultItems.filter {
                    it.title.contains(vaultSearchQuery, ignoreCase = true) ||
                    it.category.contains(vaultSearchQuery, ignoreCase = true) ||
                    it.secretContent.contains(vaultSearchQuery, ignoreCase = true)
                }
            }

            if (filteredVaultItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockReset,
                            contentDescription = "No Secrets Stored",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = if (vaultSearchQuery.isNotBlank()) "No Matching Secrets Found" else "Encrypted Vault is Empty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (vaultSearchQuery.isNotBlank()) "No items match '$vaultSearchQuery'. Try a different keyword." else "Store your sensitive passwords, recovery keys, or secret notes securely.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (vaultSearchQuery.isNotBlank()) {
                            OutlinedButton(onClick = { vaultSearchQuery = "" }) {
                                Text("Clear Search Filter")
                            }
                        } else {
                            Button(
                                onClick = { showAddVaultDialog = true },
                                modifier = Modifier.testTag("btn_empty_add_secret")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Store First Secret Note")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = filteredVaultItems,
                        key = { it.id }
                    ) { vaultItem ->
                        EncryptedSecretCard(
                            item = vaultItem,
                            dateFormat = dateFormat,
                            onDelete = { viewModel.moveVaultItemToTrash(vaultItem.id) }
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
                        text = "Trash Bin safeguards soft-deleted items. Items are automatically purged after $trashPurgeDays days.",
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Trash Bin Empty",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Trash Bin is Empty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Secrets deleted from your encrypted vault will remain here for $trashPurgeDays days before permanent removal.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
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
                            onPermanentDelete = { viewModel.deleteEncryptedVaultItem(trashItem.id) }
                        )
                    }
                }
            }
        }
    }

    // Dialog for Adding New Encrypted Vault Secret
    if (showAddVaultDialog) {
        var secretTitle by remember { mutableStateOf("") }
        var secretValue by remember { mutableStateOf("") }
        var secretCategory by remember { mutableStateOf("PASSWORD") }
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
                    text = "Add Encrypted Secret",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = secretTitle,
                        onValueChange = { secretTitle = it },
                        label = { Text("Title / Service Name") },
                        placeholder = { Text("e.g. Master Email, Banking Pin, Seed Phrase") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_secret_title")
                    )

                    OutlinedTextField(
                        value = secretValue,
                        onValueChange = { secretValue = it },
                        label = { Text("Secret Password / Note") },
                        placeholder = { Text("Enter or generate secret...") },
                        modifier = Modifier.fillMaxWidth().testTag("input_secret_content")
                    )

                    // Real-time Visual Password Entropy & Strength Indicator Component
                    PasswordStrengthIndicator(password = secretValue)

                    // Cryptographic Password Generator Controls
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Generator (Length: ${passLength.toInt()})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = {
                                        val config = com.example.service.PasswordGeneratorConfig(
                                            length = passLength.toInt(),
                                            includeUppercase = incUpper,
                                            includeLowercase = incLower,
                                            includeNumbers = incDigits,
                                            includeSymbols = incSymbols
                                        )
                                        secretValue = passwordService.generatePassword(config)
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp).testTag("btn_generate_password")
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Generate", fontSize = 11.sp)
                                }
                            }

                            Slider(
                                value = passLength,
                                onValueChange = { passLength = it },
                                valueRange = 8f..32f,
                                steps = 23,
                                modifier = Modifier.fillMaxWidth()
                            )

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
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = secretCategory == "PASSWORD",
                            onClick = { secretCategory = "PASSWORD" },
                            label = { Text("Password") }
                        )
                        FilterChip(
                            selected = secretCategory == "NOTE",
                            onClick = { secretCategory = "NOTE" },
                            label = { Text("Secret Note") }
                        )
                        FilterChip(
                            selected = secretCategory == "PIN",
                            onClick = { secretCategory = "PIN" },
                            label = { Text("Bank PIN") }
                        )
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
                    Text("Save Encrypted")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVaultDialog = false }) {
                    Text("Cancel")
                }
            }
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
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Intruder Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.NoPhotography,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline
                    )
                }

                // Failed Count Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.error)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${selfie.failedAttempts} Failures",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selfie.attemptedAppName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = dateFormat.format(Date(selfie.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp).testTag("btn_delete_selfie_${selfie.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EncryptedSecretCard(
    item: com.example.data.model.EncryptedVaultEntity,
    dateFormat: SimpleDateFormat,
    onDelete: () -> Unit
) {
    var isRevealed by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isCopied by remember { mutableStateOf(false) }
    var copyCountdown by remember { mutableIntStateOf(0) }
    var copyJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (item.category) {
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
                            text = "Category: ${item.category} • ${dateFormat.format(Date(item.timestamp))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
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

                        // Copy to Clipboard Button with 30s Auto-Clear
                        IconButton(
                            onClick = {
                                copyJob?.cancel()
                                copyJob = coroutineScope.launch {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("VaultSecret", item.secretContent)
                                    clipboard.setPrimaryClip(clip)
                                    isCopied = true
                                    copyCountdown = 30
                                    while (copyCountdown > 0) {
                                        delay(1000L)
                                        copyCountdown--
                                    }
                                    clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                                    isCopied = false
                                }
                            },
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

            // Clipboard Auto-Clear Countdown Banner
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
                            text = "Copied! Clipboard clears in ${copyCountdown}s",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(
                        onClick = {
                            copyJob?.cancel()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                            isCopied = false
                            copyCountdown = 0
                        },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("Clear Now", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
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
        com.example.service.PasswordStrength.WEAK -> "Weak" to MaterialTheme.colorScheme.error
        com.example.service.PasswordStrength.MEDIUM -> "Fair" to MaterialTheme.colorScheme.tertiary
        com.example.service.PasswordStrength.STRONG -> "Strong" to MaterialTheme.colorScheme.primary
        com.example.service.PasswordStrength.VERY_STRONG -> "Very Strong" to androidx.compose.ui.graphics.Color(0xFF00C853)
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
                    text = "Entropy: ${String.format(Locale.US, "%.1f", entropyBits)} bits",
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
            StrengthChip(label = "A-Z", active = hasUpper)
            StrengthChip(label = "a-z", active = hasLower)
            StrengthChip(label = "0-9", active = hasDigits)
            StrengthChip(label = "!@#", active = hasSymbols)
            StrengthChip(label = "12+ Chars", active = password.length >= 12)
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
    item: com.example.data.model.EncryptedVaultEntity,
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
                    Text("Restore")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onPermanentDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_delete_permanently_${item.id}")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}
