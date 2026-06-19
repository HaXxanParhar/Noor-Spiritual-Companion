package com.parhar.noor

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.parhar.noor.di.AppContainer

class NoorApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = true
        appContainer = AppContainer(this)
    }
}
