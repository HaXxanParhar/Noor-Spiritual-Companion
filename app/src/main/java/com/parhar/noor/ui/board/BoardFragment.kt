package com.parhar.noor.ui.board

import android.app.Dialog
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.parhar.noor.R
import com.parhar.noor.databinding.FragmentBoardBinding
import com.parhar.noor.di.appContainer
import com.parhar.noor.domain.model.LeaderboardEntry
import com.parhar.noor.domain.model.WeekResultSummary
import com.parhar.noor.ui.friends.FriendsActivity
import com.parhar.noor.ui.usertasks.UserTasksActivity
import com.parhar.noor.utils.BaseFragment
import com.parhar.noor.utils.NoorDialogs
import kotlinx.coroutines.launch

class BoardFragment : BaseFragment<FragmentBoardBinding>() {

    private val viewModel: BoardViewModel by viewModels {
        requireContext().appContainer().viewModelFactory
    }
    private lateinit var leaderboardAdapter: LeaderboardUiRenderer.LeaderboardAdapter
    private var weekResultDialog: Dialog? = null

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentBoardBinding = FragmentBoardBinding.inflate(inflater, container, false)

    override fun setupViews() {
        leaderboardAdapter = LeaderboardUiRenderer.LeaderboardAdapter { entry ->
            startActivity(UserTasksActivity.createIntent(requireContext(), entry.uid))
        }
        binding.leaderboardListRecyclerView.apply {
            adapter = leaderboardAdapter
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
        }

        binding.friendsTopTextView.setOnClickListener {
            startActivity(FriendsActivity.createIntent(requireContext()))
        }

        binding.addInviteFriendTextView.setOnClickListener { openInviteFriends() }
        observeViewModel()
    }

    override fun onStart() {
        super.onStart()
        viewModel.refreshWeekState()
    }

    override fun onDestroyView() {
        weekResultDialog?.dismiss()
        weekResultDialog = null
        setBlockingLoading(false)
        super.onDestroyView()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isWeekPreparing.collect { preparing ->
                        setBlockingLoading(
                            preparing,
                            if (preparing) viewModel.preparingWeekMessage else null,
                        )
                    }
                }
                launch {
                    viewModel.boardState.collect { state ->
                        binding.weekTitleTextView.text = state.weekTitle
                        if (!state.countdownText.isNullOrBlank()) {
                            binding.weekCountdownTextView.text = state.countdownText
                            binding.weekCountdownTextView.visibility = View.VISIBLE
                        } else {
                            binding.weekCountdownTextView.visibility = View.GONE
                        }
                        if (state.hasFriends) {
                            showFriendsBoard(state.entries)
                        } else {
                            showNoFriendsState()
                        }
                    }
                }
                launch {
                    viewModel.weekResultEvent.collect { result ->
                        setBlockingLoading(false)
                        showWeekResultDialog(result)
                    }
                }
            }
        }
    }

    private fun showWeekResultDialog(result: WeekResultSummary) {
        if (!isAdded) return
        weekResultDialog?.dismiss()
        weekResultDialog = NoorDialogs.showWeekResult(
            context = requireActivity(),
            result = result,
            onViewLeaderboard = {
                weekResultDialog = null
                startActivity(LeaderBoardActivity.createIntent(requireContext(), result.weekKey))
            },
            onDismiss = {
                weekResultDialog = null
            },
        )
    }

    private fun showFriendsBoard(entries: List<LeaderboardEntry>) {
        binding.noFriendsContainer.visibility = View.GONE
        binding.friendsBoardContainer.visibility = View.VISIBLE
        LeaderboardUiRenderer.renderPodium(binding, entries) { openInviteFriends() }
        leaderboardAdapter.submitEntries(entries)
    }

    private fun showNoFriendsState() {
        binding.friendsBoardContainer.visibility = View.GONE
        binding.noFriendsContainer.visibility = View.VISIBLE
        leaderboardAdapter.submitEntries(emptyList())
        LeaderboardUiRenderer.renderPodium(binding, emptyList()) { openInviteFriends() }
    }

    private fun openInviteFriends() {
        startActivity(Intent(requireContext(), InviteFriendsActivity::class.java))
    }
}
