package org.dpdns.bnai.anime.ui.owner
import androidx.compose.ui.platform.testTag
import org.dpdns.bnai.anime.ui.testing.TestTags

import org.dpdns.bnai.anime.ui.LocalAppContainer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.*

@Composable
fun AuditLogScreen() {
    val repository = LocalAppContainer.current.repository
    val scope = rememberCoroutineScope()
    val tokenManager = LocalAppContainer.current.tokenManager
    val userEmail = remember { tokenManager.getUserEmail().ifBlank { "studio@bnai.dpdns.org" } }

    var activeProjectId by remember { mutableStateOf(repository.currentProjectId()) }
    var searchQuery by remember { mutableStateOf("") }
    var auditState by remember { mutableStateOf<Resource<List<JsonMap>>>(Resource.Loading) }

    fun loadAudit(pid: String) {
        if (pid.isBlank()) return
        scope.launch {
            auditState = Resource.Loading
            auditState = repository.fetchAuditLogs(pid)
        }
    }

    LaunchedEffect(activeProjectId) {
        loadAudit(activeProjectId)
    }

    Column(
        modifier = Modifier.testTag(TestTags.Screen.AUDIT_LOG)
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Audit Activity Trail", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        ProjectSelectorBar(
            onProjectChanged = { newPid ->
                activeProjectId = newPid
                loadAudit(newPid)
            },
            onRefreshClick = { loadAudit(activeProjectId) }
        )

        // Fix #383: Search / Filter Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Filter audit events by action or state...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        ResourceStateBox(
            resource = auditState,
            emptyMessage = "No audit activity recorded for this project yet.",
            onRetry = { loadAudit(activeProjectId) }
        ) { logs ->
            val filtered = logs.filter {
                searchQuery.isBlank() || it.toString().contains(searchQuery, ignoreCase = true)
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filtered) { log ->
                    val action = (log["action"] ?: log["event_type"] ?: "STATE_TRANSITION").toString().replace("_", " ")
                    val oldVal = log["old_value"]?.toString()?.takeIf { it.isNotBlank() }
                    val newVal = log["new_value"]?.toString()?.takeIf { it.isNotBlank() }
                    val actorEmail = log["actor_email"]?.toString()?.takeIf { it.isNotBlank() } ?: userEmail
                    val createdAt = log["created_at"]?.toString()

                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text(cleanDisplayTitle(action), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                }
                                Text(formatRelativeTime(createdAt), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                            }

                            // Fix #379: Diff View (OLD ➡️ NEW)
                            if (oldVal != null || newVal != null) {
                                Text(
                                    text = "${oldVal ?: "INIT"} ➡️ ${newVal ?: "UPDATED"}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Fix #385: Show User Email instead of raw user_id UUID
                            Text("Actor: $actorEmail", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}
