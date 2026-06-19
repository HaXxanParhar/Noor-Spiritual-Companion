package com.parhar.noor.ui.board

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.parhar.noor.R
import com.parhar.noor.databinding.ActivityInviteFriendsBinding
import com.parhar.noor.di.appContainer
import com.parhar.noor.utils.BaseActivity
import kotlinx.coroutines.launch

class InviteFriendsActivity : BaseActivity<ActivityInviteFriendsBinding>() {

    private val viewModel: InviteFriendsViewModel by viewModels {
        appContainer().viewModelFactory
    }

    override fun inflateBinding(): ActivityInviteFriendsBinding =
        ActivityInviteFriendsBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.userIdTextView.text = viewModel.userId.orEmpty()

        binding.toolbar.backImageView.setOnClickListener { finish() }
        binding.toolbar.toolbarTitleTextView.setText(R.string.invite_friends_title)
        binding.copyUserIdTextView.setOnClickListener {
            copyUserId(viewModel.userId)
        }
        binding.addFriendTextView.setOnClickListener {
            addFriend()
        }
        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.statusMessage.collect { message ->
                        if (!message.isNullOrBlank()) {
                            binding.statusTextView.text = message
                        }
                    }
                }
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        setBlockingLoading(
                            isLoading,
                            getString(R.string.loading_overlay_message),
                        )
                    }
                }
            }
        }
    }

    private fun copyUserId(userId: String?) {
        if (userId.isNullOrBlank()) {
            Toast.makeText(this, R.string.invite_user_id_unavailable, Toast.LENGTH_SHORT).show()
            return
        }

        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("user_id", userId))
        Toast.makeText(this, R.string.invite_user_id_copied, Toast.LENGTH_SHORT).show()
    }

    private fun addFriend() {
        val friendId = binding.friendIdEditText.text.toString().trim()
        viewModel.addFriend(friendId)
        if (friendId.isNotBlank()) {
            binding.friendIdEditText.text?.clear()
        }
    }
}
