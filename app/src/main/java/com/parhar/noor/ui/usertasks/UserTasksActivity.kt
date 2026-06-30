package com.parhar.noor.ui.usertasks

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.parhar.noor.R
import com.parhar.noor.databinding.ActivityUserTasksBinding
import com.parhar.noor.databinding.ItemHomeTaskBinding
import com.parhar.noor.databinding.ItemHomeTaskSectionBinding
import com.parhar.noor.di.appContainer
import com.parhar.noor.domain.model.HomeTaskSection
import com.parhar.noor.domain.model.TaskItem
import com.parhar.noor.ui.profile.ProfileActivity
import com.parhar.noor.ui.tasks.TaskDetailActivity
import com.parhar.noor.utils.AvatarRenderer
import com.parhar.noor.utils.BaseActivity
import com.parhar.noor.utils.DateKeyUtils
import kotlinx.coroutines.launch

class UserTasksActivity : BaseActivity<ActivityUserTasksBinding>() {

    private val targetUserId: String by lazy {
        intent.getStringExtra(EXTRA_USER_ID).orEmpty()
    }

    private val viewModel: UserTasksViewModel by viewModels {
        UserTasksViewModelFactory(
            targetUserId = targetUserId,
            appContainer = appContainer(),
        )
    }

    private lateinit var sectionAdapter: ReadOnlyTaskSectionAdapter

    override fun inflateBinding(): ActivityUserTasksBinding =
        ActivityUserTasksBinding.inflate(layoutInflater)

    override fun setupViews() {
        if (targetUserId.isBlank()) {
            finish()
            return
        }

        binding.toolbar.backImageView.setOnClickListener { finish() }
        binding.toolbar.toolbarTitleTextView.setText(R.string.user_tasks_title)
        binding.toolbar.toolbarActionTextView.visibility = View.GONE

        sectionAdapter = ReadOnlyTaskSectionAdapter { taskItem ->
            startActivity(TaskDetailActivity.createIntent(this, taskItem))
        }
        binding.tasksContainer.apply {
            adapter = sectionAdapter
            layoutManager = LinearLayoutManager(this@UserTasksActivity)
            isNestedScrollingEnabled = false
            itemAnimator = null
        }

        binding.dateNavigator.previousDateTextView.setOnClickListener { viewModel.goToPreviousDate() }
        binding.dateNavigator.nextDateTextView.setOnClickListener { viewModel.goToNextDate() }
        binding.userAvatarTextView.setOnClickListener { openProfile() }
        binding.userNameTextView.setOnClickListener { openProfile() }
        binding.viewProfileTextView.setOnClickListener { openProfile() }
        observeViewModel()
    }

    private fun openProfile() {
        startActivity(ProfileActivity.createIntent(this, targetUserId))
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.userNameTextView.text = state.name
                    AvatarRenderer.apply(
                        binding.userAvatarTextView,
                        state.name,
                        state.avatar,
                        sizeDp = 72,
                    )
                    binding.streakStatTextView.text = getString(
                        R.string.user_tasks_streak_format,
                        state.streak,
                    )
                    binding.allTimeStatTextView.text = getString(
                        R.string.user_tasks_all_time_format,
                        state.allTimePoints,
                    )
                    binding.weeklyStatTextView.text = getString(
                        R.string.user_tasks_weekly_format,
                        state.weeklyPoints,
                    )

                    binding.dateNavigator.root.isVisible = state.showDateNavigator
                    binding.dateNavigator.selectedDateTextView.text =
                        DateKeyUtils.formatDisplayDate(state.selectedDateKey)
                    binding.dateNavigator.previousDateTextView.alpha =
                        if (state.canGoPrevious) 1f else 0.35f
                    binding.dateNavigator.nextDateTextView.alpha =
                        if (state.canGoNext) 1f else 0.35f
                    binding.dateNavigator.previousDateTextView.isEnabled = state.canGoPrevious
                    binding.dateNavigator.nextDateTextView.isEnabled = state.canGoNext
                    binding.dateNavigator.previousDateTextView.isClickable = state.canGoPrevious
                    binding.dateNavigator.nextDateTextView.isClickable = state.canGoNext

                    binding.privateTasksMessageTextView.isVisible = state.isPrivate

                    val showTasksLoading = state.showTasks && state.isLoading
                    binding.tasksShimmerContainer.isVisible = showTasksLoading
                    binding.tasksContainer.isVisible = state.showTasks && !state.isLoading
                    binding.tasksStatusTextView.isVisible = false
                    if (showTasksLoading) {
                        binding.tasksShimmerContainer.startShimmer()
                    } else {
                        binding.tasksShimmerContainer.stopShimmer()
                    }

                    if (state.showTasks && !state.isLoading) {
                        if (state.sections.isEmpty()) {
                            binding.tasksStatusTextView.isVisible = true
                            binding.tasksStatusTextView.text = getString(R.string.home_tasks_empty)
                            sectionAdapter.submitList(emptyList())
                        } else {
                            sectionAdapter.submitList(state.sections)
                            sectionAdapter.updateCheckedTaskIds(state.taskState.checkedTaskIds)
                        }
                    } else if (!state.showTasks) {
                        sectionAdapter.submitList(emptyList())
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_USER_ID = "extra_user_id"

        fun createIntent(context: Context, userId: String): Intent {
            return Intent(context, UserTasksActivity::class.java)
                .putExtra(EXTRA_USER_ID, userId)
        }
    }

    private class ReadOnlyTaskSectionAdapter(
        private val onTaskDetailClicked: (TaskItem) -> Unit,
    ) :
        ListAdapter<HomeTaskSection, ReadOnlyTaskSectionAdapter.SectionViewHolder>(SectionDiffCallback()) {

        private var checkedTaskIds: Set<String> = emptySet()

        fun updateCheckedTaskIds(newCheckedTaskIds: Set<String>) {
            if (checkedTaskIds == newCheckedTaskIds) return
            checkedTaskIds = newCheckedTaskIds
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
            val binding = ItemHomeTaskSectionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
            return SectionViewHolder(binding, onTaskDetailClicked)
        }

        override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
            holder.bind(getItem(position), checkedTaskIds)
        }

        private class SectionViewHolder(
            private val binding: ItemHomeTaskSectionBinding,
            private val onTaskDetailClicked: (TaskItem) -> Unit,
        ) : RecyclerView.ViewHolder(binding.root) {

            private val taskAdapter = ReadOnlyTaskAdapter(onTaskDetailClicked)

            init {
                binding.cannotOfferRow.root.visibility = View.GONE
                binding.tasksRecyclerView.apply {
                    adapter = taskAdapter
                    layoutManager = LinearLayoutManager(context)
                    isNestedScrollingEnabled = false
                    itemAnimator = null
                }
            }

            fun bind(section: HomeTaskSection, checkedTaskIds: Set<String>) {
                val isSectionChecked = section.tasks.isNotEmpty() &&
                    section.tasks.all { taskItem -> checkedTaskIds.contains(taskItem.id) }
                binding.sectionStatusTextView.renderStatus(isSectionChecked)
                binding.sectionTitleTextView.text = section.title.uppercase()
                if (section.description.isBlank()) {
                    binding.sectionDescriptionTextView.visibility = View.GONE
                } else {
                    binding.sectionDescriptionTextView.visibility = View.VISIBLE
                    binding.sectionDescriptionTextView.text = section.description
                }
                taskAdapter.bindSection(section.category, section.tasks, checkedTaskIds)
            }

            private fun TextView.renderStatus(isChecked: Boolean) {
                setBackgroundResource(
                    if (isChecked) R.drawable.bg_check_circle else R.drawable.bg_empty_circle,
                )
                text = if (isChecked) context.getString(R.string.status_done) else ""
                setTextColor(context.getColor(R.color.navy_base))
            }
        }
    }

    private class SectionDiffCallback : DiffUtil.ItemCallback<HomeTaskSection>() {
        override fun areItemsTheSame(oldItem: HomeTaskSection, newItem: HomeTaskSection): Boolean {
            return oldItem.category == newItem.category
        }

        override fun areContentsTheSame(oldItem: HomeTaskSection, newItem: HomeTaskSection): Boolean {
            return oldItem == newItem
        }
    }

    private class ReadOnlyTaskAdapter(
        private val onTaskDetailClicked: (TaskItem) -> Unit,
    ) : RecyclerView.Adapter<ReadOnlyTaskAdapter.TaskViewHolder>() {

        private var category: String = ""
        private var tasks: List<TaskItem> = emptyList()
        private var checkedTaskIds: Set<String> = emptySet()

        fun bindSection(category: String, tasks: List<TaskItem>, checkedTaskIds: Set<String>) {
            this.category = category
            this.tasks = tasks
            this.checkedTaskIds = checkedTaskIds
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
            val binding = ItemHomeTaskBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
            return TaskViewHolder(binding, onTaskDetailClicked)
        }

        override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
            holder.bind(tasks[position], category, checkedTaskIds)
        }

        override fun getItemCount(): Int = tasks.size

        private class TaskViewHolder(
            private val binding: ItemHomeTaskBinding,
            private val onTaskDetailClicked: (TaskItem) -> Unit,
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(taskItem: TaskItem, category: String, checkedTaskIds: Set<String>) {
                val task = taskItem.task
                val context = binding.root.context
                binding.iconTextView.text = task.emoji.takeIf { it.isNotBlank() } ?: "✦"
                binding.iconTextView.setBackgroundResource(resolveIconBackground(category))
                binding.taskNameTextView.text = task.name
                if (task.shortDescription.isBlank()) {
                    binding.shortDescriptionTextView.visibility = View.GONE
                } else {
                    binding.shortDescriptionTextView.visibility = View.VISIBLE
                    binding.shortDescriptionTextView.text = task.shortDescription
                }
                binding.pointsTextView.visibility = if (task.points > 0) View.VISIBLE else View.GONE
                binding.pointsTextView.text = context.resources.getQuantityString(
                    R.plurals.home_task_points,
                    task.points,
                    task.points,
                )
                val isChecked = checkedTaskIds.contains(taskItem.id)
                binding.statusTextView.renderStatus(isChecked)
                binding.statusTapArea.isClickable = false
                binding.taskRow.isClickable = true
                binding.taskRow.alpha = 1f
                binding.taskRow.setOnClickListener { onTaskDetailClicked(taskItem) }
            }

            private fun TextView.renderStatus(isChecked: Boolean) {
                setBackgroundResource(
                    if (isChecked) R.drawable.bg_check_circle else R.drawable.bg_empty_circle,
                )
                text = if (isChecked) context.getString(R.string.status_done) else ""
                setTextColor(context.getColor(R.color.navy_base))
            }

            private fun resolveIconBackground(category: String): Int {
                return when {
                    category.contains("quran", ignoreCase = true) -> R.drawable.bg_icon_box_teal
                    category.contains("dua", ignoreCase = true) -> R.drawable.bg_icon_box_warm
                    else -> R.drawable.bg_icon_box
                }
            }
        }
    }
}
