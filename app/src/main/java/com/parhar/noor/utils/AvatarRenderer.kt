package com.parhar.noor.utils

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.parhar.noor.R
import com.parhar.noor.domain.model.UserAvatar

object AvatarRenderer {

    const val MAX_AVATAR_TEXT_LENGTH = 7

    fun apply(
        textView: TextView,
        name: String,
        avatar: UserAvatar?,
        sizeDp: Int? = null,
    ) {
        val typeface = AvatarStyleResolver.resolveTypeface(textView.context, avatar?.style)
        if (typeface != null) {
            textView.typeface = typeface
        }

        val bgColor = parseColorOrNull(avatar?.bg)
        val borderColor = parseColorOrNull(avatar?.border)

        if (bgColor != null || borderColor != null) {
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(bgColor ?: ContextCompat.getColor(textView.context, R.color.card_surface_soft))
                if (borderColor != null) {
                    setStroke(dpToPx(textView, 2), borderColor)
                }
            }
            textView.background = drawable
        } else {
            textView.setBackgroundResource(R.drawable.bg_leaderboard_avatar_default)
        }

        sizeDp?.let { dp ->
            val px = dpToPx(textView, dp)
            textView.layoutParams = textView.layoutParams?.apply {
                width = px
                height = px
            }
        }

        val displayText = avatar?.text?.takeIf { it.isNotBlank() }
            ?: TaskStatsCalculator.toInitials(name)
        applyTextDisplay(textView, displayText)
    }

    fun applyTextDisplay(textView: TextView, text: String) {
        textView.text = text
        textView.maxLines = 1
        configureAutoSize(textView)
        val horizontalPadding = dpToPx(textView, 4)
        textView.setPadding(horizontalPadding, 0, horizontalPadding, 0)
    }

    private fun configureAutoSize(textView: TextView) {
        val density = textView.resources.displayMetrics.scaledDensity
        val maxSizeSp = (textView.textSize / density).toInt().coerceAtLeast(10)
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
            textView,
            8,
            maxSizeSp,
            1,
            TypedValue.COMPLEX_UNIT_SP,
        )
    }

    private fun parseColorOrNull(hex: String?): Int? {
        if (hex.isNullOrBlank()) return null
        return runCatching { Color.parseColor(hex) }.getOrNull()
    }

    private fun dpToPx(view: TextView, dp: Int): Int {
        return (dp * view.resources.displayMetrics.density).toInt()
    }
}
