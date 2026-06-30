package com.parhar.noor.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parhar.noor.data.repository.AyatsRepository
import com.parhar.noor.data.repository.CategoryPositionDirection
import com.parhar.noor.data.repository.CatalogRepository
import com.parhar.noor.data.repository.TrophiesRepository
import com.parhar.noor.data.repository.TaskPositionDirection
import com.parhar.noor.domain.model.Ayat
import com.parhar.noor.domain.model.Category
import com.parhar.noor.domain.model.HomeTaskSection
import com.parhar.noor.domain.model.Trophy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val catalogRepository: CatalogRepository,
    private val trophiesRepository: TrophiesRepository,
    private val ayatsRepository: AyatsRepository,
) : ViewModel() {

    val categories: StateFlow<List<Category>> = catalogRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _optimisticCategories = MutableStateFlow<List<Category>?>(null)

    val categoryList: StateFlow<List<Category>> = combine(
        categories,
        _optimisticCategories,
    ) { repositoryCategories, optimisticCategories ->
        (optimisticCategories ?: repositoryCategories).sortedWith(
            compareBy<Category> { it.position }.thenBy { it.category },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _optimisticTaskSections = MutableStateFlow<List<HomeTaskSection>?>(null)

    val taskSections: StateFlow<List<HomeTaskSection>> = combine(
        catalogRepository.observeTaskSections(
            includeEmptyCategories = true,
            includeHiddenTasks = true,
        ),
        _optimisticTaskSections,
    ) { repositorySections, optimisticSections ->
        optimisticSections ?: repositorySections
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isActionInProgress = MutableStateFlow(false)
    val isActionInProgress: StateFlow<Boolean> = _isActionInProgress.asStateFlow()

    private val _categorySaved = MutableStateFlow(false)
    val categorySaved: StateFlow<Boolean> = _categorySaved.asStateFlow()

    private val _categoryDeleted = MutableStateFlow(false)
    val categoryDeleted: StateFlow<Boolean> = _categoryDeleted.asStateFlow()

    private val _taskSaved = MutableStateFlow(false)
    val taskSaved: StateFlow<Boolean> = _taskSaved.asStateFlow()

    private val _taskDeleted = MutableStateFlow(false)
    val taskDeleted: StateFlow<Boolean> = _taskDeleted.asStateFlow()

    val trophies: StateFlow<List<Trophy>> = trophiesRepository.observeTrophies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _trophySaved = MutableStateFlow(false)
    val trophySaved: StateFlow<Boolean> = _trophySaved.asStateFlow()

    private val _trophyDeleted = MutableStateFlow(false)
    val trophyDeleted: StateFlow<Boolean> = _trophyDeleted.asStateFlow()

    val ayats: StateFlow<List<Ayat>> = ayatsRepository.observeAyats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _ayatSaved = MutableStateFlow(false)
    val ayatSaved: StateFlow<Boolean> = _ayatSaved.asStateFlow()

    private val _ayatDeleted = MutableStateFlow(false)
    val ayatDeleted: StateFlow<Boolean> = _ayatDeleted.asStateFlow()

    fun addTask(
        category: String,
        name: String,
        points: Int,
        emoji: String = "",
        shortDescription: String = "",
        detailedDescription: String = "",
        arabic: String = "",
        visible: Boolean = true,
    ) {
        runBlockingAdminAction(
            onFailure = { error -> _statusMessage.value = error.message ?: "Unable to add task." },
        ) {
            catalogRepository.addTask(
                category = category,
                name = name,
                points = points,
                emoji = emoji,
                shortDescription = shortDescription,
                detailedDescription = detailedDescription,
                arabic = arabic,
                visible = visible,
            )
            _statusMessage.value = "Task added."
            _taskSaved.value = true
        }
    }

    fun updateTask(
        taskId: String,
        originalCategory: String,
        category: String,
        name: String,
        points: Int,
        emoji: String = "",
        shortDescription: String = "",
        detailedDescription: String = "",
        arabic: String = "",
        visible: Boolean = true,
    ) {
        runBlockingAdminAction(
            onFailure = { error -> _statusMessage.value = error.message ?: "Unable to update task." },
        ) {
            catalogRepository.updateTask(
                taskId = taskId,
                originalCategory = originalCategory,
                category = category,
                name = name,
                points = points,
                emoji = emoji,
                shortDescription = shortDescription,
                detailedDescription = detailedDescription,
                arabic = arabic,
                visible = visible,
            )
            _statusMessage.value = "Task updated."
            _taskSaved.value = true
        }
    }

    fun setTaskVisible(taskId: String, category: String, visible: Boolean) {
        runBlockingAdminAction(
            onFailure = { error -> _statusMessage.value = error.message ?: "Unable to update visibility." },
        ) {
            catalogRepository.setTaskVisible(taskId, category, visible)
            _statusMessage.value = if (visible) "Task is now visible." else "Task is now hidden."
        }
    }

    fun deleteTask(taskId: String, category: String) {
        runBlockingAdminAction(
            onFailure = { error -> _statusMessage.value = error.message ?: "Unable to delete task." },
        ) {
            catalogRepository.deleteTask(taskId, category)
            _statusMessage.value = "Task deleted."
            _taskDeleted.value = true
        }
    }

    fun moveCategoryUp(categoryId: String) {
        moveCategory(categoryId, CategoryPositionDirection.UP)
    }

    fun moveCategoryDown(categoryId: String) {
        moveCategory(categoryId, CategoryPositionDirection.DOWN)
    }

    fun addCategory(title: String, categoryName: String, description: String) {
        runBlockingAdminAction(
            onFailure = { error -> _statusMessage.value = error.message ?: "Unable to add category." },
        ) {
            catalogRepository.addCategory(title, categoryName, description)
            _statusMessage.value = "Category added."
            _categorySaved.value = true
        }
    }

    fun updateCategory(
        categoryId: String,
        originalCategoryName: String,
        title: String,
        categoryName: String,
        description: String,
    ) {
        runBlockingAdminAction(
            onFailure = { error -> _statusMessage.value = error.message ?: "Unable to update category." },
        ) {
            catalogRepository.updateCategory(
                categoryId = categoryId,
                originalCategoryName = originalCategoryName,
                title = title,
                categoryName = categoryName,
                description = description,
            )
            _statusMessage.value = "Category updated."
            _categorySaved.value = true
        }
    }

    fun deleteCategory(categoryId: String, categoryName: String) {
        runBlockingAdminAction(
            onFailure = { error -> _statusMessage.value = error.message ?: "Unable to delete category." },
        ) {
            catalogRepository.deleteCategory(categoryId, categoryName)
            _statusMessage.value = "Category deleted."
            _categoryDeleted.value = true
        }
    }

    fun addTrophy(name: String, icon: String, requirement: Int) {
        runBlockingAdminAction(
            onFailure = { error -> _statusMessage.value = error.message ?: "Unable to add trophy." },
        ) {
            trophiesRepository.addTrophy(name, icon, requirement)
            _statusMessage.value = "Trophy added."
            _trophySaved.value = true
        }
    }

    fun updateTrophy(trophyId: String, name: String, icon: String, requirement: Int) {
        runBlockingAdminAction(
            onFailure = { error -> _statusMessage.value = error.message ?: "Unable to update trophy." },
        ) {
            trophiesRepository.updateTrophy(trophyId, name, icon, requirement)
            _statusMessage.value = "Trophy updated."
            _trophySaved.value = true
        }
    }

    fun deleteTrophy(trophyId: String) {
        runBlockingAdminAction(
            onFailure = { error -> _statusMessage.value = error.message ?: "Unable to delete trophy." },
        ) {
            trophiesRepository.deleteTrophy(trophyId)
            _statusMessage.value = "Trophy deleted."
            _trophyDeleted.value = true
        }
    }

    fun addAyat(ayat: String, english: String, urdu: String, reference: String) {
        runBlockingAdminAction(
            onFailure = { error -> _statusMessage.value = error.message ?: "Unable to add ayat." },
        ) {
            ayatsRepository.addAyat(ayat, english, urdu, reference)
            _statusMessage.value = "Ayat added."
            _ayatSaved.value = true
        }
    }

    fun updateAyat(ayatId: String, ayat: String, english: String, urdu: String, reference: String) {
        runBlockingAdminAction(
            onFailure = { error -> _statusMessage.value = error.message ?: "Unable to update ayat." },
        ) {
            ayatsRepository.updateAyat(ayatId, ayat, english, urdu, reference)
            _statusMessage.value = "Ayat updated."
            _ayatSaved.value = true
        }
    }

    fun deleteAyat(ayatId: String) {
        runBlockingAdminAction(
            onFailure = { error -> _statusMessage.value = error.message ?: "Unable to delete ayat." },
        ) {
            ayatsRepository.deleteAyat(ayatId)
            _statusMessage.value = "Ayat deleted."
            _ayatDeleted.value = true
        }
    }

    private fun moveCategory(categoryId: String, direction: CategoryPositionDirection) {
        val current = _optimisticCategories.value ?: categoryList.value
        val optimistic = CategoryOptimisticReorder.apply(current, categoryId, direction) ?: return
        _optimisticCategories.value = optimistic

        viewModelScope.launch {
            runCatching {
                catalogRepository.swapCategoryPosition(categoryId, direction)
            }.onSuccess {
                _optimisticCategories.value = null
            }.onFailure { error ->
                _optimisticCategories.value = null
                _statusMessage.value = error.message ?: "Unable to reorder category."
            }
        }
    }

    fun moveTaskUp(taskId: String, category: String) {
        moveTask(taskId, category, TaskPositionDirection.UP)
    }

    fun moveTaskDown(taskId: String, category: String) {
        moveTask(taskId, category, TaskPositionDirection.DOWN)
    }

    private fun moveTask(taskId: String, category: String, direction: TaskPositionDirection) {
        val currentSections = _optimisticTaskSections.value ?: taskSections.value
        val optimisticSections = TaskSectionOptimisticReorder.apply(
            sections = currentSections,
            taskId = taskId,
            category = category,
            direction = direction,
        ) ?: return

        _optimisticTaskSections.value = optimisticSections

        viewModelScope.launch {
            runCatching {
                catalogRepository.swapTaskPosition(taskId, category, direction)
            }.onSuccess {
                _optimisticTaskSections.value = null
            }.onFailure { error ->
                _optimisticTaskSections.value = null
                _statusMessage.value = error.message ?: "Unable to reorder task."
            }
        }
    }

    fun clearStatusFlags() {
        _taskSaved.value = false
        _taskDeleted.value = false
        _categorySaved.value = false
        _categoryDeleted.value = false
        _trophySaved.value = false
        _trophyDeleted.value = false
        _ayatSaved.value = false
        _ayatDeleted.value = false
    }

    private fun runBlockingAdminAction(
        onFailure: (Throwable) -> Unit,
        block: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            _isActionInProgress.value = true
            runCatching { block() }.onFailure(onFailure)
            _isActionInProgress.value = false
        }
    }
}
