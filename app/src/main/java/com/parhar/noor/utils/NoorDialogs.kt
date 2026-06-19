package com.parhar.noor.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import androidx.annotation.StringRes
import com.parhar.noor.R
import com.parhar.noor.databinding.DialogConfirmBinding
import com.parhar.noor.databinding.DialogPasswordBinding
import com.parhar.noor.databinding.DialogRemindBinding
import com.parhar.noor.databinding.DialogWeekResultBinding
import com.parhar.noor.domain.model.WeekResultSummary

object NoorDialogs {

    enum class ConfirmTone {
        DEFAULT,
        DESTRUCTIVE,
        LOGOUT,
    }

    fun showConfirm(
        context: Context,
        title: String,
        message: String,
        tone: ConfirmTone = ConfirmTone.DEFAULT,
        negativeText: String = context.getString(R.string.action_no),
        positiveText: String = context.getString(R.string.action_yes),
        onPositive: () -> Unit,
        onNegative: (() -> Unit)? = null,
    ): Dialog {
        val binding = DialogConfirmBinding.inflate(LayoutInflater.from(context))
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        binding.titleTextView.text = title
        binding.messageTextView.text = message
        binding.negativeButton.text = negativeText
        binding.positiveButton.text = positiveText
        binding.iconTextView.text = when (tone) {
            ConfirmTone.DEFAULT -> "✦"
            ConfirmTone.DESTRUCTIVE -> "🗑"
            ConfirmTone.LOGOUT -> "👋"
        }
        if (tone == ConfirmTone.DESTRUCTIVE) {
            binding.positiveButton.setBackgroundResource(R.drawable.bg_dialog_destructive_button)
            binding.positiveButton.setTextColor(context.getColor(R.color.navy_base))
        }

        binding.negativeButton.setOnClickListener {
            dialog.dismiss()
            onNegative?.invoke()
        }
        binding.positiveButton.setOnClickListener {
            dialog.dismiss()
            onPositive()
        }

        styleDialogWindow(dialog)
        dialog.show()
        return dialog
    }

    fun showConfirm(
        context: Context,
        @StringRes titleRes: Int,
        @StringRes messageRes: Int,
        tone: ConfirmTone = ConfirmTone.DEFAULT,
        @StringRes negativeTextRes: Int = R.string.action_no,
        @StringRes positiveTextRes: Int = R.string.action_yes,
        onPositive: () -> Unit,
        onNegative: (() -> Unit)? = null,
    ): Dialog {
        return showConfirm(
            context = context,
            title = context.getString(titleRes),
            message = context.getString(messageRes),
            tone = tone,
            negativeText = context.getString(negativeTextRes),
            positiveText = context.getString(positiveTextRes),
            onPositive = onPositive,
            onNegative = onNegative,
        )
    }

    fun showPassword(
        context: Context,
        title: String,
        message: String,
        hint: String = context.getString(R.string.admin_password_hint),
        negativeText: String = context.getString(R.string.action_cancel),
        positiveText: String = context.getString(R.string.admin_password_confirm),
        onPositive: (password: String) -> Unit,
        onNegative: (() -> Unit)? = null,
    ): Dialog {
        val binding = DialogPasswordBinding.inflate(LayoutInflater.from(context))
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        binding.titleTextView.text = title
        binding.messageTextView.text = message
        binding.passwordEditText.hint = hint
        binding.negativeButton.text = negativeText
        binding.positiveButton.text = positiveText

        fun submitPassword() {
            onPositive(binding.passwordEditText.text.toString())
            dialog.dismiss()
        }

        binding.negativeButton.setOnClickListener {
            dialog.dismiss()
            onNegative?.invoke()
        }
        binding.positiveButton.setOnClickListener { submitPassword() }
        binding.passwordEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitPassword()
                true
            } else {
                false
            }
        }

        styleDialogWindow(dialog)
        dialog.show()
        binding.passwordEditText.requestFocus()
        return dialog
    }

    fun showPassword(
        context: Context,
        @StringRes titleRes: Int,
        @StringRes messageRes: Int,
        onPositive: (password: String) -> Unit,
        onNegative: (() -> Unit)? = null,
    ): Dialog {
        return showPassword(
            context = context,
            title = context.getString(titleRes),
            message = context.getString(messageRes),
            onPositive = onPositive,
            onNegative = onNegative,
        )
    }

    fun showWeekResult(
        context: Context,
        result: WeekResultSummary,
        onViewLeaderboard: () -> Unit,
        onDismiss: () -> Unit,
    ): Dialog {
        val binding = DialogWeekResultBinding.inflate(LayoutInflater.from(context))
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnDismissListener {
            onDismiss()
        }

        binding.weekTitleTextView.text = result.title
        binding.weekPointsTextView.text = context.getString(
            R.string.week_result_points_format,
            result.points,
        )
        binding.weekPositionTextView.text = context.getString(
            R.string.week_result_position_format,
            result.position,
        )

        if (result.medalEmoji != null) {
            binding.weekMedalTextView.text = result.medalEmoji
            binding.weekMedalTextView.visibility = android.view.View.VISIBLE
            binding.weekCongratsTextView.text = result.congratsMessage
            binding.weekCongratsTextView.visibility = android.view.View.VISIBLE
        } else {
            binding.weekMedalTextView.visibility = android.view.View.GONE
            binding.weekCongratsTextView.visibility = android.view.View.GONE
        }

        binding.viewLeaderboardTextView.setOnClickListener {
            onViewLeaderboard()
            dialog.dismiss()
        }
        binding.okayTextView.setOnClickListener {
            dialog.dismiss()
        }

        styleDialogWindow(dialog)
        dialog.show()
        return dialog
    }

    fun showRemind(
        context: Context,
        @StringRes titleRes: Int,
        @StringRes messageRes: Int,
        defaultMessage: String,
        onSend: (message: String) -> Unit,
        onCancel: (() -> Unit)? = null,
    ): Dialog {
        val binding = DialogRemindBinding.inflate(LayoutInflater.from(context))
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(binding.root)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        binding.titleTextView.setText(titleRes)
        binding.messageTextView.setText(messageRes)
        binding.reminderMessageEditText.setText(defaultMessage)
        binding.reminderMessageEditText.setSelection(defaultMessage.length)

        val maxLength = context.resources.getInteger(R.integer.reminder_message_max_length)
        fun updateCharCount() {
            val length = binding.reminderMessageEditText.text?.length ?: 0
            binding.charCountTextView.text =
                context.getString(R.string.friends_remind_char_count, length, maxLength)
        }
        updateCharCount()
        binding.reminderMessageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = updateCharCount()
        })

        fun submitMessage() {
            val message = binding.reminderMessageEditText.text.toString().trim()
            if (message.isBlank()) return
            onSend(message)
            dialog.dismiss()
        }

        binding.negativeButton.setOnClickListener {
            dialog.dismiss()
            onCancel?.invoke()
        }
        binding.positiveButton.setOnClickListener { submitMessage() }
        binding.reminderMessageEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitMessage()
                true
            } else {
                false
            }
        }

        styleDialogWindow(dialog)
        dialog.show()
        binding.reminderMessageEditText.requestFocus()
        return dialog
    }

    private fun styleDialogWindow(dialog: Dialog) {
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
            )
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.72f)
        }
    }
}
