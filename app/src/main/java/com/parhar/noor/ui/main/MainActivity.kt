package com.parhar.noor.ui.main

import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.parhar.noor.R
import com.parhar.noor.databinding.ActivityMainBinding
import com.parhar.noor.di.appContainer
import com.parhar.noor.ui.board.BoardFragment
import com.parhar.noor.ui.discover.DiscoverFragment
import com.parhar.noor.ui.home.HomeFragment
import com.parhar.noor.ui.more.MoreFragment
import com.parhar.noor.ui.salah.SalahFragment
import com.parhar.noor.utils.BaseActivity
import kotlinx.coroutines.launch

class MainActivity : BaseActivity<ActivityMainBinding>() {

    private val appContainer by lazy { appContainer() }
    private val viewModel: MainViewModel by viewModels {
        appContainer.viewModelFactory
    }
    private var infoHideRunnable: Runnable? = null

    override fun inflateBinding(): ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)

    override fun setupViews() {
        appContainer.sessionManager.getUserId()?.let { uid ->
            appContainer.syncCoordinator.startLeaderboardSync(uid)
        }

        showFragment(HomeFragment(), R.id.homeTabTextView)
        observeFavoriteInfo()

        binding.homeTabTextView.setOnClickListener {
            showFragment(HomeFragment(), R.id.homeTabTextView)
        }
        binding.salahTabTextView.setOnClickListener {
            showFragment(SalahFragment(), R.id.salahTabTextView)
        }
        binding.boardTabTextView.setOnClickListener {
            showFragment(BoardFragment(), R.id.boardTabTextView)
        }
        binding.discoverTabTextView.setOnClickListener {
            showFragment(DiscoverFragment(), R.id.discoverTabTextView)
        }
        binding.moreTabTextView.setOnClickListener {
            showFragment(MoreFragment(), R.id.moreTabTextView)
        }
    }

    override fun onDestroy() {
        infoHideRunnable?.let(binding.pointsInfoContainer::removeCallbacks)
        binding.pointsInfoContainer.animate().cancel()
        infoHideRunnable = null
        super.onDestroy()
    }

    private fun showFragment(fragment: Fragment, selectedTabId: Int) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
        updateSelectedTab(selectedTabId)
    }

    private fun updateSelectedTab(selectedTabId: Int) {
        val tabs = listOf(
            binding.homeTabTextView,
            binding.salahTabTextView,
            binding.boardTabTextView,
            binding.discoverTabTextView,
            binding.moreTabTextView,
        )
        tabs.forEach { tab ->
            tab.setTextColor(
                if (tab.id == selectedTabId) {
                    getColor(R.color.gold_primary)
                } else {
                    getColor(R.color.text_muted)
                },
            )
        }
    }

    private fun observeFavoriteInfo() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.favoriteBannerPopup.collect { banner ->
                    showFavoriteInfo(
                        message = banner.message,
                        emoji = banner.emoji.ifBlank { DEFAULT_INFO_EMOJI },
                    )
                }
            }
        }
    }

    private fun showFavoriteInfo(message: String, emoji: String) {
        val infoContainer = binding.pointsInfoContainer
        infoHideRunnable?.let(infoContainer::removeCallbacks)
        binding.tvEmoji.text = emoji
        binding.pointsInfoTextView.text = message
        infoContainer.animate().cancel()
        infoContainer.alpha = 1f
        infoContainer.visibility = View.VISIBLE

        infoHideRunnable = Runnable {
            infoContainer.animate()
                .alpha(0f)
                .setDuration(FADE_OUT_DURATION_MS)
                .withEndAction {
                    infoContainer.visibility = View.GONE
                }
                .start()
        }
        infoContainer.postDelayed(infoHideRunnable, FAV_INFO_VISIBLE_MS)
    }

    private companion object {
        private const val FADE_OUT_DURATION_MS = 700L
        private const val FAV_INFO_VISIBLE_MS = 10_000L
        private const val DEFAULT_INFO_EMOJI = "👏"
    }
}
