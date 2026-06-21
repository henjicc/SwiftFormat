package com.henjicc.swiftformat.di

import android.content.Context
import com.henjicc.swiftformat.core.common.AndroidLogger
import com.henjicc.swiftformat.core.common.Logger
import com.henjicc.swiftformat.core.datastore.SettingsRepository

/**
 * 第一版的轻量手动依赖容器（见 SPEC 9.4「初期不过度模块化」）。
 * 后续可整体替换为 Hilt，替换点局限在 Application 与各 ViewModel 工厂。
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val logger: Logger = AndroidLogger
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
}
