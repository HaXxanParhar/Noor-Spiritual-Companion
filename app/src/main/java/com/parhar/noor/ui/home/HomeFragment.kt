package com.parhar.noor.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.parhar.noor.R
import com.parhar.noor.databinding.FragmentHomeBinding
import com.parhar.noor.databinding.ItemHomeTaskBinding
import com.parhar.noor.databinding.ItemHomeTaskSectionBinding
import com.parhar.noor.di.appContainer
import com.parhar.noor.domain.model.HomeTaskSection
import com.parhar.noor.domain.model.TaskItem
import com.parhar.noor.ui.profile.ProfileActivity
import com.parhar.noor.utils.AvatarRenderer
import com.parhar.noor.utils.BaseFragment
import com.parhar.noor.utils.DateKeyUtils
import com.parhar.noor.utils.TaskStatsCalculator
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private val viewModel: HomeViewModel by viewModels {
        requireContext().appContainer().viewModelFactory
    }
    private val auth = FirebaseAuth.getInstance()
    private var pointsInfoHideRunnable: Runnable? = null
    private var lastRenderedStreak: Int? = null
    private lateinit var sectionAdapter: TaskSectionAdapter

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentHomeBinding = FragmentHomeBinding.inflate(inflater, container, false)

    override fun setupViews() {
        sectionAdapter = TaskSectionAdapter(
            onTaskClicked = ::onTaskClicked,
            onCannotOfferClicked = ::onCannotOfferClicked,
        )
        binding.tasksContainer.apply {
            adapter = sectionAdapter
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
            itemAnimator = null
        }

        binding.avatar.setOnClickListener {
            startActivity(ProfileActivity.createIntent(requireContext()))
        }
        binding.dateNavigator.previousDateTextView.setOnClickListener { viewModel.goToPreviousDate() }
        binding.dateNavigator.nextDateTextView.setOnClickListener { viewModel.goToNextDate() }
        renderDate()
        observeViewModel()
        currentUserId()?.let(viewModel::setUserId)
    }

    override fun onStart() {
        super.onStart()
        requireContext().appContainer().syncCoordinator.startFavoriteSync()
    }

    override fun onStop() {
        requireContext().appContainer().syncCoordinator.stopFavoriteSync()
        super.onStop()
    }

    override fun onDestroyView() {
        pointsInfoHideRunnable?.let(binding.pointsInfoContainer::removeCallbacks)
        binding.pointsInfoContainer.animate().cancel()
        pointsInfoHideRunnable = null
        lastRenderedStreak = null
        super.onDestroyView()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.sections.collect { sections ->
                        renderTaskSections(sections)
                    }
                }
                launch {
                    viewModel.todayTaskState.collect { state ->
                        sectionAdapter.updateCheckedTaskIds(state.checkedTaskIds)
                    }
                }
                launch {
                    combine(
                        viewModel.showCannotOfferOption,
                        viewModel.cannotOffer,
                        viewModel.isPrimaryPrayerLocked,
                        viewModel.canCheckSelectedDate,
                    ) { showOption, cannotOffer, isLocked, canCheck ->
                        CannotOfferUiState(
                            showOption = showOption && canCheck,
                            cannotOffer = cannotOffer,
                            isPrimaryLocked = isLocked,
                        )
                    }.collect { uiState ->
                        sectionAdapter.updateCannotOfferUiState(uiState)
                    }
                }
                launch {
                    viewModel.canCheckSelectedDate.collect { canCheck ->
                        sectionAdapter.updateCanCheckTasks(canCheck)
                    }
                }
                launch {
                    viewModel.isTasksLoading.collect { isLoading ->
                        binding.tasksShimmerContainer.visibility =
                            if (isLoading) View.VISIBLE else View.GONE
                        binding.tasksContainer.visibility =
                            if (isLoading) View.GONE else View.VISIBLE
                        if (isLoading) {
                            binding.tasksStatusTextView.visibility = View.GONE
                            binding.tasksShimmerContainer.startShimmer()
                        } else {
                            binding.tasksShimmerContainer.stopShimmer()
                        }
                    }
                }
                launch {
                    viewModel.taskStats.collect { stats ->
                        binding.todayPointsTextView.text = getString(
                            R.string.home_today_points_format,
                            stats.todayPoints,
                        )
                        renderStreakIfChanged(stats.streak)
                        binding.weeklyPointsTextView.text = getString(
                            R.string.home_weekly_points_format,
                            stats.weeklyPoints,
                        )
                        binding.allTimePointsTextView.text = getString(
                            R.string.home_all_time_points_format,
                            stats.allTimePoints,
                        )
                    }
                }
                launch {
                    combine(
                        viewModel.userName,
                        viewModel.userProfile,
                    ) { name, profile ->
                        name to profile
                    }.collect { (name, profile) ->
                        val displayName = name
                            ?: auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
                            ?: "Abdullah"
                        binding.userNameTextView.text = "$displayName!"
                        AvatarRenderer.apply(
                            binding.avatar,
                            displayName,
                            profile?.avatar,
                            sizeDp = 48,
                        )
                    }
                }
                launch {
                    viewModel.pointsInfoEvent.collect { todayPoints ->
                        showPointsInfo(todayPoints)
                    }
                }
                launch {
                    viewModel.errorMessage.collect { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                }
                launch {
                    viewModel.selectedDateKey.collect { dateKey ->
                        binding.dateNavigator.selectedDateTextView.text =
                            DateKeyUtils.formatDisplayDate(dateKey)
                    }
                }
            }
        }
    }

    private fun onTaskClicked(taskItem: TaskItem) {
        val isChecked = viewModel.todayTaskState.value.checkedTaskIds.contains(taskItem.id)
        viewModel.toggleTask(
            taskId = taskItem.id,
            points = taskItem.task.points,
            isCurrentlyChecked = isChecked,
        )
    }

    private fun onCannotOfferClicked() {
        viewModel.toggleCannotOffer(!viewModel.cannotOffer.value)
    }

    private fun renderTaskSections(sections: List<HomeTaskSection>) {
        if (sections.isEmpty()) {
            binding.tasksStatusTextView.visibility = View.VISIBLE
            binding.tasksStatusTextView.text = getString(R.string.home_tasks_empty)
            sectionAdapter.submitList(emptyList())
            return
        }

        binding.tasksStatusTextView.visibility = View.GONE
        sectionAdapter.submitList(sections)
    }

    private fun renderStreakIfChanged(streak: Int) {
        if (lastRenderedStreak == streak) return
        lastRenderedStreak = streak
        renderStreak(streak)
    }

    private fun renderStreak(streak: Int) {
        val isActive = streak > 0
        val activeColor = requireContext().getColor(R.color.gold_primary)
        val inactiveColor = requireContext().getColor(R.color.text_muted)

        binding.streakContainer.setBackgroundResource(
            if (isActive) R.drawable.bg_home_streak else R.drawable.bg_home_streak_disabled,
        )
        binding.streakCountTextView.text = streak.toString()
        binding.streakDaysTextView.text = getString(R.string.home_streak_days_format, streak)
        binding.streakHintTextView.text = getString(
            if (isActive) R.string.home_streak_hint else R.string.home_streak_hint_empty,
        )
        binding.streakFireTextView.alpha = if (isActive) 1f else 0.35f

        val textColor = if (isActive) activeColor else inactiveColor
        binding.streakLabelTextView.setTextColor(textColor)
        binding.streakCountTextView.setTextColor(textColor)
        binding.streakDaysTextView.setTextColor(textColor)
        binding.streakHintTextView.setTextColor(textColor)
    }

    private fun showPointsInfo(todayPoints: Int) {
        showInfoView(
            message = resources.getQuantityString(
                R.plurals.home_points_info,
                todayPoints,
                todayPoints,
            ),
            emoji = DEFAULT_INFO_EMOJI,
            visibleDurationMs = POINTS_INFO_VISIBLE_MS,
        )
    }

    private fun showInfoView(message: String, emoji: String, visibleDurationMs: Long) {
        val infoContainer = binding.pointsInfoContainer
        pointsInfoHideRunnable?.let(infoContainer::removeCallbacks)
        binding.tvEmoji.text = emoji
        binding.pointsInfoTextView.text = message
        infoContainer.animate().cancel()
        infoContainer.alpha = 1f
        infoContainer.visibility = View.VISIBLE

        pointsInfoHideRunnable = Runnable {
            infoContainer.animate()
                .alpha(0f)
                .setDuration(FADE_OUT_DURATION_MS)
                .withEndAction {
                    infoContainer.visibility = View.GONE
                }
                .start()
        }
        infoContainer.postDelayed(pointsInfoHideRunnable, visibleDurationMs)
    }

    private fun renderDate() {
        binding.dateTextView.text = "${formatIslamicDate()}\n${formatGregorianDate()}"
    }

    private fun formatGregorianDate(): String {
        return SimpleDateFormat("EEEE, d MMM yyyy • h:mm a", Locale.getDefault()).format(Date())
    }

    private fun currentUserId(): String? {
        return auth.currentUser?.uid
            ?: requireContext().appContainer().sessionManager.getUserProfile()?.uid?.takeIf { it.isNotBlank() }
    }

    private fun formatIslamicDate(): String {
        val calendar = android.icu.util.IslamicCalendar()
        val day = calendar.get(android.icu.util.Calendar.DAY_OF_MONTH)
        val month = ISLAMIC_MONTHS[calendar.get(android.icu.util.Calendar.MONTH)]
        val year = calendar.get(android.icu.util.Calendar.YEAR)
        return "$day $month $year AH"
    }

    private data class CannotOfferUiState(
        val showOption: Boolean = false,
        val cannotOffer: Boolean = false,
        val isPrimaryLocked: Boolean = false,
    )

    private class TaskSectionAdapter(
        private val onTaskClicked: (TaskItem) -> Unit,
        private val onCannotOfferClicked: () -> Unit,
    ) : ListAdapter<HomeTaskSection, TaskSectionAdapter.SectionViewHolder>(SectionDiffCallback()) {

        private var checkedTaskIds: Set<String> = emptySet()
        private var cannotOfferUiState = CannotOfferUiState()
        private var canCheckTasks: Boolean = true
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

        fun updateCheckedTaskIds(newCheckedTaskIds: Set<String>) {
            if (checkedTaskIds == newCheckedTaskIds) return
            checkedTaskIds = newCheckedTaskIds
            refreshVisibleTaskRows()
        }

        fun updateCannotOfferUiState(uiState: CannotOfferUiState) {
            if (cannotOfferUiState == uiState) return
            cannotOfferUiState = uiState
            refreshVisibleTaskRows()
        }

        fun updateCanCheckTasks(canCheck: Boolean) {
            if (canCheckTasks == canCheck) return
            canCheckTasks = canCheck
            refreshVisibleTaskRows()
        }

        private fun refreshVisibleTaskRows() {
            val recyclerView = parentRecyclerView ?: return
            for (index in 0 until itemCount) {
                (recyclerView.findViewHolderForAdapterPosition(index) as? SectionViewHolder)
                    ?.refresh(checkedTaskIds, cannotOfferUiState, canCheckTasks)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
            val binding = ItemHomeTaskSectionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
            return SectionViewHolder(binding, onTaskClicked, onCannotOfferClicked)
        }

        override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
            holder.bind(getItem(position), checkedTaskIds, cannotOfferUiState, canCheckTasks)
        }

        private class SectionViewHolder(
            private val binding: ItemHomeTaskSectionBinding,
            private val onTaskClicked: (TaskItem) -> Unit,
            private val onCannotOfferClicked: () -> Unit,
        ) : RecyclerView.ViewHolder(binding.root) {

            private val taskAdapter = TaskAdapter(onTaskClicked)
            private var boundSection: HomeTaskSection? = null

            init {
                binding.tasksRecyclerView.apply {
                    adapter = taskAdapter
                    layoutManager = LinearLayoutManager(context)
                    isNestedScrollingEnabled = false
                    itemAnimator = null
                }
            }

            fun bind(
                section: HomeTaskSection,
                checkedTaskIds: Set<String>,
                cannotOfferUiState: CannotOfferUiState,
                canCheckTasks: Boolean,
            ) {
                boundSection = section
                val isPrimarySection = section.category.equals(PRIMARY_SECTION_NAME, ignoreCase = true)
                val showCannotOffer = isPrimarySection && cannotOfferUiState.showOption

                binding.cannotOfferRow.root.visibility = if (showCannotOffer) View.VISIBLE else View.GONE
                if (showCannotOffer) {
                    binding.cannotOfferRow.cannotOfferStatusTextView.renderStatus(cannotOfferUiState.cannotOffer)
                    binding.cannotOfferRow.root.setOnClickListener { onCannotOfferClicked() }
                } else {
                    binding.cannotOfferRow.root.setOnClickListener(null)
                }

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
                taskAdapter.bindSection(
                    category = section.category,
                    tasks = section.tasks,
                    checkedTaskIds = checkedTaskIds,
                    isPrimaryLocked = isPrimarySection && cannotOfferUiState.isPrimaryLocked,
                    isReadOnly = !canCheckTasks,
                )
            }

            fun refresh(
                checkedTaskIds: Set<String>,
                cannotOfferUiState: CannotOfferUiState,
                canCheckTasks: Boolean,
            ) {
                val section = boundSection ?: return
                bind(section, checkedTaskIds, cannotOfferUiState, canCheckTasks)
            }

            private fun TextView.renderStatus(isChecked: Boolean) {
                setBackgroundResource(
                    if (isChecked) R.drawable.bg_check_circle else R.drawable.bg_empty_circle,
                )
                text = if (isChecked) context.getString(R.string.status_done) else ""
                setTextColor(context.getColor(R.color.navy_base))
            }

            private companion object {
                private const val PRIMARY_SECTION_NAME = "Primary"
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

    private class TaskAdapter(
        private val onTaskClicked: (TaskItem) -> Unit,
    ) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

        var currentSection: TaskSectionSnapshot? = null
            private set

        private var category: String = ""
        private var tasks: List<TaskItem> = emptyList()
        private var checkedTaskIds: Set<String> = emptySet()
        private var isPrimaryLocked: Boolean = false
        private var isReadOnly: Boolean = false

        fun bindSection(
            category: String,
            tasks: List<TaskItem>,
            checkedTaskIds: Set<String>,
            isPrimaryLocked: Boolean,
            isReadOnly: Boolean,
        ) {
            val categoryChanged = this.category != category
            val tasksChanged = this.tasks != tasks
            val lockChanged = this.isPrimaryLocked != isPrimaryLocked
            val readOnlyChanged = this.isReadOnly != isReadOnly
            this.category = category
            this.tasks = tasks
            this.checkedTaskIds = checkedTaskIds
            this.isPrimaryLocked = isPrimaryLocked
            this.isReadOnly = isReadOnly
            currentSection = TaskSectionSnapshot(category, tasks)

            when {
                categoryChanged || tasksChanged || lockChanged || readOnlyChanged -> notifyDataSetChanged()
                else -> notifyItemRangeChanged(0, itemCount, PAYLOAD_CHECKED)
            }
        }

        fun updateCheckedTaskIds(newCheckedTaskIds: Set<String>) {
            if (checkedTaskIds == newCheckedTaskIds) return
            checkedTaskIds = newCheckedTaskIds
            notifyItemRangeChanged(0, itemCount, PAYLOAD_CHECKED)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
            val binding = ItemHomeTaskBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
            return TaskViewHolder(binding, onTaskClicked)
        }

        override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
            holder.bindFull(tasks[position], category, checkedTaskIds, isPrimaryLocked, isReadOnly)
        }

        override fun onBindViewHolder(holder: TaskViewHolder, position: Int, payloads: MutableList<Any>) {
            if (payloads.contains(PAYLOAD_CHECKED)) {
                holder.bindCheckedState(tasks[position], checkedTaskIds, isPrimaryLocked, isReadOnly)
            } else {
                super.onBindViewHolder(holder, position, payloads)
            }
        }

        override fun getItemCount(): Int = tasks.size

        private class TaskViewHolder(
            private val binding: ItemHomeTaskBinding,
            private val onTaskClicked: (TaskItem) -> Unit,
        ) : RecyclerView.ViewHolder(binding.root) {

            private var boundTaskId: String? = null

            fun bindFull(
                taskItem: TaskItem,
                category: String,
                checkedTaskIds: Set<String>,
                isPrimaryLocked: Boolean,
                isReadOnly: Boolean,
            ) {
                val task = taskItem.task
                val context = binding.root.context
                boundTaskId = taskItem.id

                binding.iconTextView.text = task.emoji.takeIf { it.isNotBlank() } ?: DEFAULT_TASK_ICON
                binding.iconTextView.setBackgroundResource(resolveIconBackground(category))
                binding.taskNameTextView.text = task.name
                binding.pointsTextView.visibility = if (task.points > 0) View.VISIBLE else View.GONE
                binding.pointsTextView.text = context.resources.getQuantityString(
                    R.plurals.home_task_points,
                    task.points,
                    task.points,
                )
                bindCheckedState(taskItem, checkedTaskIds, isPrimaryLocked, isReadOnly)
            }

            fun bindCheckedState(
                taskItem: TaskItem,
                checkedTaskIds: Set<String>,
                isPrimaryLocked: Boolean,
                isReadOnly: Boolean,
            ) {
                boundTaskId = taskItem.id
                val isChecked = checkedTaskIds.contains(taskItem.id)
                binding.statusTextView.renderStatus(isChecked)
                val isInteractive = !isPrimaryLocked && !isReadOnly
                binding.taskRow.alpha = if (isInteractive) 1f else 0.65f
                binding.taskRow.isClickable = isInteractive
                binding.taskRow.setOnClickListener(
                    if (isInteractive) {
                        { onTaskClicked(taskItem) }
                    } else {
                        null
                    },
                )
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

        private companion object {
            private const val PAYLOAD_CHECKED = "checked"
            private const val DEFAULT_TASK_ICON = "✦"
        }
    }

    private data class TaskSectionSnapshot(
        val category: String,
        val tasks: List<TaskItem>,
    )

    private companion object {
        private const val FADE_OUT_DURATION_MS = 700L
        private const val POINTS_INFO_VISIBLE_MS = 2200L
        private const val DEFAULT_INFO_EMOJI = "👏"
        private val ISLAMIC_MONTHS = listOf(
            "Muharram",
            "Safar",
            "Rabi al-Awwal",
            "Rabi al-Thani",
            "Jumada al-Awwal",
            "Jumada al-Thani",
            "Rajab",
            "Sha'ban",
            "Ramadan",
            "Shawwal",
            "Dhul-Qadah",
            "Dhul-Hijjah",
        )
    }
}
