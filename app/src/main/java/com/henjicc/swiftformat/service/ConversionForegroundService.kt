package com.henjicc.swiftformat.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.henjicc.swiftformat.MainActivity
import com.henjicc.swiftformat.R
import com.henjicc.swiftformat.SwiftFormatApplication
import com.henjicc.swiftformat.conversion.ConversionBatchSummary
import com.henjicc.swiftformat.conversion.ConversionOrchestrator
import com.henjicc.swiftformat.conversion.ConversionTask
import com.henjicc.swiftformat.core.model.ConversionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 转换执行的前台服务（见 SPEC 13.1）：只负责"保活 + 展示通知 + 处理取消"，
 * 真正的调度/并发/状态机在 [ConversionOrchestrator]（由 [SwiftFormatApplication] 持有，
 * 生命周期不依赖本 Service）。本服务只是观察者，被杀掉不影响已入队任务的最终一致性
 * （历史记录已经在 Room 里），只是用户会失去通知与"保活"效果。
 */
class ConversionForegroundService : Service() {

    private val orchestrator: ConversionOrchestrator
        get() = (application as SwiftFormatApplication).container.conversionOrchestrator
    private val settingsRepository
        get() = (application as SwiftFormatApplication).container.settingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var hasStartedForeground = false
    private var hasObservedActiveTask = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        serviceScope.launch {
            orchestrator.tasks.collect { tasks -> onTasksChanged(tasks.values) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL_ALL -> orchestrator.cancelAll()
            ACTION_CANCEL_TASK -> intent.getStringExtra(EXTRA_TASK_ID)?.let(orchestrator::cancel)
        }
        startForeground(NOTIFICATION_ID, buildNotification(orchestrator.tasks.value.values))
        hasStartedForeground = true
        return START_NOT_STICKY
    }

    /** API 35+：达到系统给前台服务的时间配额时回调，必须在几秒内 stopSelf，否则触发 ANR。 */
    override fun onTimeout(startId: Int, fgsType: Int) {
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun onTasksChanged(tasks: Collection<ConversionTask>) {
        val activeCount = tasks.count { it.status.isActive() }
        if (activeCount > 0) {
            hasObservedActiveTask = true
        }
        if (activeCount == 0) {
            // 忽略服务启动后的第一帧空任务，避免在 startForeground() 之前就 stopSelf()
            // 触发“startForegroundService 后未及时进入前台”的系统崩溃。
            if (!hasStartedForeground || !hasObservedActiveTask) return
            serviceScope.launch {
                val settings = settingsRepository.settings.first()
                if (settings.showCompletionNotification) {
                    NotificationManagerCompat.from(this@ConversionForegroundService).notify(
                        COMPLETION_NOTIFICATION_ID,
                        buildCompletionNotification(ConversionBatchSummary.from(tasks)),
                    )
                }
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification(tasks))
    }

    private fun buildNotification(tasks: Collection<ConversionTask>): android.app.Notification {
        val activeTasks = tasks.filter { it.status.isActive() }
        val currentFile = activeTasks.firstOrNull()?.request?.input?.displayName ?: ""
        val overallPercent = overallPercent(tasks)

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val cancelAllIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, ConversionForegroundService::class.java).setAction(ACTION_CANCEL_ALL),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_title, activeTasks.size))
            .setContentText(getString(R.string.notification_text, currentFile, overallPercent))
            .setProgress(100, overallPercent, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .addAction(0, getString(R.string.notification_action_cancel_all), cancelAllIntent)
            .build()
    }

    private fun buildCompletionNotification(summary: ConversionBatchSummary): android.app.Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                getString(
                    if (summary.failed == 0 && summary.cancelled == 0) {
                        R.string.notification_complete_title_success
                    } else {
                        R.string.notification_complete_title_mixed
                    },
                ),
            )
            .setContentText(
                getString(
                    R.string.notification_complete_text,
                    summary.completed,
                    summary.failed,
                    summary.cancelled,
                ),
            )
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
    }

    /** 已完成任务记 100%，进行中任务按自身 [ConversionTask.progress] 折算，取整体均值。 */
    private fun overallPercent(tasks: Collection<ConversionTask>): Int {
        if (tasks.isEmpty()) return 0
        val sum = tasks.sumOf { task ->
            when (task.status) {
                ConversionStatus.COMPLETED -> 1.0
                ConversionStatus.FAILED, ConversionStatus.CANCELLED -> 1.0
                else -> task.progress.toDouble()
            }
        }
        return ((sum / tasks.size) * 100).toInt().coerceIn(0, 100)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "conversion_progress"
        private const val NOTIFICATION_ID = 1
        private const val COMPLETION_NOTIFICATION_ID = 2
        private const val ACTION_CANCEL_ALL = "com.henjicc.swiftformat.action.CANCEL_ALL"
        private const val ACTION_CANCEL_TASK = "com.henjicc.swiftformat.action.CANCEL_TASK"
        private const val EXTRA_TASK_ID = "task_id"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, ConversionForegroundService::class.java))
        }

        fun cancelTaskIntent(context: Context, taskId: String): Intent =
            Intent(context, ConversionForegroundService::class.java)
                .setAction(ACTION_CANCEL_TASK)
                .putExtra(EXTRA_TASK_ID, taskId)
    }
}

private fun ConversionStatus.isActive(): Boolean = this in setOf(
    ConversionStatus.PENDING,
    ConversionStatus.PREPARING,
    ConversionStatus.CONVERTING,
    ConversionStatus.SAVING,
)
