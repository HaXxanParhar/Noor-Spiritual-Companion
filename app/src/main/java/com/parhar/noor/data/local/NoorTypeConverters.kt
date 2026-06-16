package com.parhar.noor.data.local

import androidx.room.TypeConverter
import com.parhar.noor.data.local.SyncOpType
import com.parhar.noor.data.local.SyncStatus

class NoorTypeConverters {

    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String = value.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)

    @TypeConverter
    fun fromSyncOpType(value: SyncOpType): String = value.name

    @TypeConverter
    fun toSyncOpType(value: String): SyncOpType = SyncOpType.valueOf(value)
}
