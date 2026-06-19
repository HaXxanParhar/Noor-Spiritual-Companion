package com.parhar.noor.utils

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.parhar.noor.databinding.LayoutLoadingOverlayBinding

object LoadingOverlay {

    private const val OVERLAY_TAG = "noor_loading_overlay"

    fun show(activity: Activity, message: String? = null) {
        val decor = activity.window.decorView as ViewGroup
        var overlay = decor.findViewWithTag<View>(OVERLAY_TAG)
        if (overlay == null) {
            val binding = LayoutLoadingOverlayBinding.inflate(LayoutInflater.from(activity), decor, false)
            binding.root.tag = OVERLAY_TAG
            decor.addView(binding.root)
            overlay = binding.root
        }

        val binding = LayoutLoadingOverlayBinding.bind(overlay)
        if (message.isNullOrBlank()) {
            binding.loadingMessageTextView.visibility = View.GONE
        } else {
            binding.loadingMessageTextView.visibility = View.VISIBLE
            binding.loadingMessageTextView.text = message
        }
        overlay.visibility = View.VISIBLE
    }

    fun hide(activity: Activity) {
        val decor = activity.window.decorView as ViewGroup
        decor.findViewWithTag<View>(OVERLAY_TAG)?.visibility = View.GONE
    }

    fun isVisible(activity: Activity): Boolean {
        val decor = activity.window.decorView as ViewGroup
        return decor.findViewWithTag<View>(OVERLAY_TAG)?.visibility == View.VISIBLE
    }
}
