package com.parhar.noor.ui.tasks



import android.content.Context

import android.content.Intent

import android.os.Build

import androidx.activity.result.contract.ActivityResultContracts

import androidx.core.view.isVisible

import com.parhar.noor.R

import com.parhar.noor.databinding.ActivityTaskDetailBinding

import com.parhar.noor.di.appContainer

import com.parhar.noor.domain.model.TaskItem

import com.parhar.noor.ui.admin.AddTaskActivity

import com.parhar.noor.utils.BaseActivity



class TaskDetailActivity : BaseActivity<ActivityTaskDetailBinding>() {



    private lateinit var taskItem: TaskItem



    private val editTaskLauncher = registerForActivityResult(

        ActivityResultContracts.StartActivityForResult(),

    ) { result ->

        if (result.resultCode != RESULT_OK) return@registerForActivityResult

        val data = result.data

        if (data?.getBooleanExtra(AddTaskActivity.EXTRA_RESULT_TASK_DELETED, false) == true) {

            finish()

            return@registerForActivityResult

        }

        readUpdatedTaskItem(data)?.let { updated ->

            taskItem = updated

            intent.putExtra(EXTRA_TASK_ITEM, updated)

            renderTaskContent()

        }

    }



    override fun inflateBinding(): ActivityTaskDetailBinding =

        ActivityTaskDetailBinding.inflate(layoutInflater)



    override fun setupViews() {

        val item = readTaskItem()

        if (item == null) {

            finish()

            return

        }

        taskItem = item



        binding.toolbar.backImageView.setOnClickListener { finish() }

        binding.toolbar.toolbarTitleTextView.setText(R.string.task_detail_title)

        setupAdminEditAction()

        renderTaskContent()

    }



    private fun renderTaskContent() {

        val task = taskItem.task



        val emojiPrefix = task.emoji.takeIf { it.isNotBlank() }?.let { "$it " }.orEmpty()

        binding.taskNameTextView.text = "$emojiPrefix${task.name}"



        if (task.points > 0) {

            binding.pointsTextView.isVisible = true

            binding.pointsTextView.text = resources.getQuantityString(

                R.plurals.home_task_points,

                task.points,

                task.points,

            )

        } else {

            binding.pointsTextView.isVisible = false

        }



        if (task.shortDescription.isBlank()) {

            binding.shortDescriptionTextView.isVisible = false

        } else {

            binding.shortDescriptionTextView.isVisible = true

            binding.shortDescriptionTextView.text = task.shortDescription

        }



        val hasArabic = task.arabic.isNotBlank()

        val hasDetailedDescription = task.detailedDescription.isNotBlank()

        binding.detailsSection.isVisible = hasArabic || hasDetailedDescription



        binding.arabicTextView.isVisible = hasArabic

        if (hasArabic) {

            binding.arabicTextView.text = task.arabic

        }



        binding.arabicDetailDivider.isVisible = hasArabic && hasDetailedDescription



        binding.detailedDescriptionTextView.isVisible = hasDetailedDescription

        if (hasDetailedDescription) {

            binding.detailedDescriptionTextView.text = task.detailedDescription

        }

    }



    private fun setupAdminEditAction() {

        val isAdmin = appContainer().sessionManager.isAdminAuthenticated()

        binding.toolbar.toolbarActionTextView.isVisible = isAdmin

        if (!isAdmin) return



        binding.toolbar.toolbarActionTextView.setText(R.string.profile_edit)

        binding.toolbar.toolbarActionTextView.setOnClickListener {

            editTaskLauncher.launch(AddTaskActivity.createEditIntent(this, taskItem))

        }

    }



    @Suppress("DEPRECATION")

    private fun readTaskItem(): TaskItem? {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            intent.getSerializableExtra(EXTRA_TASK_ITEM, TaskItem::class.java)

        } else {

            intent.getSerializableExtra(EXTRA_TASK_ITEM) as? TaskItem

        }

    }



    @Suppress("DEPRECATION")

    private fun readUpdatedTaskItem(data: Intent?): TaskItem? {

        if (data == null) return null

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            data.getSerializableExtra(AddTaskActivity.EXTRA_RESULT_TASK_ITEM, TaskItem::class.java)

        } else {

            data.getSerializableExtra(AddTaskActivity.EXTRA_RESULT_TASK_ITEM) as? TaskItem

        }

    }



    companion object {

        private const val EXTRA_TASK_ITEM = "extra_task_item"



        fun createIntent(context: Context, taskItem: TaskItem): Intent {

            return Intent(context, TaskDetailActivity::class.java)

                .putExtra(EXTRA_TASK_ITEM, taskItem)

        }

    }

}


