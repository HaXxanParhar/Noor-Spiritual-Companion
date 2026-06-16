package com.parhar.noor.data.local.mapper

import com.parhar.noor.data.local.entity.DailyTaskEntryEntity
import com.parhar.noor.data.local.SyncStatus
import com.parhar.noor.utils.TaskStatsCalculator

object DailyTaskSerializer {

    fun parse(
        userUid: String,
        dateKey: String,
        rawValue: String?,
        remoteUpdatedAt: Long,
    ): List<DailyTaskEntryEntity> {
        return TaskStatsCalculator.parseDailyTaskPoints(rawValue).map { (taskId, points) ->
            DailyTaskEntryEntity(
                userUid = userUid,
                dateKey = dateKey,
                taskId = taskId,
                points = points,
                updatedAt = remoteUpdatedAt,
                syncStatus = SyncStatus.SYNCED,
            )
        }
    }

    fun serialize(entries: List<DailyTaskEntryEntity>): String {
        return entries.joinToString(separator = ",") { "${it.taskId}=${it.points}" }
    }

    fun serializeMap(taskPoints: Map<String, Int>): String {
        return taskPoints.entries.joinToString(separator = ",") { (taskId, points) ->
            "$taskId=$points"
        }
    }
}
