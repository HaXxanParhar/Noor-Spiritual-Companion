package com.parhar.noor.ui.discover

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import com.parhar.noor.databinding.FragmentDiscoverBinding
import com.parhar.noor.utils.BaseFragment

class DiscoverFragment : BaseFragment<FragmentDiscoverBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
    ): FragmentDiscoverBinding = FragmentDiscoverBinding.inflate(inflater, container, false)

    override fun setupViews() {
        binding.dailyDuaTextView.setOnClickListener {
            startActivity(Intent(requireContext(), DailyDuaActivity::class.java))
        }
        binding.daroodTextView.setOnClickListener {
            startActivity(Intent(requireContext(), DaroodActivity::class.java))
        }
        binding.tasbihTextView.setOnClickListener {
            startActivity(Intent(requireContext(), TasbihCounterActivity::class.java))
        }
    }
}
