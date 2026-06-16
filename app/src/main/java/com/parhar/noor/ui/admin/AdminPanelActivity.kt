package com.parhar.noor.ui.admin

import android.content.Intent
import com.parhar.noor.databinding.ActivityAdminPanelBinding
import com.parhar.noor.utils.BaseActivity

class AdminPanelActivity : BaseActivity<ActivityAdminPanelBinding>() {

    override fun inflateBinding(): ActivityAdminPanelBinding =
        ActivityAdminPanelBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.tasksTextView.setOnClickListener {
            startActivity(Intent(this, AddTaskActivity::class.java))
        }
    }
}
