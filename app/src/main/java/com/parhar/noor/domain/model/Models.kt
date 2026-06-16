package com.parhar.noor.domain.model

data class UserProfile(
    val uid: String,
    val email: String = "",
    val name: String = "",
    val gender: String = "",
)

data class Category(
    val id: String,
    val categoryKey: String,
    val category: String,
    val title: String,
)

data class TaskDefinition(
    val id: String,
    val category: String,
    val name: String,
    val points: Int,
    val sortOrder: Long = 0,
)

data class TaskItem(
    val id: String,
    val task: TaskDefinition,
)

data class HomeTaskSection(
    val category: String,
    val title: String,
    val tasks: List<TaskItem>,
)

data class DailyTaskState(
    val taskPoints: Map<String, Int>,
    val checkedTaskIds: Set<String>,
)

data class UserTaskStats(
    val todayPoints: Int,
    val weeklyPoints: Int,
    val allTimePoints: Int,
    val streak: Int,
)

data class FavoriteBanner(
    val email: String,
    val emoji: String,
    val message: String,
)

data class LeaderboardEntry(
    val rank: Int,
    val initials: String,
    val name: String,
    val points: Int,
    val streak: Int,
    val isCurrentUser: Boolean = false,
)

data class BoardState(
    val dateRangeLabel: String,
    val entries: List<LeaderboardEntry>,
    val hasFriends: Boolean,
    val isOffline: Boolean = false,
)
