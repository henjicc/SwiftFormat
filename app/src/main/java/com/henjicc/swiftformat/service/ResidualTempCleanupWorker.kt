package com.henjicc.swiftformat.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.henjicc.swiftformat.SwiftFormatApplication

/**
 * 清理应用缓存目录中遗留的中间文件（SPEC 12.2 / 13.2）。
 * 只删除引擎已知命名规则的临时文件，不碰 Download/转个格式 下的正式输出。
 */
class ResidualTempCleanupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as SwiftFormatApplication).container
        val logger = container.logger
        return runCatching {
            val deletedCount = container.cacheMaintenance.clearResidualTempFiles()
            logger.i(TAG, "cleanup finished, deleted=$deletedCount")
            Result.success()
        }.getOrElse { error ->
            logger.e(TAG, "cleanup failed", error)
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "residual_temp_cleanup"
        private const val TAG = "ResidualCleanup"

        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<ResidualTempCleanupWorker>().build(),
            )
        }
    }
}
