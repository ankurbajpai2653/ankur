package com.example.model

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

enum class LogType {
    SENT,
    RECEIVED,
    STATUS,
    ERROR
}

data class MqttLogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: LogType,
    val topic: String,
    val payload: String,
    val qos: Int = 0,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)

data class MqttConfig(
    val brokerHost: String = "broker.emqx.io",
    val brokerPort: Int = 1883,
    val useSsl: Boolean = false,
    val clientId: String = "",
    val username: String = "",
    val password: String = "",
    val cleanSession: Boolean = true,
    val keepAliveSeconds: Int = 60,
    val publishTopic: String = "devices/{imei}/telemetry",
    val subscribeTopic: String = "devices/{imei}/commands",
    val qos: Int = 1,
    val isRetained: Boolean = false,
    val pushIntervalSeconds: Int = 5,
    val customImei: String = "",
    val customJsonTemplate: String = """
{
  "imei": "{imei}",
  "timestamp": {timestamp},
  "datetime": "{datetime}",
  "battery": {battery},
  "charging": {charging},
  "network": "{network}",
  "latitude": {latitude},
  "longitude": {longitude},
  "status": "online"
}
""".trimIndent()
) {
    val serverUri: String
        get() {
            val protocol = if (useSsl) "ssl" else "tcp"
            return "$protocol://$brokerHost:$brokerPort"
        }
}

data class DeviceTelemetry(
    val imei: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isoDateTime: String = "",
    val batteryLevel: Int = -1,
    val isCharging: Boolean = false,
    val networkType: String = "UNKNOWN",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val model: String = android.os.Build.MODEL,
    val manufacturer: String = android.os.Build.MANUFACTURER,
    val androidVersion: String = android.os.Build.VERSION.RELEASE
)
