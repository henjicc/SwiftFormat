package com.henjicc.swiftformat.core.localization

import com.henjicc.swiftformat.core.model.AppLanguage
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLocaleManagerTest {

    @Test
    fun resolveSystemLanguage_usesChineseForSimplifiedChineseSystem() {
        assertEquals(
            AppLanguage.CHINESE,
            resolveSystemLanguage(listOf(Locale.SIMPLIFIED_CHINESE)),
        )
    }

    @Test
    fun resolveSystemLanguage_usesChineseForHansScriptSystem() {
        assertEquals(
            AppLanguage.CHINESE,
            resolveSystemLanguage(listOf(Locale.forLanguageTag("zh-Hans-CN"))),
        )
    }

    @Test
    fun resolveSystemLanguage_usesEnglishForTraditionalChineseSystem() {
        assertEquals(
            AppLanguage.ENGLISH,
            resolveSystemLanguage(listOf(Locale.TRADITIONAL_CHINESE)),
        )
    }

    @Test
    fun resolveSystemLanguage_usesEnglishWhenChineseIsNotPresent() {
        assertEquals(
            AppLanguage.ENGLISH,
            resolveSystemLanguage(listOf(Locale.US, Locale.JAPAN)),
        )
    }
}
