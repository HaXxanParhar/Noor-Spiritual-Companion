package com.parhar.noor.ui.admin

import android.R.layout.simple_spinner_dropdown_item
import android.R.layout.simple_spinner_item
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.parhar.noor.databinding.ActivityAddTaskBinding
import com.parhar.noor.di.appContainer
import com.parhar.noor.utils.BaseActivity
import kotlinx.coroutines.launch

class AddTaskActivity : BaseActivity<ActivityAddTaskBinding>() {

    private val viewModel: AdminViewModel by viewModels {
        appContainer().viewModelFactory
    }

    override fun inflateBinding(): ActivityAddTaskBinding =
        ActivityAddTaskBinding.inflate(layoutInflater)

    override fun setupViews() {
        binding.backTextView.setOnClickListener { finish() }
        binding.addTaskTextView.setOnClickListener { addTask() }
        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.categories.collect { categories ->
                        bindCategories(categories.map { it.category })
                    }
                }
                launch {
                    viewModel.statusMessage.collect { message ->
                        if (!message.isNullOrBlank()) {
                            binding.statusTextView.text = message
                        }
                    }
                }
            }
        }
    }

    private fun bindCategories(categoryNames: List<String>) {
        val adapter = ArrayAdapter(this, simple_spinner_item, categoryNames).apply {
            setDropDownViewResource(simple_spinner_dropdown_item)
        }
        binding.categorySpinner.adapter = adapter

        val hasCategories = categoryNames.isNotEmpty()
        binding.statusTextView.text = if (hasCategories) {
            "Categories loaded."
        } else {
            "No categories found."
        }
        setFormEnabled(hasCategories)
    }

    private fun addTask() {
        val taskName = binding.taskNameEditText.text.toString().trim()
        val points = binding.taskPointsEditText.text.toString().trim().toIntOrNull()
        val category = viewModel.categories.value
            .getOrNull(binding.categorySpinner.selectedItemPosition)
            ?.category
            .orEmpty()

        when {
            taskName.isBlank() -> {
                binding.statusTextView.text = "Enter a task name."
                return
            }
            category.isBlank() -> {
                binding.statusTextView.text = "Select a category."
                return
            }
            points == null -> {
                binding.statusTextView.text = "Enter numeric points."
                return
            }
        }

        setFormEnabled(false)
        binding.statusTextView.text = "Adding task..."
        viewModel.addTask(category, taskName, points)
        binding.taskNameEditText.text?.clear()
        setFormEnabled(viewModel.categories.value.isNotEmpty())
    }

    private fun setFormEnabled(enabled: Boolean) {
        binding.taskNameEditText.isEnabled = enabled
        binding.categorySpinner.isEnabled = enabled
        binding.taskPointsEditText.isEnabled = enabled
        binding.addTaskTextView.isEnabled = enabled
        binding.addTaskTextView.alpha = if (enabled) 1f else 0.5f
    }
}
