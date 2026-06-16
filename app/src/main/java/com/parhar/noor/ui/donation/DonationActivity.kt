package com.parhar.noor.ui.donation

import android.view.ViewGroup
import android.widget.TextView
import com.parhar.noor.R
import com.parhar.noor.databinding.ActivityDonationBinding
import com.parhar.noor.utils.BaseActivity

class DonationActivity : BaseActivity<ActivityDonationBinding>() {

    override fun inflateBinding(): ActivityDonationBinding =
        ActivityDonationBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.backTextView.setOnClickListener {
            finish()
        }
        setupDonationRangeToggles(binding.root)
    }

    private fun setupDonationRangeToggles(parent: ViewGroup) {
        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            if (child is ViewGroup) {
                if (child.childCount == 3 && child.layoutParams.height > 0) {
                    child.setOnClickListener { toggleDonationRow(child) }
                }
                setupDonationRangeToggles(child)
            }
        }
    }

    private fun toggleDonationRow(row: ViewGroup) {
        val statusView = row.getChildAt(row.childCount - 1)
        val isChecked = row.tag as? Boolean ?: ((statusView as? TextView)?.text?.isNotBlank() == true)
        row.tag = !isChecked
        row.setBackgroundResource(if (isChecked) R.drawable.bg_home_row else R.drawable.bg_donation_selected)

        statusView.setBackgroundResource(if (isChecked) R.drawable.bg_empty_circle else R.drawable.bg_check_circle)
        if (statusView is TextView) {
            statusView.text = if (isChecked) "" else getString(R.string.status_done)
            statusView.setTextColor(getColor(R.color.navy_base))
        }
    }
}
