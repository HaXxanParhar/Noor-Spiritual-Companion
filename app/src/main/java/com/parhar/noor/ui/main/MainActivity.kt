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

import com.parhar.noor.domain.model.MainBanner

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

    private var activeReminderSenderId: String? = null



    override fun inflateBinding(): ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)



    override fun setupViews() {

        appContainer.sessionManager.getUserId()?.let { uid ->

            appContainer.startLeaderboardSyncIfNeeded(uid)

        }



        showFragment(HomeFragment(), R.id.homeTabTextView)

        observeBanners()



        binding.dismissBannerTextView.setOnClickListener { dismissActiveBanner() }



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

        clearBannerAutoHide()

        binding.pointsInfoContainer.animate().cancel()

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



    private fun observeBanners() {

        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {

                viewModel.ensureRemindersObserver()

                viewModel.retryPendingBanners()

                viewModel.bannerEvent.collect { banner ->

                    showBanner(banner)

                }

            }

        }

    }



    private fun showBanner(banner: MainBanner) {

        clearBannerAutoHide()

        val infoContainer = binding.pointsInfoContainer

        infoContainer.animate().cancel()

        infoContainer.alpha = 1f

        infoContainer.visibility = View.VISIBLE



        when (banner) {

            is MainBanner.Favorite -> {

                activeReminderSenderId = null

                binding.tvEmoji.text = banner.banner.emoji.ifBlank { DEFAULT_FAV_EMOJI }

                binding.bannerSenderTextView.visibility = View.GONE

                binding.pointsInfoTextView.text = banner.banner.message

                binding.dismissBannerTextView.visibility = View.GONE

                scheduleAutoHide(senderId = null)

            }

            is MainBanner.Reminder -> {

                activeReminderSenderId = banner.reminder.senderId

                binding.tvEmoji.text = REMINDER_EMOJI

                binding.bannerSenderTextView.visibility = View.VISIBLE

                binding.bannerSenderTextView.text = banner.reminder.sender

                binding.pointsInfoTextView.text = banner.reminder.message

                binding.dismissBannerTextView.visibility = View.VISIBLE

                scheduleAutoHide(senderId = banner.reminder.senderId)

            }

        }

    }



    private fun dismissActiveBanner() {

        val senderId = activeReminderSenderId

        clearBannerAutoHide()

        if (!senderId.isNullOrBlank()) {

            viewModel.deleteReminder(senderId)

        }

        hideBanner(senderId)

    }



    private fun scheduleAutoHide(senderId: String?) {

        infoHideRunnable = Runnable {

            if (!senderId.isNullOrBlank()) {

                viewModel.deleteReminder(senderId)

            }

            hideBanner(senderId)

        }

        binding.pointsInfoContainer.postDelayed(infoHideRunnable, BANNER_VISIBLE_MS)

    }



    private fun hideBanner(senderId: String?) {

        val infoContainer = binding.pointsInfoContainer

        infoContainer.animate()

            .alpha(0f)

            .setDuration(FADE_OUT_DURATION_MS)

            .withEndAction {

                infoContainer.visibility = View.GONE

                activeReminderSenderId = null

                viewModel.onBannerFinished(senderId)

            }

            .start()

    }



    private fun clearBannerAutoHide() {

        infoHideRunnable?.let(binding.pointsInfoContainer::removeCallbacks)

        infoHideRunnable = null

    }



    private companion object {

        private const val FADE_OUT_DURATION_MS = 700L

        private const val BANNER_VISIBLE_MS = 10_000L

        private const val DEFAULT_FAV_EMOJI = "👏"

        private const val REMINDER_EMOJI = "🔔"

    }

}


