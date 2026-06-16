package com.parhar.noor.ui.discover

import com.parhar.noor.databinding.ActivityDaroodBinding
import com.parhar.noor.utils.BaseActivity

class DaroodActivity : BaseActivity<ActivityDaroodBinding>() {

    override fun inflateBinding(): ActivityDaroodBinding =
        ActivityDaroodBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.backTextView.setOnClickListener { finish() }
    }
}
