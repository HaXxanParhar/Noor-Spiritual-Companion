package com.parhar.noor.utils

import android.content.Context
import com.parhar.noor.data.user.UserProfile
import java.util.Locale

class SessionManager(context: Context) {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun hasCompletedOnboarding(): Boolean {
        return preferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted() {
        preferences.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, true)
            .apply()
    }

    fun isLoggedIn(): Boolean {
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun saveUserSession(userProfile: UserProfile) {
        preferences.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_UID, userProfile.uid)
            .putString(KEY_EMAIL, userProfile.email)
            .putString(KEY_NAME, userProfile.name)
            .putString(KEY_GENDER, userProfile.gender)
            .apply()
    }

    fun getUserName(): String? {
        return preferences.getString(KEY_NAME, null)?.takeIf { it.isNotBlank() }
    }

    fun getUserProfile(): UserProfile? {
        val uid = preferences.getString(KEY_UID, null).orEmpty()
        if (uid.isBlank()) return null

        return UserProfile(
            uid = uid,
            email = preferences.getString(KEY_EMAIL, null).orEmpty(),
            name = preferences.getString(KEY_NAME, null).orEmpty(),
            gender = preferences.getString(KEY_GENDER, null).orEmpty(),
        )
    }

    fun getUserId(): String? {
        return preferences.getString(KEY_UID, null)?.takeIf { it.isNotBlank() }
    }

    fun clearUserSession() {
        preferences.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_UID)
            .remove(KEY_EMAIL)
            .remove(KEY_NAME)
            .remove(KEY_GENDER)
            .remove(KEY_FRIEND_COUNT)
            .putBoolean(KEY_ADMIN_AUTHENTICATED, false)
            .apply()
    }

    fun isAdminAuthenticated(): Boolean {
        return preferences.getBoolean(KEY_ADMIN_AUTHENTICATED, false)
    }

    fun setAdminAuthenticated(authenticated: Boolean) {
        preferences.edit()
            .putBoolean(KEY_ADMIN_AUTHENTICATED, authenticated)
            .apply()
    }

    fun savePrimaryTaskIds(taskIds: Set<String>) {
        preferences.edit()
            .putStringSet(KEY_PRIMARY_TASK_IDS, taskIds)
            .apply()
    }

    fun getPrimaryTaskIds(): Set<String> {
        return preferences.getStringSet(KEY_PRIMARY_TASK_IDS, emptySet()).orEmpty()
    }

    fun saveFriendCount(count: Int) {
        preferences.edit()
            .putInt(KEY_FRIEND_COUNT, count.coerceAtLeast(0))
            .apply()
    }

    fun getFriendCount(): Int {
        return preferences.getInt(KEY_FRIEND_COUNT, 0).coerceAtLeast(0)
    }

    fun consumeNextSplashMotivationQuoteIndex(quoteCount: Int): Int {
        if (quoteCount <= 0) return 0
        val index = preferences.getInt(KEY_SPLASH_QUOTE_INDEX, 0) % quoteCount
        preferences.edit()
            .putInt(KEY_SPLASH_QUOTE_INDEX, (index + 1) % quoteCount)
            .apply()
        return index
    }

    fun getLastFavPopupMessage(email: String): String? {
        return preferences.getString(favPopupMessageKey(email), null)?.takeIf { it.isNotBlank() }
    }

    fun setLastFavPopupMessage(email: String, message: String) {
        preferences.edit()
            .putString(favPopupMessageKey(email), message)
            .apply()
    }

    private fun favPopupMessageKey(email: String): String {
        return "$KEY_LAST_FAV_POPUP_MESSAGE:${email.lowercase(Locale.getDefault())}"
    }

    private companion object {
        private const val PREFERENCES_NAME = "noor_session"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_UID = "uid"
        private const val KEY_EMAIL = "email"
        private const val KEY_NAME = "name"
        private const val KEY_GENDER = "gender"
        private const val KEY_PRIMARY_TASK_IDS = "primary_task_ids"
        private const val KEY_LAST_FAV_POPUP_MESSAGE = "last_fav_popup_message"
        private const val KEY_ADMIN_AUTHENTICATED = "admin_authenticated"
        private const val KEY_FRIEND_COUNT = "friend_count"
        private const val KEY_SPLASH_QUOTE_INDEX = "splash_quote_index"
    }
}
