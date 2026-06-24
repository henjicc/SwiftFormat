package com.henjicc.swiftformat.core.localization

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import com.henjicc.swiftformat.core.model.AppLanguage
import java.util.Locale

/**
 * 应用内语言切换封装。
 *
 * 不调用 AppCompatDelegate.setApplicationLocales(...)，因为它会触发 Activity 重建，
 * 真机上表现为切换语言时短暂黑屏。这里仅为 Compose 资源读取提供本地化 Resources，
 * 其它 Context 能力仍委托给原 Activity。
 */
object AppLocaleManager {

    fun localizedContext(base: Context, language: AppLanguage): Context {
        if (language == AppLanguage.SYSTEM) return base
        val localized = base.createConfigurationContext(localizedConfiguration(base, language))
        return LocalizedResourcesContext(base, localized)
    }

    fun localizedConfiguration(base: Context, language: AppLanguage): Configuration {
        val configuration = Configuration(base.resources.configuration)
        val locale = language.toLocale() ?: return configuration
        configuration.setLocales(LocaleList(locale))
        configuration.setLayoutDirection(locale)
        return configuration
    }
}

private fun AppLanguage.toLocale(): Locale? = when (this) {
    AppLanguage.SYSTEM -> null
    AppLanguage.CHINESE -> Locale.forLanguageTag("zh-CN")
    AppLanguage.ENGLISH -> Locale.ENGLISH
}

private class LocalizedResourcesContext(
    base: Context,
    private val localized: Context,
) : ContextWrapper(base) {
    override fun getResources(): Resources = localized.resources

    override fun getAssets(): AssetManager = localized.assets
}
