package com.parhar.noor.ui.privacy

import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.parhar.noor.R
import com.parhar.noor.databinding.ActivityPrivacyBinding
import com.parhar.noor.databinding.LayoutPrivacyOptionCardBinding
import com.parhar.noor.di.appContainer
import com.parhar.noor.domain.model.PrivacyVisibility
import com.parhar.noor.utils.BaseActivity
import kotlinx.coroutines.launch

class PrivacyActivity : BaseActivity<ActivityPrivacyBinding>() {

    private val viewModel: PrivacyViewModel by viewModels {
        appContainer().viewModelFactory
    }

    override fun inflateBinding(): ActivityPrivacyBinding =
        ActivityPrivacyBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.toolbar.backImageView.setOnClickListener { finish() }
        binding.toolbar.toolbarTitleTextView.setText(R.string.privacy_title)
        binding.toolbar.toolbarActionTextView.visibility = android.view.View.GONE

        setupOptionCard(
            card = binding.tasksTodayCard,
            titleRes = R.string.privacy_tasks_today_title,
            descriptionRes = R.string.privacy_tasks_today_desc,
            onNoOne = { viewModel.selectTasksToday(PrivacyVisibility.PRIVATE) },
            onFriends = { viewModel.selectTasksToday(PrivacyVisibility.FRIENDS) },
            selectedValue = { viewModel.tasksToday.value },
        )
        setupOptionCard(
            card = binding.tasksHistoryCard,
            titleRes = R.string.privacy_tasks_history_title,
            descriptionRes = R.string.privacy_tasks_history_desc,
            onNoOne = { viewModel.selectTasksHistory(PrivacyVisibility.PRIVATE) },
            onFriends = { viewModel.selectTasksHistory(PrivacyVisibility.FRIENDS) },
            selectedValue = { viewModel.tasksHistory.value },
        )

        binding.savePrivacyTextView.setOnClickListener { viewModel.save() }
        viewModel.load()
        observeViewModel()
    }

    private fun setupOptionCard(
        card: LayoutPrivacyOptionCardBinding,
        titleRes: Int,
        descriptionRes: Int,
        onNoOne: () -> Unit,
        onFriends: () -> Unit,
        selectedValue: () -> String,
    ) {
        card.privacyOptionTitleTextView.setText(titleRes)
        card.privacyOptionDescriptionTextView.setText(descriptionRes)
        card.noOneOptionTextView.setOnClickListener {
            onNoOne()
            highlightSelection(card, selectedValue())
        }
        card.friendsOptionTextView.setOnClickListener {
            onFriends()
            highlightSelection(card, selectedValue())
        }
        highlightSelection(card, selectedValue())
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.tasksToday.collect { value ->
                        highlightSelection(binding.tasksTodayCard, value)
                    }
                }
                launch {
                    viewModel.tasksHistory.collect { value ->
                        highlightSelection(binding.tasksHistoryCard, value)
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
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun highlightSelection(card: LayoutPrivacyOptionCardBinding, selected: String) {
        val isNoOne = selected == PrivacyVisibility.PRIVATE
        card.noOneOptionTextView.setBackgroundResource(
            if (isNoOne) R.drawable.bg_primary_action else R.drawable.bg_home_row,
        )
        card.friendsOptionTextView.setBackgroundResource(
            if (!isNoOne) R.drawable.bg_primary_action else R.drawable.bg_home_row,
        )
        val selectedColor = ContextCompat.getColor(this, R.color.navy_base)
        val defaultColor = ContextCompat.getColor(this, R.color.text_primary)
        card.noOneOptionTextView.setTextColor(if (isNoOne) selectedColor else defaultColor)
        card.friendsOptionTextView.setTextColor(if (!isNoOne) selectedColor else defaultColor)
    }
}
