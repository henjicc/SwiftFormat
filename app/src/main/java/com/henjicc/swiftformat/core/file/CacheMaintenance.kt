package com.henjicc.swiftformat.core.file

import android.content.Context
import java.io.File

/**
 * 应用缓存维护：供设置页“清理缓存”和后台残留临时文件清理共用。
 * 只触碰应用缓存目录，不影响 Download/转个格式 下的正式输出文件。
 */
class CacheMaintenance(context: Context) {

    private val appContext = context.applicationContext

    fun cacheSizeBytes(): Long = appContext.cacheDir.totalSize()

    fun clearAppCache(): Long {
        val cacheDir = appContext.cacheDir
        val before = cacheDir.totalSize()
        cacheDir.listFiles().orEmpty().forEach(File::deleteRecursively)
        return before
    }

    fun clearResidualTempFiles(): Int =
        appContext.cacheDir.listFiles()
            .orEmpty()
            .filter(::isResidualTempFile)
            .count { file -> file.deleteRecursively() }

    companion object {
        fun isResidualTempFile(file: File): Boolean {
            val name = file.name
            return name.startsWith("media3_") ||
                name.startsWith("ffmpeg_in_") ||
                name.startsWith("ffmpeg_out_")
        }
    }
}

private fun File.totalSize(): Long = when {
    !exists() -> 0L
    isFile -> length()
    else -> listFiles().orEmpty().sumOf(File::totalSize)
}
