package org.dpdns.bnai.anime.ui.aibrain

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.auth.PersistedChatMessage
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.detectSuggestedIntentFromReply
import org.dpdns.bnai.anime.ui.components.friendlyPipelineStatus
import org.dpdns.bnai.anime.ui.designsystem.*

@Composable
fun AIBrainScreen() {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var projectId by remember { mutableStateOf(repository.currentProjectId()) }
    var providerBadge by remember { mutableStateOf(tokenManager.getConfiguredProviderLabel()) }
    var messages by remember(projectId) {
        mutableStateOf(tokenManager.getProjectChatHistory(projectId))
    }
    var inputPrompt by rememberSaveable(projectId) { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    var activeCmdId by remember(projectId) {
        mutableStateOf(tokenManager.getActiveCommandId(projectId, "ai_brain_cmd"))
    }
    var activeCmdStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(projectId) {
        providerBadge = repository.resolveConfiguredAiProviderBadge()
        messages = tokenManager.getProjectChatHistory(projectId)
    }

    LaunchedEffect(activeCmdId, projectId) {
        val cid = activeCmdId ?: return@LaunchedEffect
        while (true) {
            when (val res = repository.fetchCommandStatus(cid)) {
                is Resource.Success -> {
                    val st = res.data.status.uppercase()
                    activeCmdStatus = st
                    if (st in listOf("SUCCEEDED", "COMPLETED", "FAILED", "ERROR", "CANCELLED")) {
                        tokenManager.saveActiveCommandId(projectId, "ai_brain_cmd", null)
                        break
                    }
                }
                else -> break
            }
            delay(3000)
        }
    }

    fun sendMessage(textToSend: String) {
        val clean = textToSend.trim()
        if (clean.isEmpty() || isSending) return
        inputPrompt = ""
        isSending = true
        messages = tokenManager.appendProjectChatMessage(
            projectId,
            PersistedChatMessage(role = "user", content = clean)
        )
        scope.launch {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.lastIndex)
            }
            val convId = tokenManager.getProjectConversationId(projectId)
            when (val res = repository.askAiDirector(clean, "AI_BRAIN", convId)) {
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
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.lastIndex)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ProjectSelectorBar(
            onProjectChanged = { newPid ->
                projectId = newPid
                messages = tokenManager.getProjectChatHistory(newPid)
            }
        )

        ScreenHeader(
            title = "AI Studio Director",
            subtitle = "Project-isolated conversation with Markdown & explicit command confirmation",
            trailingContent = {
                StatusChip(label = providerBadge)
                if (messages.isNotEmpty()) {
                    IconButton(onClick = {
                        tokenManager.clearProjectChatHistory(projectId)
                        messages = emptyList()
                    }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear project chat")
                    }
                }
            }
        )

        if (!activeCmdId.isNullOrBlank()) {
            StandardCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dispatched Command #${activeCmdId?.take(8)}: ${friendlyPipelineStatus(activeCmdStatus ?: "QUEUED")}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    StatusChip(label = activeCmdStatus ?: "QUEUED")
                }
            }
        }

        // Quick Suggestion Chips when conversation is empty
        if (messages.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Audit Story Bible",
                    "Check Continuity",
                    "Plan Next Episode"
                ).forEach { chipText ->
                    SuggestionChip(
                        onClick = { sendMessage(chipText) },
                        label = { Text(chipText, fontSize = 12.sp) }
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.role == "user"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUser) StudioTokens.Primary.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isUser) "You" else "AI Studio Director",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUser) StudioTokens.Primary else StudioTokens.Secondary
                        )
                        MarkdownText(markdown = msg.content)

                        // EXTRA-BUG-01: Explicit user confirmation before running any command
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
                                        text = "Suggested Studio Action: ${intentEnum.name}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val res = repository.submitStudioCommand(projectId, intentEnum)
                                                if (res is Resource.Success && res.data.command_id.isNotBlank()) {
                                                    activeCmdId = res.data.command_id
                                                    activeCmdStatus = res.data.status
                                                    tokenManager.saveActiveCommandId(projectId, "ai_brain_cmd", res.data.command_id)
                                                    Toast.makeText(context, "Command #${res.data.command_id.take(6)} dispatched", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Confirm & Execute", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputPrompt,
                onValueChange = { inputPrompt = it },
                placeholder = { Text("Ask AI Director about this project...") },
                modifier = Modifier.weight(1f),
                maxLines = 4
            )
            IconButton(
                onClick = { sendMessage(inputPrompt) },
                enabled = inputPrompt.isNotBlank() && !isSending,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (inputPrompt.isNotBlank() && !isSending) StudioTokens.Primary else Color.Gray.copy(alpha = 0.3f))
            ) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }
}
