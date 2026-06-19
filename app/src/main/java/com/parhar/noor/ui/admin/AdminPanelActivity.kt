package com.parhar.noor.ui.admin

import android.view.View
import androidx.core.content.ContextCompat
import com.parhar.noor.R
import com.parhar.noor.databinding.ActivityAdminPanelBinding
import com.parhar.noor.databinding.LayoutMoreMenuRowBinding
import com.parhar.noor.utils.BaseActivity

class AdminPanelActivity : BaseActivity<ActivityAdminPanelBinding>() {

    override fun inflateBinding(): ActivityAdminPanelBinding =
        ActivityAdminPanelBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.toolbar.backImageView.setOnClickListener { finish() }
        binding.toolbar.toolbarTitleTextView.setText(R.string.admin_panel_title)

        bindMenuRow(binding.tasksTextView, R.string.admin_tasks_label)
        bindMenuRow(binding.categoriesTextView, R.string.admin_categories_label)
        bindMenuRow(binding.trophiesTextView, R.string.admin_trophies_label)

        binding.tasksTextView.root.setOnClickListener {
            startActivity(TasksListActivity.createIntent(this))
        }
        binding.categoriesTextView.root.setOnClickListener {
            startActivity(CategoriesListActivity.createIntent(this))
        }
        binding.trophiesTextView.root.setOnClickListener {
//            startActivity(TrophiesListActivity.createIntent(this))
        }
    }

    private fun bindMenuRow(row: LayoutMoreMenuRowBinding, labelRes: Int) {
        row.menuLabelTextView.setText(labelRes)
        row.menuLabelTextView.setTextColor(
            ContextCompat.getColor(this, R.color.text_primary),
        )
        row.menuValueTextView.visibility = View.GONE
    }
}
