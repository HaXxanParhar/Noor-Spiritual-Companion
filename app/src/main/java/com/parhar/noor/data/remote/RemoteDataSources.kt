package com.parhar.noor.data.remote

import com.parhar.noor.data.remote.dto.RemoteCategory
import com.parhar.noor.data.remote.dto.RemoteFavoriteInfo
import com.parhar.noor.data.remote.dto.RemoteSteakSnapshot
import com.parhar.noor.data.remote.dto.RemoteTaskDefinition
import com.parhar.noor.data.remote.dto.RemoteUserProfile

interface CatalogRemoteDataSource {
    suspend fun fetchCategories(): List<RemoteCategory>
    suspend fun fetchTaskDefinitions(): List<RemoteTaskDefinition>
    suspend fun pushTaskDefinition(task: RemoteTaskDefinition)
    fun observeTaskDefinitions(onChanged: (List<RemoteTaskDefinition>) -> Unit, onError: (String) -> Unit): AutoCloseable
    fun observeCategories(onChanged: (List<RemoteCategory>) -> Unit, onError: (String) -> Unit): AutoCloseable
}

interface UserRemoteDataSource {
    suspend fun fetchUserProfile(uid: String): RemoteUserProfile?
    suspend fun pushUserProfile(profile: RemoteUserProfile)
    suspend fun userExists(uid: String): Boolean
    suspend fun fetchDailyTaskString(uid: String, dateKey: String): String?
    suspend fun pushDailyTaskString(uid: String, dateKey: String, value: String)
    suspend fun fetchUserTaskHistory(uid: String): Map<String, String>
    suspend fun fetchFriendIds(uid: String): List<String>
    suspend fun pushFriendship(currentUid: String, friendUid: String)
    suspend fun fetchFavoriteInfo(): RemoteFavoriteInfo?
    suspend fun fetchSteak(uid: String): RemoteSteakSnapshot?
    suspend fun pushSteak(uid: String, steak: RemoteSteakSnapshot)
    fun observeDailyTasks(uid: String, dateKey: String, onChanged: (String?) -> Unit, onError: (String) -> Unit): AutoCloseable
    fun observeUserTaskHistory(uid: String, onChanged: (Map<String, String>) -> Unit, onError: (String) -> Unit): AutoCloseable
    fun observeFriends(uid: String, onChanged: (List<String>) -> Unit, onError: (String) -> Unit): AutoCloseable
    fun observeFavoriteInfo(onChanged: (RemoteFavoriteInfo?) -> Unit, onError: (String) -> Unit): AutoCloseable
    fun observeUserProfile(uid: String, onChanged: (RemoteUserProfile?) -> Unit, onError: (String) -> Unit): AutoCloseable
}
