package com.parhar.noor.utils

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.parhar.noor.R

object AvatarStyleResolver {
    const val STYLE_AMIRI_CLASSIC = "amiri_classic"
    const val STYLE_NASKH_MODERN = "naskh_modern"
    const val STYLE_LATIN_BOLD = "latin_bold"
    const val STYLE_NOTO_NASTALIQ = "noto_nastaliq"
    const val STYLE_GULZAR = "gulzar"
    const val STYLE_OUTFIT_MEDIUM = "outfit_medium"

    val allStyles: List<String> = listOf(
        STYLE_AMIRI_CLASSIC,
        STYLE_NASKH_MODERN,
        STYLE_LATIN_BOLD,
        STYLE_NOTO_NASTALIQ,
        STYLE_GULZAR,
        STYLE_OUTFIT_MEDIUM,
    )

    fun resolveTypeface(context: Context, style: String?): Typeface? {
        return when (style) {
            STYLE_NASKH_MODERN, STYLE_LATIN_BOLD -> ResourcesCompat.getFont(context, R.font.poppins)?.let { base ->
                Typeface.create(base, Typeface.BOLD)
            }
            STYLE_NOTO_NASTALIQ -> ResourcesCompat.getFont(context, R.font.noto_nastaliq_regular)
            STYLE_GULZAR -> ResourcesCompat.getFont(context, R.font.gulzar_regular)
            STYLE_OUTFIT_MEDIUM -> ResourcesCompat.getFont(context, R.font.outfit_medium)
            STYLE_AMIRI_CLASSIC, null, "" -> ResourcesCompat.getFont(context, R.font.amiri)
            else -> ResourcesCompat.getFont(context, R.font.amiri)
        }
    }

    fun displayName(style: String): String {
        return when (style) {
            STYLE_NASKH_MODERN -> "Naskh Modern"
            STYLE_LATIN_BOLD -> "Latin Bold"
            STYLE_NOTO_NASTALIQ -> "Noto Nastaliq"
            STYLE_GULZAR -> "Gulzar"
            STYLE_OUTFIT_MEDIUM -> "Outfit Medium"
            else -> "Amiri Classic"
        }
    }
}
