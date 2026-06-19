package com.parhar.noor.ui.admin

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.parhar.noor.utils.NoorDialogs
import com.parhar.noor.R
import com.parhar.noor.databinding.ActivityTasksListBinding
import com.parhar.noor.databinding.ItemAdminTaskRowBinding
import com.parhar.noor.databinding.ItemAdminTaskSectionBinding
import com.parhar.noor.di.appContainer
import com.parhar.noor.domain.model.HomeTaskSection
import com.parhar.noor.domain.model.TaskItem
import com.parhar.noor.utils.BaseActivity
import kotlinx.coroutines.launch

class TasksListActivity : BaseActivity<ActivityTasksListBinding>() {

    private val viewModel: AdminViewModel by lazy {
        ViewModelProvider(this, appContainer().viewModelFactory)[AdminViewModel::class.java]
    }

    private var selectedTask: TaskItem? = null
    private var currentSections: List<HomeTaskSection> = emptyList()

    private val sectionAdapter = AdminTaskSectionAdapter(
        onTaskClicked = { taskItem ->
            startActivity(AddTaskActivity.createEditIntent(this, taskItem))
        },
        onTaskLongClicked = { taskItem ->
            selectTask(taskItem)
            true
        },
        selectedTaskIdProvider = { selectedTask?.id },
    )

    override fun inflateBinding(): ActivityTasksListBinding =
        ActivityTasksListBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.toolbar.backImageView.setOnClickListener { finish() }
        binding.toolbar.toolbarTitleTextView.setText(R.string.admin_tasks_list_title)
        binding.addTaskTextView.setOnClickListener {
            startActivity(AddTaskActivity.createAddIntent(this))
        }
        binding.closeSelectionTextView.setOnClickListener { clearSelection() }
        binding.deleteTaskTextView.setOnClickListener { confirmDeleteSelectedTask() }
        binding.moveUpTextView.setOnClickListener { moveSelectedTask(up = true) }
        binding.moveDownTextView.setOnClickListener { moveSelectedTask(up = false) }

        binding.tasksRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.tasksRecyclerView.adapter = sectionAdapter
    }

    override fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isActionInProgress.collect { inProgress ->
                        if (!inProgress) {
                            setBlockingLoading(false)
                        }
                    }
                }
                launch {
                    viewModel.taskSections.collect { sections ->
                        currentSections = sections
                        selectedTask = selectedTask?.let { selected ->
                            sections.flatMap { it.tasks }.firstOrNull { it.id == selected.id }
                                ?: selected
                        }
                        sectionAdapter.submitList(sections)
                        val totalTasks = sections.sumOf { section -> section.tasks.size }
                        binding.emptyTextView.visibility = if (totalTasks == 0) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                        updateActionBarState()
                    }
                }
            }
        }
    }

    private fun selectTask(taskItem: TaskItem) {
        selectedTask = taskItem
        binding.selectionActionBar.visibility = View.VISIBLE
        sectionAdapter.notifySelectionChanged()
        updateActionBarState()
    }

    private fun clearSelection() {
        selectedTask = null
        binding.selectionActionBar.visibility = View.GONE
        sectionAdapter.notifySelectionChanged()
    }

    private fun updateActionBarState() {
        val selected = selectedTask ?: return
        val sectionIndex = currentSections.indexOfFirst { homeSection ->
            homeSection.tasks.any { it.id == selected.id }
        }
        if (sectionIndex < 0) return

        val section = currentSections[sectionIndex]
        val index = section.tasks.indexOfFirst { it.id == selected.id }
        val canMoveUp = index > 0 || sectionIndex > 0
        val canMoveDown =
            index < section.tasks.lastIndex || sectionIndex < currentSections.lastIndex

        binding.moveUpTextView.isEnabled = canMoveUp
        binding.moveDownTextView.isEnabled = canMoveDown
        binding.moveUpTextView.alpha = if (canMoveUp) 1f else 0.4f
        binding.moveDownTextView.alpha = if (canMoveDown) 1f else 0.4f
    }

    private fun confirmDeleteSelectedTask() {
        val selected = selectedTask ?: return
        NoorDialogs.showConfirm(
            context = this,
            titleRes = R.string.admin_delete_task_title,
            messageRes = R.string.admin_delete_task_message,
            tone = NoorDialogs.ConfirmTone.DESTRUCTIVE,
            onPositive = {
                setBlockingLoading(true, getString(R.string.admin_deleting_task))
                viewModel.deleteTask(selected.id, selected.task.category)
                clearSelection()
            },
        )
    }

    private fun moveSelectedTask(up: Boolean) {
        val selected = selectedTask ?: return
        if (up) {
            viewModel.moveTaskUp(selected.id, selected.task.category)
        } else {
            viewModel.moveTaskDown(selected.id, selected.task.category)
        }
    }

    companion object {
        fun createIntent(context: Context): Intent =
            Intent(context, TasksListActivity::class.java)
    }
}

private class AdminTaskSectionAdapter(
    private val onTaskClicked: (TaskItem) -> Unit,
    private val onTaskLongClicked: (TaskItem) -> Boolean,
    private val selectedTaskIdProvider: () -> String?,
) : ListAdapter<HomeTaskSection, AdminTaskSectionAdapter.SectionViewHolder>(SectionDiffCallback) {

    private var parentRecyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        parentRecyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        if (parentRecyclerView === recyclerView) {
            parentRecyclerView = null
        }
    }

    fun notifySelectionChanged() {
        val recyclerView = parentRecyclerView ?: return
        for (index in 0 until itemCount) {
            (recyclerView.findViewHolderForAdapterPosition(index) as? SectionViewHolder)
                ?.refreshSelection()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val binding = ItemAdminTaskSectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return SectionViewHolder(binding, onTaskClicked, onTaskLongClicked, selectedTaskIdProvider)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SectionViewHolder(
        private val binding: ItemAdminTaskSectionBinding,
        private val onTaskClicked: (TaskItem) -> Unit,
        private val onTaskLongClicked: (TaskItem) -> Boolean,
        private val selectedTaskIdProvider: () -> String?,
    ) : RecyclerView.ViewHolder(binding.root) {

        private val taskAdapter = AdminTaskRowAdapter(
            onTaskClicked = onTaskClicked,
            onTaskLongClicked = onTaskLongClicked,
            selectedTaskIdProvider = selectedTaskIdProvider,
        )

        init {
            binding.tasksRecyclerView.apply {
                adapter = taskAdapter
                layoutManager = LinearLayoutManager(context)
                isNestedScrollingEnabled = false
                itemAnimator = null
            }
        }

        fun bind(section: HomeTaskSection) {
            binding.sectionTitleTextView.text = section.title.uppercase()
            taskAdapter.submitList(section.tasks)
        }

        fun refreshSelection() {
            taskAdapter.notifySelectionChanged()
        }
    }

    private object SectionDiffCallback : DiffUtil.ItemCallback<HomeTaskSection>() {
        override fun areItemsTheSame(oldItem: HomeTaskSection, newItem: HomeTaskSection): Boolean =
            oldItem.category == newItem.category

        override fun areContentsTheSame(
            oldItem: HomeTaskSection,
            newItem: HomeTaskSection
        ): Boolean =
            oldItem == newItem
    }
}

private class AdminTaskRowAdapter(
    private val onTaskClicked: (TaskItem) -> Unit,
    private val onTaskLongClicked: (TaskItem) -> Boolean,
    private val selectedTaskIdProvider: () -> String?,
) : ListAdapter<TaskItem, AdminTaskRowAdapter.TaskViewHolder>(TaskDiffCallback) {

    fun notifySelectionChanged() {
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemAdminTaskRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(
        private val binding: ItemAdminTaskRowBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(taskItem: TaskItem) {
            val isSelected = selectedTaskIdProvider() == taskItem.id
            val emojiPrefix = taskItem.task.emoji.takeIf { it.isNotBlank() }?.let { "$it " }.orEmpty()
            binding.taskNameTextView.text = "$emojiPrefix${taskItem.task.name}"
            binding.pointsTextView.text = "+${taskItem.task.points} pts"
            binding.root.setBackgroundResource(
                if (isSelected) R.drawable.bg_quote_card else R.drawable.bg_home_row,
            )
            binding.root.setOnClickListener { onTaskClicked(taskItem) }
            binding.root.setOnLongClickListener { onTaskLongClicked(taskItem) }
        }
    }

    private object TaskDiffCallback : DiffUtil.ItemCallback<TaskItem>() {
        override fun areItemsTheSame(oldItem: TaskItem, newItem: TaskItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: TaskItem, newItem: TaskItem): Boolean =
            oldItem == newItem
    }
}
