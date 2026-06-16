package com.parhar.noor.ui.salah

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.parhar.noor.R
import com.parhar.noor.databinding.FragmentSalahBinding
import com.parhar.noor.utils.BaseFragment

class SalahFragment : BaseFragment<FragmentSalahBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentSalahBinding = FragmentSalahBinding.inflate(inflater, container, false)

    override fun setupViews() {
        val reminderIntent = Intent(requireContext(), PrayerReminderActivity::class.java)
        listOf(
            R.id.fajrPrayerRow,
            R.id.sunrisePrayerRow,
            R.id.zuhrPrayerRow,
            R.id.asrPrayerRow,
            R.id.maghribPrayerRow,
            R.id.ishaPrayerRow,
            R.id.tahajjudPrayerRow,
        ).forEach { rowId ->
            binding.root.findViewById<View>(rowId).setOnClickListener {
                startActivity(reminderIntent)
            }
        }
    }
}
