package com.parhar.noor.ui.board

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.parhar.noor.R
import com.parhar.noor.databinding.FragmentBoardBinding
import com.parhar.noor.databinding.ItemLeaderboardRowBinding
import com.parhar.noor.di.appContainer
import com.parhar.noor.domain.model.LeaderboardEntry
import com.parhar.noor.utils.BaseFragment
import kotlinx.coroutines.launch

class BoardFragment : BaseFragment<FragmentBoardBinding>() {

    private val viewModel: BoardViewModel by viewModels {
        requireContext().appContainer().viewModelFactory
    }
    private lateinit var leaderboardAdapter: LeaderboardAdapter

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentBoardBinding = FragmentBoardBinding.inflate(inflater, container, false)

    override fun setupViews() {
        leaderboardAdapter = LeaderboardAdapter()
        binding.leaderboardListRecyclerView.apply {
            adapter = leaderboardAdapter
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
        }

        binding.addFriendTopTextView.setOnClickListener { openInviteFriends() }
        binding.addInviteFriendTextView.setOnClickListener { openInviteFriends() }
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.boardState.collect { state ->
                    binding.dateRangeTextView.text = state.dateRangeLabel
                    if (state.hasFriends) {
                        showFriendsBoard(state.entries)
                    } else {
                        showNoFriendsState()
                    }
                }
            }
        }
    }

    private fun showFriendsBoard(entries: List<LeaderboardEntry>) {
        binding.noFriendsContainer.visibility = View.GONE
        binding.friendsBoardContainer.visibility = View.VISIBLE
        renderPodium(entries)
        leaderboardAdapter.submitEntries(entries)
    }

    private fun showNoFriendsState() {
        binding.friendsBoardContainer.visibility = View.GONE
        binding.noFriendsContainer.visibility = View.VISIBLE
        leaderboardAdapter.submitEntries(emptyList())
        hidePodium()
    }

    private fun renderPodium(entries: List<LeaderboardEntry>) {
        bindPodiumPlace(
            container = binding.firstPlaceContainer,
            avatarView = binding.firstAvatarTextView,
            nameView = binding.firstNameTextView,
            pointsView = binding.firstPointsTextView,
            entry = entries.getOrNull(0),
        )
        bindPodiumPlace(
            container = binding.secondPlaceContainer,
            avatarView = binding.secondAvatarTextView,
            nameView = binding.secondNameTextView,
            pointsView = binding.secondPointsTextView,
            entry = entries.getOrNull(1),
        )
        bindPodiumPlace(
            container = binding.thirdPlaceContainer,
            avatarView = binding.thirdAvatarTextView,
            nameView = binding.thirdNameTextView,
            pointsView = binding.thirdPointsTextView,
            entry = entries.getOrNull(2),
        )
    }

    private fun bindPodiumPlace(
        container: View,
        avatarView: android.widget.TextView,
        nameView: android.widget.TextView,
        pointsView: android.widget.TextView,
        entry: LeaderboardEntry?,
    ) {
        container.visibility = View.VISIBLE

        if (entry != null) {
            avatarView.text = entry.initials
            avatarView.setBackgroundResource(
                when (container.id) {
                    binding.firstPlaceContainer.id -> R.drawable.bg_leaderboard_avatar_gold
                    binding.secondPlaceContainer.id -> R.drawable.bg_leaderboard_avatar_silver
                    else -> R.drawable.bg_leaderboard_avatar_bronze
                },
            )
            nameView.text = entry.name
            pointsView.text = entry.points.toString()
            pointsView.visibility = View.VISIBLE
            container.isClickable = false
            container.setOnClickListener(null)
            return
        }

        avatarView.text = "+"
        avatarView.setBackgroundResource(R.drawable.bg_leaderboard_avatar_default)
        nameView.text = getString(R.string.leaderboard_podium_add_friend)
        pointsView.visibility = View.GONE
        container.isClickable = true
        container.setOnClickListener { openInviteFriends() }
    }

    private fun hidePodium() {
        renderPodium(emptyList())
    }

    private fun openInviteFriends() {
        startActivity(Intent(requireContext(), InviteFriendsActivity::class.java))
    }

    private class LeaderboardAdapter : RecyclerView.Adapter<LeaderboardAdapter.EntryViewHolder>() {

        private val entries = mutableListOf<LeaderboardEntry>()

        fun submitEntries(newEntries: List<LeaderboardEntry>) {
            entries.clear()
            entries.addAll(newEntries)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
            val binding = ItemLeaderboardRowBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
            return EntryViewHolder(binding)
        }

        override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
            holder.bind(entries[position])
        }

        override fun getItemCount(): Int = entries.size

        private class EntryViewHolder(
            private val binding: ItemLeaderboardRowBinding,
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(entry: LeaderboardEntry) {
                binding.rankTextView.text = entry.rank.toString()
                binding.avatarTextView.text = entry.initials
                binding.nameTextView.text = entry.name
                binding.pointsTextView.text = entry.points.toString()
                binding.streakTextView.text = "🔥 ${entry.streak}"
                binding.leaderboardRow.setBackgroundResource(
                    if (entry.isCurrentUser) {
                        R.drawable.bg_leaderboard_you_row
                    } else {
                        R.drawable.bg_home_row
                    },
                )
            }
        }
    }
}
