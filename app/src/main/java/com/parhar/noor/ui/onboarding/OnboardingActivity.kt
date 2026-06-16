package com.parhar.noor.ui.onboarding

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.parhar.noor.R
import com.parhar.noor.databinding.ActivityOnboardingBinding
import com.parhar.noor.databinding.ItemOnboardingPageBinding
import com.parhar.noor.ui.auth.LoginActivity
import com.parhar.noor.ui.main.MainActivity
import com.parhar.noor.utils.BaseActivity
import com.parhar.noor.utils.SessionManager

class OnboardingActivity : BaseActivity<ActivityOnboardingBinding>() {

    private val sessionManager by lazy { SessionManager(this) }

    private val pages = listOf(
        OnboardingPage(
            titleRes = R.string.onboarding_title_welcome,
            bodyRes = R.string.onboarding_body_welcome,
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_title_how,
            bodyRes = R.string.onboarding_body_how,
            showPoints = true,
        ),
        OnboardingPage(
            titleRes = R.string.onboarding_title_intention,
            bodyRes = R.string.onboarding_body_intention,
            showQuote = true,
        ),
    )

    private var currentPageIndex = 0

    override fun inflateBinding(): ActivityOnboardingBinding =
        ActivityOnboardingBinding.inflate(layoutInflater)

    override fun setupViews() {
        if (sessionManager.hasCompletedOnboarding()) {
            openNextScreen()
            return
        }

        binding.onboardingViewPager.adapter = OnboardingPagerAdapter(pages)
        binding.onboardingViewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    currentPageIndex = position
                    renderControls()
                }
            },
        )
        renderControls()

        binding.primaryActionTextView.setOnClickListener {
            if (currentPageIndex == pages.lastIndex) {
                openLogin()
            } else {
                currentPageIndex += 1
                binding.onboardingViewPager.currentItem = currentPageIndex
            }
        }

        binding.skipTextView.setOnClickListener {
            openLogin()
        }
    }

    private fun renderControls() {
        binding.primaryActionTextView.setText(
            if (currentPageIndex == pages.lastIndex) {
                R.string.action_next_start_journey
            } else {
                R.string.action_next
            },
        )
        binding.skipTextView.visibility =
            if (currentPageIndex == pages.lastIndex) View.INVISIBLE else View.VISIBLE

        updateIndicators()
    }

    private fun updateIndicators() {
        val indicators = listOf(binding.indicatorOne, binding.indicatorTwo, binding.indicatorThree)
        val activeWidth = resources.getDimensionPixelSize(R.dimen.indicator_active_width)
        val inactiveWidth = resources.getDimensionPixelSize(R.dimen.indicator_dot)

        indicators.forEachIndexed { index, indicator ->
            indicator.layoutParams = (indicator.layoutParams as ViewGroup.LayoutParams).apply {
                width = if (index == currentPageIndex) activeWidth else inactiveWidth
            }
            indicator.setBackgroundResource(
                if (index == currentPageIndex) {
                    R.drawable.bg_indicator_active
                } else {
                    R.drawable.bg_indicator_inactive
                },
            )
        }
    }

    private fun openLogin() {
        sessionManager.setOnboardingCompleted()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun openNextScreen() {
        val destination = if (sessionManager.isLoggedIn()) {
            MainActivity::class.java
        } else {
            LoginActivity::class.java
        }
        startActivity(Intent(this, destination))
        finish()
    }

    private data class OnboardingPage(
        val titleRes: Int,
        val bodyRes: Int,
        val showPoints: Boolean = false,
        val showQuote: Boolean = false,
    )

    private class OnboardingPagerAdapter(
        private val pages: List<OnboardingPage>,
    ) : RecyclerView.Adapter<OnboardingPagerAdapter.OnboardingPageViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingPageViewHolder {
            val binding = ItemOnboardingPageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
            return OnboardingPageViewHolder(binding)
        }

        override fun onBindViewHolder(holder: OnboardingPageViewHolder, position: Int) {
            holder.bind(pages[position])
        }

        override fun getItemCount(): Int = pages.size

        private class OnboardingPageViewHolder(
            private val binding: ItemOnboardingPageBinding,
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(page: OnboardingPage) {
                binding.titleTextView.setText(page.titleRes)
                binding.bodyTextView.setText(page.bodyRes)
                binding.pointsTextView.visibility = if (page.showPoints) View.VISIBLE else View.GONE
                binding.quoteContainer.visibility = if (page.showQuote) View.VISIBLE else View.GONE
            }
        }
    }
}
