package com.parhar.noor.data.repository.impl

import com.parhar.noor.data.local.dao.FavoriteBannerDao
import com.parhar.noor.data.local.mapper.EntityMappers.toDomain
import com.parhar.noor.data.repository.FavoriteRepository
import com.parhar.noor.domain.model.FavoriteBanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoriteRepositoryImpl(
    private val favoriteBannerDao: FavoriteBannerDao,
) : FavoriteRepository {

    override fun observeFavoriteBanner(): Flow<FavoriteBanner?> {
        return favoriteBannerDao.observeBanner().map { entity -> entity?.toDomain() }
    }
}
