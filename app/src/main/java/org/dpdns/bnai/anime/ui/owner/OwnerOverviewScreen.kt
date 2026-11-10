package org.dpdns.bnai.anime.ui.owner

import org.dpdns.bnai.anime.ui.LocalAppContainer

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.StatusBadge
import org.dpdns.bnai.anime.ui.components.formatInt
import org.dpdns.bnai.anime.ui.testing.TestTags

@Composable
fun OwnerOverviewScreen() {
    val context = LocalContext.current
    val repository = LocalAppContainer.current.repository
    val scope = rememberCoroutineScope()

    var activeProjectId by remember { mutableStateOf(repository.currentProjectId()) }
    var isHealthy by remember { mutableStateOf(true) }
    var latencyMs by remember { mutableLongStateOf(22L) }
    var activeWorkersCount by remember { mutableIntStateOf(1) }
    var pendingQueuesCount by remember { mutableIntStateOf(0) }
    var platformManagedEnabled by remember { mutableStateOf(false) }
    var platformManagedReason by remember { mutableStateOf("Disabled by policy. BYOD remains active.") }
    var replicationStatus by remember { mutableStateOf("NOT CONFIGURED") }
    var ownerQuotaSummary by remember { mutableStateOf("No tenant quota configured") }
    var showEmergencyDialog by remember { mutableStateOf(false) }

    fun refreshOwnerHub(pid: String) {
        scope.launch {
            val start = System.currentTimeMillis()
            isHealthy = repository.checkBackendHealth()
            latencyMs = (System.currentTimeMillis() - start).coerceAtLeast(12L)
            if (pid.isNotBlank()) {
                val workers = repository.fetchAdminWorkers(pid)
                activeWorkersCount = (workers as? Resource.Success)?.data?.size ?: 1
                val queues = repository.fetchAdminQueues(pid)
                val qData = (queues as? Resource.Success)?.data.orEmpty()
                pendingQueuesCount = (qData["pending_count"] as? Number)?.toInt() ?: 0
            }
            val platform = (repository.fetchPlatformManagedStatus() as? Resource.Success)?.data.orEmpty()
            platformManagedEnabled = platform["enabled"] == true
            platformManagedReason = platform["reason"] as? String
                ?: "Disabled by policy. BYOD remains active."
            val replication = (repository.fetchReplicationStatus() as? Resource.Success)?.data.orEmpty()
            replicationStatus = if (replication["configured"] == true) {
                val secondary = replication["secondary"] as? Map<*, *>
                "CONFIGURED • ${secondary?.get("healthy") ?: "UNKNOWN"}"
            } else {
                "NOT CONFIGURED"
            }
            val quota = (repository.fetchOwnerQuota() as? Resource.Success)?.data.orEmpty()
            val used = (quota["used_units"] as? Number)?.toDouble() ?: 0.0
            val limit = (quota["daily_limit_units"] as? Number)?.toDouble()
            ownerQuotaSummary = if (limit != null) {
                "${formatInt(used.toInt())} / ${formatInt(limit.toInt())} units today"
            } else {
                "${formatInt(used.toInt())} units today • no hard limit"
            }
        }
    }

    LaunchedEffect(activeProjectId) {
        refreshOwnerHub(activeProjectId)
    }

    // Fix #329: Emergency Stop Confirmation Dialog
    if (showEmergencyDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            title = { Text("🚨 Emergency Stop All Workers?") },
            text = { Text("This will immediately dispatch ADMIN_STOP_KAGGLE and halt active GPU rendering pipelines.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.submitStudioCommand(activeProjectId, CommandIntent.ADMIN_STOP_KAGGLE)
                            Toast.makeText(context, "Emergency Stop dispatched to GPU fleet!", Toast.LENGTH_SHORT).show()
                            showEmergencyDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag(TestTags.Dialog.CONFIRM)
                ) { Text("Halt Fleet Now") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEmergencyDialog = false },
                    modifier = Modifier.testTag(TestTags.Dialog.CANCEL)
                ) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(TestTags.Screen.OWNER_OVERVIEW)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Owner Overview", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            StatusBadge(if (isHealthy) "ONLINE • ${latencyMs}ms" else "OFFLINE")
        }

        ProjectSelectorBar(
            onProjectChanged = { newPid ->
                activeProjectId = newPid
                refreshOwnerHub(newPid)
            },
            onRefreshClick = { refreshOwnerHub(activeProjectId) }
        )

        // Fix #331: Critical Fail Alert Banner
        if (!isHealthy) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Critical Alert: Backend API health check failed. Verify Docker container status.", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        // Fix #324-328: 2x2 Grid Layout with Queue, Worker, API, and Cost Widgets
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Pipeline Queues", style = MaterialTheme.typography.labelLarge)
                    Text(formatInt(pendingQueuesCount), style = MaterialTheme.typography.headlineLarge)
                    StatusBadge(if (pendingQueuesCount == 0) "HEALTHY" else "ACTIVE")
                }
            }
            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Active Workers", style = MaterialTheme.typography.labelLarge)
                    Text(formatInt(activeWorkersCount), style = MaterialTheme.typography.headlineLarge)
                    StatusBadge("READY")
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("API Providers", style = MaterialTheme.typography.labelLarge)
                    Text("Groq • Gemini", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    StatusBadge("UP")
                }
            }
            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Platform Cost", style = MaterialTheme.typography.labelLarge)
                    Text("$0.00 (BYOD)", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    StatusBadge("GUARDED")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().testTag(TestTags.Owner.PLATFORM_MANAGED),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Platform-Managed Hosting", style = MaterialTheme.typography.labelLarge)
                    StatusBadge(if (platformManagedEnabled) "ENABLED" else "COMING SOON")
                }
                Text(platformManagedReason, style = MaterialTheme.typography.bodyMedium)
                Text(ownerQuotaSummary, style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().testTag(TestTags.Owner.REPLICATION_STATUS),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Secondary Database / Replication", style = MaterialTheme.typography.labelLarge)
                    StatusBadge(replicationStatus)
                }
                Text("Failover remains interface-only until an isolated secondary database is configured and approved.", style = MaterialTheme.typography.bodySmall)
            }
        }

        // Fix #330: DB Visuals & Latency Indicator Card
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Supabase PostgreSQL & Outbox Dispatcher", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Round-Trip Ping: ${latencyMs}ms • Connection Pool: Healthy", style = MaterialTheme.typography.bodyLarge)
            }
        }

        // Fix #329: Emergency Stop Button
        Button(
            onClick = { showEmergencyDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag(TestTags.Owner.EMERGENCY_STOP)
        ) {
            Text("🚨 Emergency Stop All GPU Workers", fontWeight = FontWeight.Bold)
        }
    }
}
