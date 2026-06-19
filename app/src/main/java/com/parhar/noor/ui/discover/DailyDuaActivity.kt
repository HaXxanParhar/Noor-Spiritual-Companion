package com.parhar.noor.ui.discover

import com.parhar.noor.R
import com.parhar.noor.databinding.ActivityDailyDuaBinding
import com.parhar.noor.utils.BaseActivity

class DailyDuaActivity : BaseActivity<ActivityDailyDuaBinding>() {

    override fun inflateBinding(): ActivityDailyDuaBinding =
        ActivityDailyDuaBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.toolbar.backImageView.setOnClickListener { finish() }
        binding.toolbar.toolbarTitleTextView.setText(R.string.daily_dua_title)
    }
}
