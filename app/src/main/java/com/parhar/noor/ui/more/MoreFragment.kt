package com.parhar.noor.ui.more

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.parhar.noor.R
import com.parhar.noor.databinding.FragmentMoreBinding
import com.parhar.noor.di.appContainer
import com.parhar.noor.ui.admin.AdminPanelActivity
import com.parhar.noor.ui.auth.LoginActivity
import com.parhar.noor.ui.discover.TasbihCounterActivity
import com.parhar.noor.utils.BaseFragment
import com.parhar.noor.utils.SessionManager

class MoreFragment : BaseFragment<FragmentMoreBinding>() {

    private val auth = FirebaseAuth.getInstance()
    private val sessionManager by lazy { SessionManager(requireContext()) }
    private var languageTapCount = 0
    private var languageTapWindowStartMs = 0L

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentMoreBinding = FragmentMoreBinding.inflate(inflater, container, false)

    override fun setupViews() {
        binding.qiblaTextView.setOnClickListener {
            startActivity(Intent(requireContext(), QiblaActivity::class.java))
        }
        binding.tasbihTextView.setOnClickListener {
            startActivity(Intent(requireContext(), TasbihCounterActivity::class.java))
        }
        binding.profileTextView.setOnClickListener {
            openComingSoon(getString(R.string.more_profile_title))
        }
        binding.medalsTextView.setOnClickListener {
            openComingSoon(getString(R.string.more_medals_title))
        }
        binding.friendsTextView.setOnClickListener {
            openComingSoon(getString(R.string.more_friends_title))
        }
        binding.prayerRemindersTextView.setOnClickListener {
            openComingSoon(getString(R.string.more_prayer_reminders_title))
        }
        binding.locationMethodTextView.setOnClickListener {
            openComingSoon(getString(R.string.more_location_method_title))
        }
        binding.languageTextView.setOnClickListener {
            handleLanguageTap()
        }
        binding.aboutTextView.setOnClickListener {
            startActivity(Intent(requireContext(), AboutActivity::class.java))
        }
        binding.logoutTextView.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun openComingSoon(title: String) {
        startActivity(ComingSoonActivity.createIntent(requireContext(), title))
    }

    private fun handleLanguageTap() {
        val now = System.currentTimeMillis()
        if (languageTapWindowStartMs == 0L || now - languageTapWindowStartMs > LANGUAGE_TAP_WINDOW_MS) {
            languageTapWindowStartMs = now
            languageTapCount = 1
        } else {
            languageTapCount++
        }

        if (languageTapCount >= LANGUAGE_TAP_COUNT_REQUIRED) {
            languageTapCount = 0
            languageTapWindowStartMs = 0L
            startActivity(Intent(requireContext(), AdminPanelActivity::class.java))
        }
    }

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.logout_dialog_title)
            .setMessage(R.string.logout_dialog_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_yes) { _, _ ->
                logout()
            }
            .show()
    }

    private fun logout() {
        auth.signOut()
        requireContext().appContainer().onUserLoggedOut()
        sessionManager.clearUserSession()
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    private companion object {
        private const val LANGUAGE_TAP_COUNT_REQUIRED = 5
        private const val LANGUAGE_TAP_WINDOW_MS = 3_000L
    }
}
