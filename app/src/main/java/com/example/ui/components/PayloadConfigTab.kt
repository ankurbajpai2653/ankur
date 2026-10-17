package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MqttConfig
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.EmeraldGreen
import com.example.util.ImeiUtils

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PayloadConfigTab(
    config: MqttConfig,
    onConfigChange: ((MqttConfig) -> MqttConfig) -> Unit,
    onResetTemplate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCustomImeiValid = remember(config.customImei) {
        if (config.customImei.isBlank()) true else ImeiUtils.isValidImei(config.customImei)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Topics & QoS
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Topic Routing & QoS",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Publish Topic
                OutlinedTextField(
                    value = config.publishTopic,
                    onValueChange = { t -> onConfigChange { it.copy(publishTopic = t) } },
                    label = { Text("Publish Topic ({imei} supported)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("publish_topic_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        focusedLabelColor = CyberCyan
                    )
                )

                // Subscribe Topic
                OutlinedTextField(
                    value = config.subscribeTopic,
                    onValueChange = { s -> onConfigChange { it.copy(subscribeTopic = s) } },
                    label = { Text("Subscribe / Command Topic") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        focusedLabelColor = CyberCyan
                    )
                )

                // QoS Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Quality of Service (QoS)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0 to "0 (At most once)", 1 to "1 (At least once)", 2 to "2 (Exactly once)").forEach { (q, label) ->
                            ElevatedFilterChip(
                                selected = config.qos == q,
                                onClick = { onConfigChange { it.copy(qos = q) } },
                                label = { Text(label, fontSize = 11.sp) },
                                leadingIcon = if (config.qos == q) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                colors = FilterChipDefaults.elevatedFilterChipColors(
                                    selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = CyberCyan
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Retain Flag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Retain Message",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Broker saves last payload for new subscribers",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = config.isRetained,
                        onCheckedChange = { r -> onConfigChange { it.copy(isRetained = r) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = CyberCyan.copy(alpha = 0.5f))
                    )
                }
            }
        }

        // Custom IMEI Override
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Manual IMEI Override",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Enter a custom 15-digit IMEI to simulate any physical device",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = config.customImei,
                    onValueChange = { newImei ->
                        onConfigChange { it.copy(customImei = newImei.filter { c -> c.isDigit() }.take(15)) }
                    },
                    label = { Text("Custom 15-digit IMEI") },
                    placeholder = { Text("Leave blank for device-derived IMEI") },
                    singleLine = true,
                    isError = config.customImei.isNotBlank() && !isCustomImeiValid,
                    supportingText = {
                        if (config.customImei.isNotBlank()) {
                            if (isCustomImeiValid) {
                                Text("Valid Luhn checksum (15 digits)", color = EmeraldGreen)
                            } else {
                                Text("Invalid Luhn checksum for 15 digits", color = AmberWarning)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        focusedLabelColor = CyberCyan
                    )
                )
            }
        }

        // Custom JSON Template Editor
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "JSON Payload Template",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedButton(
                        onClick = onResetTemplate,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset Template", fontSize = 11.sp)
                    }
                }

                Text(
                    text = "Tap tokens below to inject dynamic sensor data:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Placeholder chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val tokens = listOf(
                        "{imei}", "{timestamp}", "{datetime}",
                        "{battery}", "{charging}", "{network}",
                        "{latitude}", "{longitude}", "{accuracy}",
                        "{model}", "{manufacturer}"
                    )
                    tokens.forEach { token ->
                        AssistChip(
                            onClick = {
                                onConfigChange {
                                    it.copy(customJsonTemplate = it.customJsonTemplate + "\n  \"field\": $token,")
                                }
                            },
                            label = { Text(token, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp)) },
                            colors = AssistChipDefaults.assistChipColors(labelColor = CyberCyan)
                        )
                    }
                }

                OutlinedTextField(
                    value = config.customJsonTemplate,
                    onValueChange = { t -> onConfigChange { it.copy(customJsonTemplate = t) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("payload_template_editor"),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkBorder
                    )
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
