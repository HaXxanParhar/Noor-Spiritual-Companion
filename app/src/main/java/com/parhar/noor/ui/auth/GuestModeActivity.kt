package com.parhar.noor.ui.auth

import com.parhar.noor.databinding.ActivityGuestModeBinding
import com.parhar.noor.utils.BaseActivity

class GuestModeActivity : BaseActivity<ActivityGuestModeBinding>() {

    override fun inflateBinding(): ActivityGuestModeBinding =
        ActivityGuestModeBinding.inflate(layoutInflater)
}
