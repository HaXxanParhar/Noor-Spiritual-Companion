package com.parhar.noor.ui.profile

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.parhar.noor.R
import com.parhar.noor.databinding.ActivityCreateAvatarBinding
import com.parhar.noor.di.appContainer
import com.parhar.noor.domain.model.UserAvatar
import com.parhar.noor.utils.AvatarRenderer
import com.parhar.noor.utils.AvatarStyleResolver
import com.parhar.noor.utils.BaseActivity
import kotlinx.coroutines.launch

class CreateAvatarActivity : BaseActivity<ActivityCreateAvatarBinding>() {

    private val viewModel: CreateAvatarViewModel by viewModels {
        appContainer().viewModelFactory
    }

    private var selectedBackgroundView: View? = null
    private var selectedBorderView: View? = null

    private lateinit var styleOptions: List<StyleOption>

    private data class StyleOption(
        val view: TextView,
        val styleKey: String,
    )

    override fun inflateBinding(): ActivityCreateAvatarBinding =
        ActivityCreateAvatarBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.toolbar.backImageView.setOnClickListener { finish() }
        binding.toolbar.toolbarTitleTextView.setText(R.string.create_avatar_title)
        binding.toolbar.toolbarActionTextView.visibility = View.VISIBLE
        binding.toolbar.toolbarActionTextView.setText(R.string.create_avatar_save)
        binding.toolbar.toolbarActionTextView.setOnClickListener { viewModel.save() }
        binding.saveAvatarTextView.setOnClickListener { viewModel.save() }

        styleOptions = listOf(
            StyleOption(binding.styleAmiriTextView, AvatarStyleResolver.STYLE_AMIRI_CLASSIC),
            StyleOption(binding.styleNaskhTextView, AvatarStyleResolver.STYLE_NASKH_MODERN),
            StyleOption(binding.styleLatinTextView, AvatarStyleResolver.STYLE_LATIN_BOLD),
            StyleOption(binding.styleNotoNastaliqTextView, AvatarStyleResolver.STYLE_NOTO_NASTALIQ),
            StyleOption(binding.styleGulzarTextView, AvatarStyleResolver.STYLE_GULZAR),
            StyleOption(binding.styleOutfitMediumTextView, AvatarStyleResolver.STYLE_OUTFIT_MEDIUM),
        )
        styleOptions.forEach { option ->
            option.view.setOnClickListener { viewModel.selectStyle(option.styleKey) }
            AvatarStyleResolver.resolveTypeface(this, option.styleKey)?.let { typeface ->
                option.view.typeface = typeface
            }
        }

        val fallbackName = intent.getStringExtra(EXTRA_FALLBACK_NAME).orEmpty()
        val initialAvatar = intent.getSerializableExtra(EXTRA_INITIAL_AVATAR) as? UserAvatar
        viewModel.load(initialAvatar, fallbackName)

        buildPalettes()
        observeViewModel()
    }

    private fun buildPalettes() {
        viewModel.backgroundColors.forEach { color ->
            val swatch = createSwatch(color, isBackground = true)
            swatch.setOnClickListener {
                viewModel.selectBackground(color)
            }
            binding.backgroundPaletteContainer.addView(swatch)
        }
        viewModel.borderColors.forEachIndexed { index, color ->
            val swatch = createSwatch(color, isBackground = false)
            swatch.setOnClickListener {
                viewModel.selectBorder(color)
            }
            if (index < 20) {
                binding.borderPaletteRow1.addView(swatch)
            } else {
                binding.borderPaletteRow2.addView(swatch)
            }
        }
    }

    private fun createSwatch(colorHex: String, isBackground: Boolean): View {
        val size = dpToPx(36)
        val margin = dpToPx(6)
        val container = FrameLayout(this).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(size, size).apply {
                setMargins(margin, margin, margin, margin)
            }
            tag = colorHex
        }
        val circle = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(size, size)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(colorHex))
                if (!isBackground) {
                    setStroke(dpToPx(1), ContextCompat.getColor(context, R.color.card_stroke))
                }
            }
        }
        container.addView(circle)
        return container
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.avatarText.collect { text ->
                        if (binding.avatarTextEditText.text.toString() != text) {
                            binding.avatarTextEditText.setText(text)
                        }
                        renderPreview()
                    }
                }
                launch {
                    viewModel.backgroundColor.collect {
                        highlightSwatch(binding.backgroundPaletteContainer, it, true)
                        renderPreview()
                    }
                }
                launch {
                    viewModel.borderColor.collect {
                        highlightSwatch(binding.borderPaletteRow1, it, false)
                        highlightSwatch(binding.borderPaletteRow2, it, false)
                        renderPreview()
                    }
                }
                launch {
                    viewModel.style.collect { style ->
                        highlightStyle(style)
                        renderPreview()
                    }
                }
                launch {
                    viewModel.isLoading.collect { loading ->
                        setBlockingLoading(loading, getString(R.string.loading_overlay_message))
                    }
                }
                launch {
                    viewModel.saved.collect { saved ->
                        if (saved) {
                            viewModel.clearSaved()
                            setResult(
                                RESULT_OK,
                                Intent().putExtra(EXTRA_RESULT_AVATAR, viewModel.currentAvatar()),
                            )
                            finish()
                        }
                    }
                }
            }
        }
        binding.avatarTextEditText.addTextChangedListener(SimpleTextWatcher {
            viewModel.updateText(it)
        })
    }

    private fun renderPreview() {
        val name = intent.getStringExtra(EXTRA_FALLBACK_NAME).orEmpty().ifBlank { "User" }
        AvatarRenderer.apply(
            binding.avatarPreviewTextView,
            name,
            viewModel.currentAvatar(),
            sizeDp = 112,
        )
    }

    private fun highlightSwatch(container: android.view.ViewGroup, colorHex: String, isBackground: Boolean) {
        for (index in 0 until container.childCount) {
            val child = container.getChildAt(index)
            val selected = child.tag == colorHex
            child.foreground = if (selected) {
                ContextCompat.getDrawable(this, R.drawable.bg_avatar_swatch_selected)
            } else {
                null
            }
            if (isBackground && selected) {
                selectedBackgroundView = child
            } else if (!isBackground && selected) {
                selectedBorderView = child
            }
        }
    }

    private fun highlightStyle(style: String) {
        val selectedBg = R.drawable.bg_primary_action
        val defaultBg = R.drawable.bg_home_row
        val selectedTextColor = ContextCompat.getColor(this, R.color.black)
        val defaultTextColor = ContextCompat.getColor(this, R.color.text_secondary)

        styleOptions.forEach { option ->
            val selected = style == option.styleKey
            option.view.setBackgroundResource(if (selected) selectedBg else defaultBg)
            option.view.setTextColor(if (selected) selectedTextColor else defaultTextColor)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val EXTRA_FALLBACK_NAME = "extra_fallback_name"
        private const val EXTRA_INITIAL_AVATAR = "extra_initial_avatar"
        private const val EXTRA_RESULT_AVATAR = "extra_result_avatar"

        fun createIntent(
            context: Context,
            fallbackName: String,
            initialAvatar: UserAvatar?,
        ): Intent {
            return Intent(context, CreateAvatarActivity::class.java).apply {
                putExtra(EXTRA_FALLBACK_NAME, fallbackName)
                initialAvatar?.let {
                    putExtra(EXTRA_INITIAL_AVATAR, it)
                }
            }
        }

        fun parseResult(data: Intent?): UserAvatar? {
            return data?.getSerializableExtra(EXTRA_RESULT_AVATAR) as? UserAvatar
        }
    }
}
