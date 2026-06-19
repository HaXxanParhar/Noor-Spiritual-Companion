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
import com.parhar.noor.databinding.ActivityTrophiesListBinding
import com.parhar.noor.databinding.ItemAdminTrophyRowBinding
import com.parhar.noor.di.appContainer
import com.parhar.noor.domain.model.Trophy
import com.parhar.noor.utils.BaseActivity
import kotlinx.coroutines.launch

class TrophiesListActivity : BaseActivity<ActivityTrophiesListBinding>() {

    private val viewModel: AdminViewModel by lazy {
        ViewModelProvider(this, appContainer().viewModelFactory)[AdminViewModel::class.java]
    }

    private val trophyAdapter = AdminTrophyAdapter { trophy ->
        startActivity(AddTrophyActivity.createEditIntent(this, trophy))
    }

    override fun inflateBinding(): ActivityTrophiesListBinding =
        ActivityTrophiesListBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.toolbar.backImageView.setOnClickListener { finish() }
        binding.toolbar.toolbarTitleTextView.setText(R.string.admin_trophies_list_title)
        binding.addTrophyTextView.setOnClickListener {
            startActivity(AddTrophyActivity.createAddIntent(this))
        }
        binding.trophiesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.trophiesRecyclerView.adapter = trophyAdapter
    }

    override fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.trophies.collect { trophies ->
                    trophyAdapter.submitList(trophies)
                    binding.emptyTextView.visibility =
                        if (trophies.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent =
            Intent(context, TrophiesListActivity::class.java)
    }
}

private class AdminTrophyAdapter(
    private val onTrophyClicked: (Trophy) -> Unit,
) : ListAdapter<Trophy, AdminTrophyAdapter.TrophyViewHolder>(TrophyDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrophyViewHolder {
        val binding = ItemAdminTrophyRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return TrophyViewHolder(binding, onTrophyClicked)
    }

    override fun onBindViewHolder(holder: TrophyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TrophyViewHolder(
        private val binding: ItemAdminTrophyRowBinding,
        private val onTrophyClicked: (Trophy) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(trophy: Trophy) {
            binding.trophyIconTextView.text = trophy.icon.ifBlank { "🏆" }
            binding.trophyNameTextView.text = trophy.name
            binding.trophyRequirementTextView.text = binding.root.context.getString(
                R.string.admin_trophy_requirement_format,
                trophy.requirement,
            )
            binding.root.setOnClickListener { onTrophyClicked(trophy) }
        }
    }

    private object TrophyDiffCallback : DiffUtil.ItemCallback<Trophy>() {
        override fun areItemsTheSame(oldItem: Trophy, newItem: Trophy): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Trophy, newItem: Trophy): Boolean =
            oldItem == newItem
    }
}
