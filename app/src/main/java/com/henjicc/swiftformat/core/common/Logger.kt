package com.henjicc.swiftformat.core.common

import android.util.Log

/**
 * 统一日志抽象（见 SPEC 9.1 / 16.3）。
 * 禁止记录文件正文、完整私密路径与用户媒体数据；只记录类型/扩展名/大小区间/引擎/错误码/耗时等。
 */
interface Logger {
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}

/** 默认基于 Android Logcat 的实现。 */
object AndroidLogger : Logger {
    override fun d(tag: String, message: String) {
        InMemoryLogStore.append("D", tag, message)
        Log.d(tag, message)
    }

    override fun i(tag: String, message: String) {
        InMemoryLogStore.append("I", tag, message)
        Log.i(tag, message)
    }

    override fun w(tag: String, message: String, throwable: Throwable?) {
        InMemoryLogStore.append("W", tag, throwable?.let { "$message\n${it.message}" } ?: message)
        Log.w(tag, message, throwable)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        InMemoryLogStore.append("E", tag, throwable?.let { "$message\n${it.message}" } ?: message)
        Log.e(tag, message, throwable)
    }
}
