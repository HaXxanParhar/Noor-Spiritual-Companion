package com.parhar.noor.data.local.mapper

import com.parhar.noor.data.local.entity.AyatEntity
import com.parhar.noor.domain.model.Ayat

fun AyatEntity.toDomain(): Ayat = Ayat(
    id = id,
    ayat = ayat,
    english = english,
    urdu = urdu,
    reference = reference,
)

fun Ayat.toEntity(): AyatEntity = AyatEntity(
    id = id,
    ayat = ayat,
    english = english,
    urdu = urdu,
    reference = reference,
)
