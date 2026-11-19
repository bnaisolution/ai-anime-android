package org.dpdns.bnai.anime.ui.components

import org.dpdns.bnai.anime.ui.LocalAppContainer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.auth.PersistedChatMessage
import org.dpdns.bnai.anime.core.model.Project
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.brain.DirectorActionCard
import org.dpdns.bnai.anime.ui.brain.rememberSharedAIDirectorViewModel
import org.dpdns.bnai.anime.ui.designsystem.MarkdownText
import org.dpdns.bnai.anime.ui.designsystem.StatusChip
import org.dpdns.bnai.anime.ui.designsystem.StudioTokens
import org.dpdns.bnai.anime.ui.testing.TestTags
import java.time.Instant
import java.time.temporal.ChronoUnit

fun formatInt(value: Any?): String {
    return when (value) {
        null -> "0"
        is Int -> value.toString()
        is Long -> value.toString()
        is Double -> value.toLong().toString()
        is Float -> value.toLong().toString()
        is Number -> value.toLong().toString()
        is String -> value.toDoubleOrNull()?.toLong()?.toString() ?: value
        else -> value.toString()
    }
}

fun formatRelativeTime(isoString: String?): String {
    if (isoString.isNullOrBlank()) return "Just now"
    return try {
        val instant = Instant.parse(isoString)
        val now = Instant.now()
        val mins = ChronoUnit.MINUTES.between(instant, now)
        when {
            mins < 1 -> "Just now"
            mins < 60 -> "${mins}m ago"
            mins < 1440 -> "${mins / 60}h ago"
            else -> "${mins / 1440}d ago"
        }
    } catch (_: Exception) {
        isoString.take(16).replace("T", " ")
    }
}

fun cleanDisplayTitle(raw: String?, fallback: String = "Studio Item"): String {
    if (raw.isNullOrBlank()) return fallback
    val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    if (uuidRegex.matches(raw.trim())) {
        return "$fallback (#${raw.take(6)})"
    }
    return raw
}

fun friendlyPipelineStatus(rawStatus: String?): String {
    return when (val upper = rawStatus?.uppercase()?.trim()) {
        "RECONCILING" -> "Rendering..."
        "QUEUED" -> "Waiting in Queue..."
        "DISPATCHED", "ACKNOWLEDGED", "RUNNING" -> "Generating..."
        "SUCCEEDED", "COMPLETED" -> "Completed"
        "FAILED", "ERROR", "DEAD_LETTER" -> "Failed"
        "CANCELLED" -> "Cancelled"
        "IDLE", null, "" -> "Idle"
        // Raw SCREAMING_SNAKE enum values must never reach the user (B-04).
        else -> if (upper != null && upper.matches(Regex("[A-Z0-9_]+"))) humanizeStatus(upper) else rawStatus
    }
}

/** "SCENE_PLAN_READY" -> "Scene plan ready" */
internal fun humanizeStatus(rawStatus: String): String {
    val words = rawStatus.trim().lowercase().split(Regex("[_\\s]+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return rawStatus
    return words.joinToString(" ").replaceFirstChar { it.uppercase() }
}

@Composable
fun <T> ResourceStateBox(
    resource: Resource<T>,
    emptyMessage: String = "No records found for this project.",
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    content: @Composable (T) -> Unit
) {
    when (resource) {
        is Resource.Idle, is Resource.Loading -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag(TestTags.State.LOADING)
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = StudioTokens.Primary)
            }
        }
        is Resource.Empty -> {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag(TestTags.State.EMPTY),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(emptyMessage, style = MaterialTheme.typography.bodyMedium)
                    if (onRetry != null) {
                        OutlinedButton(
                            onClick = onRetry,
                            modifier = Modifier.testTag(TestTags.State.RETRY)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Refresh")
                        }
                    }
                }
            }
        }
        is Resource.Error -> {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag(TestTags.State.ERROR),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = StudioTokens.Error.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Failed to Load Data",
                        style = MaterialTheme.typography.titleMedium,
                        color = StudioTokens.Error,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = resource.message, style = MaterialTheme.typography.bodySmall)
                    if (onRetry != null) {
                        OutlinedButton(
                            onClick = onRetry,
                            modifier = Modifier.testTag(TestTags.State.RETRY)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry")
                        }
                    }
                }
            }
        }
        is Resource.Success -> {
            content(resource.data)
        }
    }
}

@Composable
fun StatusBadge(label: String) {
    StatusChip(label = label)
}

@Composable
fun InfoTooltipButton(title: String, description: String) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title, fontWeight = FontWeight.Bold) },
            text = { Text(description) },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("Got it") }
            }
        )
    }
    IconButton(
        onClick = { showDialog = true },
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Info about $title",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
    }
}

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
                        Text(cleanDisplayTitle(rawTitle), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        if (!status.isNullOrBlank()) StatusBadge(status)
                    }

                    map.entries.forEach { (k, v) ->
                        if (k !in setOf("id", "project_id", "user_id", "workspace_id", "title", "name", "status", "state", "created_at", "updated_at", "checked_at") && v != null) {
                            val label = k.replace("_", " ").replaceFirstChar { it.uppercase() }
                            val formattedVal = if (v is Number) formatInt(v) else v.toString()
                            Text("$label: $formattedVal", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    if (!timestamp.isNullOrBlank()) {
                        Text(
                            formatRelativeTime(timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSelectorBar(
    modifier: Modifier = Modifier,
    selectedProjectIdOverride: String? = null,
    persistSelection: Boolean = true,
    onProjectChanged: (String) -> Unit = {},
    onRefreshClick: (() -> Unit)? = null
) {
    val tokenManager = LocalAppContainer.current.tokenManager
    val repository = LocalAppContainer.current.repository
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    var selectedProjectId by remember { mutableStateOf(selectedProjectIdOverride ?: repository.currentProjectId()) }

    LaunchedEffect(selectedProjectIdOverride) {
        if (!selectedProjectIdOverride.isNullOrBlank()) {
            selectedProjectId = selectedProjectIdOverride
        }
    }

    LaunchedEffect(Unit) {
        when (val res = repository.fetchProjects()) {
            is Resource.Success -> {
                projects = res.data
                if (selectedProjectId.isBlank() && res.data.isNotEmpty()) {
                    selectedProjectId = res.data.first().id
                    tokenManager.saveActiveProjectId(selectedProjectId)
                    onProjectChanged(selectedProjectId)
                }
            }
            else -> {}
        }
    }

    val currentProjectName = projects.find { it.id == selectedProjectId }?.name
        ?: if (selectedProjectId.isNotBlank()) "Project #${selectedProjectId.take(6)}" else "Select Project"

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = currentProjectName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Active Studio Project") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                singleLine = true,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .testTag(TestTags.Navigation.PROJECT_SELECTOR)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                projects.forEach { proj ->
                    DropdownMenuItem(
                        text = { Text(proj.name, fontWeight = if (proj.id == selectedProjectId) FontWeight.Bold else FontWeight.Normal) },
                        onClick = {
                            selectedProjectId = proj.id
                            if (persistSelection) tokenManager.saveActiveProjectId(proj.id)
                            expanded = false
                            onProjectChanged(proj.id)
                        }
                    )
                }
            }
        }
        if (onRefreshClick != null) {
            IconButton(
                onClick = onRefreshClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag(TestTags.Navigation.PROJECT_REFRESH)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh project data")
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalAiDirectorBottomSheet(
    currentScreen: String = "STUDIO",
    currentScreenName: String = currentScreen,
    onDismiss: () -> Unit,
    onNavigateToRoute: (String) -> Unit = {},
    onOpenFullAiBrain: () -> Unit = { onNavigateToRoute("ai_brain") }
) {
    val viewModel = rememberSharedAIDirectorViewModel()
    val state by viewModel.state.collectAsState()
    val projectId = LocalAppContainer.current.repository.currentProjectId()
    val effectiveScreen = currentScreenName.ifBlank { currentScreen }
    var prompt by rememberSaveable(projectId) { mutableStateOf("") }

    LaunchedEffect(projectId) {
        viewModel.setProject(projectId)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.AiDirector.SHEET)
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StudioTokens.Primary)
                    Column {
                        Text("AI Studio Director", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (projectId.isBlank()) {
                                "Context: $effectiveScreen • Project onboarding"
                            } else {
                                "Context: $effectiveScreen • Project #${projectId.take(6)}"
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (state.messages.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearConversation) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear project conversation")
                        }
                    }
                    TextButton(onClick = {
                        onDismiss()
                        onOpenFullAiBrain()
                    }) {
                        Text("Full Screen")
                    }
                }
            }

            if (!state.activeCommandId.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = StudioTokens.Primary.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Command #${state.activeCommandId?.take(8)}: ${friendlyPipelineStatus(state.activeCommandStatus ?: "QUEUED")}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        StatusBadge(state.activeCommandStatus ?: "QUEUED")
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.messages.isEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (projectId.isBlank()) {
                                    "No project yet. Tell the Director what you want to create and it will collect the project brief before you create it."
                                } else {
                                    "Ask the AI Director about your Story Bible, Episode Script, Character DNA, or Continuity. BYOD AI is required; the Director proposes server-approved actions instead of guessing."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                            if (projectId.isBlank()) {
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onOpenFullAiBrain()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Create project in Director")
                                }
                            }
                        }
                    }
                }
                items(state.messages, key = { it.id }) { msg ->
                    val isUser = msg.role == "user"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) StudioTokens.Primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isUser) "You" else "AI Director",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUser) StudioTokens.Primary else StudioTokens.Secondary
                            )
                            MarkdownText(markdown = msg.content)
                        }
                    }
                }
                items(state.actions) { action ->
                    DirectorActionCard(
                        action = action,
                        enabled = !state.isSending,
                        onConfirm = { viewModel.confirmAction(action) }
                    )
                }
                state.error?.let { errorText ->
                    item("director_sheet_error") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StudioTokens.Error.copy(alpha = 0.12f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                errorText,
                                fontSize = 12.sp,
                                color = StudioTokens.Error,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = { Text("Ask AI Director...") },
                    modifier = Modifier.weight(1f),
                    maxLines = 3
                )
                IconButton(
                    onClick = {
                        val cleanPrompt = prompt.trim()
                        if (cleanPrompt.isEmpty() || state.isSending) return@IconButton
                        viewModel.updateInput(cleanPrompt)
                        viewModel.sendMessage(cleanPrompt, effectiveScreen)
                        prompt = ""
                    },
                    enabled = prompt.isNotBlank() && !state.isSending,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag(TestTags.AiDirector.SEND)
                        .clip(CircleShape)
                        .background(if (prompt.isNotBlank() && !state.isSending) StudioTokens.Primary else Color.Gray.copy(alpha = 0.3f))
                ) {
                    if (state.isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "Send message", tint = Color.White)
                    }
                }
            }
        }
    }
}
