package com.parhar.noor.ui.admin

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.parhar.noor.R
import com.parhar.noor.databinding.ActivityAddTrophyBinding
import com.parhar.noor.di.appContainer
import com.parhar.noor.domain.model.Trophy
import com.parhar.noor.utils.BaseActivity
import com.parhar.noor.utils.EmojiInputUtils
import com.parhar.noor.utils.NoorDialogs
import kotlinx.coroutines.launch

class AddTrophyActivity : BaseActivity<ActivityAddTrophyBinding>() {

    private val viewModel: AdminViewModel by viewModels {
        appContainer().viewModelFactory
    }

    private var blockingMessage: String? = null

    private val isEditMode: Boolean
        get() = intent.getBooleanExtra(EXTRA_EDIT_MODE, false)

    private val trophyId: String
        get() = intent.getStringExtra(EXTRA_TROPHY_ID).orEmpty()

    override fun inflateBinding(): ActivityAddTrophyBinding =
        ActivityAddTrophyBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.toolbar.backImageView.setOnClickListener { finish() }
        binding.saveTrophyTextView.setOnClickListener { saveTrophy() }
        binding.deleteTrophyTextView.setOnClickListener { confirmDeleteTrophy() }
        EmojiInputUtils.attachSingleEmojiLimiter(binding.trophyIconEditText)
        renderMode()
        observeViewModel()
    }

    private fun renderMode() {
        if (isEditMode) {
            binding.toolbar.toolbarTitleTextView.setText(R.string.admin_edit_trophy_title)
            binding.saveTrophyTextView.text = getString(R.string.admin_save_trophy)
            binding.deleteTrophyTextView.visibility = View.VISIBLE
            binding.trophyNameEditText.setText(intent.getStringExtra(EXTRA_TROPHY_NAME).orEmpty())
            binding.trophyIconEditText.setText(intent.getStringExtra(EXTRA_TROPHY_ICON).orEmpty())
            binding.trophyRequirementEditText.setText(
                intent.getIntExtra(EXTRA_TROPHY_REQUIREMENT, 0)
                    .takeIf { it > 0 }
                    ?.toString()
                    .orEmpty(),
            )
        } else {
            binding.toolbar.toolbarTitleTextView.setText(R.string.admin_add_trophy_title)
            binding.saveTrophyTextView.text = getString(R.string.admin_add_trophy_action)
            binding.deleteTrophyTextView.visibility = View.GONE
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
                    viewModel.trophySaved.collect { saved ->
                        if (saved) {
                            viewModel.clearStatusFlags()
                            finish()
                        }
                    }
                }
                launch {
                    viewModel.trophyDeleted.collect { deleted ->
                        if (deleted) {
                            viewModel.clearStatusFlags()
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun saveTrophy() {
        val name = binding.trophyNameEditText.text.toString().trim()
        val icon = EmojiInputUtils.firstEmoji(binding.trophyIconEditText.text.toString())
        val requirementText = binding.trophyRequirementEditText.text.toString().trim()

        when {
            name.isBlank() -> {
                binding.statusTextView.setText(R.string.admin_trophy_name_required)
                return
            }
            icon.isBlank() -> {
                binding.statusTextView.setText(R.string.admin_trophy_icon_required)
                return
            }
            requirementText.isBlank() || requirementText.toIntOrNull() == null -> {
                binding.statusTextView.setText(R.string.admin_trophy_requirement_required)
                return
            }
        }

        val requirement = requirementText.toInt()
        blockingMessage = if (isEditMode) {
            getString(R.string.admin_saving_trophy)
        } else {
            getString(R.string.admin_adding_trophy)
        }

        if (isEditMode) {
            viewModel.updateTrophy(trophyId, name, icon, requirement)
        } else {
            viewModel.addTrophy(name, icon, requirement)
        }
    }

    private fun confirmDeleteTrophy() {
        NoorDialogs.showConfirm(
            context = this,
            titleRes = R.string.admin_delete_trophy_title,
            messageRes = R.string.admin_delete_trophy_message,
            tone = NoorDialogs.ConfirmTone.DESTRUCTIVE,
            onPositive = {
                blockingMessage = getString(R.string.admin_deleting_trophy)
                viewModel.deleteTrophy(trophyId)
            },
        )
    }

    companion object {
        private const val EXTRA_EDIT_MODE = "extra_edit_mode"
        private const val EXTRA_TROPHY_ID = "extra_trophy_id"
        private const val EXTRA_TROPHY_NAME = "extra_trophy_name"
        private const val EXTRA_TROPHY_ICON = "extra_trophy_icon"
        private const val EXTRA_TROPHY_REQUIREMENT = "extra_trophy_requirement"

        fun createAddIntent(context: Context): Intent =
            Intent(context, AddTrophyActivity::class.java)

        fun createEditIntent(context: Context, trophy: Trophy): Intent {
            return Intent(context, AddTrophyActivity::class.java).apply {
                putExtra(EXTRA_EDIT_MODE, true)
                putExtra(EXTRA_TROPHY_ID, trophy.id)
                putExtra(EXTRA_TROPHY_NAME, trophy.name)
                putExtra(EXTRA_TROPHY_ICON, trophy.icon)
                putExtra(EXTRA_TROPHY_REQUIREMENT, trophy.requirement)
            }
        }
    }
}
