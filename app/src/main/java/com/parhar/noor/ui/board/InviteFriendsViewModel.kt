package com.parhar.noor.ui.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parhar.noor.data.repository.FriendsRepository
import com.parhar.noor.data.repository.UserProfileRepository
import com.parhar.noor.data.sync.ConnectivityMonitor
import com.parhar.noor.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InviteFriendsViewModel(
    private val friendsRepository: FriendsRepository,
    private val userProfileRepository: UserProfileRepository,
    private val sessionManager: SessionManager,
    private val connectivityMonitor: ConnectivityMonitor,
) : ViewModel() {

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val userId: String?
        get() = sessionManager.getUserId()

    fun addFriend(friendId: String) {
        val currentUserId = sessionManager.getUserId()
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                when {
                    currentUserId.isNullOrBlank() -> error("User ID unavailable.")
                    friendId.isBlank() -> error("Enter a friend ID.")
                    friendId == currentUserId -> error("You cannot add yourself.")
                    !userProfileRepository.userExists(friendId) -> error("Friend not found.")
                    friendsRepository.getFriendIds(currentUserId).contains(friendId) ->
                        error("Friend already added.")
                    else -> friendsRepository.addFriend(currentUserId, friendId)
                }
            }.onSuccess {
                _statusMessage.value = if (connectivityMonitor.checkOnline()) {
                    "Friend added."
                } else {
                    "Friend queued. Will sync when online."
                }
            }.onFailure { error ->
                _statusMessage.value = error.message ?: "Unable to add friend."
            }
            _isLoading.value = false
        }
    }

    fun clearStatus() {
        _statusMessage.value = null
    }
}
