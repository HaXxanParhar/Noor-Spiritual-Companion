package com.parhar.noor.data.remote.dto

data class RemoteUserProfile(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val gender: String = "",
)

data class RemoteCategory(
    val id: String,
    val category: String,
    val title: String,
)

data class RemoteTaskDefinition(
    val id: String,
    val category: String,
    val name: String,
    val points: Int,
)

data class RemoteFavoriteInfo(
    val email: String = "",
    val emoji: String = "",
    val top: String = "",
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
