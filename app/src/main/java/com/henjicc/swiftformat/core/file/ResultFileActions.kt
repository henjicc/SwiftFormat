package com.henjicc.swiftformat.core.file

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.henjicc.swiftformat.core.common.Logger

/**
 * 结果文件相关的平台动作：打开、分享、查看保存位置、删除。
 * 统一收口在这里，避免 Compose 页面和 ViewModel 直接散落 Intent/ContentResolver 细节。
 */
class ResultFileActions(
    private val appContext: Context,
    private val logger: Logger,
) {

    fun open(uri: Uri): Boolean {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, appContext.contentResolver.getType(uri) ?: "*/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return startActivity("open", intent)
    }

    fun share(uri: Uri): Boolean {
        val shareIntent = Intent(Intent.ACTION_SEND)
            .setType(appContext.contentResolver.getType(uri) ?: "*/*")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val chooser = Intent.createChooser(shareIntent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return startActivity("share", chooser)
    }

    /**
     * 当前输出统一保存到 Download/转个格式，因此“查看位置”先打开系统 Downloads 界面。
     * 若后续改为按媒体类型分目录，只需调整此处实现。
     */
    fun showInFolder(): Boolean =
        startActivity("showInFolder", Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    fun delete(uri: Uri): Boolean = runCatching {
        appContext.contentResolver.delete(uri, null, null) > 0
    }.getOrElse { error ->
        logger.e(TAG, "delete failed: $uri", error)
        false
    }

    private fun startActivity(action: String, intent: Intent): Boolean = runCatching {
        appContext.startActivity(intent)
        true
    }.getOrElse { error ->
        logger.e(TAG, "$action failed", error)
        false
    }

    private companion object {
        const val TAG = "ResultFileActions"
    }
}
