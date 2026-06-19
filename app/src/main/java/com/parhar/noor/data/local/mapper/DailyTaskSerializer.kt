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
        return serializeOrdered(entries.filter { it.points > 0 })
    }

    fun serializeMap(taskPoints: Map<String, Int>): String {
        val active = taskPoints.filterValues { points -> points > 0 }
        val cannotOfferPair = active[DailyTaskKeys.CANNOT_OFFER_TASK_ID]
        val others = active
            .filterKeys { taskId -> !DailyTaskKeys.isCannotOfferKey(taskId) }
            .toList()
            .sortedBy { (taskId, _) -> taskId }

        return buildList {
            if (cannotOfferPair != null) {
                add("${DailyTaskKeys.CANNOT_OFFER_TASK_ID}=$cannotOfferPair")
            }
            others.forEach { (taskId, points) ->
                add("$taskId=$points")
            }
        }.joinToString(separator = ",")
    }

    private fun serializeOrdered(activeEntries: List<DailyTaskEntryEntity>): String {
        val cannotOffer = activeEntries.firstOrNull { DailyTaskKeys.isCannotOfferKey(it.taskId) }
        val others = activeEntries
            .filter { !DailyTaskKeys.isCannotOfferKey(it.taskId) }
            .sortedBy { it.taskId }

        return buildList {
            cannotOffer?.let { add("${it.taskId}=${it.points}") }
            others.forEach { add("${it.taskId}=${it.points}") }
        }.joinToString(separator = ",")
    }
}
