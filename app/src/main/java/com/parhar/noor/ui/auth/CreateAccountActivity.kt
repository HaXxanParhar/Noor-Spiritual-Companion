package com.parhar.noor.ui.auth

import android.content.Intent
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.parhar.noor.R
import com.parhar.noor.databinding.ActivityCreateAccountBinding
import com.parhar.noor.di.appContainer
import com.parhar.noor.ui.splash.SplashActivity
import com.parhar.noor.utils.BaseActivity
import kotlinx.coroutines.launch

class CreateAccountActivity : BaseActivity<ActivityCreateAccountBinding>() {

    private val auth = FirebaseAuth.getInstance()
    private val viewModel: AuthViewModel by viewModels {
        appContainer().viewModelFactory
    }
    private var selectedGender: String? = null

    override fun inflateBinding(): ActivityCreateAccountBinding =
        ActivityCreateAccountBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.maleTextView.setOnClickListener {
            selectGender(GENDER_MALE)
        }
        binding.femaleTextView.setOnClickListener {
            selectGender(GENDER_FEMALE)
        }
        binding.createAccountTextView.setOnClickListener {
            createAccount()
        }
        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        setLoading(isLoading)
                    }
                }
                launch {
                    viewModel.statusMessage.collect { message ->
                        if (!message.isNullOrBlank()) {
                            binding.statusTextView.text = message
                        }
                    }
                }
                launch {
                    viewModel.accountCreated.collect { created ->
                        if (created) {
                            startActivity(SplashActivity.createIntent(this@CreateAccountActivity))
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun selectGender(gender: String) {
        selectedGender = gender
        binding.maleTextView.setBackgroundResource(
            if (gender == GENDER_MALE) R.drawable.bg_primary_action else R.drawable.bg_secondary_action,
        )
        binding.femaleTextView.setBackgroundResource(
            if (gender == GENDER_FEMALE) R.drawable.bg_primary_action else R.drawable.bg_secondary_action,
        )
        binding.maleTextView.setTextColor(getColor(if (gender == GENDER_MALE) R.color.navy_base else R.color.text_primary))
        binding.femaleTextView.setTextColor(getColor(if (gender == GENDER_FEMALE) R.color.navy_base else R.color.text_primary))
    }

    private fun createAccount() {
        val currentUser = auth.currentUser
        val name = binding.nameEditText.text.toString().trim()
        val gender = selectedGender

        when {
            currentUser == null -> {
                binding.statusTextView.text = "Please sign in with Google first."
                return
            }
            name.isBlank() -> {
                binding.statusTextView.text = "Enter a display name."
                return
            }
            gender.isNullOrBlank() -> {
                binding.statusTextView.text = "Select a gender."
                return
            }
        }

        viewModel.createAccount(
            uid = currentUser.uid,
            email = currentUser.email.orEmpty(),
            name = name,
            gender = gender,
        )
    }

    private fun setLoading(isLoading: Boolean) {
        binding.createAccountTextView.isEnabled = !isLoading
        binding.createAccountTextView.alpha = if (isLoading) 0.5f else 1f
        if (isLoading) {
            binding.statusTextView.text = "Creating account..."
        }
    }

    private companion object {
        private const val GENDER_MALE = "male"
        private const val GENDER_FEMALE = "female"
    }
}
