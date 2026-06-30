package com.parhar.noor.ui.splash

import android.content.Intent
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.parhar.noor.R
import com.parhar.noor.data.notifications.FcmTokenManager
import com.parhar.noor.databinding.ActivitySplashBinding
import com.parhar.noor.di.appContainer
import com.parhar.noor.domain.model.Ayat
import com.parhar.noor.ui.auth.LoginActivity
import com.parhar.noor.ui.main.MainActivity
import com.parhar.noor.ui.onboarding.OnboardingActivity
import com.parhar.noor.utils.AyatQuoteBinder
import com.parhar.noor.utils.BaseActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SplashActivity : BaseActivity<ActivitySplashBinding>() {

    private val appContainer by lazy { appContainer() }
    private var syncJob: Job? = null

    override fun inflateBinding(): ActivitySplashBinding =
        ActivitySplashBinding.inflate(layoutInflater)

    override fun setupViews() {
        loadMotivationQuote()
        binding.retryTextView.setOnClickListener {
            startSync()
        }
        startSync()
    }

    private fun loadMotivationQuote() {
        lifecycleScope.launch {
            val ayats = appContainer.ayatsRepository.getSplashAyats()
            if (ayats.isNotEmpty() && isActive && !isActivityGone()) {
                val index = appContainer.sessionManager.consumeNextSplashMotivationQuoteIndex(ayats.size)
                bindAyat(ayats[index])
            }
            runCatching { appContainer.ayatsRepository.syncAyatsFromRemote() }
        }
    }

    private fun bindAyat(ayat: Ayat) {
        AyatQuoteBinder.bind(
            arabicTextView = binding.tvArabic,
            urduTextView = binding.tvUrduTranslation,
            englishTextView = binding.tvEnglishTranslation,
            referenceTextView = binding.tvReference,
            ayat = ayat,
        )
    }

    private fun startSync() {
        syncJob?.cancel()
        showLoading()
        syncJob = lifecycleScope.launch {
            val uid = appContainer.sessionManager.getUserId()
            val synced = appContainer.bootstrapOnSplash(uid)
            if (!isActive || isActivityGone()) return@launch

            if (!synced && appContainer.connectivityMonitor.checkOnline()) {
                showRetry(getString(R.string.splash_sync_failed))
                return@launch
            }
            if (!synced) {
                updateStatus(getString(R.string.splash_offline_cached))
            } else if (!uid.isNullOrBlank() && appContainer.connectivityMonitor.checkOnline()) {
                updateStatus(getString(R.string.splash_syncing_streak))
                runCatching { appContainer.syncSteakOnSplash(uid) }
            }
            if (!uid.isNullOrBlank() && appContainer.connectivityMonitor.checkOnline()) {
                runCatching {
                    FcmTokenManager.registerToken(appContainer.userProfileRepository, uid)
                }
            }
            if (!uid.isNullOrBlank()) {
                appContainer.cacheFriendCount(uid)
            }
            if (!isActive || isActivityGone()) return@launch
            routeNext()
        }
    }

    private fun isActivityGone(): Boolean = isFinishing || isDestroyed

    private fun showLoading() {
        if (isActivityGone()) return
        binding.loadingProgressBar.visibility = View.VISIBLE
        binding.loadingStatusTextView.text = getString(R.string.splash_loading)
        binding.retryTextView.visibility = View.GONE
    }

    private fun updateStatus(message: String) {
        if (isActivityGone()) return
        binding.loadingStatusTextView.text = message
    }

    private fun showRetry(message: String) {
        if (isActivityGone()) return
        binding.loadingProgressBar.visibility = View.GONE
        binding.loadingStatusTextView.text = message
        binding.retryTextView.visibility = View.VISIBLE
    }

    private fun routeNext() {
        if (isActivityGone()) return
        if (!appContainer.sessionManager.hasCompletedOnboarding()) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        if (appContainer.sessionManager.isLoggedIn()) {
            appContainer.sessionManager.getUserId()?.let { uid ->
                appContainer.startLeaderboardSyncIfNeeded(uid)
            }
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    companion object {
        fun createIntent(context: android.content.Context): Intent {
            return Intent(context, SplashActivity::class.java)
        }
    }
}
