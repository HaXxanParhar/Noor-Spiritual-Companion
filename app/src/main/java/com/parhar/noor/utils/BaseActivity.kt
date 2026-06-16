package com.parhar.noor.utils

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    private var _binding: VB? = null
    protected val binding: VB
        get() = requireNotNull(_binding) { "Binding is only valid after onCreate and before onDestroy." }

    abstract fun inflateBinding(): VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = inflateBinding()
        setContentView(binding.root)
        setupViews()
        observeState()
    }

    protected open fun setupViews() = Unit

    protected open fun observeState() = Unit

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}
