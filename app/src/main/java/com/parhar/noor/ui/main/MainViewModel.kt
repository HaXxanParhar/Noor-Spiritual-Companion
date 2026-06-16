package com.parhar.noor.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parhar.noor.data.repository.FavoriteRepository
import com.parhar.noor.domain.model.FavoriteBanner
import com.parhar.noor.utils.SessionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val favoriteRepository: FavoriteRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _favoriteBannerPopup = MutableSharedFlow<FavoriteBanner>()
    val favoriteBannerPopup: SharedFlow<FavoriteBanner> = _favoriteBannerPopup.asSharedFlow()

    init {
        viewModelScope.launch {
            favoriteRepository.observeFavoriteBanner().collect { banner ->
                val email = sessionManager.getUserProfile()?.email.orEmpty()
                if (banner == null || email.isBlank()) return@collect
                if (!banner.email.equals(email, ignoreCase = true)) return@collect
                if (banner.message.isBlank()) return@collect
                if (banner.message == sessionManager.getLastFavPopupMessage(email)) return@collect

                sessionManager.setLastFavPopupMessage(email, banner.message)
                _favoriteBannerPopup.emit(banner)
            }
        }
    }
}
