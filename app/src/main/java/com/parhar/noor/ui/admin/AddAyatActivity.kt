package com.parhar.noor.ui.admin

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.parhar.noor.R
import com.parhar.noor.databinding.ActivityAddAyatBinding
import com.parhar.noor.di.appContainer
import com.parhar.noor.domain.model.Ayat
import com.parhar.noor.utils.BaseActivity
import com.parhar.noor.utils.NoorDialogs
import kotlinx.coroutines.launch

class AddAyatActivity : BaseActivity<ActivityAddAyatBinding>() {

    private val viewModel: AdminViewModel by viewModels {
        appContainer().viewModelFactory
    }

    private var blockingMessage: String? = null

    private val isEditMode: Boolean
        get() = intent.getBooleanExtra(EXTRA_EDIT_MODE, false)

    private val ayatId: String
        get() = intent.getStringExtra(EXTRA_AYAT_ID).orEmpty()

    override fun inflateBinding(): ActivityAddAyatBinding =
        ActivityAddAyatBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.toolbar.backImageView.setOnClickListener { finish() }
        binding.saveAyatTextView.setOnClickListener { saveAyat() }
        binding.deleteAyatTextView.setOnClickListener { confirmDeleteAyat() }
        renderMode()
        observeViewModel()
    }

    private fun renderMode() {
        if (isEditMode) {
            binding.toolbar.toolbarTitleTextView.setText(R.string.admin_edit_ayat_title)
            binding.saveAyatTextView.text = getString(R.string.admin_save_ayat)
            binding.deleteAyatTextView.visibility = View.VISIBLE
            binding.ayatArabicEditText.setText(intent.getStringExtra(EXTRA_AYAT_TEXT).orEmpty())
            binding.ayatEnglishEditText.setText(intent.getStringExtra(EXTRA_AYAT_ENGLISH).orEmpty())
            binding.ayatUrduEditText.setText(intent.getStringExtra(EXTRA_AYAT_URDU).orEmpty())
            binding.ayatReferenceEditText.setText(intent.getStringExtra(EXTRA_AYAT_REFERENCE).orEmpty())
        } else {
            binding.toolbar.toolbarTitleTextView.setText(R.string.admin_add_ayat_title)
            binding.saveAyatTextView.text = getString(R.string.admin_add_ayat_action)
            binding.deleteAyatTextView.visibility = View.GONE
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isActionInProgress.collect { inProgress ->
                        setBlockingLoading(
                            inProgress,
                            blockingMessage ?: getString(R.string.loading_overlay_message),
                        )
                        if (!inProgress) {
                            blockingMessage = null
                        }
                    }
                }
                launch {
                    viewModel.statusMessage.collect { message ->
                        if (!message.isNullOrBlank()) {
                            binding.statusTextView.text = message
                        }
                    }
                }
                launch {
                    viewModel.ayatSaved.collect { saved ->
                        if (saved) {
                            viewModel.clearStatusFlags()
                            finish()
                        }
                    }
                }
                launch {
                    viewModel.ayatDeleted.collect { deleted ->
                        if (deleted) {
                            viewModel.clearStatusFlags()
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun saveAyat() {
        val ayatText = binding.ayatArabicEditText.text.toString().trim()
        val english = binding.ayatEnglishEditText.text.toString().trim()
        val urdu = binding.ayatUrduEditText.text.toString().trim()
        val reference = binding.ayatReferenceEditText.text.toString().trim()

        when {
            ayatText.isBlank() -> {
                binding.statusTextView.setText(R.string.admin_ayat_arabic_required)
                return
            }
            english.isBlank() -> {
                binding.statusTextView.setText(R.string.admin_ayat_english_required)
                return
            }
        }

        blockingMessage = if (isEditMode) {
            getString(R.string.admin_saving_ayat)
        } else {
            getString(R.string.admin_adding_ayat)
        }

        if (isEditMode) {
            viewModel.updateAyat(ayatId, ayatText, english, urdu, reference)
        } else {
            viewModel.addAyat(ayatText, english, urdu, reference)
        }
    }

    private fun confirmDeleteAyat() {
        NoorDialogs.showConfirm(
            context = this,
            titleRes = R.string.admin_delete_ayat_title,
            messageRes = R.string.admin_delete_ayat_message,
            tone = NoorDialogs.ConfirmTone.DESTRUCTIVE,
            onPositive = {
                blockingMessage = getString(R.string.admin_deleting_ayat)
                viewModel.deleteAyat(ayatId)
            },
        )
    }

    companion object {
        private const val EXTRA_EDIT_MODE = "extra_edit_mode"
        private const val EXTRA_AYAT_ID = "extra_ayat_id"
        private const val EXTRA_AYAT_TEXT = "extra_ayat_text"
        private const val EXTRA_AYAT_ENGLISH = "extra_ayat_english"
        private const val EXTRA_AYAT_URDU = "extra_ayat_urdu"
        private const val EXTRA_AYAT_REFERENCE = "extra_ayat_reference"

        fun createAddIntent(context: Context): Intent =
            Intent(context, AddAyatActivity::class.java)

        fun createEditIntent(context: Context, ayat: Ayat): Intent {
            return Intent(context, AddAyatActivity::class.java).apply {
                putExtra(EXTRA_EDIT_MODE, true)
                putExtra(EXTRA_AYAT_ID, ayat.id)
                putExtra(EXTRA_AYAT_TEXT, ayat.ayat)
                putExtra(EXTRA_AYAT_ENGLISH, ayat.english)
                putExtra(EXTRA_AYAT_URDU, ayat.urdu)
                putExtra(EXTRA_AYAT_REFERENCE, ayat.reference)
            }
        }
    }
}
