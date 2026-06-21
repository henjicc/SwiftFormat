package com.henjicc.swiftformat

import android.app.Application
import com.henjicc.swiftformat.di.AppContainer

class SwiftFormatApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
