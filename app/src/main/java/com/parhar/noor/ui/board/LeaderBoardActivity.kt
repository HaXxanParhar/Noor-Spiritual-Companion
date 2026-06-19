package com.parhar.noor.ui.board

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.parhar.noor.R
import com.parhar.noor.databinding.ActivityLeaderboardBinding
import com.parhar.noor.di.appContainer
import com.parhar.noor.ui.usertasks.UserTasksActivity
import com.parhar.noor.utils.BaseActivity
import kotlinx.coroutines.launch

class LeaderBoardActivity : BaseActivity<ActivityLeaderboardBinding>() {

    private val viewModel: LeaderBoardViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val container = appContainer()
                return LeaderBoardViewModel(
                    application = application,
                    leaderboardRepository = container.leaderboardRepository,
                    sessionManager = container.sessionManager,
                    weekKey = intent.getStringExtra(EXTRA_WEEK_KEY).orEmpty(),
                ) as T
            }
        }
    }

    private lateinit var leaderboardAdapter: LeaderboardUiRenderer.LeaderboardAdapter

    override fun inflateBinding(): ActivityLeaderboardBinding =
        ActivityLeaderboardBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.toolbar.backImageView.setOnClickListener { finish() }
        binding.toolbar.toolbarTitleTextView.setText(R.string.leaderboard_title)
        binding.weekTitleTextView.text = viewModel.weekTitle

        leaderboardAdapter = LeaderboardUiRenderer.LeaderboardAdapter { entry ->
            startActivity(UserTasksActivity.createIntent(this, entry.uid))
        }
        binding.leaderboardContent.leaderboardListRecyclerView.apply {
            adapter = leaderboardAdapter
            layoutManager = LinearLayoutManager(this@LeaderBoardActivity)
            isNestedScrollingEnabled = false
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.entries.collect { entries ->
                    LeaderboardUiRenderer.renderPodium(
                        contentBinding = binding.leaderboardContent,
                        entries = entries,
                        onInviteFriends = {},
                    )
                    leaderboardAdapter.submitEntries(entries)
                }
            }
        }
    }

    companion object {
        const val EXTRA_WEEK_KEY = "extra_week_key"

        fun createIntent(context: Context, weekKey: String): Intent {
            return Intent(context, LeaderBoardActivity::class.java).apply {
                putExtra(EXTRA_WEEK_KEY, weekKey)
            }
        }
    }
}
