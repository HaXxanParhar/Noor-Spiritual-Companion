package com.parhar.noor.ui.main

import com.parhar.noor.domain.model.FavoriteBanner
import com.parhar.noor.domain.model.FriendReminder
import com.parhar.noor.domain.model.MainBanner
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MainBannerCoordinator {

    private val _bannerEvent = MutableSharedFlow<MainBanner>(extraBufferCapacity = 4)
    val bannerEvent: SharedFlow<MainBanner> = _bannerEvent.asSharedFlow()

    private var pendingFavorite: FavoriteBanner? = null
    private var pendingReminders: List<FriendReminder> = emptyList()
    private var isDisplaying = false

    fun onFavoriteReceived(banner: FavoriteBanner) {
        pendingFavorite = banner
        emitNextIfIdle()
    }

    fun onRemindersUpdated(reminders: List<FriendReminder>) {
        pendingReminders = reminders.sortedBy { it.createdAt }
        emitNextIfIdle()
    }

    fun retryPendingBanners() {
        emitNextIfIdle()
    }

    fun onBannerFinished(senderId: String?) {
        isDisplaying = false
        emitNextIfIdle()
    }

    private fun emitNextIfIdle() {
        if (isDisplaying) return

        val favorite = pendingFavorite
        if (favorite != null) {
            isDisplaying = true
            pendingFavorite = null
            _bannerEvent.tryEmit(MainBanner.Favorite(favorite))
            return
        }

        val reminder = pendingReminders.firstOrNull() ?: return
        isDisplaying = true
        _bannerEvent.tryEmit(MainBanner.Reminder(reminder))
    }
}
