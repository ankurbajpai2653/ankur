package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import com.example.model.DeviceTelemetry
import com.example.model.MqttConfig
import com.example.util.ImeiUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class TelemetryManager(private val context: Context) {

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun getEffectiveImei(config: MqttConfig): String {
        return if (config.customImei.isNotBlank()) {
            config.customImei.trim()
        } else {
            ImeiUtils.getDeviceDerivedImei(context)
        }
    }

    fun collectTelemetry(config: MqttConfig): DeviceTelemetry {
        val imei = getEffectiveImei(config)
        val now = System.currentTimeMillis()
        val isoDate = isoDateFormat.format(Date(now))

        val batteryInfo = getBatteryStatus()
        val networkType = getNetworkType()
        val location = getLastKnownLocation()

        return DeviceTelemetry(
            imei = imei,
            timestamp = now,
            isoDateTime = isoDate,
            batteryLevel = batteryInfo.first,
            isCharging = batteryInfo.second,
            networkType = networkType,
            latitude = location?.latitude,
            longitude = location?.longitude,
            accuracyMeters = location?.accuracy,
            model = Build.MODEL ?: "Unknown",
            manufacturer = Build.MANUFACTURER ?: "Unknown",
            androidVersion = Build.VERSION.RELEASE ?: "Unknown"
        )
    }

    fun formatPayload(template: String, telemetry: DeviceTelemetry): String {
        val lat = telemetry.latitude ?: 0.0
        val lon = telemetry.longitude ?: 0.0

        return template
            .replace("{imei}", telemetry.imei)
            .replace("{timestamp}", telemetry.timestamp.toString())
            .replace("{datetime}", telemetry.isoDateTime)
            .replace("{battery}", telemetry.batteryLevel.toString())
            .replace("{charging}", telemetry.isCharging.toString())
            .replace("{network}", telemetry.networkType)
            .replace("{latitude}", String.format(Locale.US, "%.6f", lat))
            .replace("{longitude}", String.format(Locale.US, "%.6f", lon))
            .replace("{accuracy}", String.format(Locale.US, "%.1f", telemetry.accuracyMeters ?: 0f))
            .replace("{model}", telemetry.model)
            .replace("{manufacturer}", telemetry.manufacturer)
            .replace("{androidVersion}", telemetry.androidVersion)
    }

    fun formatTopic(topicTemplate: String, imei: String): String {
        return topicTemplate.replace("{imei}", imei)
    }

    private fun getBatteryStatus(): Pair<Int, Boolean> {
        return try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent? = context.registerReceiver(null, filter)
            val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            val batteryPct = if (level >= 0 && scale > 0) {
                ((level / scale.toFloat()) * 100).toInt()
            } else {
                100 // Emulator or fallback default
            }
            Pair(batteryPct, isCharging)
        } catch (_: Exception) {
            Pair(100, false)
        }
    }

    private fun getNetworkType(): String {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return "DISCONNECTED"
            val capabilities = cm.getNetworkCapabilities(network) ?: return "DISCONNECTED"

            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
                else -> "OTHER"
            }
        } catch (_: Exception) {
            "UNKNOWN"
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(): Location? {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val gpsLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val netLoc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val passiveLoc = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)

            gpsLoc ?: netLoc ?: passiveLoc
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
    }
}
