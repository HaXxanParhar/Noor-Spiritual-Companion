package com.parhar.noor.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.BreakIterator

object EmojiInputUtils {

    fun firstEmoji(text: String): String {
        if (text.isEmpty()) return ""
        val iterator = BreakIterator.getCharacterInstance()
        iterator.setText(text)
        val start = iterator.first()
        val end = iterator.next()
        return if (end == BreakIterator.DONE) {
            ""
        } else {
            text.substring(start, end)
        }
    }

    fun attachSingleEmojiLimiter(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            private var selfChange = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                if (selfChange || s == null) return
                val single = firstEmoji(s.toString())
                if (s.toString() != single) {
                    selfChange = true
                    editText.setText(single)
                    editText.setSelection(single.length)
                    selfChange = false
                }
            }
        })
    }
}
