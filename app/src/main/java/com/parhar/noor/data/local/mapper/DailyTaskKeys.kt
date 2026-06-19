package com.parhar.noor.data.local.mapper

object DailyTaskKeys {
    const val CANNOT_OFFER_TASK_ID = "cannot_offer"

    fun isCannotOfferKey(taskId: String): Boolean = taskId == CANNOT_OFFER_TASK_ID

    fun filterPointsForStats(taskPoints: Map<String, Int>): Map<String, Int> =
        taskPoints.filterKeys { !isCannotOfferKey(it) }

    fun sumPointsForStats(taskPoints: Map<String, Int>): Int =
        filterPointsForStats(taskPoints).values.sumOf { points -> points.coerceAtLeast(0) }
}
