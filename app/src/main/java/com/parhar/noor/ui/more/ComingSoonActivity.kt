package com.parhar.noor.ui.more

import android.content.Context
import android.content.Intent
import com.parhar.noor.R
import com.parhar.noor.databinding.ActivityComingSoonBinding
import com.parhar.noor.utils.BaseActivity

class ComingSoonActivity : BaseActivity<ActivityComingSoonBinding>() {

    override fun inflateBinding(): ActivityComingSoonBinding =
        ActivityComingSoonBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.toolbar.backImageView.setOnClickListener { finish() }
        binding.toolbar.toolbarTitleTextView.setText(R.string.coming_soon_back)
        intent.getStringExtra(EXTRA_TITLE)?.takeIf { it.isNotBlank() }?.let { title ->
            binding.titleTextView.text = title
        }
    }

    companion object {
        private const val EXTRA_TITLE = "extra_title"

        fun createIntent(context: Context, title: String): Intent {
            return Intent(context, ComingSoonActivity::class.java).apply {
                putExtra(EXTRA_TITLE, title)
            }
        }
    }
}
