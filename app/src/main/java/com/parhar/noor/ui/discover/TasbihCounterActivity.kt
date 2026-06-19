package com.parhar.noor.ui.discover

import com.parhar.noor.R
import com.parhar.noor.databinding.ActivityTasbihCounterBinding
import com.parhar.noor.utils.BaseActivity

class TasbihCounterActivity : BaseActivity<ActivityTasbihCounterBinding>() {

    private var count = 0

    override fun inflateBinding(): ActivityTasbihCounterBinding =
        ActivityTasbihCounterBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.toolbar.backImageView.setOnClickListener { finish() }
        binding.toolbar.toolbarTitleTextView.setText(R.string.tasbih_title)
        binding.tapTextView.setOnClickListener {
            count += 1
            renderCount()
        }
        binding.resetTextView.setOnClickListener {
            count = 0
            renderCount()
        }
    }

    private fun renderCount() {
        binding.countTextView.text = count.toString()
    }
}
