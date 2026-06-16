package com.parhar.noor.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parhar.noor.data.repository.CatalogRepository
import com.parhar.noor.domain.model.Category
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val catalogRepository: CatalogRepository,
) : ViewModel() {

    val categories: StateFlow<List<Category>> = catalogRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun addTask(category: String, name: String, points: Int) {
        viewModelScope.launch {
            runCatching {
                catalogRepository.addTask(category, name, points)
            }.onSuccess {
                _statusMessage.value = "Task added."
            }.onFailure { error ->
                _statusMessage.value = error.message ?: "Unable to add task."
            }
        }
    }
}
