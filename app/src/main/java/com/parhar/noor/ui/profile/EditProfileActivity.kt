package com.parhar.noor.ui.profile

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.parhar.noor.R
import com.parhar.noor.databinding.ActivityEditProfileBinding
import com.parhar.noor.di.appContainer
import com.parhar.noor.domain.model.UserAvatar
import com.parhar.noor.utils.AvatarRenderer
import com.parhar.noor.utils.BaseActivity
import kotlinx.coroutines.launch

class EditProfileActivity : BaseActivity<ActivityEditProfileBinding>() {

    private val viewModel: EditProfileViewModel by viewModels {
        appContainer().viewModelFactory
    }

    private val createAvatarLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val avatar = CreateAvatarActivity.parseResult(result.data)
            if (avatar != null) {
                viewModel.updateAvatar(avatar)
                renderAvatarPreview()
            }
        }
    }

    override fun inflateBinding(): ActivityEditProfileBinding =
        ActivityEditProfileBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.toolbar.backImageView.setOnClickListener { finish() }
        binding.toolbar.toolbarTitleTextView.setText(R.string.edit_profile_title)
        binding.toolbar.toolbarActionTextView.visibility = android.view.View.VISIBLE
        binding.toolbar.toolbarActionTextView.setText(R.string.edit_profile_save)
        binding.toolbar.toolbarActionTextView.setOnClickListener { viewModel.save() }
        binding.avatarPreviewTextView.setOnClickListener { openCreateAvatar() }
        binding.maleTextView.setOnClickListener { selectGender(GENDER_MALE) }
        binding.femaleTextView.setOnClickListener { selectGender(GENDER_FEMALE) }
        binding.saveProfileTextView.setOnClickListener { viewModel.save() }
        viewModel.load()
        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.name.collect { name ->
                        if (binding.nameEditText.text.toString() != name) {
                            binding.nameEditText.setText(name)
                        }
                    }
                }
                launch {
                    viewModel.gender.collect { gender ->
                        selectGender(gender, fromUser = false)
                    }
                }
                launch {
                    viewModel.avatar.collect {
                        renderAvatarPreview()
                    }
                }
                launch {
                    viewModel.isLoading.collect { loading ->
                        setBlockingLoading(loading, getString(R.string.loading_overlay_message))
                    }
                }
                launch {
                    viewModel.saved.collect { saved ->
                        if (saved) {
                            viewModel.clearSaved()
                            finish()
                        }
                    }
                }
            }
        }
        binding.nameEditText.addTextChangedListener(SimpleTextWatcher {
            viewModel.updateName(binding.nameEditText.text.toString())
        })
    }

    private fun renderAvatarPreview() {
        val name = viewModel.name.value.ifBlank { "User" }
        AvatarRenderer.apply(binding.avatarPreviewTextView, name, viewModel.avatar.value, sizeDp = 80)
    }

    private fun openCreateAvatar() {
        createAvatarLauncher.launch(
            CreateAvatarActivity.createIntent(
                context = this,
                fallbackName = viewModel.name.value,
                initialAvatar = viewModel.avatar.value,
            ),
        )
    }

    private fun selectGender(gender: String, fromUser: Boolean = true) {
        if (fromUser) {
            viewModel.updateGender(gender)
        }
        binding.maleTextView.setBackgroundResource(
            if (gender == GENDER_MALE) R.drawable.bg_primary_action else R.drawable.bg_secondary_action,
        )
        binding.femaleTextView.setBackgroundResource(
            if (gender == GENDER_FEMALE) R.drawable.bg_primary_action else R.drawable.bg_secondary_action,
        )
        binding.maleTextView.setTextColor(getColor(if (gender == GENDER_MALE) R.color.navy_base else R.color.text_primary))
        binding.femaleTextView.setTextColor(getColor(if (gender == GENDER_FEMALE) R.color.navy_base else R.color.text_primary))
    }

    companion object {
        private const val GENDER_MALE = "male"
        private const val GENDER_FEMALE = "female"

        fun createIntent(context: Context): Intent =
            Intent(context, EditProfileActivity::class.java)
    }
}
