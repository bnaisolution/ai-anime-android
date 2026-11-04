package org.dpdns.bnai.anime.ui.brain

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.StatusBadge
import java.util.UUID

data class DirectorChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "USER", "AI", "ERROR"
    val text: String,
    val mediaUrl: String? = null,
    val triggeredCommandId: String? = null,
    val modelUsed: String? = null
)

@Composable
fun AIBrainScreen(
    onTrackCommand: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Fix #118: Persistent conversation_id for session context memory
    var conversationId by remember { mutableStateOf("conv-${UUID.randomUUID().toString().take(8)}") }
    var inputPrompt by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var webSearchEnabled by remember { mutableStateOf(false) }
    var activeModelBadge by remember { mutableStateOf("Groq Llama-3.3 • Gemini") }
    var messages by remember { mutableStateOf<List<DirectorChatMessage>>(emptyList()) }

    val starterChips = listOf(
        "Create Project" to "Help me create a new dark fantasy anime project and outline its core hook.",
        "Discuss Bible" to "Review my current Story Bible and suggest 3 unique power system rules.",
        "Check Canon" to "Check locked canon facts and character consistency for Episode 1.",
        "Generate Episode" to "Plan Episode 1 scene breakdown and trigger GENERATE_EPISODE."
    )

    // Fix #115: Speech-to-Text Mic Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                inputPrompt = if (inputPrompt.isBlank()) spoken else "$inputPrompt$spoken"
            }
        }
    }

    // Fix #116: File Picker to attach reference image & trigger Vision (WF-05)
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val refUri = it.toString()
            messages = messages + DirectorChatMessage(
                sender = "USER",
                text = "📎 Attached Reference Media for Vision DNA Analysis",
                mediaUrl = refUri
            )
            scope.launch {
                val res = repository.triggerVisionAnalysis(
                    projectId = repository.currentProjectId(),
                    mediaUrl = refUri,
                    prompt = "Extract 11-field Visual DNA from attached reference"
                )
                val cmdId = (res as? Resource.Success)?.data?.command_id
                messages = messages + DirectorChatMessage(
                    sender = "AI",
                    text = "Vision DNA extraction triggered on your attached reference image.",
                    triggeredCommandId = cmdId
                )
            }
        }
    }

    // Fix #108: Auto-scroll to newest message
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun sendMessage(rawText: String) {
        val clean = rawText.trim()
        if (clean.isBlank() || isThinking) return
        inputPrompt = ""
        messages = messages + DirectorChatMessage(sender = "USER", text = clean)
        isThinking = true

        scope.launch {
            val augmentedPrompt = buildString {
                append("[Anime Studio Director Scope: StoryWF-00..13 Only] ")
                if (webSearchEnabled) append("[WebSearch: ON] ")
                append(clean)
            }
            // Fix #113 & #118: Pass current_screen and conversation_id
            when (val res = repository.chatWithBrain(
                message = augmentedPrompt,
                projectId = repository.currentProjectId(),
                currentScreen = "ai_director",
                conversationId = conversationId
            )) {
                is Resource.Success -> {
                    val replyText = res.data.reply ?: res.data.message ?: "Action processed."
                    res.data.model?.let { activeModelBadge = it }

                    // Fix #119: Detect media URLs inside AI response
                    val urlRegex = Regex("(https?://[^\\s\"'>]+)")
                    val detectedUrl = urlRegex.find(replyText)?.value

                    // Fix #114 & #120: Action Agent parser for hidden JSON / command intents
                    var dispatchedCmdId: String? = null
                    val upperReply = (replyText + " " + (res.data.action ?: "")).uppercase()
                    val matchedIntent = when {
                        upperReply.contains("GENERATE_EPISODE") -> CommandIntent.GENERATE_EPISODE
                        upperReply.contains("SAVE_BIBLE") -> CommandIntent.SAVE_BIBLE
                        upperReply.contains("CHECK_CONTINUITY") -> CommandIntent.CHECK_CONTINUITY
                        upperReply.contains("CHECK_CANON") -> CommandIntent.CHECK_CANON
                        else -> null
                    }
                    if (matchedIntent != null) {
                        val cmdRes = repository.submitStudioCommand(
                            projectId = repository.currentProjectId(),
                            intent = matchedIntent,
                            mode = "SAVE",
                            payload = mapOf("user_prompt" to clean)
                        )
                        dispatchedCmdId = (cmdRes as? Resource.Success)?.data?.command_id
                            ?: (cmdRes as? Resource.Success)?.data?.command?.id
                    }

                    messages = messages + DirectorChatMessage(
                        sender = "AI",
                        text = replyText,
                        mediaUrl = detectedUrl,
                        triggeredCommandId = dispatchedCmdId,
                        modelUsed = res.data.model ?: res.data.provider
                    )
                }
                is Resource.Error -> {
                    // Fix #121: Explicit red error bubble (Quota 429, network, etc.)
                    messages = messages + DirectorChatMessage(
                        sender = "ERROR",
                        text = "⚠️ ${res.message}"
                    )
                }
                else -> Unit
            }
            isThinking = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ProjectSelectorBar(
            onRefreshClick = {
                conversationId = "conv-${UUID.randomUUID().toString().take(8)}"
                messages = emptyList()
                Toast.makeText(context, "New AI Director session started", Toast.LENGTH_SHORT).show()
            }
        )

        // Fix #106, #117, #197: Header "AI Director" + Active Model Badge + Web Search Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("AI Director", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("Model: $activeModelBadge", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
            }
            FilterChip(
                selected = webSearchEnabled,
                onClick = { webSearchEnabled = !webSearchEnabled },
                label = { Text(if (webSearchEnabled) "🌐 Search ON" else "🌐 Web Search", fontSize = 11.sp) }
            )
        }

        // Fix #110: Quick Starter Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(starterChips) { (label, promptText) ->
                AssistChip(
                    onClick = { sendMessage(promptText) },
                    label = { Text(label) }
                )
            }
        }

        // Chat Messages Area
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (messages.isEmpty() && !isThinking) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        )
                        Text("Your Anime AI Director is Ready", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Ask about Story Bible, Character DNA, Canon rules, or tap a chip above to dispatch studio workflows.",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val isUser = msg.sender == "USER"
                        val isError = msg.sender == "ERROR"
                        val bgColor = when {
                            isError -> Color(0xFFEF4444).copy(alpha = 0.16f)
                            isUser -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Card(
                                modifier = Modifier.widthIn(max = 320.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = bgColor)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = if (isUser) "You" else if (isError) "System Alert" else "AI Director",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isError) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                                    )

                                    // Fix #107: Clean formatted markdown-style rendering (headers/bullets/code blocks)
                                    msg.text.lines().forEach { line ->
                                        val trimmed = line.trim()
                                        when {
                                            trimmed.startsWith("###") || trimmed.startsWith("##") -> {
                                                Text(
                                                    trimmed.trimStart('#', ' '),
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.headlineMedium
                                                )
                                            }
                                            trimmed.startsWith("```") -> Unit
                                            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                                                Text("• ${trimmed.drop(2).replace("**", "")}", style = MaterialTheme.typography.bodyLarge)
                                            }
                                            else -> {
                                                Text(
                                                    line.replace("**", ""),
                                                    color = if (isError) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                            }
                                        }
                                    }

                                    // Fix #119: Clickable Media Preview Card when URL is detected
                                    msg.mediaUrl?.let { url ->
                                        OutlinedButton(
                                            onClick = {
                                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Open Media Preview", fontSize = 12.sp)
                                        }
                                    }

                                    // Fix #120: Command Dispatched Feedback Card with "Track Progress" CTA
                                    msg.triggeredCommandId?.let { cmdId ->
                                        Surface(
                                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("✅ Workflow Queued", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                TextButton(onClick = { onTrackCommand(cmdId) }) {
                                                    Text("Track Progress")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Fix #109: "AI is thinking..." bubble loader
                    if (isThinking) {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Text("AI Director is thinking...", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Fix #111, #115, #116: Aligned Input Row with Attach (📎), Mic (🎤), and Send button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(onClick = { fileLauncher.launch("image/*") }) {
                Icon(Icons.Default.AttachFile, contentDescription = "Attach Reference Image")
            }
            IconButton(onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to AI Director...")
                }
                runCatching { speechLauncher.launch(intent) }.onFailure {
                    Toast.makeText(context, "Speech recognition not available on this device", Toast.LENGTH_SHORT).show()
                }
            }) {
                Icon(Icons.Default.Mic, contentDescription = "Voice Input")
            }
            OutlinedTextField(
                value = inputPrompt,
                onValueChange = { inputPrompt = it },
                placeholder = { Text("Ask AI Director...") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { sendMessage(inputPrompt) },
                enabled = inputPrompt.isNotBlank() && !isThinking,
                modifier = Modifier.height(52.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}
