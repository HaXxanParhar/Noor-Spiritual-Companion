package com.parhar.noor

import android.app.Application
import com.parhar.noor.di.AppContainer

class NoorApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
