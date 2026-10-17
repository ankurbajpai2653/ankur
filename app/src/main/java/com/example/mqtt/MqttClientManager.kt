package com.example.mqtt

import android.content.Context
import com.example.model.ConnectionState
import com.example.model.LogType
import com.example.model.MqttConfig
import com.example.model.MqttLogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.nio.charset.StandardCharsets

class MqttClientManager(
    @Suppress("unused") private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private var client: MqttAsyncClient? = null
    private var currentConfig: MqttConfig? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _logs = MutableStateFlow<List<MqttLogEntry>>(emptyList())
    val logs: StateFlow<List<MqttLogEntry>> = _logs.asStateFlow()

    private val _publishedCount = MutableStateFlow(0)
    val publishedCount: StateFlow<Int> = _publishedCount.asStateFlow()

    private val _receivedCount = MutableStateFlow(0)
    val receivedCount: StateFlow<Int> = _receivedCount.asStateFlow()

    private fun addLog(entry: MqttLogEntry) {
        scope.launch {
            val currentList = _logs.value.toMutableList()
            currentList.add(0, entry)
            // Keep recent 200 logs to prevent memory buildup
            if (currentList.size > 200) {
                _logs.value = currentList.subList(0, 200)
            } else {
                _logs.value = currentList
            }
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun resetStats() {
        _publishedCount.value = 0
        _receivedCount.value = 0
    }

    fun isConnected(): Boolean {
        return client?.isConnected == true
    }

    @Synchronized
    fun connect(config: MqttConfig, effectiveImei: String, onComplete: ((Boolean, String?) -> Unit)? = null) {
        currentConfig = config

        val clientId = if (config.clientId.isNotBlank()) {
            config.clientId.trim()
        } else {
            "IMEI_${effectiveImei.takeLast(6)}_${System.currentTimeMillis() % 10000}"
        }

        val serverUri = config.serverUri

        addLog(
            MqttLogEntry(
                type = LogType.STATUS,
                topic = "System",
                payload = "Initiating connection to $serverUri (Client ID: $clientId)..."
            )
        )
        _connectionState.value = ConnectionState.CONNECTING

        scope.launch(Dispatchers.IO) {
            try {
                // Disconnect and close existing client if any
                client?.let {
                    try {
                        if (it.isConnected) {
                            it.disconnect(0)
                        }
                        it.close()
                    } catch (_: Exception) {}
                }

                val newClient = MqttAsyncClient(serverUri, clientId, MemoryPersistence())
                client = newClient

                newClient.setCallback(object : MqttCallbackExtended {
                    override fun connectComplete(reconnect: Boolean, s: String?) {
                        _connectionState.value = ConnectionState.CONNECTED
                        addLog(
                            MqttLogEntry(
                                type = LogType.STATUS,
                                topic = "Connection",
                                payload = if (reconnect) "Reconnected to $serverUri" else "Connected successfully to $serverUri"
                            )
                        )

                        // Auto-subscribe to configured subscribe topic if provided
                        val subTopic = config.subscribeTopic.replace("{imei}", effectiveImei)
                        if (subTopic.isNotBlank()) {
                            subscribe(subTopic, config.qos)
                        }
                    }

                    override fun connectionLost(cause: Throwable?) {
                        _connectionState.value = ConnectionState.DISCONNECTED
                        addLog(
                            MqttLogEntry(
                                type = LogType.ERROR,
                                topic = "Connection",
                                payload = "Connection lost: ${cause?.localizedMessage ?: "Unknown reason"}",
                                isSuccess = false
                            )
                        )
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        _receivedCount.value += 1
                        val payloadStr = message?.payload?.let { String(it, StandardCharsets.UTF_8) } ?: ""
                        addLog(
                            MqttLogEntry(
                                type = LogType.RECEIVED,
                                topic = topic ?: "unknown",
                                payload = payloadStr,
                                qos = message?.qos ?: 0
                            )
                        )
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {
                        // Handled in publish listener
                    }
                })

                val options = MqttConnectOptions().apply {
                    isCleanSession = config.cleanSession
                    keepAliveInterval = config.keepAliveSeconds
                    connectionTimeout = 15
                    isAutomaticReconnect = true

                    if (config.username.isNotBlank()) {
                        userName = config.username
                    }
                    if (config.password.isNotBlank()) {
                        password = config.password.toCharArray()
                    }
                }

                newClient.connect(options, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        _connectionState.value = ConnectionState.CONNECTED
                        onComplete?.invoke(true, null)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        _connectionState.value = ConnectionState.ERROR
                        val error = exception?.localizedMessage ?: "Connection failed"
                        addLog(
                            MqttLogEntry(
                                type = LogType.ERROR,
                                topic = "Connection",
                                payload = "Connect error: $error",
                                isSuccess = false,
                                errorMessage = error
                            )
                        )
                        onComplete?.invoke(false, error)
                    }
                })

            } catch (e: Exception) {
                _connectionState.value = ConnectionState.ERROR
                val error = e.localizedMessage ?: "Client initialization failed"
                addLog(
                    MqttLogEntry(
                        type = LogType.ERROR,
                        topic = "Connection",
                        payload = "Init error: $error",
                        isSuccess = false,
                        errorMessage = error
                    )
                )
                onComplete?.invoke(false, error)
            }
        }
    }

    fun disconnect(onComplete: (() -> Unit)? = null) {
        scope.launch(Dispatchers.IO) {
            try {
                client?.let {
                    if (it.isConnected) {
                        it.disconnect(1000, null, object : IMqttActionListener {
                            override fun onSuccess(asyncActionToken: IMqttToken?) {
                                _connectionState.value = ConnectionState.DISCONNECTED
                                addLog(
                                    MqttLogEntry(
                                        type = LogType.STATUS,
                                        topic = "Connection",
                                        payload = "Disconnected from broker."
                                    )
                                )
                                onComplete?.invoke()
                            }

                            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                                _connectionState.value = ConnectionState.DISCONNECTED
                                onComplete?.invoke()
                            }
                        })
                    } else {
                        _connectionState.value = ConnectionState.DISCONNECTED
                        onComplete?.invoke()
                    }
                } ?: run {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    onComplete?.invoke()
                }
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.DISCONNECTED
                onComplete?.invoke()
            }
        }
    }

    fun publish(
        topic: String,
        payload: String,
        qos: Int = 1,
        retained: Boolean = false,
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        val currentClient = client
        if (currentClient == null || !currentClient.isConnected) {
            val error = "Cannot publish: Not connected to MQTT broker"
            addLog(
                MqttLogEntry(
                    type = LogType.ERROR,
                    topic = topic,
                    payload = payload,
                    qos = qos,
                    isSuccess = false,
                    errorMessage = error
                )
            )
            onResult?.invoke(false, error)
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                val message = MqttMessage(payload.toByteArray(StandardCharsets.UTF_8)).apply {
                    this.qos = qos
                    isRetained = retained
                }

                currentClient.publish(topic, message, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        _publishedCount.value += 1
                        addLog(
                            MqttLogEntry(
                                type = LogType.SENT,
                                topic = topic,
                                payload = payload,
                                qos = qos,
                                isSuccess = true
                            )
                        )
                        onResult?.invoke(true, null)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        val error = exception?.localizedMessage ?: "Publish failed"
                        addLog(
                            MqttLogEntry(
                                type = LogType.ERROR,
                                topic = topic,
                                payload = payload,
                                qos = qos,
                                isSuccess = false,
                                errorMessage = error
                            )
                        )
                        onResult?.invoke(false, error)
                    }
                })
            } catch (e: MqttException) {
                val error = "Publish error: ${e.message}"
                addLog(
                    MqttLogEntry(
                        type = LogType.ERROR,
                        topic = topic,
                        payload = payload,
                        qos = qos,
                        isSuccess = false,
                        errorMessage = error
                    )
                )
                onResult?.invoke(false, error)
            }
        }
    }

    fun subscribe(topic: String, qos: Int = 1) {
        val currentClient = client
        if (currentClient == null || !currentClient.isConnected) return

        scope.launch(Dispatchers.IO) {
            try {
                currentClient.subscribe(topic, qos, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        addLog(
                            MqttLogEntry(
                                type = LogType.STATUS,
                                topic = topic,
                                payload = "Subscribed to $topic (QoS $qos)"
                            )
                        )
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        addLog(
                            MqttLogEntry(
                                type = LogType.ERROR,
                                topic = topic,
                                payload = "Failed to subscribe to $topic: ${exception?.localizedMessage}",
                                isSuccess = false
                            )
                        )
                    }
                })
            } catch (_: Exception) {}
        }
    }
}
