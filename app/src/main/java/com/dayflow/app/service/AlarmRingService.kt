package com.dayflow.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.dayflow.app.MainActivity
import com.dayflow.app.data.AlarmScheduler

/** Plays a looping alarm sound on the ALARM audio stream until stopped, snoozed or timed out. */
class AlarmRingService : Service() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoStop = Runnable { stopRinging() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getIntExtra(AlarmScheduler.EXTRA_ID, -1) ?: -1
        val label = intent?.getStringExtra(AlarmScheduler.EXTRA_LABEL).orEmpty().ifBlank { "DAYFLOW alarm" }
        when (intent?.action) {
            ACTION_STOP -> stopRinging()
            ACTION_SNOOZE -> {
                if (id >= 0) AlarmScheduler.scheduleSnooze(this, id, label)
                stopRinging()
            }
            else -> {
                val duration = intent?.getLongExtra(EXTRA_DURATION_MS, DEFAULT_DURATION_MS) ?: DEFAULT_DURATION_MS
                startRinging(id, label, duration)
            }
        }
        return START_NOT_STICKY
    }

    private fun startRinging(id: Int, label: String, durationMs: Long) {
        createChannel()
        val notification = buildNotification(id, label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        startSound()
        startVibration()
        handler.removeCallbacks(autoStop)
        handler.postDelayed(autoStop, durationMs)
    }

    private fun startSound() {
        stopSound()
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        player = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmRingService, uri)
                isLooping = true
                prepare()
                start()
            }
        }.getOrNull()
    }

    private fun stopSound() {
        player?.let {
            runCatching { if (it.isPlaying) it.stop() }
            runCatching { it.release() }
        }
        player = null
    }

    private fun startVibration() {
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator = v
        runCatching { v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 600, 400), 0)) }
    }

    private fun stopRinging() {
        handler.removeCallbacks(autoStop)
        stopSound()
        vibrator?.cancel()
        vibrator = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.deleteNotificationChannel("dayflow_alarms")
        // Sound is played by this service itself, so the channel stays silent to avoid doubling.
        val channel = NotificationChannel(CHANNEL_ID, "DAYFLOW alarm ringing", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun serviceIntent(action: String, id: Int, label: String, request: Int): PendingIntent =
        PendingIntent.getService(
            this, request,
            Intent(this, AlarmRingService::class.java).setAction(action)
                .putExtra(AlarmScheduler.EXTRA_ID, id)
                .putExtra(AlarmScheduler.EXTRA_LABEL, label),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun buildNotification(id: Int, label: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = serviceIntent(ACTION_STOP, id, label, 1)
        val snooze = serviceIntent(ACTION_SNOOZE, id, label, 2)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("It's time")
            .setContentText(label)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(open)
            .setDeleteIntent(stop)
            .addAction(0, "Snooze ${AlarmScheduler.SNOOZE_MINUTES} min", snooze)
            .addAction(0, "Stop", stop)
            .build()
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoStop)
        stopSound()
        vibrator?.cancel()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_DURATION_MS = "duration_ms"
        const val DEFAULT_DURATION_MS = 90_000L
        private const val CHANNEL_ID = "dayflow_alarm_ring"
        private const val NOTIFICATION_ID = 7001
        private const val ACTION_STOP = "com.dayflow.app.action.ALARM_STOP"
        private const val ACTION_SNOOZE = "com.dayflow.app.action.ALARM_SNOOZE"
    }
}
