package org.dpdns.bnai.anime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.model.Project
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import java.time.Duration
import java.time.Instant

// Fix #10, #280, #349, #356, #392: Never show "1.0" for integer counts
fun formatInt(value: Any?): String {
    return when (value) {
        null -> "0"
        is Number -> value.toLong().toString()
        is String -> value.toDoubleOrNull()?.toLong()?.toString() ?: value
        else -> value.toString()
    }
}

// Fix #6, #134, #317, #381: Convert ISO timestamps to human-friendly "2h ago"
fun formatRelativeTime(isoString: String?): String {
    if (isoString.isNullOrBlank()) return "Just now"
    return try {
        val parsed = Instant.parse(isoString)
        val diff = Duration.between(parsed, Instant.now())
        val mins = diff.toMinutes()
        when {
            mins < 1 -> "Just now"
            mins < 60 -> "${mins}m ago"
            mins < 1440 -> "${mins / 60}h ago"
            else -> "${mins / 1440}d ago"
        }
    } catch (_: Exception) {
        isoString.take(10)
    }
}

// Fix #133, #314, #378: Hide raw 36-char UUIDs in titles
fun cleanDisplayTitle(raw: String?): String {
    if (raw.isNullOrBlank()) return "Studio Record"
    val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    return if (uuidRegex.matches(raw.trim())) "Item #${raw.take(6).uppercase()}" else raw
}

// Fix #49: Friendly pipeline status labels
fun friendlyPipelineStatus(rawStatus: String?): String {
    val upper = rawStatus?.uppercase() ?: "IDLE"
    return when (upper) {
        "RECONCILING" -> "Rendering..."
        "QUEUED" -> "Waiting in Queue..."
        "DISPATCHED", "ACKNOWLEDGED", "RUNNING" -> "Generating..."
        "SUCCEEDED", "COMPLETED" -> "Completed"
        "MANUAL_REVIEW" -> "Needs Review"
        "FAILED" -> "Failed"
        else -> rawStatus ?: "Idle"
    }
}

@Composable
fun <T> ResourceStateBox(
    resource: Resource<T>,
    emptyMessage: String = "No records found for this project yet.",
    onRetry: (() -> Unit)? = null,
    content: @Composable (T) -> Unit
) {
    when (resource) {
        is Resource.Idle, is Resource.Loading -> {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Syncing live studio data...", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        is Resource.Empty -> {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(emptyMessage, style = MaterialTheme.typography.bodyLarge)
                    if (onRetry != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = onRetry) { Text("Refresh") }
                    }
                }
            }
        }
        is Resource.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val isForbidden = resource.code == 403
                    Text(
                        text = if (isForbidden) "Access Denied: You do not have permission for this action." else resource.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (onRetry != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onRetry) { Text("Retry") }
                    }
                }
            }
        }
        is Resource.Success -> content(resource.data)
    }
}

@Composable
fun StatusBadge(label: String) {
    val upper = label.uppercase()
    val bgColor = when {
        upper in listOf("SUCCEEDED", "ACTIVE", "COMMITTED", "PASSED", "APPROVED", "ONLINE", "COMPLETED", "HEALTHY", "UP", "ALIVE", "LOCKED", "RESOLVED") -> Color(0xFF10B981)
        upper in listOf("QUEUED", "DISPATCHED", "ACKNOWLEDGED", "RECONCILING", "RUNNING", "DRAFT", "WARNING", "PENDING", "BUSY", "RENDERING...", "WAITING IN QUEUE...", "GENERATING...") -> Color(0xFFF59E0B)
        upper in listOf("FAILED", "ERROR", "BLOCKED", "REJECTED", "OFFLINE", "DOWN", "DEAD", "CRITICAL") -> Color(0xFFEF4444)
        else -> Color(0xFF3B82F6)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = bgColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

// Fix #19, #29, #37, #148, #162, #202, #247, #262, #279, #290, #301, #360: Reusable Project Selector Bar
@Composable
fun ProjectSelectorBar(
    onProjectChanged: (String) -> Unit = {},
    onRefreshClick: (() -> Unit)? = null
) {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var activeProjectId by remember { mutableStateOf(repository.currentProjectId()) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val res = repository.fetchProjects()
        if (res is Resource.Success) {
            projects = res.data
            activeProjectId = repository.currentProjectId()
        }
    }

    val activeName = projects.find { it.id == activeProjectId }?.name ?: "Active Project"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(activeName, maxLines = 1)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                projects.forEach { proj ->
                    DropdownMenuItem(
                        text = { Text("${proj.name} (${proj.activeEpisode})") },
                        onClick = {
                            expanded = false
                            TokenManager.getInstance()?.saveActiveProjectId(proj.id)
                            activeProjectId = proj.id
                            onProjectChanged(proj.id)
                        }
                    )
                }
            }
        }
        if (onRefreshClick != null) {
            IconButton(onClick = onRefreshClick) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
    }
}

// Fix #20, #127, #355: Reusable (i) Info Tooltip Button
@Composable
fun InfoTooltipButton(title: String, explanation: String) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = { Text(explanation) },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("Got It") }
            }
        )
    }
    IconButton(onClick = { showDialog = true }, modifier = Modifier.size(24.dp)) {
        Icon(
            Icons.Default.Info,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

// Clean Card List without raw UUIDs or decimal .0 bugs
@Composable
fun KeyValueMapList(items: List<JsonMap>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(items) { map ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val rawTitle = (map["title"] ?: map["name"] ?: map["arc_name"] ?: map["power_name"]
                        ?: map["rule_code"] ?: map["location_code"] ?: map["artifact_code"]
                        ?: map["character_code"] ?: map["wf_key"] ?: map["action"] ?: "Studio Item").toString()
                    val status = (map["status"] ?: map["state"] ?: map["verification_status"] ?: map["priority"])?.toString()
                    val timestamp = (map["updated_at"] ?: map["created_at"] ?: map["checked_at"])?.toString()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cleanDisplayTitle(rawTitle), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                        if (!status.isNullOrBlank()) StatusBadge(status)
                    }

                    map.entries.forEach { (k, v) ->
                        if (k !in setOf("id", "project_id", "user_id", "workspace_id", "title", "name", "status", "state", "created_at", "updated_at", "checked_at") && v != null) {
                            val label = k.replace("_", " ").replaceFirstChar { it.uppercase() }
                            val formattedVal = if (v is Number) formatInt(v) else v.toString()
                            Text("$label: $formattedVal", style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    if (!timestamp.isNullOrBlank()) {
                        Text(
                            formatRelativeTime(timestamp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

// Fix #77, #106, #110, #113, #114: Global Floating AI Director Bottom Sheet
@Composable
fun GlobalAiDirectorBottomSheet(
    currentScreenName: String,
    onDismiss: () -> Unit,
    onNavigateToRoute: (String) -> Unit = {}
) {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var lastReply by remember { mutableStateOf<String?>(null) }
    var dispatchedCmdId by remember { mutableStateOf<String?>(null) }

    val quickChips = listOf(
        "Generate Story Bible" to "Auto-generate a dark fantasy anime Story Bible for my project",
        "Check Continuity" to "Check current project continuity for plot holes",
        "Generate Next Episode" to "Plan and generate the next episode outline"
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("AI Director • Context: $currentScreenName", style = MaterialTheme.typography.headlineMedium)
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quickChips) { (chipTitle, chipPrompt) ->
                    AssistChip(
                        onClick = { prompt = chipPrompt },
                        label = { Text(chipTitle) }
                    )
                }
            }

            if (isThinking) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("AI Director is thinking...")
                }
            }

            lastReply?.let { reply ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(reply, style = MaterialTheme.typography.bodyLarge)
                        dispatchedCmdId?.let { cmdId ->
                            Button(onClick = {
                                onDismiss()
                                onNavigateToRoute("production/$cmdId")
                            }) {
                                Text("Track Progress in Pipeline")
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = { Text("Ask AI Director or trigger an action...") },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val q = prompt.trim()
                        if (q.isNotBlank() && !isThinking) {
                            isThinking = true
                            scope.launch {
                                when (val res = repository.chatWithBrain(q, currentScreen = currentScreenName)) {
                                    is Resource.Success -> {
                                        val text = res.data.reply ?: res.data.message ?: "Done."
                                        lastReply = text
                                        // Fix #114: Action Agent parser
                                        if (text.contains("GENERATE_EPISODE", ignoreCase = true)) {
                                            val cmd = repository.submitStudioCommand(repository.currentProjectId(), CommandIntent.GENERATE_EPISODE)
                                            dispatchedCmdId = (cmd as? Resource.Success)?.data?.command_id
                                        }
                                    }
                                    is Resource.Error -> lastReply = res.message
                                    else -> Unit
                                }
                                isThinking = false
                            }
                        }
                    },
                    enabled = !isThinking && prompt.isNotBlank()
                ) {
                    Text("Ask")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
