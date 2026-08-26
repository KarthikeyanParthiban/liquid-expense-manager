package com.expensemanager.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.expensemanager.app.ExpenseApplication
import com.expensemanager.app.MainActivity
import com.expensemanager.app.core.model.SyncStage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground Service for Uninterrupted SMS Sync.
 * Ensures data sync never halts when the app is minimized, screen is locked, or in Doze mode.
 * Acquires a partial WakeLock and posts a live progress notification.
 */
class SmsSyncService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var wakeLock: PowerManager.WakeLock? = null
    private var syncJob: Job? = null
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "liquidexpense:sms_sync_wakelock"
        ).apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val forceFull = intent?.getBooleanExtra(EXTRA_FORCE_FULL, false) ?: false

        // Acquire WakeLock to keep CPU awake while screen is locked
        wakeLock?.acquire(10 * 60 * 1000L) // 10 min max timeout safety

        // Start Foreground immediately
        val initialNotification = buildNotification("Initializing sync...", 0, 0, true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
            startForeground(NOTIFICATION_ID, initialNotification, foregroundServiceType)
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        startSyncOperation(forceFull)

        return START_NOT_STICKY
    }

    private fun startSyncOperation(forceFull: Boolean) {
        val app = applicationContext as? ExpenseApplication ?: run {
            stopSelf()
            return
        }

        // Cancel any previous sync coroutine
        syncJob?.cancel()

        // Observe sync progress flow to update foreground notification
        serviceScope.launch {
            app.smsRepository.syncState.collectLatest { state ->
                if (state.isSyncing) {
                    val message = when (state.stage) {
                        SyncStage.SCANNING_INBOX -> "Scanning inbox for bank alerts..."
                        SyncStage.CLASSIFYING -> {
                            val merchantInfo = if (!state.latestMerchant.isNullOrEmpty()) " (${state.latestMerchant})" else ""
                            "Analyzing SMS ${state.current} of ${state.total}$merchantInfo"
                        }
                        SyncStage.FINALIZING -> "Finalizing balances & widgets..."
                        SyncStage.COMPLETED -> "Sync complete: Imported ${state.parsedTransactionsCount} transactions"
                        SyncStage.FAILED -> "Sync failed: ${state.errorMessage ?: "Unknown error"}"
                        else -> "Processing..."
                    }

                    val isIndeterminate = state.total == 0 || state.stage == SyncStage.SCANNING_INBOX || state.stage == SyncStage.FINALIZING
                    val notif = buildNotification(message, state.current, state.total, isIndeterminate)
                    notificationManager.notify(NOTIFICATION_ID, notif)
                }
            }
        }

        // Execute background sync on IO dispatcher
        syncJob = serviceScope.launch {
            try {
                app.smsRepository.syncAllInboxSms(forceFull = forceFull)
                delay(1000)
            } catch (e: Exception) {
                android.util.Log.e("SmsSyncService", "Background sync encountered error", e)
            } finally {
                releaseWakeLockAndStop()
            }
        }
    }

    private fun releaseWakeLockAndStop() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(
        contentText: String,
        current: Int,
        total: Int,
        isIndeterminate: Boolean
    ): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Syncing Transactions")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        if (total > 0 && !isIndeterminate) {
            builder.setProgress(total, current, false)
        } else {
            builder.setProgress(0, 0, true)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Transaction Sync Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress while scanning and classifying financial SMS in the background"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "sms_sync_foreground_channel"
        const val NOTIFICATION_ID = 2001
        const val EXTRA_FORCE_FULL = "extra_force_full"

        fun startSync(context: Context, forceFull: Boolean = false) {
            val intent = Intent(context, SmsSyncService::class.java).apply {
                putExtra(EXTRA_FORCE_FULL, forceFull)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
