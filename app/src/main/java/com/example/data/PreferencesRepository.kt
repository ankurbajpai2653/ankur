package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.MqttConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("imei_mqtt_pusher_prefs", Context.MODE_PRIVATE)

    private val _configFlow = MutableStateFlow(loadConfig())
    val configFlow: StateFlow<MqttConfig> = _configFlow.asStateFlow()

    fun loadConfig(): MqttConfig {
        return MqttConfig(
            brokerHost = prefs.getString("broker_host", "broker.emqx.io") ?: "broker.emqx.io",
            brokerPort = prefs.getInt("broker_port", 1883),
            useSsl = prefs.getBoolean("use_ssl", false),
            clientId = prefs.getString("client_id", "") ?: "",
            username = prefs.getString("username", "") ?: "",
            password = prefs.getString("password", "") ?: "",
            cleanSession = prefs.getBoolean("clean_session", true),
            keepAliveSeconds = prefs.getInt("keep_alive_seconds", 60),
            publishTopic = prefs.getString("publish_topic", "devices/{imei}/telemetry") ?: "devices/{imei}/telemetry",
            subscribeTopic = prefs.getString("subscribe_topic", "devices/{imei}/commands") ?: "devices/{imei}/commands",
            qos = prefs.getInt("qos", 1),
            isRetained = prefs.getBoolean("is_retained", false),
            pushIntervalSeconds = prefs.getInt("push_interval_seconds", 5),
            customImei = prefs.getString("custom_imei", "") ?: "",
            customJsonTemplate = prefs.getString("custom_json_template", null) ?: MqttConfig().customJsonTemplate
        )
    }

    fun saveConfig(config: MqttConfig) {
        prefs.edit()
            .putString("broker_host", config.brokerHost)
            .putInt("broker_port", config.brokerPort)
            .putBoolean("use_ssl", config.useSsl)
            .putString("client_id", config.clientId)
            .putString("username", config.username)
            .putString("password", config.password)
            .putBoolean("clean_session", config.cleanSession)
            .putInt("keep_alive_seconds", config.keepAliveSeconds)
            .putString("publish_topic", config.publishTopic)
            .putString("subscribe_topic", config.subscribeTopic)
            .putInt("qos", config.qos)
            .putBoolean("is_retained", config.isRetained)
            .putInt("push_interval_seconds", config.pushIntervalSeconds)
            .putString("custom_imei", config.customImei)
            .putString("custom_json_template", config.customJsonTemplate)
            .apply()

        _configFlow.value = config
    }
}
