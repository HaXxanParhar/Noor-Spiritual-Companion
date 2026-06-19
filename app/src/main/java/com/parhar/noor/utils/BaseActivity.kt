package com.parhar.noor.utils

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    private var _binding: VB? = null
    protected val binding: VB
        get() = requireNotNull(_binding) { "Binding is only valid after onCreate and before onDestroy." }

    private var loadingBackPressCallback: OnBackPressedCallback? = null

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

    fun setBlockingLoading(visible: Boolean, message: String? = null) {
        if (visible) {
            LoadingOverlay.show(this, message)
            if (loadingBackPressCallback == null) {
                loadingBackPressCallback = object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() = Unit
                }.also { callback ->
                    onBackPressedDispatcher.addCallback(this, callback)
                }
            } else {
                loadingBackPressCallback?.isEnabled = true
            }
        } else {
            LoadingOverlay.hide(this)
            loadingBackPressCallback?.isEnabled = false
        }
    }

    override fun onDestroy() {
        loadingBackPressCallback?.remove()
        loadingBackPressCallback = null
        LoadingOverlay.hide(this)
        _binding = null
        super.onDestroy()
    }
}
