package com.parhar.noor.ui.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parhar.noor.data.repository.UserProfileRepository
import com.parhar.noor.domain.model.PrivacyVisibility
import com.parhar.noor.domain.model.UserPrivacy
import com.parhar.noor.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PrivacyViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _tasksToday = MutableStateFlow(PrivacyVisibility.PRIVATE)
    val tasksToday: StateFlow<String> = _tasksToday.asStateFlow()

    private val _tasksHistory = MutableStateFlow(PrivacyVisibility.PRIVATE)
    val tasksHistory: StateFlow<String> = _tasksHistory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    fun load() {
        val uid = sessionManager.getUserId().orEmpty()
        if (uid.isBlank()) return
        viewModelScope.launch {
            userProfileRepository.getUser(uid)?.privacy?.let { privacy ->
                _tasksToday.value = privacy.tasksToday
                _tasksHistory.value = privacy.tasksHistory
            }
        }
    }

    fun selectTasksToday(value: String) {
        _tasksToday.value = value
    }

    fun selectTasksHistory(value: String) {
        _tasksHistory.value = value
    }

    fun save() {
        val uid = sessionManager.getUserId().orEmpty()
        if (uid.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                userProfileRepository.savePrivacy(
                    uid = uid,
                    privacy = UserPrivacy(
                        tasksToday = _tasksToday.value,
                        tasksHistory = _tasksHistory.value,
                    ),
                )
            }.onSuccess {
                _saved.value = true
            }
            _isLoading.value = false
        }
    }

    fun clearSaved() {
        _saved.value = false
    }
}
