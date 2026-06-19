package com.parhar.noor.data.remote

import com.parhar.noor.data.remote.dto.RemoteCategory
import com.parhar.noor.data.remote.dto.RemoteFavoriteInfo
import com.parhar.noor.data.remote.dto.RemoteReminder
import com.parhar.noor.data.remote.dto.RemoteSteakSnapshot
import com.parhar.noor.data.remote.dto.RemoteTaskDefinition
import com.parhar.noor.data.remote.dto.RemoteUserMedals
import com.parhar.noor.data.remote.dto.RemoteUserProfile
import com.parhar.noor.data.remote.dto.RemoteWeekCycle

interface CatalogRemoteDataSource {
    suspend fun fetchCategories(): List<RemoteCategory>
    suspend fun fetchTaskDefinitions(): List<RemoteTaskDefinition>
    suspend fun pushCategory(category: RemoteCategory)
    suspend fun deleteCategory(categoryId: String)
    suspend fun deleteTasksForCategory(category: String)
    suspend fun pushTaskDefinition(task: RemoteTaskDefinition)
    suspend fun deleteTaskDefinition(category: String, taskId: String)
    fun observeTaskDefinitions(onChanged: (List<RemoteTaskDefinition>) -> Unit, onError: (String) -> Unit): AutoCloseable
    fun observeCategories(onChanged: (List<RemoteCategory>) -> Unit, onError: (String) -> Unit): AutoCloseable
}

interface UserRemoteDataSource {
    suspend fun fetchUserProfile(uid: String): RemoteUserProfile?
    suspend fun pushUserProfile(profile: RemoteUserProfile)
    suspend fun userExists(uid: String): Boolean
    suspend fun saveFcmToken(uid: String, token: String)
    suspend fun fetchFcmToken(uid: String): String?
    suspend fun fetchDailyTaskString(uid: String, dateKey: String): String?
    suspend fun pushDailyTaskString(uid: String, dateKey: String, value: String)
    suspend fun fetchUserTaskHistory(uid: String): Map<String, String>
    suspend fun fetchFriendIds(uid: String): List<String>
    suspend fun pushFriendship(currentUid: String, friendUid: String)
    suspend fun removeFriendship(currentUid: String, friendUid: String)
    suspend fun fetchFavoriteInfo(): RemoteFavoriteInfo?
    suspend fun fetchSteak(uid: String): RemoteSteakSnapshot?
    suspend fun pushSteak(uid: String, steak: RemoteSteakSnapshot)
    suspend fun fetchWeeks(uid: String): Map<String, RemoteWeekCycle>
    suspend fun fetchWeek(uid: String, weekKey: String): RemoteWeekCycle?
    suspend fun createWeekIfMissing(uid: String, week: RemoteWeekCycle)
    suspend fun updateWeek(uid: String, weekKey: String, fields: Map<String, Any>)
    suspend fun fetchUserMedals(uid: String): RemoteUserMedals
    suspend fun incrementMedalTier(uid: String, position: Int)
    suspend fun hasReminderFromSender(targetUid: String, senderUid: String): Boolean
    suspend fun pushReminder(targetUid: String, reminder: RemoteReminder)
    suspend fun deleteReminder(targetUid: String, senderUid: String)
    fun observeDailyTasks(uid: String, dateKey: String, onChanged: (String?) -> Unit, onError: (String) -> Unit): AutoCloseable
    fun observeUserTaskHistory(uid: String, onChanged: (Map<String, String>) -> Unit, onError: (String) -> Unit): AutoCloseable
    fun observeFriends(uid: String, onChanged: (List<String>) -> Unit, onError: (String) -> Unit): AutoCloseable
    fun observeReminders(uid: String, onChanged: (List<RemoteReminder>) -> Unit, onError: (String) -> Unit): AutoCloseable
    fun observeReminderFromSender(
        targetUid: String,
        senderUid: String,
        onChanged: (RemoteReminder?) -> Unit,
        onError: (String) -> Unit,
    ): AutoCloseable
    fun observeFavoriteInfo(onChanged: (RemoteFavoriteInfo?) -> Unit, onError: (String) -> Unit): AutoCloseable
    fun observeUserProfile(uid: String, onChanged: (RemoteUserProfile?) -> Unit, onError: (String) -> Unit): AutoCloseable
}
