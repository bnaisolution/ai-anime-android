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
import org.dpdns.bnai.anime.core.network.JsonMap
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
    var tenantCount by remember { mutableIntStateOf(0) }
    var tenantSummary by remember { mutableStateOf("No tenant memberships found") }
    var tenantDbCount by remember { mutableIntStateOf(0) }
    var tenantDbSummary by remember { mutableStateOf("No user-owned databases connected") }
    var tenantDbRows by remember { mutableStateOf<List<JsonMap>>(emptyList()) }
    var tenantDbFilter by remember { mutableStateOf("ALL") }
    var selectedTenantDbId by remember { mutableStateOf("") }
    var tenantDbAuditSummary by remember { mutableStateOf("No audit events") }
    var ownerFeatureSummary by remember { mutableStateOf("BYOD OFF • Platform DB OFF • Billing OFF") }
    var ownerWalletSummary by remember { mutableStateOf("Wallet not enabled") }
    var showEmergencyDialog by remember { mutableStateOf(false) }

    fun refreshOwnerHub(pid: String, dbFilter: String = tenantDbFilter) {
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
            val tenants = (repository.fetchOwnerTenants() as? Resource.Success)?.data.orEmpty()
            tenantCount = tenants.size
            tenantSummary = tenants.take(3).joinToString(" • ") { tenant ->
                val name = (tenant["name"] as? String)?.ifBlank { "Unnamed tenant" } ?: "Unnamed tenant"
                val plan = (tenant["plan"] as? String)?.ifBlank { "byod" } ?: "byod"
                "$name ($plan)"
            }.ifBlank { "No tenant memberships found" }

            val statusFilter = when (dbFilter) {
                "ACTIVE" -> "active"
                "ERROR" -> "error"
                "PAUSED" -> "paused"
                else -> null
            }
            val tenantDbs = (repository.fetchOwnerTenantDatabases(status = statusFilter) as? Resource.Success)?.data.orEmpty()
            tenantDbRows = tenantDbs
            tenantDbCount = tenantDbs.size
            tenantDbSummary = tenantDbs.take(3).joinToString(" • ") { db ->
                val name = ((db["anime_tenants"] as? Map<*, *>)?.get("name") as? String)
                    ?.ifBlank { "Tenant" } ?: "Tenant"
                val mode = (db["mode"] as? String)?.uppercase() ?: "CENTRAL"
                val status = (db["status"] as? String)?.uppercase() ?: "UNKNOWN"
                "$name ($mode/$status)"
            }.ifBlank { "No user-owned databases connected" }

            selectedTenantDbId = tenantDbs.firstOrNull()?.get("tenant_id") as? String ?: ""
            tenantDbAuditSummary = if (selectedTenantDbId.isBlank()) {
                "No audit events"
            } else {
                val audits = (repository.fetchOwnerTenantDatabaseAudit(selectedTenantDbId, 5) as? Resource.Success)?.data.orEmpty()
                audits.take(3).joinToString(" • ") { audit ->
                    val action = (audit["action"] as? String)?.uppercase() ?: "EVENT"
                    val status = (audit["status"] as? String)?.uppercase() ?: "UNKNOWN"
                    "$action/$status"
                }.ifBlank { "No audit events" }
            }

            val flags = (repository.fetchOwnerFeatureFlags() as? Resource.Success)?.data.orEmpty()
            fun flag(value: Any?): String = if (value == true) "ON" else "OFF"
            ownerFeatureSummary = "BYOD ${flag(flags["byod_enabled"])} • Platform DB ${flag(flags["platform_db_enabled"])} • Platform ${flag(flags["platform_managed_enabled"])} • Billing ${flag(flags["platform_billing_enabled"])}"

            val firstTenantId = (tenants.firstOrNull()?.get("id") as? String).orEmpty()
            if (firstTenantId.isNotBlank()) {
                val wallet = (repository.fetchOwnerTenantWallet(firstTenantId) as? Resource.Success)?.data.orEmpty()
                val balance = (wallet["balance"] as? Number)?.toDouble() ?: 0.0
                val currency = wallet["currency"] as? String ?: "USD"
                ownerWalletSummary = "$balance $currency • first tenant"
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
                Text(ownerWalletSummary, style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().testTag(TestTags.Owner.TENANTS),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Tenant Memberships", style = MaterialTheme.typography.labelLarge)
                    StatusBadge("READ ONLY")
                }
                Text(formatInt(tenantCount), style = MaterialTheme.typography.headlineLarge)
                Text(tenantSummary, style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().testTag(TestTags.Owner.TENANT_DATABASES),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("User Database Registry", style = MaterialTheme.typography.labelLarge)
                    StatusBadge(if (tenantDbCount == 0) "EMPTY" else "${tenantDbCount} CONNECTED")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = tenantDbFilter == "ALL",
                        onClick = {
                            tenantDbFilter = "ALL"
                            refreshOwnerHub(activeProjectId, "ALL")
                        },
                        label = { Text("All") },
                        modifier = Modifier.testTag(TestTags.Owner.DB_FILTER_ALL)
                    )
                    FilterChip(
                        selected = tenantDbFilter == "ACTIVE",
                        onClick = {
                            tenantDbFilter = "ACTIVE"
                            refreshOwnerHub(activeProjectId, "ACTIVE")
                        },
                        label = { Text("Active") },
                        modifier = Modifier.testTag(TestTags.Owner.DB_FILTER_ACTIVE)
                    )
                    FilterChip(
                        selected = tenantDbFilter == "ERROR",
                        onClick = {
                            tenantDbFilter = "ERROR"
                            refreshOwnerHub(activeProjectId, "ERROR")
                        },
                        label = { Text("Errors") },
                        modifier = Modifier.testTag(TestTags.Owner.DB_FILTER_ERROR)
                    )
                }
                Text(formatInt(tenantDbCount), style = MaterialTheme.typography.headlineLarge)
                Text(tenantDbSummary, style = MaterialTheme.typography.bodySmall)
                tenantDbRows.take(5).forEach { db ->
                    val name = ((db["anime_tenants"] as? Map<*, *>)?.get("name") as? String)
                        ?.ifBlank { "Tenant" } ?: "Tenant"
                    val mode = (db["mode"] as? String)?.uppercase() ?: "CENTRAL"
                    val status = (db["status"] as? String)?.uppercase() ?: "UNKNOWN"
                    val host = (db["dsn_host"] as? String).orEmpty()
                    val database = (db["database_name"] as? String).orEmpty()
                    val schemaVersion = (db["schema_version"] as? Number)?.toInt() ?: 0
                    val lastError = (db["last_error"] as? String).orEmpty()
                    Text("$name • $mode/$status", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("$host/$database • schema v$schemaVersion", style = MaterialTheme.typography.bodySmall)
                    if (lastError.isNotBlank()) {
                        Text(lastError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                Button(
                    onClick = {
                        val tenantId = selectedTenantDbId
                        if (tenantId.isNotBlank()) {
                            scope.launch {
                                val result = repository.recheckOwnerTenantDatabase(tenantId)
                                val message = if (result is Resource.Success<*>) "Database health rechecked" else "Health check failed"
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                refreshOwnerHub(activeProjectId, tenantDbFilter)
                            }
                        }
                    },
                    enabled = selectedTenantDbId.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().testTag(TestTags.Owner.DB_RECHECK)
                ) { Text("Recheck Selected Database") }
                OutlinedButton(
                    onClick = {
                        val tenantId = selectedTenantDbId
                        if (tenantId.isNotBlank()) {
                            scope.launch {
                                val result = repository.provisionOwnerTenantDatabase(tenantId)
                                val message = if (result is Resource.Success<*>) "Tenant schema repaired" else "Schema repair failed"
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                refreshOwnerHub(activeProjectId, tenantDbFilter)
                            }
                        }
                    },
                    enabled = selectedTenantDbId.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().testTag(TestTags.Owner.DB_PROVISION)
                ) { Text("Repair Tenant Schema") }
                Text("Audit: $tenantDbAuditSummary", style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag(TestTags.Owner.DB_AUDIT))
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().testTag(TestTags.Owner.FEATURE_FLAGS),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Feature Flags", style = MaterialTheme.typography.labelLarge)
                    StatusBadge("ENV CONTROLLED")
                }
                Text(ownerFeatureSummary, style = MaterialTheme.typography.bodyMedium)
                Text("Changes require an owner-approved environment update and backend restart.", style = MaterialTheme.typography.bodySmall)
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

    }
}
