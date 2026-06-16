package com.parhar.noor.ui.splash

import android.content.Intent
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.parhar.noor.R
import com.parhar.noor.databinding.ActivitySplashBinding
import com.parhar.noor.di.appContainer
import com.parhar.noor.ui.auth.LoginActivity
import com.parhar.noor.ui.main.MainActivity
import com.parhar.noor.ui.onboarding.OnboardingActivity
import com.parhar.noor.utils.BaseActivity
import kotlinx.coroutines.launch

class SplashActivity : BaseActivity<ActivitySplashBinding>() {

    private val appContainer by lazy { appContainer() }

    override fun inflateBinding(): ActivitySplashBinding =
        ActivitySplashBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.retryTextView.setOnClickListener {
            startSync()
        }
        startSync()
    }

    private fun startSync() {
        showLoading()
        lifecycleScope.launch {
            val uid = appContainer.sessionManager.getUserId()
            val synced = appContainer.bootstrapOnSplash(uid)
            if (!synced && appContainer.connectivityMonitor.checkOnline()) {
                showRetry(getString(R.string.splash_sync_failed))
                return@launch
            }
            if (!synced) {
                binding.loadingStatusTextView.text = getString(R.string.splash_offline_cached)
            } else if (!uid.isNullOrBlank() && appContainer.connectivityMonitor.checkOnline()) {
                binding.loadingStatusTextView.text = getString(R.string.splash_syncing_streak)
                runCatching { appContainer.syncSteakOnSplash(uid) }
            }
            routeNext()
        }
    }

    private fun showLoading() {
        binding.loadingProgressBar.visibility = View.VISIBLE
        binding.loadingStatusTextView.text = getString(R.string.splash_loading)
        binding.retryTextView.visibility = View.GONE
    }

    private fun showRetry(message: String) {
        binding.loadingProgressBar.visibility = View.GONE
        binding.loadingStatusTextView.text = message
        binding.retryTextView.visibility = View.VISIBLE
    }

    private fun routeNext() {
        if (!appContainer.sessionManager.hasCompletedOnboarding()) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        if (appContainer.sessionManager.isLoggedIn()) {
            appContainer.sessionManager.getUserId()?.let { uid ->
                appContainer.syncCoordinator.startLeaderboardSync(uid)
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
