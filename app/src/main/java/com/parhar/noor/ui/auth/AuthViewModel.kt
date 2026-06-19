package com.parhar.noor.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parhar.noor.data.repository.UserProfileRepository
import com.parhar.noor.domain.model.UserProfile
import com.parhar.noor.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _accountCreated = MutableStateFlow(false)
    val accountCreated: StateFlow<Boolean> = _accountCreated.asStateFlow()

    fun createAccount(uid: String, email: String, name: String, gender: String) {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                val now = System.currentTimeMillis()
                val profile = UserProfile(
                    uid = uid,
                    email = email,
                    name = name,
                    gender = gender,
                    createdAt = now,
                )
                userProfileRepository.saveProfile(profile)
                sessionManager.saveUserSession(
                    com.parhar.noor.data.user.UserProfile(
                        uid = uid,
                        email = email,
                        name = name,
                        gender = gender,
                    ),
                )
            }.onSuccess {
                _accountCreated.value = true
            }.onFailure { error ->
                _statusMessage.value = error.message ?: "Unable to create account."
            }
            _isLoading.value = false
        }
    }
}
