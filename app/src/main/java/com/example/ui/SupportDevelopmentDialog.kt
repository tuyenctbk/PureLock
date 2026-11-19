package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.service.DonationBillingUiState
import com.example.service.DonationTier
import com.example.service.findActivity
import com.example.util.DonationChannelsHelper
import com.example.util.DonationEngagementState
import com.example.util.ExternalDonationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class DonationChannelTab(val titleRes: Int) {
    GOOGLE_PLAY(R.string.donation_tab_google_play),
    PAYPAL_QR(R.string.donation_tab_paypal_qr),
    KOFI(R.string.donation_tab_kofi)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportDevelopmentDialog(
    engagementState: DonationEngagementState,
    billingState: DonationBillingUiState,
    onLaunchGooglePlay: (Context, DonationTier) -> Unit,
    onClearBillingStatus: () -> Unit,
    onDismiss: () -> Unit
) {
    val hostContext = LocalContext.current
    val activityLaunchContext = remember(hostContext) {
        hostContext.findActivity() ?: hostContext
    }

    var selectedTab by remember { mutableStateOf(DonationChannelTab.GOOGLE_PLAY) }
    var selectedTier by remember { mutableStateOf(DonationTier.TIER_2) }

    Dialog(
        onDismissRequest = {
            onClearBillingStatus()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 520.dp)
                .heightIn(max = 680.dp)
                .testTag("dialog_support_development")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (engagementState.isActiveSupporter) Icons.Default.Verified else Icons.Default.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.donation_dialog_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.donation_dialog_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            onClearBillingStatus()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("btn_close_support_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                }

                if (engagementState.isActiveSupporter) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_supporter_verified")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.donation_supporter_active_title),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = stringResource(R.string.donation_supporter_active_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }

                // Channel Tabs
                PrimaryTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    DonationChannelTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = {
                                Text(
                                    text = stringResource(tab.titleRes),
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            },
                            modifier = Modifier.testTag("tab_donation_${tab.name.lowercase()}")
                        )
                    }
                }

                // Scrollable Channel Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (selectedTab) {
                        DonationChannelTab.GOOGLE_PLAY -> {
                            GooglePlayDonationTabContent(
                                selectedTier = selectedTier,
                                onSelectTier = { selectedTier = it },
                                billingState = billingState,
                                onLaunchPurchase = {
                                    onLaunchGooglePlay(activityLaunchContext, selectedTier)
                                }
                            )
                        }

                        DonationChannelTab.PAYPAL_QR -> {
                            PayPalQrDonationTabContent(context = activityLaunchContext)
                        }

                        DonationChannelTab.KOFI -> {
                            KofiDonationTabContent(context = activityLaunchContext)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GooglePlayDonationTabContent(
    selectedTier: DonationTier,
    onSelectTier: (DonationTier) -> Unit,
    billingState: DonationBillingUiState,
    onLaunchPurchase: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DonationTier.entries.forEach { tier ->
            val isSelected = selectedTier == tier
            val displayPrice = billingState.localizedPrices[tier.productId] ?: tier.defaultPriceLabel
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                },
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectTier(tier) }
                    .testTag("tier_card_${tier.productId}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectTier(tier) }
                        )
                        Column {
                            Text(
                                text = stringResource(tier.titleRes),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(tier.subtitleRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = displayPrice,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = billingState.statusMessage != null) {
            val statusText = billingState.statusMessage ?: ""
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (billingState.isError) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("billing_status_banner")
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (billingState.isError) Icons.Default.Info else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (billingState.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (billingState.isError) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
            }
        }

        val activePrice = billingState.localizedPrices[selectedTier.productId] ?: selectedTier.defaultPriceLabel
        Button(
            onClick = onLaunchPurchase,
            enabled = !billingState.isPurchaseInProgress,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("btn_launch_google_play_billing")
        ) {
            if (billingState.isPurchaseInProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.donation_play_connecting))
            } else {
                Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.donation_play_cta_format, activePrice))
            }
        }
    }
}

@Composable
private fun PayPalQrDonationTabContent(context: Context) {
    var selectedPreset by remember { mutableStateOf<Int?>(5) }
    var customAmountInput by remember { mutableStateOf("") }

    val effectiveAmount: Double? = remember(selectedPreset, customAmountInput) {
        if (selectedPreset != null) {
            selectedPreset?.toDouble()
        } else {
            customAmountInput.trim().toDoubleOrNull()?.takeIf { it > 0.0 }
        }
    }
    val paypalUrl = remember(effectiveAmount) {
        DonationChannelsHelper.buildPayPalUrl(effectiveAmount)
    }

    val qrBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = paypalUrl) {
        value = withContext(Dispatchers.IO) {
            DonationChannelsHelper.generateQrCodeBitmap(paypalUrl, sizePx = 440)?.asImageBitmap()
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.donation_paypal_choose_amount),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DonationChannelsHelper.PAYPAL_PRESET_AMOUNTS.forEach { preset ->
                val isSelected = selectedPreset == preset
                val label = if (preset != null) "\$$preset" else stringResource(R.string.donation_paypal_any_amount)
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedPreset = preset },
                    label = {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    },
                    modifier = Modifier
                        .weight(if (preset == null) 1.35f else 1f)
                        .testTag("chip_paypal_${preset ?: "any"}")
                )
            }
        }

        AnimatedVisibility(visible = selectedPreset == null) {
            OutlinedTextField(
                value = customAmountInput,
                onValueChange = { raw ->
                    val filtered = raw.filter { it.isDigit() || it == '.' }
                    if (filtered.count { it == '.' } <= 1 && filtered.length <= 7) {
                        customAmountInput = filtered
                    }
                },
                label = { Text(stringResource(R.string.donation_paypal_custom_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_paypal_custom_amount")
            )
        }

        // White quiet-zone QR code card
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.size(184.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                val bmp = qrBitmap
                if (bmp != null) {
                    Image(
                        bitmap = bmp,
                        contentDescription = stringResource(R.string.donation_paypal_qr_caption),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("img_paypal_qr_code")
                    )
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            }
        }

        Text(
            text = stringResource(R.string.donation_paypal_qr_caption),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = paypalUrl,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("text_paypal_url")
            )
        }

        val browserFallbackMsg = stringResource(R.string.donation_browser_fallback_toast)
        val linkCopiedMsg = stringResource(R.string.donation_link_copied_toast)
        val shareChooserTitle = stringResource(R.string.donation_paypal_share_chooser)
        val shareBody = stringResource(R.string.donation_paypal_share_body)

        Button(
            onClick = {
                val result = DonationChannelsHelper.openUrlOrCopyFallback(
                    context = context,
                    url = paypalUrl,
                    clipboardLabel = "PayPal Support Link"
                )
                if (result == ExternalDonationResult.CopiedToClipboardFallback) {
                    Toast.makeText(context, browserFallbackMsg, Toast.LENGTH_SHORT).show()
                }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("btn_open_paypal")
        ) {
            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.donation_paypal_open))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    if (DonationChannelsHelper.copyLinkToClipboard(context, "PayPal Support Link", paypalUrl)) {
                        Toast.makeText(context, linkCopiedMsg, Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("btn_copy_paypal_link")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.donation_paypal_copy), fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = {
                    DonationChannelsHelper.shareDonationLink(
                        context = context,
                        url = paypalUrl,
                        chooserTitle = shareChooserTitle,
                        shareBody = shareBody
                    )
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("btn_share_paypal_link")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.donation_paypal_share), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun KofiDonationTabContent(context: Context) {
    val kofiUrl = remember { DonationChannelsHelper.buildKofiUrl() }
    val browserFallbackMsg = stringResource(R.string.donation_browser_fallback_toast)
    val linkCopiedMsg = stringResource(R.string.donation_link_copied_toast)

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalCafe,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.donation_kofi_card_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = stringResource(R.string.donation_kofi_card_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = kofiUrl,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Button(
            onClick = {
                val result = DonationChannelsHelper.openUrlOrCopyFallback(
                    context = context,
                    url = kofiUrl,
                    clipboardLabel = "Ko-fi Support Link"
                )
                if (result == ExternalDonationResult.CopiedToClipboardFallback) {
                    Toast.makeText(context, browserFallbackMsg, Toast.LENGTH_SHORT).show()
                }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("btn_open_kofi")
        ) {
            Icon(Icons.Default.LocalCafe, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.donation_kofi_open_btn))
        }

        OutlinedButton(
            onClick = {
                if (DonationChannelsHelper.copyLinkToClipboard(context, "Ko-fi Support Link", kofiUrl)) {
                    Toast.makeText(context, linkCopiedMsg, Toast.LENGTH_SHORT).show()
                }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("btn_copy_kofi_link")
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.donation_paypal_copy))
        }
    }
}

@Composable
fun PostOperationSupportDialog(
    milestoneCount: Int,
    onSupportClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.post_op_support_title, milestoneCount),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = stringResource(R.string.post_op_support_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onSupportClick,
                modifier = Modifier.testTag("btn_post_op_support_confirm")
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.post_op_support_cta))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_post_op_support_dismiss")
            ) {
                Text(stringResource(R.string.post_op_support_later))
            }
        }
    )
}
