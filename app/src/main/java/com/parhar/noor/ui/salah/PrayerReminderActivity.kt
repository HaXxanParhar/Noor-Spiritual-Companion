package com.parhar.noor.ui.salah

import com.parhar.noor.databinding.ActivityPrayerReminderBinding
import com.parhar.noor.utils.BaseActivity

class PrayerReminderActivity : BaseActivity<ActivityPrayerReminderBinding>() {

    override fun inflateBinding(): ActivityPrayerReminderBinding =
        ActivityPrayerReminderBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.backTextView.setOnClickListener { finish() }
    }
}
