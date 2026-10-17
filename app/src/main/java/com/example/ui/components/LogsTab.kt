package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LogType
import com.example.model.MqttLogEntry
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.TechBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsTab(
    logs: List<MqttLogEntry>,
    publishedCount: Int,
    receivedCount: Int,
    activeFilter: LogType?,
    onFilterChange: (LogType?) -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

    val filteredLogs = remember(logs, activeFilter) {
        if (activeFilter == null) logs else logs.filter { it.type == activeFilter }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Stats and Clear Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Sent: $publishedCount",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreen
                )
                Text(
                    text = "Recv: $receivedCount",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TechBlue
                )
                Text(
                    text = "Logs: ${logs.size}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = onClearLogs,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("clear_logs_button")
            ) {
                Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Clear", fontSize = 12.sp)
            }
        }

        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ElevatedFilterChip(
                selected = activeFilter == null,
                onClick = { onFilterChange(null) },
                label = { Text("All", fontSize = 11.sp) },
                colors = FilterChipDefaults.elevatedFilterChipColors(
                    selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                    selectedLabelColor = CyberCyan
                )
            )

            listOf(
                LogType.SENT to "Sent",
                LogType.RECEIVED to "Recv",
                LogType.ERROR to "Errors",
                LogType.STATUS to "Status"
            ).forEach { (type, label) ->
                ElevatedFilterChip(
                    selected = activeFilter == type,
                    onClick = { onFilterChange(if (activeFilter == type) null else type) },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.elevatedFilterChipColors(
                        selectedContainerColor = when (type) {
                            LogType.SENT -> EmeraldGreen.copy(alpha = 0.2f)
                            LogType.RECEIVED -> TechBlue.copy(alpha = 0.2f)
                            LogType.ERROR -> CrimsonError.copy(alpha = 0.2f)
                            LogType.STATUS -> CyberCyan.copy(alpha = 0.2f)
                        },
                        selectedLabelColor = when (type) {
                            LogType.SENT -> EmeraldGreen
                            LogType.RECEIVED -> TechBlue
                            LogType.ERROR -> CrimsonError
                            LogType.STATUS -> CyberCyan
                        }
                    )
                )
            }
        }

        // Log Entries List
        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "No MQTT traffic logs yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Connect to a broker and push telemetry to view live packets",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("logs_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLogs, key = { it.id }) { log ->
                    LogCard(
                        log = log,
                        formattedTime = timeFormat.format(Date(log.timestamp)),
                        onCopy = { clipboardManager.setText(AnnotatedString(log.payload)) }
                    )
                }
            }
        }
    }
}

@Composable
fun LogCard(
    log: MqttLogEntry,
    formattedTime: String,
    onCopy: () -> Unit
) {
    val (typeColor, typeLabel, typeIcon) = when (log.type) {
        LogType.SENT -> Triple(EmeraldGreen, "SENT", Icons.Default.ArrowUpward)
        LogType.RECEIVED -> Triple(TechBlue, "RECV", Icons.Default.ArrowDownward)
        LogType.STATUS -> Triple(CyberCyan, "STAT", Icons.Default.Info)
        LogType.ERROR -> Triple(CrimsonError, "ERR", Icons.Default.ErrorOutline)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = typeColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = typeIcon, contentDescription = null, tint = typeColor, modifier = Modifier.size(12.dp))
                            Text(text = typeLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = typeColor)
                        }
                    }

                    Text(
                        text = log.topic,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formattedTime,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Payload Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp)
            ) {
                Text(
                    text = log.payload,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (log.type == LogType.ERROR) CrimsonError else CyberCyan,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
