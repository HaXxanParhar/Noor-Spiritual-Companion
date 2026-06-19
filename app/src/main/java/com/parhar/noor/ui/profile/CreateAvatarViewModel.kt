package com.parhar.noor.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parhar.noor.data.repository.UserProfileRepository
import com.parhar.noor.domain.model.UserAvatar
import com.parhar.noor.utils.AvatarColorPalettes
import com.parhar.noor.utils.AvatarRenderer
import com.parhar.noor.utils.AvatarStyleResolver
import com.parhar.noor.utils.SessionManager
import com.parhar.noor.utils.TaskStatsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreateAvatarViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    val backgroundColors: List<String> = AvatarColorPalettes.backgroundColors
    val borderColors: List<String> = AvatarColorPalettes.borderColors

    private val _avatarText = MutableStateFlow("")
    val avatarText: StateFlow<String> = _avatarText.asStateFlow()

    private val _backgroundColor = MutableStateFlow(backgroundColors.first())
    val backgroundColor: StateFlow<String> = _backgroundColor.asStateFlow()

    private val _borderColor = MutableStateFlow(borderColors.first())
    val borderColor: StateFlow<String> = _borderColor.asStateFlow()

    private val _style = MutableStateFlow(AvatarStyleResolver.STYLE_AMIRI_CLASSIC)
    val style: StateFlow<String> = _style.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    fun load(initialAvatar: UserAvatar?, fallbackName: String) {
        val existing = initialAvatar
        _avatarText.value = (existing?.text?.takeIf { it.isNotBlank() }
            ?: TaskStatsCalculator.toInitials(fallbackName))
            .take(AvatarRenderer.MAX_AVATAR_TEXT_LENGTH)
        _backgroundColor.value = existing?.bg?.takeIf { it.isNotBlank() } ?: backgroundColors.first()
        _borderColor.value = existing?.border?.takeIf { it.isNotBlank() } ?: borderColors.first()
        _style.value = existing?.style?.takeIf { it.isNotBlank() }
            ?: AvatarStyleResolver.STYLE_AMIRI_CLASSIC
    }

    fun updateText(value: String) {
        _avatarText.value = value.take(AvatarRenderer.MAX_AVATAR_TEXT_LENGTH)
    }

    fun selectBackground(color: String) {
        _backgroundColor.value = color
    }

    fun selectBorder(color: String) {
        _borderColor.value = color
    }

    fun selectStyle(style: String) {
        _style.value = style
    }

    fun currentAvatar(): UserAvatar = UserAvatar(
        text = _avatarText.value.trim(),
        bg = _backgroundColor.value,
        border = _borderColor.value,
        style = _style.value,
    )

    fun save() {
        val uid = sessionManager.getUserId().orEmpty()
        if (uid.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                userProfileRepository.saveAvatar(uid, currentAvatar())
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
