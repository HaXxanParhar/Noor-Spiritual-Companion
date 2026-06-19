package com.parhar.noor.ui.friends

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.parhar.noor.R
import com.parhar.noor.data.repository.RemindResult
import com.parhar.noor.databinding.ActivityFriendsBinding
import com.parhar.noor.databinding.ItemFriendRowBinding
import com.parhar.noor.di.appContainer
import com.parhar.noor.ui.board.InviteFriendsActivity
import com.parhar.noor.ui.profile.ProfileActivity
import com.parhar.noor.utils.AvatarRenderer
import com.parhar.noor.utils.BaseActivity
import com.parhar.noor.utils.NoorDialogs
import kotlinx.coroutines.launch

class FriendsActivity : BaseActivity<ActivityFriendsBinding>() {

    private val viewModel: FriendsViewModel by viewModels {
        appContainer().viewModelFactory
    }

    private val adapter = FriendsAdapter(
        onFriendClicked = { friend ->
            startActivity(ProfileActivity.createIntent(this, friend.uid))
        },
        onRemindClicked = { friend ->
            if (!friend.canRemind) return@FriendsAdapter
            showRemindDialog(friend)
        },
    )

    override fun inflateBinding(): ActivityFriendsBinding =
        ActivityFriendsBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.toolbar.backImageView.setOnClickListener { finish() }
        binding.toolbar.toolbarTitleTextView.setText(R.string.friends_title)
        binding.addFriendTextView.setOnClickListener {
            startActivity(Intent(this, InviteFriendsActivity::class.java))
        }
        binding.friendsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.friendsRecyclerView.adapter = adapter
        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.friends.collect { friends ->
                        adapter.submitList(friends)
                        binding.emptyTextView.visibility =
                            if (friends.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.remindResult.collect { result ->
                        val message = when (result) {
                            RemindResult.Sent -> getString(R.string.friends_remind_sent)
                            RemindResult.Offline -> getString(R.string.friends_remind_offline)
                            RemindResult.PendingUnseen -> getString(R.string.friends_remind_pending_unseen)
                            is RemindResult.Failed -> result.message
                        }
                        Toast.makeText(this@FriendsActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }
                launch {
                    viewModel.isReminding.collect { reminding ->
                        setBlockingLoading(reminding, getString(R.string.friends_sending_remind))
                    }
                }
            }
        }
    }

    private fun showRemindDialog(friend: FriendListItem) {
        NoorDialogs.showRemind(
            context = this,
            titleRes = R.string.friends_remind_dialog_title,
            messageRes = R.string.friends_remind_dialog_message,
            defaultMessage = getString(R.string.friends_remind_default_message),
            onSend = { message -> viewModel.remindFriend(friend.uid, message) },
        )
    }

    companion object {
        fun createIntent(context: Context): Intent =
            Intent(context, FriendsActivity::class.java)
    }
}

private class FriendsAdapter(
    private val onFriendClicked: (FriendListItem) -> Unit,
    private val onRemindClicked: (FriendListItem) -> Unit,
) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {

    private val items = mutableListOf<FriendListItem>()

    fun submitList(friends: List<FriendListItem>) {
        items.clear()
        items.addAll(friends)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = ItemFriendRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return FriendViewHolder(binding, onFriendClicked, onRemindClicked)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class FriendViewHolder(
        private val binding: ItemFriendRowBinding,
        private val onFriendClicked: (FriendListItem) -> Unit,
        private val onRemindClicked: (FriendListItem) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FriendListItem) {
            binding.nameTextView.text = item.name
            AvatarRenderer.apply(
                binding.avatarTextView,
                item.name,
                item.profile?.avatar,
                sizeDp = 44,
            )
            binding.root.setOnClickListener { onFriendClicked(item) }
            binding.remindTextView.alpha = if (item.canRemind) 1f else 0.35f
            binding.remindTextView.isEnabled = item.canRemind
            binding.remindTextView.isClickable = item.canRemind
            binding.remindTextView.setOnClickListener {
                if (item.canRemind) onRemindClicked(item)
            }
        }
    }
}
