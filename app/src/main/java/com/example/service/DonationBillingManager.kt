package com.example.service

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.lang.ref.WeakReference
import kotlin.coroutines.resume

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

internal interface BillingGateway {
    val isReady: Boolean
    fun startConnection(onSetupFinished: (Boolean) -> Unit, onDisconnected: () -> Unit)
    fun queryProductDetails(productIds: List<String>, onResult: (BillingResult, List<ProductDetails>) -> Unit)
    fun queryOwnedPurchases(onResult: (BillingResult, List<Purchase>) -> Unit)
    fun acknowledgePurchase(purchaseToken: String, onResult: (BillingResult) -> Unit)
    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails): BillingResult
    fun endConnection()
}

private class PlayBillingGateway(
    appContext: Context,
    purchasesUpdatedListener: PurchasesUpdatedListener
) : BillingGateway {

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    override val isReady: Boolean
        get() = billingClient.isReady

    override fun startConnection(onSetupFinished: (Boolean) -> Unit, onDisconnected: () -> Unit) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                onSetupFinished(billingResult.responseCode == BillingClient.BillingResponseCode.OK)
            }

            override fun onBillingServiceDisconnected() {
                onDisconnected()
            }
        })
    }

    override fun queryProductDetails(
        productIds: List<String>,
        onResult: (BillingResult, List<ProductDetails>) -> Unit
    ) {
        val products = productIds.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()
        billingClient.queryProductDetailsAsync(params, onResult)
    }

    override fun queryOwnedPurchases(onResult: (BillingResult, List<Purchase>) -> Unit) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params, onResult)
    }

    override fun acknowledgePurchase(purchaseToken: String, onResult: (BillingResult) -> Unit) {
        val ackParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        billingClient.acknowledgePurchase(ackParams, onResult)
    }

    override fun launchBillingFlow(activity: Activity, productDetails: ProductDetails): BillingResult {
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        return billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun endConnection() {
        billingClient.endConnection()
    }
}

/**
 * Lifecycle-aware, ViewModel-scoped Google Play Billing v7 manager.
 *
 * Implements [DefaultLifecycleObserver] to bind safely to an active [ComponentActivity] or
 * [Fragment] session via a [WeakReference], automatically connecting on [onStart],
 * restoring/synchronizing owned purchases on [onResume], and releasing stale Activity
 * references on [onDestroy] without leaking across configuration changes.
 */
class DonationBillingManager internal constructor(
    context: Context,
    private val coroutineScope: CoroutineScope,
    private val ownsScope: Boolean,
    private val ioDispatcher: CoroutineDispatcher,
    private val mainDispatcher: CoroutineDispatcher,
    gatewayFactory: ((Context, PurchasesUpdatedListener) -> BillingGateway)?,
    private val onSupporterVerified: (tierId: String) -> Unit
) : DefaultLifecycleObserver {

    constructor(
        context: Context,
        coroutineScope: CoroutineScope,
        onSupporterVerified: (tierId: String) -> Unit
    ) : this(
        context = context,
        coroutineScope = coroutineScope,
        ownsScope = false,
        ioDispatcher = Dispatchers.IO,
        mainDispatcher = Dispatchers.Main.immediate,
        gatewayFactory = null,
        onSupporterVerified = onSupporterVerified
    )

    constructor(
        context: Context,
        onSupporterVerified: (tierId: String) -> Unit
    ) : this(
        context = context,
        coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        ownsScope = true,
        ioDispatcher = Dispatchers.IO,
        mainDispatcher = Dispatchers.Main.immediate,
        gatewayFactory = null,
        onSupporterVerified = onSupporterVerified
    )

    companion object {
        private const val TAG = "DonationBillingManager"
        private const val IPC_TIMEOUT_MS = 15_000L
    }

    private val appContext: Context = context.applicationContext
    private val connectMutex = Mutex()
    private val sessionLock = Any()

    private val _uiState = MutableStateFlow(DonationBillingUiState())
    val uiState: StateFlow<DonationBillingUiState> = _uiState.asStateFlow()

    @Volatile
    private var productDetailsMap: Map<String, ProductDetails> = emptyMap()

    @Volatile
    private var isReleased = false

    private var boundActivityRef: WeakReference<Activity>? = null
    private var boundLifecycleRef: WeakReference<Lifecycle>? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        handlePurchasesUpdate(billingResult, purchases)
    }

    private val gateway: BillingGateway = gatewayFactory?.invoke(appContext, purchasesUpdatedListener)
        ?: PlayBillingGateway(appContext, purchasesUpdatedListener)

    /**
     * Binds this manager as a [DefaultLifecycleObserver] to a host [ComponentActivity],
     * guaranteeing a valid Activity session for Google Play Billing sheets.
     */
    fun bindActivitySession(activity: ComponentActivity) {
        bindLifecycleHost(activity = activity, lifecycleOwner = activity)
    }

    /**
     * Binds this manager as a [DefaultLifecycleObserver] to a host [Fragment] and its
     * enclosing Activity session.
     */
    fun bindFragmentSession(fragment: Fragment) {
        val owner = runCatching { fragment.viewLifecycleOwner }.getOrElse { fragment }
        bindLifecycleHost(activity = fragment.activity, lifecycleOwner = owner)
    }

    private fun bindLifecycleHost(activity: Activity?, lifecycleOwner: LifecycleOwner) {
        synchronized(sessionLock) {
            if (isReleased) return
            val existingLifecycle = boundLifecycleRef?.get()
            if (existingLifecycle !== lifecycleOwner.lifecycle) {
                existingLifecycle?.removeObserver(this)
                boundLifecycleRef = WeakReference(lifecycleOwner.lifecycle)
                lifecycleOwner.lifecycle.addObserver(this)
            }
            if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                boundActivityRef = WeakReference(activity)
            }
        }
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            startConnectionAndLoad()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        updateBoundActivityFromOwner(owner)
        startConnectionAndLoad()
    }

    override fun onResume(owner: LifecycleOwner) {
        updateBoundActivityFromOwner(owner)
        refreshPurchases()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        val destroyedActivity = extractActivityFromOwner(owner)
        synchronized(sessionLock) {
            val currentActivity = boundActivityRef?.get()
            if (currentActivity == null || (destroyedActivity != null && currentActivity === destroyedActivity)) {
                boundActivityRef = null
            }
            if (boundLifecycleRef?.get() === owner.lifecycle) {
                boundLifecycleRef = null
            }
        }
        owner.lifecycle.removeObserver(this)
    }

    private fun updateBoundActivityFromOwner(owner: LifecycleOwner) {
        val activity = extractActivityFromOwner(owner) ?: return
        if (!activity.isFinishing && !activity.isDestroyed) {
            synchronized(sessionLock) {
                boundActivityRef = WeakReference(activity)
            }
        }
    }

    private fun extractActivityFromOwner(owner: LifecycleOwner): Activity? = when (owner) {
        is Activity -> owner
        is Fragment -> owner.activity
        is Context -> owner.findActivity()
        else -> null
    }

    internal fun hasBoundActiveSession(): Boolean = resolveLiveActivity(null) != null

    private fun resolveLiveActivity(explicitContext: Context?): Activity? {
        val fromExplicit = explicitContext?.findActivity()?.takeIf { it.isActiveBillingSession() }
        if (fromExplicit != null) return fromExplicit
        val fromLifecycle = synchronized(sessionLock) { boundActivityRef?.get() }
        return fromLifecycle?.takeIf { it.isActiveBillingSession() }
    }

    private fun Activity.isActiveBillingSession(): Boolean {
        if (isFinishing || isDestroyed) return false
        if (this is LifecycleOwner && !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            return false
        }
        return true
    }

    internal fun handlePurchasesUpdate(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases.isNullOrEmpty()) {
                    _uiState.update { it.copy(isPurchaseInProgress = false) }
                } else {
                    coroutineScope.launch(ioDispatcher) {
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

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null, isError = false) }
    }

    fun startConnectionAndLoad() {
        if (isReleased) return
        coroutineScope.launch(ioDispatcher) {
            if (ensureConnected()) {
                queryCatalogInternal()
                queryPurchasesInternal()
            }
        }
    }

    fun refreshPurchases() {
        if (isReleased) return
        coroutineScope.launch(ioDispatcher) {
            if (ensureConnected()) {
                if (productDetailsMap.isEmpty()) {
                    queryCatalogInternal()
                }
                queryPurchasesInternal()
            }
        }
    }

    /**
     * Launches the Google Play Billing flow bound to the lifecycle-tracked Activity session.
     */
    fun launchDonationFlow(tier: DonationTier) {
        launchDonationFlowInternal(explicitContext = null, tier = tier)
    }

    /**
     * Launches the Google Play Billing flow resolving a live Activity from either [context]
     * or the lifecycle-bound Activity session.
     */
    fun launchDonationFlow(context: Context, tier: DonationTier) {
        launchDonationFlowInternal(explicitContext = context, tier = tier)
    }

    private fun launchDonationFlowInternal(explicitContext: Context?, tier: DonationTier) {
        val initialHostActivity = resolveLiveActivity(explicitContext)
        if (initialHostActivity == null) {
            Log.w(TAG, "launchDonationFlow aborted: no active Activity/Fragment session available")
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

        coroutineScope.launch(ioDispatcher) {
            if (!ensureConnected()) {
                _uiState.update {
                    it.copy(
                        isPurchaseInProgress = false,
                        statusMessage = appContext.getString(R.string.donation_play_unavailable_fallback),
                        isError = true
                    )
                }
                return@launch
            }

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

            withContext(mainDispatcher) {
                val activeHostActivity = resolveLiveActivity(explicitContext)
                    ?: initialHostActivity.takeIf { it.isActiveBillingSession() }

                if (activeHostActivity == null) {
                    _uiState.update {
                        it.copy(
                            isPurchaseInProgress = false,
                            statusMessage = appContext.getString(R.string.donation_play_no_activity_error),
                            isError = true
                        )
                    }
                    return@withContext
                }

                val launchResult = try {
                    gateway.launchBillingFlow(activeHostActivity, details)
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

    private suspend fun ensureConnected(): Boolean = connectMutex.withLock {
        if (isReleased) return@withLock false
        if (gateway.isReady) {
            _uiState.update { it.copy(isConnecting = false, isBillingReady = true) }
            return@withLock true
        }

        _uiState.update { it.copy(isConnecting = true) }

        val connected = withTimeoutOrNull(IPC_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                try {
                    gateway.startConnection(
                        onSetupFinished = { ok ->
                            if (cont.isActive) {
                                cont.resume(ok)
                            }
                        },
                        onDisconnected = {
                            _uiState.update { it.copy(isConnecting = false, isBillingReady = false) }
                            if (cont.isActive) {
                                cont.resume(false)
                            }
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "startConnection failed", e)
                    if (cont.isActive) {
                        cont.resume(false)
                    }
                }
            }
        } ?: false

        _uiState.update { it.copy(isConnecting = false, isBillingReady = connected) }
        connected
    }

    private suspend fun queryCatalogInternal(): Map<String, ProductDetails> {
        if (!gateway.isReady || isReleased) return emptyMap()
        val productIds = DonationTier.entries.map { it.productId }

        return withTimeoutOrNull(IPC_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                try {
                    gateway.queryProductDetails(productIds) { billingResult, productDetailsList ->
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            val map = productDetailsList.associateBy { it.productId }
                            productDetailsMap = map
                            val prices = map.mapValues { (_, details) ->
                                details.oneTimePurchaseOfferDetails?.formattedPrice ?: ""
                            }.filterValues { it.isNotBlank() }
                            _uiState.update { it.copy(localizedPrices = prices) }
                            if (cont.isActive) cont.resume(map)
                        } else {
                            if (cont.isActive) cont.resume(emptyMap())
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "queryProductDetails failed", e)
                    if (cont.isActive) cont.resume(emptyMap())
                }
            }
        } ?: emptyMap()
    }

    private suspend fun queryPurchasesInternal() {
        if (!gateway.isReady || isReleased) return
        val purchases = withTimeoutOrNull(IPC_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                try {
                    gateway.queryOwnedPurchases { billingResult, purchasesList ->
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            if (cont.isActive) cont.resume(purchasesList)
                        } else {
                            if (cont.isActive) cont.resume(emptyList())
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "queryOwnedPurchases failed", e)
                    if (cont.isActive) cont.resume(emptyList())
                }
            }
        } ?: emptyList()

        purchases.forEach { purchase ->
            processPurchase(purchase, isRestore = true)
        }
    }

    private suspend fun processPurchase(purchase: Purchase, isRestore: Boolean = false) {
        val validProducts = DonationTier.entries.map { it.productId }.toSet()
        val matchingProductId = purchase.products.firstOrNull { it in validProducts } ?: return

        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                if (!purchase.isAcknowledged) {
                    val ackResult = withTimeoutOrNull(IPC_TIMEOUT_MS) {
                        suspendCancellableCoroutine { cont ->
                            try {
                                gateway.acknowledgePurchase(purchase.purchaseToken) { result ->
                                    if (cont.isActive) cont.resume(result)
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "acknowledgePurchase failed", e)
                                if (cont.isActive) {
                                    cont.resume(
                                        BillingResult.newBuilder()
                                            .setResponseCode(BillingClient.BillingResponseCode.ERROR)
                                            .setDebugMessage(e.message ?: "ack error")
                                            .build()
                                    )
                                }
                            }
                        }
                    }
                    if (ackResult?.responseCode == BillingClient.BillingResponseCode.OK) {
                        onSupporterVerified(matchingProductId)
                        _uiState.update {
                            it.copy(
                                isPurchaseInProgress = false,
                                statusMessage = appContext.getString(R.string.donation_play_success_thank_you),
                                isError = false
                            )
                        }
                    } else if (!isRestore) {
                        val detail = ackResult?.debugMessage?.ifBlank { "ack=${ackResult.responseCode}" }
                            ?: "timeout"
                        _uiState.update {
                            it.copy(
                                isPurchaseInProgress = false,
                                statusMessage = appContext.getString(R.string.donation_play_error_format, detail),
                                isError = true
                            )
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

    /**
     * Cleans up lifecycle observer registrations and terminates the [BillingClient] connection.
     */
    fun release() {
        synchronized(sessionLock) {
            isReleased = true
            boundLifecycleRef?.get()?.removeObserver(this)
            boundLifecycleRef = null
            boundActivityRef = null
        }
        try {
            gateway.endConnection()
        } catch (_: Exception) {
        }
        if (ownsScope) {
            coroutineScope.cancel()
        }
    }

    fun destroy() = release()
}
