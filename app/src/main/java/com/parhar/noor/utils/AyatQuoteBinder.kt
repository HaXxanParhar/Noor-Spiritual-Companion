package com.parhar.noor.utils

import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.parhar.noor.R
import com.parhar.noor.domain.model.Ayat

object AyatQuoteBinder {

    fun bind(
        arabicTextView: TextView,
        urduTextView: TextView,
        englishTextView: TextView,
        referenceTextView: TextView,
        ayat: Ayat,
    ) {
        arabicTextView.text = ayat.ayat
        urduTextView.isVisible = ayat.urdu.isNotBlank()
        urduTextView.text = ayat.urdu
        ResourcesCompat.getFont(urduTextView.context, R.font.noto_nastaliq_regular)?.let {
            urduTextView.typeface = it
        }
        englishTextView.isVisible = ayat.english.isNotBlank()
        englishTextView.text = ayat.english
        referenceTextView.isVisible = ayat.reference.isNotBlank()
        referenceTextView.text = ayat.reference
    }
}
