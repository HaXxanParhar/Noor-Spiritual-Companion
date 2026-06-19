package com.parhar.noor.utils

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null
    protected val binding: VB
        get() = requireNotNull(_binding) { "Binding is only valid between onCreateView and onDestroyView." }

    abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = inflateBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeState()
    }

    protected open fun setupViews() = Unit

    protected open fun observeState() = Unit

    protected fun setBlockingLoading(visible: Boolean, message: String? = null) {
        (activity as? BaseActivity<*>)?.setBlockingLoading(visible, message)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
