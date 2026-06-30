package com.parhar.noor.ui.board

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.parhar.noor.R
import com.parhar.noor.databinding.LayoutLeaderboardContentBinding
import com.parhar.noor.databinding.ItemLeaderboardRowBinding
import com.parhar.noor.domain.model.LeaderboardEntry
import com.parhar.noor.utils.AvatarRenderer

internal object LeaderboardUiRenderer {

    fun renderPodium(
        contentBinding: LayoutLeaderboardContentBinding,
        entries: List<LeaderboardEntry>,
        onInviteFriends: () -> Unit,
    ) {
        bindPodiumPlace(
            container = contentBinding.firstPlaceContainer,
            avatarView = contentBinding.firstAvatarTextView,
            nameView = contentBinding.firstNameTextView,
            pointsView = contentBinding.firstPointsTextView,
            entry = entries.getOrNull(0),
            avatarSizeDp = 52,
            onInviteFriends = onInviteFriends,
        )
        bindPodiumPlace(
            container = contentBinding.secondPlaceContainer,
            avatarView = contentBinding.secondAvatarTextView,
            nameView = contentBinding.secondNameTextView,
            pointsView = contentBinding.secondPointsTextView,
            entry = entries.getOrNull(1),
            avatarSizeDp = 44,
            onInviteFriends = onInviteFriends,
        )
        bindPodiumPlace(
            container = contentBinding.thirdPlaceContainer,
            avatarView = contentBinding.thirdAvatarTextView,
            nameView = contentBinding.thirdNameTextView,
            pointsView = contentBinding.thirdPointsTextView,
            entry = entries.getOrNull(2),
            avatarSizeDp = 44,
            onInviteFriends = onInviteFriends,
        )
    }

    private fun bindPodiumPlace(
        container: View,
        avatarView: TextView,
        nameView: TextView,
        pointsView: TextView,
        entry: LeaderboardEntry?,
        avatarSizeDp: Int,
        onInviteFriends: () -> Unit,
    ) {
        container.visibility = View.VISIBLE
        val context = container.context

        if (entry != null) {
            AvatarRenderer.apply(
                textView = avatarView,
                name = entry.name,
                avatar = entry.avatar,
                sizeDp = avatarSizeDp,
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
        nameView.text = context.getString(R.string.leaderboard_podium_add_friend)
        pointsView.visibility = View.GONE
        container.isClickable = true
        container.setOnClickListener { onInviteFriends() }
    }

    class LeaderboardAdapter(
        private val onEntryClicked: (LeaderboardEntry) -> Unit,
    ) : RecyclerView.Adapter<LeaderboardAdapter.EntryViewHolder>() {

        private val entries = mutableListOf<LeaderboardEntry>()

        fun submitEntries(newEntries: List<LeaderboardEntry>) {
            entries.clear()
            entries.addAll(newEntries)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): EntryViewHolder {
            val binding = ItemLeaderboardRowBinding.inflate(
                android.view.LayoutInflater.from(parent.context),
                parent,
                false,
            )
            return EntryViewHolder(binding, onEntryClicked)
        }

        override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
            holder.bind(entries[position])
        }

        override fun getItemCount(): Int = entries.size

        class EntryViewHolder(
            private val binding: ItemLeaderboardRowBinding,
            private val onEntryClicked: (LeaderboardEntry) -> Unit,
        ) : RecyclerView.ViewHolder(binding.root) {
            fun bind(entry: LeaderboardEntry) {
                binding.rankTextView.text = entry.rank.toString()
                AvatarRenderer.apply(
                    binding.avatarTextView,
                    entry.name,
                    entry.avatar,
                    sizeDp = 40,
                )
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
                binding.leaderboardRow.setOnClickListener { onEntryClicked(entry) }
            }
        }
    }
}
