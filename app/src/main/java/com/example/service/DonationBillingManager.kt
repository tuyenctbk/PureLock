package com.example.service

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections

/**
 * Walks nested ContextWrappers (e.g., Compose ContextThemeWrapper inside Dialogs/Sheets)
 * to recover a live host Activity required by Google Play Billing's launchBillingFlow().
 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> if (!isFinishing && !isDestroyed) this else null
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

enum class DonationTier(
    val productId: String,
    val defaultPriceLabel: String,
    val titleRes: Int,
    val subtitleRes: Int
) {
    TIER_1(
        productId = "purelock_support_tier_1",
        defaultPriceLabel = "$1.99",
        titleRes = R.string.donation_tier_1_title,
        subtitleRes = R.string.donation_tier_1_desc
    ),
    TIER_2(
        productId = "purelock_support_tier_2",
        defaultPriceLabel = "$4.99",
        titleRes = R.string.donation_tier_2_title,
        subtitleRes = R.string.donation_tier_2_desc
    ),
    TIER_3(
        productId = "purelock_support_tier_3",
        defaultPriceLabel = "$9.99",
        titleRes = R.string.donation_tier_3_title,
        subtitleRes = R.string.donation_tier_3_desc
    ),
    TIER_4(
        productId = "purelock_support_tier_4",
        defaultPriceLabel = "$19.99",
        titleRes = R.string.donation_tier_4_title,
        subtitleRes = R.string.donation_tier_4_desc
    )
}

data class DonationBillingUiState(
    val isConnecting: Boolean = false,
    val isBillingReady: Boolean = false,
    val isPurchaseInProgress: Boolean = false,
    val localizedPrices: Map<String, String> = emptyMap(),
    val statusMessage: String? = null,
    val isError: Boolean = false
)

/**
 * Manages Google Play Billing v7 one-time voluntary donations, receipt verification,
 * acknowledgement, purchase restoration, and Activity session unwrapping.
 */
class DonationBillingManager(
    context: Context,
    private val onSupporterVerified: (tierId: String) -> Unit
) {
    companion object {
        private const val TAG = "DonationBillingManager"
    }

    private val appContext: Context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _uiState = MutableStateFlow(DonationBillingUiState())
    val uiState: StateFlow<DonationBillingUiState> = _uiState.asStateFlow()

    @Volatile
    private var productDetailsMap: Map<String, ProductDetails> = emptyMap()

    private val pendingConnectCallbacks: MutableList<(Boolean) -> Unit> =
        Collections.synchronizedList(mutableListOf())

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases.isNullOrEmpty()) {
                    _uiState.update { it.copy(isPurchaseInProgress = false) }
                } else {
                    scope.launch {
                        purchases.forEach { purchase ->
                            processPurchase(purchase)
                        }
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _uiState.update {
                    it.copy(
                        isPurchaseInProgress = false,
                        statusMessage = appContext.getString(R.string.donation_play_cancelled),
                        isError = false
                    )
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                _uiState.update {
                    it.copy(
                        isPurchaseInProgress = false,
                        statusMessage = appContext.getString(R.string.donation_play_already_owned),
                        isError = false
                    )
                }
                refreshPurchases()
            }
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            BillingClient.BillingResponseCode.NETWORK_ERROR -> {
                _uiState.update {
                    it.copy(
                        isPurchaseInProgress = false,
                        statusMessage = appContext.getString(R.string.donation_play_unavailable_fallback),
                        isError = true
                    )
                }
            }
            else -> {
                val detail = billingResult.debugMessage.ifBlank { "code=${billingResult.responseCode}" }
                _uiState.update {
                    it.copy(
                        isPurchaseInProgress = false,
                        statusMessage = appContext.getString(R.string.donation_play_error_format, detail),
                        isError = true
                    )
                }
            }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null, isError = false) }
    }

    fun startConnectionAndLoad() {
        ensureConnected { connected ->
            if (connected) {
                scope.launch {
                    queryCatalogInternal()
                    queryPurchasesInternal()
                }
            }
        }
    }

    fun refreshPurchases() {
        ensureConnected { connected ->
            if (connected) {
                scope.launch {
                    queryPurchasesInternal()
                }
            }
        }
    }

    fun launchDonationFlow(context: Context, tier: DonationTier) {
        val hostActivity = context.findActivity()
        if (hostActivity == null) {
            Log.w(TAG, "launchDonationFlow aborted: unable to unwrap live Activity from $context")
            _uiState.update {
                it.copy(
                    isPurchaseInProgress = false,
                    statusMessage = appContext.getString(R.string.donation_play_no_activity_error),
                    isError = true
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isPurchaseInProgress = true,
                statusMessage = null,
                isError = false
            )
        }

        ensureConnected { connected ->
            if (!connected) {
                _uiState.update {
                    it.copy(
                        isPurchaseInProgress = false,
                        statusMessage = appContext.getString(R.string.donation_play_unavailable_fallback),
                        isError = true
                    )
                }
                return@ensureConnected
            }

            scope.launch {
                val details = productDetailsMap[tier.productId]
                    ?: queryCatalogInternal()[tier.productId]

                if (details == null) {
                    _uiState.update {
                        it.copy(
                            isPurchaseInProgress = false,
                            statusMessage = appContext.getString(R.string.donation_play_sku_missing_fallback),
                            isError = true
                        )
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    if (hostActivity.isFinishing || hostActivity.isDestroyed) {
                        _uiState.update {
                            it.copy(
                                isPurchaseInProgress = false,
                                statusMessage = appContext.getString(R.string.donation_play_no_activity_error),
                                isError = true
                            )
                        }
                        return@withContext
                    }

                    val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()

                    val flowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(listOf(productParams))
                        .build()

                    val launchResult = try {
                        billingClient.launchBillingFlow(hostActivity, flowParams)
                    } catch (e: Exception) {
                        Log.e(TAG, "launchBillingFlow threw exception", e)
                        _uiState.update {
                            it.copy(
                                isPurchaseInProgress = false,
                                statusMessage = appContext.getString(R.string.donation_play_unavailable_fallback),
                                isError = true
                            )
                        }
                        return@withContext
                    }

                    if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                        val msg = if (launchResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                            refreshPurchases()
                            appContext.getString(R.string.donation_play_already_owned)
                        } else {
                            appContext.getString(
                                R.string.donation_play_error_format,
                                launchResult.debugMessage.ifBlank { "code=${launchResult.responseCode}" }
                            )
                        }
                        _uiState.update {
                            it.copy(
                                isPurchaseInProgress = false,
                                statusMessage = msg,
                                isError = launchResult.responseCode != BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED
                            )
                        }
                    }
                }
            }
        }
    }

    private fun ensureConnected(onReady: (Boolean) -> Unit) {
        if (billingClient.isReady) {
            _uiState.update { it.copy(isConnecting = false, isBillingReady = true) }
            onReady(true)
            return
        }

        val shouldStartConnection: Boolean
        synchronized(pendingConnectCallbacks) {
            pendingConnectCallbacks.add(onReady)
            shouldStartConnection = !_uiState.value.isConnecting
            if (shouldStartConnection) {
                _uiState.update { it.copy(isConnecting = true) }
            }
        }

        if (!shouldStartConnection) return

        try {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    val ok = billingResult.responseCode == BillingClient.BillingResponseCode.OK
                    _uiState.update { it.copy(isConnecting = false, isBillingReady = ok) }
                    drainConnectCallbacks(ok)
                }

                override fun onBillingServiceDisconnected() {
                    _uiState.update { it.copy(isConnecting = false, isBillingReady = false) }
                    drainConnectCallbacks(false)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "startConnection failed", e)
            _uiState.update { it.copy(isConnecting = false, isBillingReady = false) }
            drainConnectCallbacks(false)
        }
    }

    private fun drainConnectCallbacks(success: Boolean) {
        val callbacks = synchronized(pendingConnectCallbacks) {
            val copy = pendingConnectCallbacks.toList()
            pendingConnectCallbacks.clear()
            copy
        }
        callbacks.forEach { it(success) }
    }

    private suspend fun queryCatalogInternal(): Map<String, ProductDetails> {
        if (!billingClient.isReady) return emptyMap()
        val products = DonationTier.entries.map { tier ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(tier.productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()

        return kotlin.coroutines.suspendCoroutine { cont ->
            try {
                billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        val map = productDetailsList.associateBy { it.productId }
                        productDetailsMap = map
                        val prices = map.mapValues { (_, details) ->
                            details.oneTimePurchaseOfferDetails?.formattedPrice ?: ""
                        }.filterValues { it.isNotBlank() }
                        _uiState.update { it.copy(localizedPrices = prices) }
                        cont.resumeWith(Result.success(map))
                    } else {
                        cont.resumeWith(Result.success(emptyMap()))
                    }
                }
            } catch (_: Exception) {
                cont.resumeWith(Result.success(emptyMap()))
            }
        }
    }

    private suspend fun queryPurchasesInternal() {
        if (!billingClient.isReady) return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        try {
            billingClient.queryPurchasesAsync(params) { billingResult, purchasesList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch {
                        purchasesList.forEach { purchase ->
                            processPurchase(purchase, isRestore = true)
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private suspend fun processPurchase(purchase: Purchase, isRestore: Boolean = false) {
        val validProducts = DonationTier.entries.map { it.productId }.toSet()
        val matchingProductId = purchase.products.firstOrNull { it in validProducts } ?: return

        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                if (!purchase.isAcknowledged) {
                    val ackParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(ackParams) { ackResult ->
                        if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            onSupporterVerified(matchingProductId)
                            _uiState.update {
                                it.copy(
                                    isPurchaseInProgress = false,
                                    statusMessage = appContext.getString(R.string.donation_play_success_thank_you),
                                    isError = false
                                )
                            }
                        } else if (!isRestore) {
                            _uiState.update {
                                it.copy(
                                    isPurchaseInProgress = false,
                                    statusMessage = appContext.getString(
                                        R.string.donation_play_error_format,
                                        ackResult.debugMessage.ifBlank { "ack=${ackResult.responseCode}" }
                                    ),
                                    isError = true
                                )
                            }
                        }
                    }
                } else {
                    onSupporterVerified(matchingProductId)
                    if (!isRestore) {
                        _uiState.update {
                            it.copy(
                                isPurchaseInProgress = false,
                                statusMessage = appContext.getString(R.string.donation_play_success_thank_you),
                                isError = false
                            )
                        }
                    }
                }
            }
            Purchase.PurchaseState.PENDING -> {
                if (!isRestore) {
                    _uiState.update {
                        it.copy(
                            isPurchaseInProgress = false,
                            statusMessage = appContext.getString(R.string.donation_play_pending),
                            isError = false
                        )
                    }
                }
            }
            else -> {
                if (!isRestore) {
                    _uiState.update { it.copy(isPurchaseInProgress = false) }
                }
            }
        }
    }

    fun destroy() {
        try {
            billingClient.endConnection()
        } catch (_: Exception) {
        }
        scope.cancel()
    }
}
