package com.parhar.noor.data.remote.dto

data class RemoteUserAvatar(
    val text: String = "",
    val bg: String = "",
    val border: String = "",
    val style: String = "",
)

data class RemoteUserPrivacy(
    val tasksToday: String = com.parhar.noor.domain.model.PrivacyVisibility.PRIVATE,
    val tasksHistory: String = com.parhar.noor.domain.model.PrivacyVisibility.PRIVATE,
)

data class RemoteUserProfile(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val gender: String = "",
    val avatar: RemoteUserAvatar? = null,
    val createdAt: Long = 0L,
    val privacy: RemoteUserPrivacy? = null,
)

data class RemoteCategory(
    val id: String,
    val category: String,
    val title: String,
    val description: String = "",
    val position: Int = 0,
)

data class RemoteTaskDefinition(
    val id: String,
    val category: String,
    val name: String,
    val points: Int,
    val position: Int = 0,
    val emoji: String = "",
    val shortDescription: String = "",
    val detailedDescription: String = "",
    val arabic: String = "",
    val visible: Boolean = true,
)

data class RemoteFavoriteInfo(
    val email: String = "",
    val emoji: String = "",
    val top: String = "",
)

data class RemoteTrophy(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    val requirement: Int = 0,
)

data class RemoteAyat(
    val id: String = "",
    val ayat: String = "",
    val english: String = "",
    val urdu: String = "",
    val reference: String = "",
)

data class DailyTasksPayload(
    val userUid: String,
    val dateKey: String,
)

data class AddFriendPayload(
    val currentUid: String,
    val friendUid: String,
)

data class AddTaskDefPayload(
    val taskId: String,
    val category: String,
    val name: String,
    val points: Int,
    val position: Int = 0,
    val emoji: String = "",
    val shortDescription: String = "",
    val detailedDescription: String = "",
    val arabic: String = "",
    val visible: Boolean = true,
)

data class RemoteSteakPeriod(
    val count: Long = 0,
    val start: Long = 0,
    val end: Long = 0,
)

data class RemoteSteakSnapshot(
    val current: RemoteSteakPeriod? = null,
    val highest: RemoteSteakPeriod? = null,
)

data class RemoteWeekCycle(
    val weekKey: String = "",
    val joinedAt: Long = 0L,
    val title: String = "",
    val start: String = "",
    val end: String = "",
    val endAt: Long = 0L,
    val myPosition: Int = -1,
    val points: Int = 0,
    val done: Boolean = false,
)

data class RemoteUserMedals(
    val total1st: Int = 0,
    val total2nd: Int = 0,
    val total3rd: Int = 0,
    val totalTop5: Int = 0,
    val totalTop10: Int = 0,
)

data class RemoteReminder(
    val senderId: String = "",
    val sender: String = "",
    val message: String = "",
    val createdAt: Long = 0L,
)
