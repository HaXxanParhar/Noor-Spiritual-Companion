package com.parhar.noor.ui.more

import com.parhar.noor.R
import com.parhar.noor.databinding.ActivityAboutBinding
import com.parhar.noor.utils.BaseActivity

class AboutActivity : BaseActivity<ActivityAboutBinding>() {

    override fun inflateBinding(): ActivityAboutBinding =
        ActivityAboutBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.toolbar.backImageView.setOnClickListener { finish() }
        binding.toolbar.toolbarTitleTextView.setText(R.string.about_back)
    }
}
