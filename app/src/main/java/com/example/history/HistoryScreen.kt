package com.example.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gesturerecognition.CoreGestureType
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlassCard
import com.example.ui.theme.LocalAppLanguage
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.NeonMint
import com.example.ui.theme.StatusPill
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    logs: List<ActivityLogEntity>,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val lang = LocalAppLanguage.current
    var selectedFilter by remember { mutableStateOf("ALL") }
    var inspectedLog by remember { mutableStateOf<ActivityLogEntity?>(null) }

    val filteredLogs = remember(logs, selectedFilter) {
        if (selectedFilter == "ALL") logs else logs.filter { it.gestureKey == selectedFilter }
    }

    val dateFormatter = remember(lang) {
        SimpleDateFormat("yyyy-MM-dd", if (lang == "ar") Locale("ar") else Locale.US)
    }
    val timeFormatter = remember(lang) {
        SimpleDateFormat("HH:mm:ss", if (lang == "ar") Locale("ar") else Locale.US)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.historyTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (logs.isNotEmpty()) {
                    TextButton(
                        onClick = onClearHistory,
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = CrimsonAlert)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.clearHistory, color = CrimsonAlert)
                    }
                }
            }
        }

        // Filter Chips by Gesture
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("${strings.filterAll} (${logs.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricBlue.copy(alpha = 0.25f),
                            selectedLabelColor = ElectricBlue
                        )
                    )
                }
                items(CoreGestureType.entries) { g ->
                    FilterChip(
                        selected = selectedFilter == g.name,
                        onClick = { selectedFilter = g.name },
                        label = {
                            Text("${g.badgeSymbol} ${if (lang == "ar") g.titleAr else g.titleEn}")
                        }
                    )
                }
            }
        }

        if (filteredLogs.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = strings.emptyHistory,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(filteredLogs, key = { it.id }) { item ->
                val date = Date(item.timestampMillis)
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    glowColor = if (item.succeeded) NeonMint else CrimsonAlert,
                    onClick = { inspectedLog = item }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.gestureLabel,
                                style = MaterialTheme.typography.titleMedium,
                                color = ElectricBlue
                            )
                            Text(
                                text = item.actionLabel,
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = "${dateFormatter.format(date)}  •  ${timeFormatter.format(date)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }

                        StatusPill(
                            label = if (item.succeeded) strings.succeeded else strings.failed,
                            active = item.succeeded,
                            icon = if (item.succeeded) Icons.Filled.CheckCircle else Icons.Filled.Error,
                            activeColor = NeonMint,
                            inactiveColor = CrimsonAlert
                        )
                    }
                }
            }
        }
    }

    inspectedLog?.let { log ->
        val date = Date(log.timestampMillis)
        AlertDialog(
            onDismissRequest = { inspectedLog = null },
            title = { Text(log.gestureLabel, color = ElectricBlue) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${log.actionLabel} (${log.actionKey})", color = TextPrimary)
                    Text(log.detailMessage, color = TextSecondary)
                    Text("${strings.confidenceLabel}: ${log.confidencePercent}%", color = NeonMint)
                    Text("${dateFormatter.format(date)} ${timeFormatter.format(date)}", color = TextSecondary)
                }
            },
            confirmButton = {
                TextButton(onClick = { inspectedLog = null }) {
                    Text(strings.close, color = ElectricBlue)
                }
            }
        )
    }
}
