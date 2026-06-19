package com.parhar.noor.data.reminders

import android.util.Log

object ReminderObserverDebug {

    private const val TAG = "ReminderObserver"

    fun notify(message: String) {
        Log.d(TAG, message)
    }

    fun warn(message: String) {
        Log.w(TAG, message)
    }
}
