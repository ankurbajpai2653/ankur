package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.PreferencesRepository
import com.example.data.TelemetryManager
import com.example.mqtt.MqttClientManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MqttPusherService : Service() {

    companion object {
        const val CHANNEL_ID = "imei_mqtt_pusher_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_SINGLE_PUSH = "com.example.service.ACTION_SINGLE_PUSH"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _packetsPushed = MutableStateFlow(0)
        val packetsPushed: StateFlow<Int> = _packetsPushed.asStateFlow()

        private val _lastPushTime = MutableStateFlow(0L)
        val lastPushTime: StateFlow<Long> = _lastPushTime.asStateFlow()

        // Shared singleton for service and UI
        var sharedMqttManager: MqttClientManager? = null

        fun start(context: Context) {
            val intent = Intent(context, MqttPusherService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MqttPusherService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var pusherJob: Job? = null
    private lateinit var prefsRepo: PreferencesRepository
    private lateinit var telemetryManager: TelemetryManager

    override fun onCreate() {
        super.onCreate()
        prefsRepo = PreferencesRepository(applicationContext)
        telemetryManager = TelemetryManager(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundPusher()
            }
            ACTION_STOP -> {
                stopPusher()
            }
        }
        return START_STICKY
    }

    private fun startForegroundPusher() {
        _isRunning.value = true
        startForeground(NOTIFICATION_ID, buildNotification("Starting MQTT Pusher..."))

        pusherJob?.cancel()
        pusherJob = serviceScope.launch {
            val config = prefsRepo.loadConfig()
            val effectiveImei = telemetryManager.getEffectiveImei(config)

            val mqttManager = sharedMqttManager ?: MqttClientManager(applicationContext).also {
                sharedMqttManager = it
            }

            if (!mqttManager.isConnected()) {
                mqttManager.connect(config, effectiveImei)
                // Wait briefly for connection
                var attempts = 0
                while (!mqttManager.isConnected() && attempts < 15 && isActive) {
                    delay(1000)
                    attempts++
                }
            }

            while (isActive) {
                val currentConfig = prefsRepo.loadConfig()
                val telemetry = telemetryManager.collectTelemetry(currentConfig)
                val payload = telemetryManager.formatPayload(currentConfig.customJsonTemplate, telemetry)
                val topic = telemetryManager.formatTopic(currentConfig.publishTopic, telemetry.imei)

                mqttManager.publish(
                    topic = topic,
                    payload = payload,
                    qos = currentConfig.qos,
                    retained = currentConfig.isRetained
                ) { success, _ ->
                    if (success) {
                        _packetsPushed.value += 1
                        _lastPushTime.value = System.currentTimeMillis()
                        updateNotification(
                            "Pushed: ${_packetsPushed.value} pkts | IMEI: ${telemetry.imei.takeLast(6)}"
                        )
                    }
                }

                val intervalMs = (currentConfig.pushIntervalSeconds.coerceAtLeast(1)) * 1000L
                delay(intervalMs)
            }
        }
    }

    private fun stopPusher() {
        _isRunning.value = false
        pusherJob?.cancel()
        pusherJob = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "IMEI MQTT Pusher Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows status of periodic MQTT telemetry pusher"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MqttPusherService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("IMEI MQTT Pusher Running")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        _isRunning.value = false
        pusherJob?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
