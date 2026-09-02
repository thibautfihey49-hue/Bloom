package com.bloom.parental.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class EnvironmentMonitorService : Service() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val CHANNEL_ID = "bloom_env_monitor"
    private val NOTIFICATION_ID = 1
    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    companion object {
        private val _soundLevel = MutableStateFlow(0)
        val soundLevel: StateFlow<Int> = _soundLevel

        fun start(context: Context) {
            val intent = Intent(context, EnvironmentMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        fun stop(context: Context) {
            context.stopService(Intent(context, EnvironmentMonitorService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRecording) startAudioMonitoring()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopAudioMonitoring()
        job.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Surveillance ambiante", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Niveau sonore ambiant"
                    setShowBadge(false); enableVibration(false); setSound(null, null)
                }
            )
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Bloom")
            .setContentText("Surveillance active")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun startAudioMonitoring() {
        if (isRecording) return
        isRecording = true

        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2

        try {
            @Suppress("DEPRECATION")
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord?.startRecording()
                scope.launch {
                    val buffer = ShortArray(bufferSize / 2)
                    while (isRecording) {
                        try {
                            val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                            if (read > 0) {
                                var sum = 0.0
                                for (i in 0 until read) sum += buffer[i] * buffer[i]
                                val rms = sqrt(sum / read)
                                val db = if (rms > 0) (20 * kotlin.math.log10(rms / 32767.0)).toInt() + 100 else 0
                                _soundLevel.value = db.coerceIn(0, 120)
                                BloomSmsManager.sendDb(this@EnvironmentMonitorService, db)
                            }
                            delay(300000)
                        } catch (e: Exception) { delay(1000) }
                    }
                }
            }
        } catch (e: SecurityException) { isRecording = false }
    }

    private fun stopAudioMonitoring() {
        isRecording = false
        audioRecord?.apply {
            if (state == AudioRecord.STATE_INITIALIZED && recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                stop()
            }
            release()
        }
        audioRecord = null
    }
}
