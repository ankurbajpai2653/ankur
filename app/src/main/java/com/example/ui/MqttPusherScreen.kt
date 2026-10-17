package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ConnectionState
import com.example.ui.components.BrokerConfigTab
import com.example.ui.components.DashboardTab
import com.example.ui.components.LogsTab
import com.example.ui.components.PayloadConfigTab
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MqttPusherScreen(
    viewModel: MqttPusherViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val telemetry by viewModel.currentTelemetry.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val publishedCount by viewModel.publishedCount.collectAsStateWithLifecycle()
    val receivedCount by viewModel.receivedCount.collectAsStateWithLifecycle()
    val isServiceRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle()
    val isPublishingNow by viewModel.isPublishingNow.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val logFilter by viewModel.logFilter.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    val formattedPayload = remember(config, telemetry) {
        viewModel.telemetryManager.formatPayload(config.customJsonTemplate, telemetry)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "IMEI MQTT Pusher",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = config.brokerHost,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Connection Status Badge
                    val statusColor = when (connectionState) {
                        ConnectionState.CONNECTED -> EmeraldGreen
                        ConnectionState.CONNECTING -> AmberWarning
                        ConnectionState.DISCONNECTED -> TextMuted
                        ConnectionState.ERROR -> CrimsonError
                    }
                    val statusText = when (connectionState) {
                        ConnectionState.CONNECTED -> "ONLINE"
                        ConnectionState.CONNECTING -> "CONNECTING"
                        ConnectionState.DISCONNECTED -> "OFFLINE"
                        ConnectionState.ERROR -> "ERROR"
                    }

                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .let {
                                        if (connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.CONNECTING) {
                                            it.alpha(pulseAlpha)
                                        } else it
                                    }
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Text(
                                text = statusText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }

                    // Quick Connect / Disconnect Action Icon
                    if (connectionState == ConnectionState.CONNECTED) {
                        IconButton(onClick = { viewModel.disconnect() }) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Disconnect",
                                tint = CrimsonError
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { viewModel.connect() },
                            enabled = connectionState != ConnectionState.CONNECTING
                        ) {
                            if (connectionState == ConnectionState.CONNECTING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = CyberCyan
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Cable,
                                    contentDescription = "Connect",
                                    tint = CyberCyan
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                // Tab 0: Pusher
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Speed, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberCyan,
                        selectedTextColor = CyberCyan,
                        indicatorColor = CyberCyan.copy(alpha = 0.15f)
                    )
                )

                // Tab 1: Broker
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.CloudQueue, contentDescription = "Broker") },
                    label = { Text("Broker") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberCyan,
                        selectedTextColor = CyberCyan,
                        indicatorColor = CyberCyan.copy(alpha = 0.15f)
                    )
                )

                // Tab 2: Payload & Topics
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.DataObject, contentDescription = "Payload") },
                    label = { Text("Payload") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberCyan,
                        selectedTextColor = CyberCyan,
                        indicatorColor = CyberCyan.copy(alpha = 0.15f)
                    )
                )

                // Tab 3: Logs
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        if (logs.isNotEmpty()) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = CyberCyan, contentColor = Color.Black) {
                                        Text(logs.size.coerceAtMost(99).toString(), fontSize = 9.sp)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Terminal, contentDescription = "Logs")
                            }
                        } else {
                            Icon(Icons.Default.Terminal, contentDescription = "Logs")
                        }
                    },
                    label = { Text("Logs") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CyberCyan,
                        selectedTextColor = CyberCyan,
                        indicatorColor = CyberCyan.copy(alpha = 0.15f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardTab(
                    config = config,
                    telemetry = telemetry,
                    connectionState = connectionState,
                    isServiceRunning = isServiceRunning,
                    isPublishingNow = isPublishingNow,
                    formattedPayload = formattedPayload,
                    publishedCount = publishedCount,
                    onConnect = { viewModel.connect() },
                    onDisconnect = { viewModel.disconnect() },
                    onPublishOnce = { viewModel.publishSingleMessage() },
                    onToggleAutoPush = { viewModel.toggleAutoPush() },
                    onIntervalChange = { newSec -> viewModel.updateConfig { it.copy(pushIntervalSeconds = newSec) } },
                    onGenerateImei = { viewModel.generateNewImei() },
                    onResetDeviceImei = { viewModel.useDeviceDerivedImei() }
                )
                1 -> BrokerConfigTab(
                    config = config,
                    connectionState = connectionState,
                    onConfigChange = { viewModel.updateConfig(it) },
                    onApplyPreset = { viewModel.applyPreset(it) },
                    onConnect = { viewModel.connect() },
                    onDisconnect = { viewModel.disconnect() }
                )
                2 -> PayloadConfigTab(
                    config = config,
                    onConfigChange = { viewModel.updateConfig(it) },
                    onResetTemplate = { viewModel.resetTemplate() }
                )
                3 -> LogsTab(
                    logs = logs,
                    publishedCount = publishedCount,
                    receivedCount = receivedCount,
                    activeFilter = logFilter,
                    onFilterChange = { viewModel.setLogFilter(it) },
                    onClearLogs = { viewModel.clearLogs() }
                )
            }
        }
    }
}
