package com.henjicc.swiftformat.core.localization

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.henjicc.swiftformat.core.model.AppLanguage

/**
 * 应用级语言切换封装。使用 AppCompat 的 per-app language API，
 * 让设置页切换语言后界面尽量立即刷新，并兼容 Android 13+ 系统级应用语言设置。
 */
object AppLocaleManager {

    fun apply(language: AppLanguage) {
        val locales = when (language) {
            AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
            AppLanguage.CHINESE -> LocaleListCompat.forLanguageTags("zh-CN")
            AppLanguage.ENGLISH -> LocaleListCompat.forLanguageTags("en")
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
