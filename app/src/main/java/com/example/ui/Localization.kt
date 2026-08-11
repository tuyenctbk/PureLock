package com.example.ui

import android.content.Context

object L10n {
    var currentLanguageCode: String = "SYSTEM"

    val supportedLanguages = mapOf(
        "SYSTEM" to "System Default",
        "en" to "English",
        "es" to "Español (Spanish)",
        "fr" to "Français (French)",
        "de" to "Deutsch (German)",
        "it" to "Italiano (Italian)",
        "pt" to "Português (Portuguese)",
        "ru" to "Русский (Russian)",
        "zh" to "简体中文 (Simplified Chinese)",
        "zh_tw" to "繁體中文 (Traditional Chinese)",
        "ja" to "日本語 (Japanese)",
        "ko" to "한국어 (Korean)",
        "vi" to "Tiếng Việt (Vietnamese)",
        "ar" to "العربية (Arabic)",
        "hi" to "हिन्दी (Hindi)",
        "bn" to "বাংলা (Bengali)",
        "pa" to "ਪੰਜਾਬੀ (Punjabi)",
        "tr" to "Türkçe (Turkish)",
        "ur" to "اردو (Urdu)",
        "pl" to "Polski (Polish)",
        "uk" to "Українська (Ukrainian)",
        "nl" to "Nederlands (Dutch)",
        "el" to "Ελληνικά (Greek)",
        "hu" to "Magyar (Hungarian)",
        "sv" to "Svenska (Swedish)",
        "cs" to "Čeština (Czech)",
        "he" to "עברית (Hebrew)",
        "id" to "Bahasa Indonesia (Indonesian)",
        "ms" to "Bahasa Melayu (Malay)",
        "th" to "ไทย (Thai)",
        "fa" to "فارسی (Persian)",
        "tl" to "Tagalog (Filipino)",
        "da" to "Dansk (Danish)",
        "fi" to "Suomi (Finnish)",
        "no" to "Norsk (Norwegian)",
        "sk" to "Slovenčina (Slovak)",
        "hr" to "Hrvatski (Croatian)",
        "bg" to "Български (Bulgarian)",
        "lt" to "Lietuvių (Lithuanian)",
        "lv" to "Latviešu (Latvian)",
        "et" to "Eesti (Estonian)",
        "sl" to "Slovenščina (Slovenian)",
        "sr" to "Српски (Serbian)",
        "ca" to "Català (Catalan)",
        "gl" to "Galego (Galician)",
        "eu" to "Euskara (Basque)",
        "gu" to "ગુજરાતી (Gujarati)",
        "kn" to "ಕನ್ನಡ (Kannada)",
        "ml" to "മലയാളം (Malayalam)",
        "ta" to "தமிழ் (Tamil)",
        "te" to "తెలుగు (Telugu)",
        "mr" to "मराठी (Marathi)",
        "az" to "Azərbaycanca (Azerbaijani)",
        "ka" to "ქართული (Georgian)",
        "hy" to "Հայերեն (Armenian)",
        "kk" to "Қазақ тілі (Kazakh)",
        "uz" to "Oʻzbekcha (Uzbek)",
        "sw" to "Kiswahili (Swahili)",
        "af" to "Afrikaans",
        "is" to "Íslenska (Icelandic)",
        "ro" to "Română (Romanian)"
    )

    fun getString(context: Context, key: String): String {
        val resId = context.resources.getIdentifier(key, "string", context.packageName)
        return if (resId != 0) {
            context.getString(resId)
        } else {
            key
        }
    }
}
