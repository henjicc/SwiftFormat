package com.henjicc.swiftformat.core.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/** 简单的进程内日志缓冲，供设置页“查看日志”使用。 */
object InMemoryLogStore {

    private const val MAX_ENTRIES = 200
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
    private val entries = CopyOnWriteArrayList<String>()

    fun append(level: String, tag: String, message: String) {
        val line = "${timeFormat.format(Date())} $level/$tag: $message"
        entries += line
        while (entries.size > MAX_ENTRIES) {
            entries.removeAt(0)
        }
    }

    fun snapshot(): List<String> = entries.toList()
}
