package com.parhar.noor.utils

import android.icu.util.IslamicCalendar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object MemberSinceFormatter {

    private val ISLAMIC_MONTHS = listOf(
        "Muharram",
        "Safar",
        "Rabi al-Awwal",
        "Rabi al-Thani",
        "Jumada al-Awwal",
        "Jumada al-Thani",
        "Rajab",
        "Sha'ban",
        "Ramadan",
        "Shawwal",
        "Dhul-Qadah",
        "Dhul-Hijjah",
    )

    fun format(createdAtMillis: Long, earliestTaskDateKey: String?): String {
        val date = resolveDate(createdAtMillis, earliestTaskDateKey) ?: return ""
        val gregorian = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(date)
        val islamic = formatIslamic(date)
        return "Member since $gregorian ($islamic)"
    }

    private fun resolveDate(createdAtMillis: Long, earliestTaskDateKey: String?): Date? {
        if (createdAtMillis > 0L) {
            return Date(createdAtMillis)
        }
        if (!earliestTaskDateKey.isNullOrBlank()) {
            return runCatching {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(earliestTaskDateKey)
            }.getOrNull()
        }
        return null
    }

    private fun formatIslamic(date: Date): String {
        val calendar = IslamicCalendar().apply { time = date }
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = ISLAMIC_MONTHS[calendar.get(Calendar.MONTH)]
        val year = calendar.get(Calendar.YEAR)
        return "$day $month ${year}H"
    }
}
