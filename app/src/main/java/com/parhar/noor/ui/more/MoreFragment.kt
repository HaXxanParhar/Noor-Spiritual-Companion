package com.parhar.noor.ui.more

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.parhar.noor.R
import com.parhar.noor.databinding.FragmentMoreBinding
import com.parhar.noor.databinding.LayoutMoreMenuRowBinding
import com.parhar.noor.di.appContainer
import com.parhar.noor.ui.admin.AdminPanelActivity
import com.parhar.noor.ui.auth.LoginActivity
import com.parhar.noor.ui.discover.TasbihCounterActivity
import com.parhar.noor.ui.friends.FriendsActivity
import com.parhar.noor.ui.privacy.PrivacyActivity
import com.parhar.noor.ui.profile.ProfileActivity
import com.parhar.noor.utils.BaseActivity
import com.parhar.noor.utils.BaseFragment
import com.parhar.noor.utils.NoorDialogs
import com.parhar.noor.utils.SessionManager
import kotlinx.coroutines.launch

class MoreFragment : BaseFragment<FragmentMoreBinding>() {

    private val auth = FirebaseAuth.getInstance()
    private val sessionManager by lazy { SessionManager(requireContext()) }
    private val adminRepository by lazy { requireContext().appContainer().adminRepository }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentMoreBinding = FragmentMoreBinding.inflate(inflater, container, false)

    override fun setupViews() {
        configureMenuRows()

        binding.adminPanelTextView.root.setOnClickListener { openAdminPanel() }
        binding.qiblaTextView.root.setOnClickListener {
            startActivity(Intent(requireContext(), QiblaActivity::class.java))
        }
        binding.tasbihTextView.root.setOnClickListener {
            startActivity(Intent(requireContext(), TasbihCounterActivity::class.java))
        }
        binding.profileTextView.root.setOnClickListener {
            startActivity(ProfileActivity.createIntent(requireContext()))
        }
        binding.privacyTextView.root.setOnClickListener {
            startActivity(Intent(requireContext(), PrivacyActivity::class.java))
        }
        binding.medalsTextView.root.setOnClickListener {
            openComingSoon(getString(R.string.more_medals_title))
        }
        binding.friendsTextView.root.setOnClickListener {
            startActivity(FriendsActivity.createIntent(requireContext()))
        }
        binding.prayerRemindersTextView.root.setOnClickListener {
            openComingSoon(getString(R.string.more_prayer_reminders_title))
        }
        binding.locationMethodTextView.root.setOnClickListener {
            openComingSoon(getString(R.string.more_location_method_title))
        }
        binding.languageTextView.root.setOnClickListener {
            openComingSoon(getString(R.string.more_language_title))
        }
        binding.aboutTextView.root.setOnClickListener {
            startActivity(Intent(requireContext(), AboutActivity::class.java))
        }
        binding.logoutTextView.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun configureMenuRows() {
        bindMenuRow(binding.adminPanelTextView, R.string.more_admin_panel, labelColorRes = R.color.gold_primary)
        bindMenuRow(binding.qiblaTextView, R.string.more_qibla_label)
        bindMenuRow(binding.tasbihTextView, R.string.more_tasbih_label)
        bindMenuRow(binding.profileTextView, R.string.more_profile_label)
        bindMenuRow(binding.privacyTextView, R.string.more_privacy_label)
        bindMenuRow(binding.medalsTextView, R.string.more_medals_label)
        bindMenuRow(binding.friendsTextView, R.string.more_friends_label)
        bindMenuRow(
            row = binding.prayerRemindersTextView,
            labelRes = R.string.more_prayer_reminders_label,
            valueRes = R.string.more_prayer_reminders_status,
        )
        bindMenuRow(
            row = binding.locationMethodTextView,
            labelRes = R.string.more_location_method_label,
            valueRes = R.string.more_location_method_status,
        )
        bindMenuRow(
            row = binding.languageTextView,
            labelRes = R.string.more_language_label,
            valueRes = R.string.more_language_status,
        )
        bindMenuRow(binding.aboutTextView, R.string.more_about_label)
    }

    private fun bindMenuRow(
        row: LayoutMoreMenuRowBinding,
        labelRes: Int,
        valueRes: Int? = null,
        labelColorRes: Int = R.color.text_primary,
    ) {
        row.menuLabelTextView.setText(labelRes)
        row.menuLabelTextView.setTextColor(
            ContextCompat.getColor(requireContext(), labelColorRes),
        )
        if (valueRes != null) {
            row.menuValueTextView.setText(valueRes)
            row.menuValueTextView.visibility = View.VISIBLE
        } else {
            row.menuValueTextView.visibility = View.GONE
        }
    }

    override fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                refreshAdminPanelVisibility()
            }
        }
    }

    private suspend fun refreshAdminPanelVisibility() {
        val email = sessionManager.getUserProfile()?.email
            ?: auth.currentUser?.email
            .orEmpty()
        val config = runCatching { adminRepository.fetchAdminConfig() }.getOrNull()
        if (!isAdded || view == null) return
        val isAdmin = config != null && adminRepository.isAdminEmail(email, config.emails)
        binding.adminPanelTextView.root.visibility = if (isAdmin) View.VISIBLE else View.GONE
    }

    private fun openAdminPanel() {
        if (sessionManager.isAdminAuthenticated()) {
            startActivity(Intent(requireContext(), AdminPanelActivity::class.java))
            return
        }
        showAdminPasswordDialog()
    }

    private fun showAdminPasswordDialog() {
        NoorDialogs.showPassword(
            context = requireContext(),
            titleRes = R.string.admin_password_title,
            messageRes = R.string.admin_password_message,
            onPositive = { password -> verifyAdminPassword(password) },
        )
    }

    private fun verifyAdminPassword(enteredPassword: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val hostActivity = requireActivity() as? BaseActivity<*>
            hostActivity?.setBlockingLoading(true, getString(R.string.admin_verifying_access))
            val config = runCatching { adminRepository.fetchAdminConfig() }.getOrNull()
            if (!isAdded || view == null) {
                hostActivity?.setBlockingLoading(false)
                return@launch
            }
            hostActivity?.setBlockingLoading(false)
            if (config != null && enteredPassword == config.password) {
                sessionManager.setAdminAuthenticated(true)
                startActivity(Intent(requireContext(), AdminPanelActivity::class.java))
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.admin_password_incorrect),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun openComingSoon(title: String) {
        startActivity(ComingSoonActivity.createIntent(requireContext(), title))
    }

    private fun showLogoutDialog() {
        NoorDialogs.showConfirm(
            context = requireContext(),
            titleRes = R.string.logout_dialog_title,
            messageRes = R.string.logout_dialog_message,
            tone = NoorDialogs.ConfirmTone.LOGOUT,
            onPositive = { logout() },
        )
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
}
