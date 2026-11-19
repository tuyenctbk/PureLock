package com.example.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.util.Locale

sealed class ExternalDonationResult {
    data object OpenedInApp : ExternalDonationResult()
    data object CopiedToClipboardFallback : ExternalDonationResult()
    data object Failed : ExternalDonationResult()
}

/**
 * Provides PayPal deep links, live ZXing QR code bitmap generation, Ko-fi links,
 * and safe Activity/Clipboard fallback intent launchers for external donation channels.
 */
object DonationChannelsHelper {

    const val PAYPAL_USERNAME = "purelockdev"
    const val KOFI_USERNAME = "purelockdev"

    val PAYPAL_PRESET_AMOUNTS: List<Int?> = listOf(3, 5, 10, null)

    fun buildPayPalUrl(amountUsd: Double?): String {
        val base = "https://paypal.me/$PAYPAL_USERNAME"
        if (amountUsd == null || amountUsd <= 0.0) {
            return base
        }
        val formatted = if (amountUsd % 1.0 == 0.0) {
            amountUsd.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", amountUsd)
        }
        return "$base/${formatted}USD"
    }

    fun buildKofiUrl(): String = "https://ko-fi.com/$KOFI_USERNAME"

    fun generateQrCodeBitmap(content: String, sizePx: Int = 512): Bitmap? {
        if (content.isBlank() || sizePx <= 0) return null
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val bitMatrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                }
            }
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun copyLinkToClipboard(context: Context, label: String, url: String): Boolean {
        return try {
            val clipboard = context.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return false
            val clip = ClipData.newPlainText(label, url)
            clipboard.setPrimaryClip(clip)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openUrlOrCopyFallback(
        context: Context,
        url: String,
        clipboardLabel: String
    ): ExternalDonationResult {
        val hostActivity = context.unwrapActivity()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            if (hostActivity == null) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        return try {
            (hostActivity ?: context).startActivity(intent)
            ExternalDonationResult.OpenedInApp
        } catch (_: Exception) {
            if (copyLinkToClipboard(context, clipboardLabel, url)) {
                ExternalDonationResult.CopiedToClipboardFallback
            } else {
                ExternalDonationResult.Failed
            }
        }
    }

    fun shareDonationLink(context: Context, url: String, chooserTitle: String, shareBody: String): Boolean {
        val hostActivity = context.unwrapActivity()
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "$shareBody\n$url")
        }
        return try {
            val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
                if (hostActivity == null) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            (hostActivity ?: context).startActivity(chooser)
            true
        } catch (_: Exception) {
            copyLinkToClipboard(context, chooserTitle, url)
        }
    }

    private tailrec fun Context.unwrapActivity(): Activity? = when (this) {
        is Activity -> if (!isFinishing && !isDestroyed) this else null
        is ContextWrapper -> baseContext.unwrapActivity()
        else -> null
    }
}
