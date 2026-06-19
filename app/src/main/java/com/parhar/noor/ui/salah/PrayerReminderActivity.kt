package com.parhar.noor.ui.salah

import com.parhar.noor.R
import com.parhar.noor.databinding.ActivityPrayerReminderBinding
import com.parhar.noor.utils.BaseActivity

class PrayerReminderActivity : BaseActivity<ActivityPrayerReminderBinding>() {

    override fun inflateBinding(): ActivityPrayerReminderBinding =
        ActivityPrayerReminderBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.toolbar.backImageView.setOnClickListener { finish() }
        binding.toolbar.toolbarTitleTextView.setText(R.string.prayer_reminders_title)
    }
}
