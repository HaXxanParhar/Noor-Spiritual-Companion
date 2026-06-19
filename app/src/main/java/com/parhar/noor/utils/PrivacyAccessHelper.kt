package com.parhar.noor.utils

import com.parhar.noor.domain.model.PrivacyVisibility
import com.parhar.noor.domain.model.UserPrivacy

object PrivacyAccessHelper {

    fun canViewTodayTasks(
        viewerUid: String,
        targetUid: String,
        privacy: UserPrivacy,
        isFriend: Boolean,
    ): Boolean {
        if (viewerUid == targetUid) return true
        return privacy.tasksToday == PrivacyVisibility.FRIENDS && isFriend
    }

    fun canViewTaskHistory(
        viewerUid: String,
        targetUid: String,
        privacy: UserPrivacy,
        isFriend: Boolean,
    ): Boolean {
        if (viewerUid == targetUid) return true
        return privacy.tasksHistory == PrivacyVisibility.FRIENDS && isFriend
    }

    fun canViewTasksForDate(
        viewerUid: String,
        targetUid: String,
        privacy: UserPrivacy,
        isFriend: Boolean,
        dateKey: String,
    ): Boolean {
        val daysFromToday = DateKeyUtils.daysFromToday(dateKey) ?: return false
        if (daysFromToday > 0) return false

        if (viewerUid == targetUid) {
            return daysFromToday <= 0
        }

        return when {
            daysFromToday == 0 -> canViewTodayTasks(viewerUid, targetUid, privacy, isFriend)
            else -> canViewTaskHistory(viewerUid, targetUid, privacy, isFriend)
        }
    }

    fun canBrowseTaskHistory(
        viewerUid: String,
        targetUid: String,
        privacy: UserPrivacy,
        isFriend: Boolean,
    ): Boolean = canViewTaskHistory(viewerUid, targetUid, privacy, isFriend)
}
