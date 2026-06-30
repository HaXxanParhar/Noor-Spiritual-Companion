package com.parhar.noor.domain.model

data class UserAvatar(
    val text: String = "",
    val bg: String = "",
    val border: String = "",
    val style: String = "",
) : java.io.Serializable {
    fun isConfigured(): Boolean {
        return text.isNotBlank() || bg.isNotBlank() || border.isNotBlank() || style.isNotBlank()
    }
}

object PrivacyVisibility {
    const val PRIVATE = "private"
    const val FRIENDS = "friends"
}

data class UserPrivacy(
    val tasksToday: String = PrivacyVisibility.PRIVATE,
    val tasksHistory: String = PrivacyVisibility.PRIVATE,
)

data class UserProfile(
    val uid: String,
    val email: String = "",
    val name: String = "",
    val gender: String = "",
    val avatar: UserAvatar? = null,
    val createdAt: Long = 0L,
    val privacy: UserPrivacy = UserPrivacy(),
)

data class Category(
    val id: String,
    val categoryKey: String,
    val category: String,
    val title: String,
    val description: String = "",
    val position: Int = 0,
)

data class TaskDefinition(
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
) : java.io.Serializable

data class TaskItem(
    val id: String,
    val task: TaskDefinition,
) : java.io.Serializable

data class HomeTaskSection(
    val category: String,
    val title: String,
    val description: String = "",
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

data class FriendReminder(
    val senderId: String,
    val sender: String,
    val message: String,
    val createdAt: Long,
)

sealed class MainBanner {
    data class Favorite(val banner: FavoriteBanner) : MainBanner()
    data class Reminder(val reminder: FriendReminder) : MainBanner()
}

data class LeaderboardEntry(
    val uid: String = "",
    val rank: Int,
    val initials: String,
    val name: String,
    val points: Int,
    val streak: Int,
    val isCurrentUser: Boolean = false,
    val avatar: UserAvatar? = null,
)

data class BoardState(
    val dateRangeLabel: String,
    val entries: List<LeaderboardEntry>,
    val hasFriends: Boolean,
    val isOffline: Boolean = false,
    val weekTitle: String = "",
    val countdownText: String? = null,
    val weekEndAtMillis: Long = 0L,
)

data class WeekCycle(
    val weekKey: String,
    val joinedAt: Long,
    val title: String,
    val start: String,
    val end: String,
    val endAt: Long,
    val myPosition: Int,
    val points: Int,
    val done: Boolean,
)

data class UserMedals(
    val total1st: Int = 0,
    val total2nd: Int = 0,
    val total3rd: Int = 0,
    val totalTop5: Int = 0,
    val totalTop10: Int = 0,
)

data class WeekResultSummary(
    val weekKey: String,
    val title: String,
    val points: Int,
    val position: Int,
    val medalEmoji: String?,
    val congratsMessage: String?,
)

data class ActiveWeekUi(
    val weekKey: String,
    val title: String,
    val endAtMillis: Long,
    val countdownText: String,
)

data class Trophy(
    val id: String,
    val name: String,
    val icon: String,
    val requirement: Int,
)

data class Ayat(
    val id: String,
    val ayat: String,
    val english: String,
    val urdu: String = "",
    val reference: String = "",
) : java.io.Serializable
