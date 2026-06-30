package com.parhar.noor.ui.admin

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.parhar.noor.R
import com.parhar.noor.databinding.ActivityAddTaskBinding
import com.parhar.noor.di.appContainer
import com.parhar.noor.domain.model.TaskDefinition
import com.parhar.noor.domain.model.TaskItem
import com.parhar.noor.utils.BaseActivity
import com.parhar.noor.utils.EmojiInputUtils
import com.parhar.noor.utils.NoorDialogs
import kotlinx.coroutines.launch

class AddTaskActivity : BaseActivity<ActivityAddTaskBinding>() {

    private val viewModel: AdminViewModel by viewModels {
        appContainer().viewModelFactory
    }

    private var blockingMessage: String? = null
    private var isTaskVisible: Boolean = true

    private val isEditMode: Boolean
        get() = intent.getBooleanExtra(EXTRA_EDIT_MODE, false)

    private val editTaskId: String
        get() = intent.getStringExtra(EXTRA_TASK_ID).orEmpty()

    private val originalCategory: String
        get() = intent.getStringExtra(EXTRA_ORIGINAL_CATEGORY).orEmpty()

    override fun inflateBinding(): ActivityAddTaskBinding =
        ActivityAddTaskBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.toolbar.backImageView.setOnClickListener { finish() }
        binding.addTaskTextView.setOnClickListener { saveTask() }
        binding.deleteTaskTextView.setOnClickListener { confirmDeleteTask() }
        binding.showTaskTextView.setOnClickListener { selectVisibility(visible = true) }
        binding.hideTaskTextView.setOnClickListener { selectVisibility(visible = false) }
        EmojiInputUtils.attachSingleEmojiLimiter(binding.taskEmojiEditText)
        renderMode()
        observeViewModel()
    }

    private fun renderMode() {
        if (isEditMode) {
            binding.toolbar.toolbarTitleTextView.setText(R.string.admin_edit_task_title)
            binding.addTaskTextView.text = getString(R.string.admin_save_task)
            binding.deleteTaskTextView.visibility = View.VISIBLE
            binding.taskNameEditText.setText(intent.getStringExtra(EXTRA_TASK_NAME).orEmpty())
            binding.taskEmojiEditText.setText(intent.getStringExtra(EXTRA_TASK_EMOJI).orEmpty())
            binding.taskShortDescriptionEditText.setText(
                intent.getStringExtra(EXTRA_TASK_SHORT_DESCRIPTION).orEmpty(),
            )
            binding.taskDetailedDescriptionEditText.setText(
                intent.getStringExtra(EXTRA_TASK_DETAILED_DESCRIPTION).orEmpty(),
            )
            binding.taskArabicEditText.setText(intent.getStringExtra(EXTRA_TASK_ARABIC).orEmpty())
            binding.taskPointsEditText.setText(
                intent.getIntExtra(EXTRA_TASK_POINTS, 0).takeIf { it > 0 }?.toString().orEmpty(),
            )
            isTaskVisible = intent.getBooleanExtra(EXTRA_TASK_VISIBLE, true)
        } else {
            binding.toolbar.toolbarTitleTextView.setText(R.string.admin_add_task_title)
            binding.addTaskTextView.text = getString(R.string.admin_add_task_action)
            binding.deleteTaskTextView.visibility = View.GONE
            isTaskVisible = true
        }
        updateVisibilityUi()
    }

    private fun selectVisibility(visible: Boolean) {
        isTaskVisible = visible
        updateVisibilityUi()
    }

    private fun updateVisibilityUi() {
        binding.showTaskTextView.setBackgroundResource(
            if (isTaskVisible) R.drawable.bg_primary_action else R.drawable.bg_secondary_action,
        )
        binding.hideTaskTextView.setBackgroundResource(
            if (!isTaskVisible) R.drawable.bg_primary_action else R.drawable.bg_secondary_action,
        )
        binding.showTaskTextView.setTextColor(
            getColor(if (isTaskVisible) R.color.navy_base else R.color.text_primary),
        )
        binding.hideTaskTextView.setTextColor(
            getColor(if (!isTaskVisible) R.color.navy_base else R.color.text_primary),
        )
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.categories.collect { categories ->
                        bindCategories(categories.map { it.category })
                    }
                }
                launch {
                    viewModel.isActionInProgress.collect { inProgress ->
                        setBlockingLoading(
                            inProgress,
                            blockingMessage ?: getString(R.string.loading_overlay_message),
                        )
                        if (!inProgress) {
                            blockingMessage = null
                            setFormEnabled(viewModel.categories.value.isNotEmpty())
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
                    viewModel.taskSaved.collect { saved ->
                        if (saved) {
                            viewModel.clearStatusFlags()
                            if (isEditMode) {
                                setResult(
                                    RESULT_OK,
                                    Intent().putExtra(EXTRA_RESULT_TASK_ITEM, buildResultTaskItem()),
                                )
                            }
                            finish()
                        }
                    }
                }
                launch {
                    viewModel.taskDeleted.collect { deleted ->
                        if (deleted) {
                            viewModel.clearStatusFlags()
                            if (isEditMode) {
                                setResult(
                                    RESULT_OK,
                                    Intent().putExtra(EXTRA_RESULT_TASK_DELETED, true),
                                )
                            }
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun bindCategories(categoryNames: List<String>) {
        val adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categoryNames,
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.categorySpinner.adapter = adapter

        if (isEditMode) {
            val selectedCategory = intent.getStringExtra(EXTRA_TASK_CATEGORY).orEmpty()
            val selectedIndex = categoryNames.indexOfFirst { it.equals(selectedCategory, ignoreCase = true) }
            if (selectedIndex >= 0) {
                binding.categorySpinner.setSelection(selectedIndex)
            }
        }

        val hasCategories = categoryNames.isNotEmpty()
        binding.statusTextView.text = if (hasCategories) {
            getString(R.string.admin_categories_loaded)
        } else {
            getString(R.string.admin_categories_empty)
        }
        setFormEnabled(hasCategories)
    }

    private fun saveTask() {
        val taskName = binding.taskNameEditText.text.toString().trim()
        val emoji = EmojiInputUtils.firstEmoji(binding.taskEmojiEditText.text.toString())
        val shortDescription = binding.taskShortDescriptionEditText.text.toString().trim()
        val detailedDescription = binding.taskDetailedDescriptionEditText.text.toString().trim()
        val arabic = binding.taskArabicEditText.text.toString().trim()
        val points = binding.taskPointsEditText.text.toString().trim().toIntOrNull()
        val category = viewModel.categories.value
            .getOrNull(binding.categorySpinner.selectedItemPosition)
            ?.category
            .orEmpty()

        when {
            taskName.isBlank() -> {
                binding.statusTextView.text = getString(R.string.admin_task_name_required)
                return
            }
            category.isBlank() -> {
                binding.statusTextView.text = getString(R.string.admin_category_required)
                return
            }
            points == null -> {
                binding.statusTextView.text = getString(R.string.admin_points_required)
                return
            }
        }

        blockingMessage = if (isEditMode) {
            getString(R.string.admin_saving_task)
        } else {
            getString(R.string.admin_adding_task)
        }

        if (isEditMode) {
            viewModel.updateTask(
                taskId = editTaskId,
                originalCategory = originalCategory,
                category = category,
                name = taskName,
                points = points,
                emoji = emoji,
                shortDescription = shortDescription,
                detailedDescription = detailedDescription,
                arabic = arabic,
                visible = isTaskVisible,
            )
        } else {
            viewModel.addTask(
                category = category,
                name = taskName,
                points = points,
                emoji = emoji,
                shortDescription = shortDescription,
                detailedDescription = detailedDescription,
                arabic = arabic,
                visible = isTaskVisible,
            )
        }
    }

    private fun confirmDeleteTask() {
        if (!isEditMode) return
        NoorDialogs.showConfirm(
            context = this,
            titleRes = R.string.admin_delete_task_title,
            messageRes = R.string.admin_delete_task_message,
            tone = NoorDialogs.ConfirmTone.DESTRUCTIVE,
            onPositive = {
                blockingMessage = getString(R.string.admin_deleting_task)
                viewModel.deleteTask(editTaskId, originalCategory)
            },
        )
    }

    private fun buildResultTaskItem(): TaskItem {
        val category = viewModel.categories.value
            .getOrNull(binding.categorySpinner.selectedItemPosition)
            ?.category
            .orEmpty()
        return TaskItem(
            id = editTaskId,
            task = TaskDefinition(
                id = editTaskId,
                category = category,
                name = binding.taskNameEditText.text.toString().trim(),
                points = binding.taskPointsEditText.text.toString().trim().toIntOrNull() ?: 0,
                emoji = EmojiInputUtils.firstEmoji(binding.taskEmojiEditText.text.toString()),
                shortDescription = binding.taskShortDescriptionEditText.text.toString().trim(),
                detailedDescription = binding.taskDetailedDescriptionEditText.text.toString().trim(),
                arabic = binding.taskArabicEditText.text.toString().trim(),
                visible = isTaskVisible,
            ),
        )
    }

    private fun setFormEnabled(enabled: Boolean) {
        binding.taskNameEditText.isEnabled = enabled
        binding.taskEmojiEditText.isEnabled = enabled
        binding.taskShortDescriptionEditText.isEnabled = enabled
        binding.taskDetailedDescriptionEditText.isEnabled = enabled
        binding.taskArabicEditText.isEnabled = enabled
        binding.categorySpinner.isEnabled = enabled
        binding.taskPointsEditText.isEnabled = enabled
        binding.showTaskTextView.isEnabled = enabled
        binding.hideTaskTextView.isEnabled = enabled
        binding.addTaskTextView.isEnabled = enabled
        binding.deleteTaskTextView.isEnabled = enabled
        binding.addTaskTextView.alpha = if (enabled) 1f else 0.5f
        binding.deleteTaskTextView.alpha = if (enabled) 1f else 0.5f
    }

    companion object {
        private const val EXTRA_EDIT_MODE = "edit_mode"
        private const val EXTRA_TASK_ID = "task_id"
        private const val EXTRA_TASK_CATEGORY = "task_category"
        private const val EXTRA_ORIGINAL_CATEGORY = "original_category"
        private const val EXTRA_TASK_NAME = "task_name"
        private const val EXTRA_TASK_EMOJI = "task_emoji"
        private const val EXTRA_TASK_SHORT_DESCRIPTION = "task_short_description"
        private const val EXTRA_TASK_DETAILED_DESCRIPTION = "task_detailed_description"
        private const val EXTRA_TASK_ARABIC = "task_arabic"
        private const val EXTRA_TASK_POINTS = "task_points"
        private const val EXTRA_TASK_VISIBLE = "task_visible"

        const val EXTRA_RESULT_TASK_ITEM = "result_task_item"
        const val EXTRA_RESULT_TASK_DELETED = "result_task_deleted"

        fun createAddIntent(context: Context): Intent =
            Intent(context, AddTaskActivity::class.java)

        fun createEditIntent(context: Context, taskItem: TaskItem): Intent =
            Intent(context, AddTaskActivity::class.java).apply {
                putExtra(EXTRA_EDIT_MODE, true)
                putExtra(EXTRA_TASK_ID, taskItem.id)
                putExtra(EXTRA_TASK_CATEGORY, taskItem.task.category)
                putExtra(EXTRA_ORIGINAL_CATEGORY, taskItem.task.category)
                putExtra(EXTRA_TASK_NAME, taskItem.task.name)
                putExtra(EXTRA_TASK_EMOJI, taskItem.task.emoji)
                putExtra(EXTRA_TASK_SHORT_DESCRIPTION, taskItem.task.shortDescription)
                putExtra(EXTRA_TASK_DETAILED_DESCRIPTION, taskItem.task.detailedDescription)
                putExtra(EXTRA_TASK_ARABIC, taskItem.task.arabic)
                putExtra(EXTRA_TASK_POINTS, taskItem.task.points)
                putExtra(EXTRA_TASK_VISIBLE, taskItem.task.visible)
            }
    }
}
