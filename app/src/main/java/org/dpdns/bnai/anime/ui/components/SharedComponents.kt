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
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
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
                modifier = Modifier
                    .size(48.dp)
                    .testTag(TestTags.Navigation.PROJECT_REFRESH)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh project data")
            }
        }
    }
}

fun detectSuggestedIntentFromReply(replyText: String?): CommandIntent? {
    val upper = replyText?.uppercase().orEmpty()
    return when {
        upper.contains("GENERATE_EPISODE") -> CommandIntent.GENERATE_EPISODE
        upper.contains("CONTINUE_EPISODE") -> CommandIntent.CONTINUE_EPISODE
        upper.contains("SAVE_BIBLE") -> CommandIntent.SAVE_BIBLE
        upper.contains("CHECK_CONTINUITY") -> CommandIntent.CHECK_CONTINUITY
        upper.contains("RUN_QC") -> CommandIntent.RUN_QC
        else -> null
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
    val context = LocalContext.current
    val tokenManager = LocalAppContainer.current.tokenManager
    val repository = LocalAppContainer.current.repository
    val scope = rememberCoroutineScope()
    val projectId = repository.currentProjectId()
    val effectiveScreen = currentScreenName.ifBlank { currentScreen }

    var messages by remember(projectId) { mutableStateOf(tokenManager.getProjectChatHistory(projectId)) }
    var prompt by rememberSaveable(projectId) { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var trackedCmdId by remember(projectId) { mutableStateOf(tokenManager.getActiveCommandId(projectId, "ai_director_sheet")) }
    var trackedCmdStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(trackedCmdId, projectId) {
        val cid = trackedCmdId ?: return@LaunchedEffect
        while (true) {
            when (val st = repository.fetchCommandStatus(cid)) {
                is Resource.Success -> {
                    val statusUpper = st.data.status?.uppercase() ?: "QUEUED"
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
                            text = "Context: $effectiveScreen • Project #${projectId.take(6).ifBlank { "Global" }}",
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
                                                    if (res is Resource.Success) {
                                                        val cid = res.data.command_id.orEmpty()
                                                        if (cid.isNotBlank()) {
                                                            trackedCmdId = cid
                                                            trackedCmdStatus = res.data.status ?: "QUEUED"
                                                            tokenManager.saveActiveCommandId(projectId, "ai_director_sheet", cid)
                                                        }
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
                            when (val res = repository.askAiDirector(cleanPrompt, effectiveScreen, convId)) {
                                is Resource.Success -> {
                                    val replyText = res.data.reply ?: res.data.message ?: "Done."
                                    val suggested = detectSuggestedIntentFromReply(replyText)?.name
                                    messages = tokenManager.appendProjectChatMessage(
                                        projectId,
                                        PersistedChatMessage(
                                            role = "assistant",
                                            content = replyText,
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
                        .testTag(TestTags.AiDirector.SEND)
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
