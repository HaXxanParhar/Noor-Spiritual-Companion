package com.parhar.noor.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parhar.noor.data.repository.FavoriteRepository
import com.parhar.noor.data.repository.RemindersRepository
import com.parhar.noor.domain.model.MainBanner
import com.parhar.noor.utils.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val favoriteRepository: FavoriteRepository,
    private val remindersRepository: RemindersRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val bannerCoordinator = MainBannerCoordinator()
    val bannerEvent: SharedFlow<MainBanner> = bannerCoordinator.bannerEvent

    private var remindersObserverJob: Job? = null

    init {
        viewModelScope.launch {
            favoriteRepository.observeFavoriteBanner().collect { banner ->
                val email = sessionManager.getUserProfile()?.email.orEmpty()
                if (banner == null || email.isBlank()) return@collect
                if (!banner.email.equals(email, ignoreCase = true)) return@collect
                if (banner.message.isBlank()) return@collect
                if (banner.message == sessionManager.getLastFavPopupMessage(email)) return@collect

                sessionManager.setLastFavPopupMessage(email, banner.message)
                bannerCoordinator.onFavoriteReceived(banner)
            }
        }
        ensureRemindersObserver()
    }

    fun ensureRemindersObserver() {
        val uid = sessionManager.getUserId().orEmpty()
        if (uid.isBlank()) return
        if (remindersObserverJob?.isActive == true) return

        remindersObserverJob = viewModelScope.launch {
            remindersRepository.observeReminders(uid).collect { reminders ->
                bannerCoordinator.onRemindersUpdated(reminders)
            }
        }
    }

    fun retryPendingBanners() {
        bannerCoordinator.retryPendingBanners()
    }

    fun onBannerFinished(senderId: String?) {
        bannerCoordinator.onBannerFinished(senderId)
    }

    fun deleteReminder(senderId: String) {
        val uid = sessionManager.getUserId().orEmpty()
        if (uid.isBlank() || senderId.isBlank()) return
        viewModelScope.launch {
            remindersRepository.deleteReminder(uid, senderId)
        }
    }
}
