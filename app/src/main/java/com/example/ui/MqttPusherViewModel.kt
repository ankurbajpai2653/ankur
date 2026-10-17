package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PreferencesRepository
import com.example.data.TelemetryManager
import com.example.model.ConnectionState
import com.example.model.DeviceTelemetry
import com.example.model.LogType
import com.example.model.MqttConfig
import com.example.model.MqttLogEntry
import com.example.mqtt.MqttClientManager
import com.example.service.MqttPusherService
import com.example.util.ImeiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MqttPusherViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = PreferencesRepository(application)
    val telemetryManager = TelemetryManager(application)

    val mqttManager: MqttClientManager = MqttPusherService.sharedMqttManager
        ?: MqttClientManager(application).also {
            MqttPusherService.sharedMqttManager = it
        }

    val config: StateFlow<MqttConfig> = prefsRepo.configFlow
    val connectionState: StateFlow<ConnectionState> = mqttManager.connectionState
    val logs: StateFlow<List<MqttLogEntry>> = mqttManager.logs
    val publishedCount: StateFlow<Int> = mqttManager.publishedCount
    val receivedCount: StateFlow<Int> = mqttManager.receivedCount

    val isServiceRunning: StateFlow<Boolean> = MqttPusherService.isRunning
    val servicePacketsPushed: StateFlow<Int> = MqttPusherService.packetsPushed

    private val _currentTelemetry = MutableStateFlow(telemetryManager.collectTelemetry(prefsRepo.loadConfig()))
    val currentTelemetry: StateFlow<DeviceTelemetry> = _currentTelemetry.asStateFlow()

    private val _isPublishingNow = MutableStateFlow(false)
    val isPublishingNow: StateFlow<Boolean> = _isPublishingNow.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _logFilter = MutableStateFlow<LogType?>(null)
    val logFilter: StateFlow<LogType?> = _logFilter.asStateFlow()

    init {
        // Refresh telemetry every 3 seconds for UI preview
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                _currentTelemetry.value = telemetryManager.collectTelemetry(config.value)
                delay(3000)
            }
        }
    }

    fun setLogFilter(filter: LogType?) {
        _logFilter.value = filter
    }

    fun updateConfig(update: (MqttConfig) -> MqttConfig) {
        val newConfig = update(config.value)
        prefsRepo.saveConfig(newConfig)
        _currentTelemetry.value = telemetryManager.collectTelemetry(newConfig)
    }

    fun applyPreset(presetName: String) {
        when (presetName) {
            "EMQX" -> {
                updateConfig {
                    it.copy(
                        brokerHost = "broker.emqx.io",
                        brokerPort = 1883,
                        useSsl = false,
                        cleanSession = true
                    )
                }
            }
            "HiveMQ" -> {
                updateConfig {
                    it.copy(
                        brokerHost = "broker.hivemq.com",
                        brokerPort = 1883,
                        useSsl = false,
                        cleanSession = true
                    )
                }
            }
            "Mosquitto" -> {
                updateConfig {
                    it.copy(
                        brokerHost = "test.mosquitto.org",
                        brokerPort = 1883,
                        useSsl = false,
                        cleanSession = true
                    )
                }
            }
        }
    }

    fun generateNewImei() {
        val newImei = ImeiUtils.generateRandomValidImei()
        updateConfig { it.copy(customImei = newImei) }
        _statusMessage.value = "Generated new valid IMEI: $newImei"
    }

    fun useDeviceDerivedImei() {
        val defaultImei = ImeiUtils.getDeviceDerivedImei(getApplication())
        updateConfig { it.copy(customImei = defaultImei) }
        _statusMessage.value = "Set to hardware-derived IMEI: $defaultImei"
    }

    fun connect() {
        val currentCfg = config.value
        val effectiveImei = telemetryManager.getEffectiveImei(currentCfg)
        mqttManager.connect(currentCfg, effectiveImei) { success, error ->
            if (success) {
                _statusMessage.value = "Connected to ${currentCfg.brokerHost}"
            } else {
                _statusMessage.value = "Connection failed: $error"
            }
        }
    }

    fun disconnect() {
        mqttManager.disconnect {
            _statusMessage.value = "Disconnected from broker"
        }
    }

    fun publishSingleMessage() {
        viewModelScope.launch(Dispatchers.IO) {
            _isPublishingNow.value = true
            val currentCfg = config.value
            val telemetry = telemetryManager.collectTelemetry(currentCfg)
            val payload = telemetryManager.formatPayload(currentCfg.customJsonTemplate, telemetry)
            val topic = telemetryManager.formatTopic(currentCfg.publishTopic, telemetry.imei)

            mqttManager.publish(
                topic = topic,
                payload = payload,
                qos = currentCfg.qos,
                retained = currentCfg.isRetained
            ) { success, error ->
                _isPublishingNow.value = false
                if (success) {
                    _statusMessage.value = "Published payload successfully to $topic"
                } else {
                    _statusMessage.value = "Publish failed: $error"
                }
            }
        }
    }

    fun toggleAutoPush() {
        val app = getApplication<Application>()
        if (isServiceRunning.value) {
            MqttPusherService.stop(app)
            _statusMessage.value = "Auto-Pusher stopped"
        } else {
            MqttPusherService.start(app)
            _statusMessage.value = "Auto-Pusher started in background"
        }
    }

    fun clearLogs() {
        mqttManager.clearLogs()
    }

    fun resetStats() {
        mqttManager.resetStats()
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun resetTemplate() {
        updateConfig { it.copy(customJsonTemplate = MqttConfig().customJsonTemplate) }
    }
}
