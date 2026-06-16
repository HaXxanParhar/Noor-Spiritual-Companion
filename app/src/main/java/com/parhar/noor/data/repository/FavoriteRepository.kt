package com.parhar.noor.data.repository

import com.parhar.noor.domain.model.FavoriteBanner
import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {
    fun observeFavoriteBanner(): Flow<FavoriteBanner?>
}
