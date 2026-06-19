package com.parhar.noor.ui.admin

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.parhar.noor.utils.NoorDialogs
import com.parhar.noor.R
import com.parhar.noor.databinding.ActivityAddCategoryBinding
import com.parhar.noor.di.appContainer
import com.parhar.noor.domain.model.Category
import com.parhar.noor.utils.BaseActivity
import kotlinx.coroutines.launch

class AddCategoryActivity : BaseActivity<ActivityAddCategoryBinding>() {

    private val viewModel: AdminViewModel by viewModels {
        appContainer().viewModelFactory
    }

    private var blockingMessage: String? = null

    private val isEditMode: Boolean
        get() = intent.getBooleanExtra(EXTRA_EDIT_MODE, false)

    private val categoryId: String
        get() = intent.getStringExtra(EXTRA_CATEGORY_ID).orEmpty()

    private val originalCategoryName: String
        get() = intent.getStringExtra(EXTRA_ORIGINAL_CATEGORY_NAME).orEmpty()

    private val isPrimaryCategory: Boolean
        get() = isEditMode && originalCategoryName.equals(PRIMARY_CATEGORY_NAME, ignoreCase = true)

    override fun inflateBinding(): ActivityAddCategoryBinding =
        ActivityAddCategoryBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.toolbar.backImageView.setOnClickListener { finish() }
        binding.saveCategoryTextView.setOnClickListener { saveCategory() }
        binding.deleteCategoryTextView.setOnClickListener { confirmDeleteCategory() }
        renderMode()
        observeViewModel()
    }

    private fun renderMode() {
        if (isEditMode) {
            binding.toolbar.toolbarTitleTextView.setText(R.string.admin_edit_category_title)
            binding.saveCategoryTextView.text = getString(R.string.admin_save_category)
            binding.deleteCategoryTextView.visibility = View.VISIBLE
            binding.categoryTitleEditText.setText(intent.getStringExtra(EXTRA_CATEGORY_TITLE).orEmpty())
            binding.categoryNameEditText.setText(intent.getStringExtra(EXTRA_CATEGORY_NAME).orEmpty())
            binding.categoryDescriptionEditText.setText(
                intent.getStringExtra(EXTRA_CATEGORY_DESCRIPTION).orEmpty(),
            )
            if (isPrimaryCategory) {
                binding.categoryNameEditText.isEnabled = false
                binding.categoryNameEditText.isFocusable = false
                binding.categoryNameEditText.alpha = 0.6f
                binding.deleteCategoryTextView.visibility = View.GONE
            }
        } else {
            binding.toolbar.toolbarTitleTextView.setText(R.string.admin_add_category_title)
            binding.saveCategoryTextView.text = getString(R.string.admin_add_category_action)
            binding.deleteCategoryTextView.visibility = View.GONE
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
                    viewModel.categorySaved.collect { saved ->
                        if (saved) {
                            viewModel.clearStatusFlags()
                            finish()
                        }
                    }
                }
                launch {
                    viewModel.categoryDeleted.collect { deleted ->
                        if (deleted) {
                            viewModel.clearStatusFlags()
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun saveCategory() {
        val title = binding.categoryTitleEditText.text.toString().trim()
        val categoryName = if (isPrimaryCategory) {
            originalCategoryName
        } else {
            binding.categoryNameEditText.text.toString().trim()
        }
        val description = binding.categoryDescriptionEditText.text.toString().trim()

        when {
            title.isBlank() -> {
                binding.statusTextView.setText(R.string.admin_category_title_required)
                return
            }
            categoryName.isBlank() -> {
                binding.statusTextView.setText(R.string.admin_category_name_required)
                return
            }
        }

        blockingMessage = if (isEditMode) {
            getString(R.string.admin_saving_category)
        } else {
            getString(R.string.admin_adding_category)
        }

        if (isEditMode) {
            viewModel.updateCategory(
                categoryId = categoryId,
                originalCategoryName = originalCategoryName,
                title = title,
                categoryName = categoryName,
                description = description,
            )
        } else {
            viewModel.addCategory(
                title = title,
                categoryName = categoryName,
                description = description,
            )
        }
    }

    private fun confirmDeleteCategory() {
        NoorDialogs.showConfirm(
            context = this,
            titleRes = R.string.admin_delete_category_title,
            messageRes = R.string.admin_delete_category_message,
            tone = NoorDialogs.ConfirmTone.DESTRUCTIVE,
            onPositive = {
                blockingMessage = getString(R.string.admin_deleting_category)
                viewModel.deleteCategory(categoryId, originalCategoryName)
            },
        )
    }

    companion object {
        private const val PRIMARY_CATEGORY_NAME = "Primary"
        private const val EXTRA_EDIT_MODE = "extra_edit_mode"
        private const val EXTRA_CATEGORY_ID = "extra_category_id"
        private const val EXTRA_ORIGINAL_CATEGORY_NAME = "extra_original_category_name"
        private const val EXTRA_CATEGORY_TITLE = "extra_category_title"
        private const val EXTRA_CATEGORY_NAME = "extra_category_name"
        private const val EXTRA_CATEGORY_DESCRIPTION = "extra_category_description"

        fun createAddIntent(context: Context): Intent =
            Intent(context, AddCategoryActivity::class.java)

        fun createEditIntent(context: Context, category: Category): Intent {
            return Intent(context, AddCategoryActivity::class.java).apply {
                putExtra(EXTRA_EDIT_MODE, true)
                putExtra(EXTRA_CATEGORY_ID, category.id)
                putExtra(EXTRA_ORIGINAL_CATEGORY_NAME, category.category)
                putExtra(EXTRA_CATEGORY_TITLE, category.title)
                putExtra(EXTRA_CATEGORY_NAME, category.category)
                putExtra(EXTRA_CATEGORY_DESCRIPTION, category.description)
            }
        }
    }
}
