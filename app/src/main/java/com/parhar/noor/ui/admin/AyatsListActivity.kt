package com.parhar.noor.ui.admin

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.parhar.noor.R
import com.parhar.noor.databinding.ActivityAyatsListBinding
import com.parhar.noor.databinding.ItemAdminAyatRowBinding
import com.parhar.noor.di.appContainer
import com.parhar.noor.domain.model.Ayat
import com.parhar.noor.utils.AyatQuoteBinder
import com.parhar.noor.utils.BaseActivity
import kotlinx.coroutines.launch

class AyatsListActivity : BaseActivity<ActivityAyatsListBinding>() {

    private val viewModel: AdminViewModel by lazy {
        ViewModelProvider(this, appContainer().viewModelFactory)[AdminViewModel::class.java]
    }

    private val ayatAdapter = AdminAyatAdapter { ayat ->
        startActivity(AddAyatActivity.createEditIntent(this, ayat))
    }

    override fun inflateBinding(): ActivityAyatsListBinding =
        ActivityAyatsListBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.toolbar.backImageView.setOnClickListener { finish() }
        binding.toolbar.toolbarTitleTextView.setText(R.string.admin_ayats_list_title)
        binding.addAyatTextView.setOnClickListener {
            startActivity(AddAyatActivity.createAddIntent(this))
        }
        binding.ayatsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.ayatsRecyclerView.adapter = ayatAdapter
    }

    override fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ayats.collect { ayats ->
                    ayatAdapter.submitList(ayats)
                    binding.emptyTextView.visibility =
                        if (ayats.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent =
            Intent(context, AyatsListActivity::class.java)
    }
}

private class AdminAyatAdapter(
    private val onAyatClicked: (Ayat) -> Unit,
) : ListAdapter<Ayat, AdminAyatAdapter.AyatViewHolder>(AyatDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AyatViewHolder {
        val binding = ItemAdminAyatRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return AyatViewHolder(binding, onAyatClicked)
    }

    override fun onBindViewHolder(holder: AyatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AyatViewHolder(
        private val binding: ItemAdminAyatRowBinding,
        private val onAyatClicked: (Ayat) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(ayat: Ayat) {
            AyatQuoteBinder.bind(
                arabicTextView = binding.ayatArabicTextView,
                urduTextView = binding.ayatUrduTextView,
                englishTextView = binding.ayatEnglishTextView,
                referenceTextView = binding.ayatReferenceTextView,
                ayat = ayat,
            )
            binding.root.setOnClickListener { onAyatClicked(ayat) }
        }
    }

    private object AyatDiffCallback : DiffUtil.ItemCallback<Ayat>() {
        override fun areItemsTheSame(oldItem: Ayat, newItem: Ayat): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Ayat, newItem: Ayat): Boolean =
            oldItem == newItem
    }
}
