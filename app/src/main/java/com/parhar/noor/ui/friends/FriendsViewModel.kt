package com.parhar.noor.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parhar.noor.data.repository.FriendsRepository
import com.parhar.noor.data.repository.RemindResult
import com.parhar.noor.data.repository.RemindersRepository
import com.parhar.noor.data.repository.UserProfileRepository
import com.parhar.noor.domain.model.UserProfile
import com.parhar.noor.utils.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FriendListItem(
    val uid: String,
    val name: String,
    val profile: UserProfile?,
    val canRemind: Boolean = true,
)

class FriendsViewModel(
    private val friendsRepository: FriendsRepository,
    private val remindersRepository: RemindersRepository,
    private val userProfileRepository: UserProfileRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val currentUid: String
        get() = sessionManager.getUserId().orEmpty()

    private val senderName: String
        get() = sessionManager.getUserProfile()?.name?.takeIf { it.isNotBlank() }
            ?: "A friend"

    private val _friends = MutableStateFlow<List<FriendListItem>>(emptyList())
    val friends: StateFlow<List<FriendListItem>> = _friends.asStateFlow()

    private val _remindResult = MutableSharedFlow<RemindResult>()
    val remindResult: SharedFlow<RemindResult> = _remindResult.asSharedFlow()

    private val _isReminding = MutableStateFlow(false)
    val isReminding: StateFlow<Boolean> = _isReminding.asStateFlow()

    private var remindObserverJob: Job? = null

    init {
        viewModelScope.launch {
            friendsRepository.observeFriendIds(currentUid).collect { friendIds ->
                loadFriends(friendIds)
                restartRemindObservers(friendIds)
            }
        }
    }

    fun remindFriend(friendUid: String, message: String) {
        if (currentUid.isBlank() || friendUid.isBlank() || _isReminding.value) return
        viewModelScope.launch {
            _isReminding.value = true
            val result = friendsRepository.sendRemind(
                currentUid = currentUid,
                friendUid = friendUid,
                senderName = senderName,
                message = message,
            )
            _remindResult.emit(result)
            if (result is RemindResult.Sent) {
                updateFriendRemindState(friendUid, canRemind = false)
            }
            _isReminding.value = false
        }
    }

    private suspend fun loadFriends(friendIds: List<String>) {
        val items = friendIds.map { friendId ->
            val profile = userProfileRepository.getUser(friendId)
            val existing = _friends.value.find { it.uid == friendId }
            FriendListItem(
                uid = friendId,
                name = profile?.name.orEmpty().ifBlank { friendId },
                profile = profile,
                canRemind = existing?.canRemind ?: true,
            )
        }
        _friends.value = items
    }

    private fun restartRemindObservers(friendIds: List<String>) {
        remindObserverJob?.cancel()
        if (currentUid.isBlank() || friendIds.isEmpty()) return

        remindObserverJob = viewModelScope.launch {
            friendIds.forEach { friendUid ->
                launch {
                    remindersRepository
                        .observeCanSendReminderTo(friendUid, currentUid)
                        .collect { canRemind ->
                            updateFriendRemindState(friendUid, canRemind)
                        }
                }
            }
        }
    }

    private fun updateFriendRemindState(friendUid: String, canRemind: Boolean) {
        _friends.value = _friends.value.map { item ->
            if (item.uid == friendUid) item.copy(canRemind = canRemind) else item
        }
    }
}
