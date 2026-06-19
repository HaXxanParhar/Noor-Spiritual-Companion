package com.parhar.noor.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parhar.noor.data.repository.UserProfileRepository
import com.parhar.noor.domain.model.UserAvatar
import com.parhar.noor.domain.model.UserProfile
import com.parhar.noor.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditProfileViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _gender = MutableStateFlow("")
    val gender: StateFlow<String> = _gender.asStateFlow()

    private val _avatar = MutableStateFlow<UserAvatar?>(null)
    val avatar: StateFlow<UserAvatar?> = _avatar.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private var currentProfile: UserProfile? = null

    fun load() {
        val uid = sessionManager.getUserId().orEmpty()
        if (uid.isBlank()) return
        viewModelScope.launch {
            userProfileRepository.getUser(uid)?.let { profile ->
                currentProfile = profile
                _name.value = profile.name
                _gender.value = profile.gender
                _avatar.value = profile.avatar
            }
        }
    }

    fun updateName(value: String) {
        _name.value = value
    }

    fun updateGender(value: String) {
        _gender.value = value
    }

    fun updateAvatar(avatar: UserAvatar) {
        _avatar.value = avatar
    }

    fun save() {
        val profile = currentProfile ?: return
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                val updated = profile.copy(
                    name = _name.value.trim(),
                    gender = _gender.value,
                    avatar = _avatar.value,
                )
                userProfileRepository.saveProfile(updated)
                sessionManager.saveUserSession(
                    com.parhar.noor.data.user.UserProfile(
                        uid = updated.uid,
                        email = updated.email,
                        name = updated.name,
                        gender = updated.gender,
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
