package com.henjicc.swiftformat

import android.app.Application
import com.henjicc.swiftformat.di.AppContainer
import com.henjicc.swiftformat.service.ResidualTempCleanupWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SwiftFormatApplication : Application() {
    lateinit var container: AppContainer
        private set
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch {
            runCatching {
                val settings = container.settingsRepository.settings.first()
                if (settings.autoCleanupTempFiles) {
                    ResidualTempCleanupWorker.enqueue(this@SwiftFormatApplication)
                }
                container.conversionRecoveryManager.recoverActiveTasks()
            }.onFailure { error ->
                container.logger.e(TAG, "startup recovery failed", error)
            }
        }
    }

    private companion object {
        const val TAG = "SwiftFormatApp"
    }
}
