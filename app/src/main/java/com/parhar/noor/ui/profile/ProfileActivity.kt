package com.parhar.noor.ui.profile

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.parhar.noor.R
import com.parhar.noor.databinding.ActivityProfileBinding
import com.parhar.noor.di.appContainer
import com.parhar.noor.utils.AvatarRenderer
import com.parhar.noor.utils.BaseActivity
import com.parhar.noor.utils.NoorDialogs
import kotlinx.coroutines.launch

class ProfileActivity : BaseActivity<ActivityProfileBinding>() {

    private val viewModel: ProfileViewModel by viewModels {
        appContainer().viewModelFactory
    }

    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.loadProfile(targetUserId())
    }

    override fun inflateBinding(): ActivityProfileBinding =
        ActivityProfileBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.toolbar.backImageView.setOnClickListener { finish() }
        binding.toolbar.toolbarTitleTextView.setText(R.string.profile_title)
        binding.removeFriendTextView.setOnClickListener { confirmRemoveFriend() }
        viewModel.loadProfile(targetUserId())
        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        renderState(state)
                    }
                }
                launch {
                    viewModel.isActionInProgress.collect { inProgress ->
                        setBlockingLoading(inProgress, getString(R.string.loading_overlay_message))
                    }
                }
                launch {
                    viewModel.friendRemoved.collect { removed ->
                        if (removed) {
                            viewModel.clearFriendRemoved()
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun renderState(state: ProfileUiState) {
        if (state.isOwnProfile) {
            binding.toolbar.toolbarActionTextView.visibility = View.VISIBLE
            binding.toolbar.toolbarActionTextView.setText(R.string.profile_edit)
            binding.toolbar.toolbarActionTextView.setOnClickListener {
                editProfileLauncher.launch(EditProfileActivity.createIntent(this))
            }
            binding.removeFriendTextView.visibility = View.GONE
        } else {
            binding.toolbar.toolbarActionTextView.visibility = View.GONE
            binding.removeFriendTextView.visibility = View.VISIBLE
        }

        AvatarRenderer.apply(binding.profileAvatarTextView, state.name, state.avatar, sizeDp = 96)
        binding.profileNameTextView.text = state.name
        binding.memberSinceTextView.text = state.memberSince
        binding.streakValueTextView.text = "🔥 ${state.streak}"
        binding.allTimeValueTextView.text = "⭐ ${state.allTimePoints}"
        binding.friendsValueTextView.text = "👥 ${state.friendsCount}"

        binding.goldTrophyTextView.text = "×${state.goldTrophies} Gold"
        binding.silverTrophyTextView.text = "×${state.silverTrophies} Silver"
        binding.bronzeTrophyTextView.text = "×${state.bronzeTrophies} Bronze"

        binding.firstPlaceMedalTextView.text = getString(R.string.profile_medal_first, state.firstPlaceMedals)
        binding.secondPlaceMedalTextView.text = getString(R.string.profile_medal_second, state.secondPlaceMedals)
        binding.thirdPlaceMedalTextView.text = getString(R.string.profile_medal_third, state.thirdPlaceMedals)
        binding.top5MedalTextView.text = getString(R.string.profile_medal_top5, state.top5Finishes)
        binding.top10MedalTextView.text = getString(R.string.profile_medal_top10, state.top10Finishes)

        val progressFraction = if (state.tierMaxPoints > 0) {
            state.tierCurrentPoints.toFloat() / state.tierMaxPoints.toFloat()
        } else {
            0f
        }
        binding.tierProgressFillView.post {
            binding.tierProgressFillView.layoutParams.width =
                (binding.tierProgressFillView.parent as View).width.times(progressFraction).toInt()
            binding.tierProgressFillView.requestLayout()
        }
        binding.tierBronzeLabelTextView.text = "${state.tierBronzeTarget} Bronze"
        binding.tierPointsTextView.text = "${state.tierCurrentPoints} pts"
        binding.tierMaxTextView.text = "${state.tierMaxPoints} max"

        binding.tierBronzeRowTextView.text =
            "Bronze · ${state.tierBronzeTarget} pts · ${getString(R.string.profile_tier_earned)}"
        binding.tierSilverRowTextView.text =
            "Silver · ${state.tierSilverTarget} pts · ${getString(R.string.profile_tier_earned)}"
        val goldAway = (state.tierGoldTarget - state.tierCurrentPoints).coerceAtLeast(0)
        binding.tierGoldRowTextView.text = getString(R.string.profile_tier_needed, state.tierGoldTarget) +
            " · " + getString(R.string.profile_tier_away, goldAway)
    }

    private fun confirmRemoveFriend() {
        NoorDialogs.showConfirm(
            context = this,
            titleRes = R.string.profile_remove_friend_title,
            messageRes = R.string.profile_remove_friend_message,
            tone = NoorDialogs.ConfirmTone.DESTRUCTIVE,
            onPositive = { viewModel.removeFriend() },
        )
    }

    private fun targetUserId(): String {
        return intent.getStringExtra(EXTRA_USER_ID)
            ?: appContainer().sessionManager.getUserId().orEmpty()
    }

    companion object {
        private const val EXTRA_USER_ID = "extra_user_id"

        fun createIntent(context: Context, userId: String? = null): Intent {
            return Intent(context, ProfileActivity::class.java).apply {
                userId?.let { putExtra(EXTRA_USER_ID, it) }
            }
        }
    }
}
