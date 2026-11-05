package org.dpdns.bnai.anime.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.auth.PersistedChatMessage
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.model.Project
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.designsystem.MarkdownText
import org.dpdns.bnai.anime.ui.designsystem.StatusChip
import org.dpdns.bnai.anime.ui.designsystem.StudioTokens
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
    return when (rawStatus?.uppercase()?.trim()) {
        "RECONCILING" -> "Rendering..."
        "QUEUED" -> "Waiting in Queue..."
        "DISPATCHED", "ACKNOWLEDGED", "RUNNING" -> "Generating..."
        "SUCCEEDED", "COMPLETED" -> "Completed"
        "FAILED", "ERROR", "DEAD_LETTER" -> "Failed"
        "CANCELLED" -> "Cancelled"
        "IDLE", null, "" -> "Idle"
        else -> rawStatus
    }
}

@Composable
fun <T> ResourceStateBox(
    resource: Resource<T>,
    emptyMessage: String = "No records found for this project.",
    onRetry: (() -> Unit)? = null,
    content: @Composable (T) -> Unit
) {
    when (resource) {
        is Resource.Idle, is Resource.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = StudioTokens.Primary)
            }
        }
        is Resource.Empty -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        OutlinedButton(onClick = onRetry) {
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
                modifier = Modifier.fillMaxWidth(),
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
                        OutlinedButton(onClick = onRetry) {
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
                TextButton(onClick = { showDialog = false }) {
                    Text("Got it")
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSelectorBar(
    onProjectChanged: (String) -> Unit = {},
    onRefreshClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val repository = remember { AnimeRepository() }
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    var selectedProjectId by remember { mutableStateOf(repository.currentProjectId()) }

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
        modifier = Modifier.fillMaxWidth(),
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
                            tokenManager.saveActiveProjectId(proj.id)
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
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh project data")
            }
        }
    }
}

fun detectSuggestedIntentFromReply(replyText: String): CommandIntent? {
    val upper = replyText.uppercase()
    return when {
        upper.contains("GENERATE_EPISODE") -> CommandIntent.GENERATE_EPISODE
        upper.contains("CONTINUE_EPISODE") -> CommandIntent.CONTINUE_EPISODE
        upper.contains("SAVE_BIBLE") -> CommandIntent.SAVE_BIBLE
        upper.contains("CHECK_CONTINUITY") -> CommandIntent.CHECK_CONTINUITY
        upper.contains("RUN_QC") -> CommandIntent.RUN_QC
        else -> null
    }
}

/**
 * BUG-005, BUG-006, BUG-028 & EXTRA-BUG-01:
 * Persistent Project-Scoped Global AI Director Bottom Sheet.
 * - Restores chat history per project across sheet close, navigation, and recreation.
 * - Renders full Markdown.
 * - Never executes commands without explicit user confirmation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalAiDirectorBottomSheet(
    currentScreen: String,
    onDismiss: () -> Unit,
    onOpenFullAiBrain: () -> Unit = {}
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()

    val projectId = repository.currentProjectId()
    var messages by remember(projectId) {
        mutableStateOf(tokenManager.getProjectChatHistory(projectId))
    }
    var prompt by rememberSaveable(projectId) { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var trackedCmdId by remember(projectId) {
        mutableStateOf(tokenManager.getActiveCommandId(projectId, "ai_director_sheet"))
    }
    var trackedCmdStatus by remember { mutableStateOf<String?>(null) }

    // Poll any active command initiated from AI Director
    LaunchedEffect(trackedCmdId, projectId) {
        val cid = trackedCmdId ?: return@LaunchedEffect
        while (true) {
            when (val st = repository.fetchCommandStatus(cid)) {
                is Resource.Success -> {
                    val statusUpper = st.data.status.uppercase()
                    trackedCmdStatus = statusUpper
                    if (statusUpper in listOf("SUCCEEDED", "COMPLETED", "FAILED", "ERROR", "CANCELLED")) {
                        tokenManager.saveActiveCommandId(projectId, "ai_director_sheet", null)
                        break
                    }
                }
                else -> break
            }
            delay(3000)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                            text = "Context: $currentScreen • Project #${projectId.take(6).ifBlank { "Global" }}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = {
                            tokenManager.clearProjectChatHistory(projectId)
                            messages = emptyList()
                        }) {
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

            if (!trackedCmdId.isNullOrBlank()) {
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
                            text = "Command #${trackedCmdId?.take(8)}: ${friendlyPipelineStatus(trackedCmdStatus ?: "QUEUED")}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        StatusBadge(trackedCmdStatus ?: "QUEUED")
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Text(
                            text = "Ask the AI Director about your Story Bible, Episode Script, Character DNA, or Continuity. Conversation persists for this project.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
                items(messages, key = { it.id }) { msg ->
                    val isUser = msg.role == "user"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) StudioTokens.Primary.copy(alpha = 0.14f)
                            else MaterialTheme.colorScheme.surfaceVariant
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

                            // EXTRA-BUG-01 FIX: Explicit confirmation button before dispatching any command
                            if (!isUser && !msg.suggestedIntent.isNullOrBlank()) {
                                val intentEnum = runCatching { CommandIntent.valueOf(msg.suggestedIntent) }.getOrNull()
                                if (intentEnum != null) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Suggested Action: ${intentEnum.name}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    val res = repository.submitStudioCommand(
                                                        projectId = projectId,
                                                        intent = intentEnum
                                                    )
                                                    if (res is Resource.Success && res.data.command_id.isNotBlank()) {
                                                        trackedCmdId = res.data.command_id
                                                        trackedCmdStatus = res.data.status
                                                        tokenManager.saveActiveCommandId(
                                                            projectId,
                                                            "ai_director_sheet",
                                                            res.data.command_id
                                                        )
                                                    }
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Confirm & Run", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
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
                        if (cleanPrompt.isEmpty() || isSending) return@IconButton
                        prompt = ""
                        isSending = true
                        messages = tokenManager.appendProjectChatMessage(
                            projectId,
                            PersistedChatMessage(role = "user", content = cleanPrompt)
                        )
                        scope.launch {
                            val convId = tokenManager.getProjectConversationId(projectId)
                            when (val res = repository.askAiDirector(cleanPrompt, currentScreen, convId)) {
                                is Resource.Success -> {
                                    val suggested = detectSuggestedIntentFromReply(res.data)?.name
                                    messages = tokenManager.appendProjectChatMessage(
                                        projectId,
                                        PersistedChatMessage(
                                            role = "assistant",
                                            content = res.data,
                                            suggestedIntent = suggested
                                        )
                                    )
                                }
                                is Resource.Error -> {
                                    messages = tokenManager.appendProjectChatMessage(
                                        projectId,
                                        PersistedChatMessage(
                                            role = "assistant",
                                            content = "**Error:** ${res.message}"
                                        )
                                    )
                                }
                                else -> {}
                            }
                            isSending = false
                        }
                    },
                    enabled = prompt.isNotBlank() && !isSending,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (prompt.isNotBlank() && !isSending) StudioTokens.Primary else Color.Gray.copy(alpha = 0.3f))
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "Send message", tint = Color.White)
                    }
                }
            }
        }
    }
}
