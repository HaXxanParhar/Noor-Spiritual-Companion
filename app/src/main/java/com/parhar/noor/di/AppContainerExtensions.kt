package com.parhar.noor.di

import android.content.Context
import com.parhar.noor.NoorApplication

fun Context.appContainer(): AppContainer {
    return (applicationContext as NoorApplication).appContainer
}
