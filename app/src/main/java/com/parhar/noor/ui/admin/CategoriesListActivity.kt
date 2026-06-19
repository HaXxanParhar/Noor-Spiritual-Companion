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
import com.parhar.noor.utils.NoorDialogs
import com.parhar.noor.R
import com.parhar.noor.databinding.ActivityCategoriesListBinding
import com.parhar.noor.databinding.ItemAdminCategoryRowBinding
import com.parhar.noor.di.appContainer
import com.parhar.noor.domain.model.Category
import com.parhar.noor.utils.BaseActivity
import kotlinx.coroutines.launch

class CategoriesListActivity : BaseActivity<ActivityCategoriesListBinding>() {

    private val viewModel: AdminViewModel by lazy {
        ViewModelProvider(this, appContainer().viewModelFactory)[AdminViewModel::class.java]
    }

    private var selectedCategory: Category? = null
    private var currentCategories: List<Category> = emptyList()

    private val categoryAdapter = AdminCategoryAdapter(
        onCategoryClicked = { category ->
            startActivity(AddCategoryActivity.createEditIntent(this, category))
        },
        onCategoryLongClicked = { category ->
            selectCategory(category)
            true
        },
        selectedCategoryIdProvider = { selectedCategory?.id },
    )

    override fun inflateBinding(): ActivityCategoriesListBinding =
        ActivityCategoriesListBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.toolbar.backImageView.setOnClickListener { finish() }
        binding.toolbar.toolbarTitleTextView.setText(R.string.admin_categories_list_title)
        binding.addCategoryTextView.setOnClickListener {
            startActivity(AddCategoryActivity.createAddIntent(this))
        }
        binding.closeSelectionTextView.setOnClickListener { clearSelection() }
        binding.deleteCategoryTextView.setOnClickListener { confirmDeleteSelectedCategory() }
        binding.moveUpTextView.setOnClickListener { moveSelectedCategory(up = true) }
        binding.moveDownTextView.setOnClickListener { moveSelectedCategory(up = false) }

        binding.categoriesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.categoriesRecyclerView.adapter = categoryAdapter
    }

    override fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isActionInProgress.collect { inProgress ->
                        if (!inProgress) {
                            setBlockingLoading(false)
                        }
                    }
                }
                launch {
                    viewModel.categoryList.collect { categories ->
                        currentCategories = categories
                        selectedCategory = selectedCategory?.let { selected ->
                            categories.firstOrNull { it.id == selected.id } ?: selected
                        }
                        categoryAdapter.submitList(categories)
                        binding.emptyTextView.visibility = if (categories.isEmpty()) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                        updateActionBarState()
                    }
                }
            }
        }
    }

    private fun selectCategory(category: Category) {
        selectedCategory = category
        binding.selectionActionBar.visibility = View.VISIBLE
        categoryAdapter.notifySelectionChanged()
        updateActionBarState()
    }

    private fun clearSelection() {
        selectedCategory = null
        binding.selectionActionBar.visibility = View.GONE
        categoryAdapter.notifySelectionChanged()
    }

    private fun updateActionBarState() {
        val selected = selectedCategory ?: return
        val index = currentCategories.indexOfFirst { it.id == selected.id }
        if (index < 0) return

        val canMoveUp = index > 0
        val canMoveDown = index < currentCategories.lastIndex

        binding.moveUpTextView.isEnabled = canMoveUp
        binding.moveDownTextView.isEnabled = canMoveDown
        binding.moveUpTextView.alpha = if (canMoveUp) 1f else 0.4f
        binding.moveDownTextView.alpha = if (canMoveDown) 1f else 0.4f
    }

    private fun confirmDeleteSelectedCategory() {
        val selected = selectedCategory ?: return
        NoorDialogs.showConfirm(
            context = this,
            titleRes = R.string.admin_delete_category_title,
            messageRes = R.string.admin_delete_category_message,
            tone = NoorDialogs.ConfirmTone.DESTRUCTIVE,
            onPositive = {
                setBlockingLoading(true, getString(R.string.admin_deleting_category))
                viewModel.deleteCategory(selected.id, selected.category)
                clearSelection()
            },
        )
    }

    private fun moveSelectedCategory(up: Boolean) {
        val selected = selectedCategory ?: return
        if (up) {
            viewModel.moveCategoryUp(selected.id)
        } else {
            viewModel.moveCategoryDown(selected.id)
        }
    }

    companion object {
        fun createIntent(context: Context): Intent =
            Intent(context, CategoriesListActivity::class.java)
    }
}

private class AdminCategoryAdapter(
    private val onCategoryClicked: (Category) -> Unit,
    private val onCategoryLongClicked: (Category) -> Boolean,
    private val selectedCategoryIdProvider: () -> String?,
) : ListAdapter<Category, AdminCategoryAdapter.CategoryViewHolder>(CategoryDiffCallback) {

    private var parentRecyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        parentRecyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        if (parentRecyclerView === recyclerView) {
            parentRecyclerView = null
        }
    }

    fun notifySelectionChanged() {
        val recyclerView = parentRecyclerView ?: return
        for (index in 0 until itemCount) {
            (recyclerView.findViewHolderForAdapterPosition(index) as? CategoryViewHolder)
                ?.refreshSelection()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemAdminCategoryRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(
        private val binding: ItemAdminCategoryRowBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category) {
            val isSelected = selectedCategoryIdProvider() == category.id
            binding.categoryTitleTextView.text = category.title
            binding.categoryNameTextView.text = category.category
            binding.root.setBackgroundResource(
                if (isSelected) R.drawable.bg_quote_card else R.drawable.bg_home_row,
            )
            binding.root.setOnClickListener { onCategoryClicked(category) }
            binding.root.setOnLongClickListener { onCategoryLongClicked(category) }
        }

        fun refreshSelection() {
            val position = bindingAdapterPosition
            if (position in 0 until itemCount) {
                bind(getItem(position))
            }
        }
    }

    private object CategoryDiffCallback : DiffUtil.ItemCallback<Category>() {
        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean =
            oldItem == newItem
    }
}
