package org.dpdns.bnai.anime.ui.tools

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.InfoTooltipButton
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.StatusBadge
import org.dpdns.bnai.anime.ui.components.friendlyPipelineStatus

data class QuickGenHistoryItem(
    val commandId: String,
    val prompt: String,
    val isVideo: Boolean,
    val status: String,
    val mediaUrl: String? = null
)

@Composable
fun QuickToolsScreen(
    onOpenProductionPoll: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()

    var prompt by remember { mutableStateOf("") }
    var stylePreset by remember { mutableStateOf("anime_cinematic") }
    var aspectRatio by remember { mutableStateOf("16:9") }

    var isGenerating by remember { mutableStateOf(false) }
    var isVideoMode by remember { mutableStateOf(false) }
    var activeCommandId by remember { mutableStateOf<String?>(null) }
    var pollStatus by remember { mutableStateOf<String?>(null) }
    var generatedMediaUrl by remember { mutableStateOf<String?>(null) }
    var recentHistory by remember { mutableStateOf<List<QuickGenHistoryItem>>(emptyList()) }

    // Fix #104, #123, #129: Poll fetchCommandStatus every 3s after submit
    LaunchedEffect(activeCommandId, isGenerating) {
        val cmdId = activeCommandId
        if (!isGenerating || cmdId.isNullOrBlank()) return@LaunchedEffect

        var attempts = 0
        while (isGenerating && attempts < 30) {
            attempts++
            when (val res = repository.fetchCommandStatus(cmdId)) {
                is Resource.Success -> {
                    val st = res.data.status?.uppercase() ?: "QUEUED"
                    pollStatus = st
                    val resultUrl = (res.data.result?.get("media_url")
                        ?: res.data.result?.get("clip_url")
                        ?: res.data.result?.get("image_url"))?.toString()

                    if (!resultUrl.isNullOrBlank()) {
                        generatedMediaUrl = resultUrl
                    }

                    if (st in setOf("SUCCEEDED", "COMPLETED", "FAILED", "MANUAL_REVIEW")) {
                        isGenerating = false
                        if (generatedMediaUrl == null && st in setOf("SUCCEEDED", "COMPLETED")) {
                            generatedMediaUrl = "[https://api.bnai.dpdns.org/v1/media/telegram](https://api.bnai.dpdns.org/v1/media/telegram)"
                        }
                        recentHistory = listOf(
                            QuickGenHistoryItem(cmdId, prompt, isVideoMode, st, generatedMediaUrl)
                        ) + recentHistory.filterNot { it.commandId == cmdId }
                        break
                    }
                }
                else -> Unit
            }
            delay(3000L)
        }
        isGenerating = false
    }

    fun startQuickGen(video: Boolean) {
        val clean = prompt.trim()
        if (clean.isBlank()) return
        isVideoMode = video
        isGenerating = true
        pollStatus = "QUEUED"
        generatedMediaUrl = null

        scope.launch {
            val pid = repository.currentProjectId()
            when (val res = repository.triggerQuickMedia(pid, video, clean)) {
                is Resource.Success -> {
                    val cmdId = res.data.command?.id ?: res.data.command_id ?: ""
                    activeCommandId = cmdId
                    recentHistory = listOf(
                        QuickGenHistoryItem(cmdId, clean, video, res.data.status ?: "QUEUED")
                    ) + recentHistory
                }
                is Resource.Error -> {
                    isGenerating = false
                    pollStatus = "FAILED: ${res.message}"
                }
                else -> isGenerating = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ProjectSelectorBar()

        // Fix #127 & #131: Renamed to "Quick Generate" + (i) Help Tooltip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Quick Generate", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                InfoTooltipButton(
                    title = "How Quick Generate Works",
                    explanation = "Instantly test visual prompts on Kaggle GPU workers without creating a full episode. You can save any generated asset directly to your active project."
                )
            }
            // Fix #126: GPU Credit Cost Badge
            AssistChip(onClick = {}, label = { Text("⚡ Uses 1 GPU credit", fontSize = 11.sp) })
        }

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Visual Scene / Character Prompt") },
            placeholder = { Text("e.g. Cyberpunk samurai standing under neon rain, Makoto Shinkai lighting...") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("16:9", "9:16", "1:1").forEach { ratio ->
                FilterChip(
                    selected = aspectRatio == ratio,
                    onClick = { aspectRatio = ratio },
                    label = { Text(ratio) }
                )
            }
            FilterChip(
                selected = stylePreset == "anime_cinematic",
                onClick = { stylePreset = if (stylePreset == "anime_cinematic") "dark_fantasy" else "anime_cinematic" },
                label = { Text("🎨 $stylePreset") }
            )
        }

        // Fix #125: Icons on Generate Image & Generate Video buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { startQuickGen(false) },
                enabled = prompt.isNotBlank() && !isGenerating,
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Text("🖼️ Generate Image")
            }
            Button(
                onClick = { startQuickGen(true) },
                enabled = prompt.isNotBlank() && !isGenerating,
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Text("🎬 Generate Video")
            }
        }

        // Fix #123 & #129: Skeleton Loader ("Generating masterpiece... Est time: 45s") + Cancel Button
        if (isGenerating) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✨ Generating masterpiece...", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        StatusBadge(friendlyPipelineStatus(pollStatus))
                    }
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Polling every 3s • Est time: 45s", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        OutlinedButton(onClick = {
                            isGenerating = false
                            pollStatus = "CANCELLED"
                        }) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }

        // Fix #124 & #130: Result Preview Card + "📥 Save to Project" Button (SAVE_TO_PROJECT)
        if (!isGenerating && pollStatus != null) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isVideoMode) "🎬 Video Render Result" else "🖼️ Image Render Result", fontWeight = FontWeight.Bold)
                        StatusBadge(friendlyPipelineStatus(pollStatus))
                    }

                    generatedMediaUrl?.let { url ->
                        OutlinedButton(
                            onClick = {
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Preview Rendered Media")
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                scope.launch {
                                    repository.submitStudioCommand(
                                        projectId = repository.currentProjectId(),
                                        intent = CommandIntent.SAVE_TO_PROJECT,
                                        mode = "SAVE",
                                        payload = mapOf(
                                            "command_id" to (activeCommandId ?: ""),
                                            "prompt" to prompt,
                                            "media_url" to (generatedMediaUrl ?: "")
                                        )
                                    )
                                    Toast.makeText(context, "📥 Saved to active project Media Library!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📥 Save to Project")
                        }

                        if (!activeCommandId.isNullOrBlank() && activeCommandId!!.length == 36) {
                            OutlinedButton(
                                onClick = { onOpenProductionPoll(activeCommandId!!) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Open Pipeline")
                            }
                        }
                    }
                }
            }
        }

        // Fix #128: Recent Generations History Links at bottom
        if (recentHistory.isNotEmpty()) {
            HorizontalDivider()
            Text("Recent Generations", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            recentHistory.take(5).forEach { item ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        prompt = item.prompt
                        activeCommandId = item.commandId
                        pollStatus = item.status
                        generatedMediaUrl = item.mediaUrl
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${if (item.isVideo) "🎬" else "🖼️"} ${item.prompt}",
                                maxLines = 1,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("Tap to reload prompt", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                        StatusBadge(friendlyPipelineStatus(item.status))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(60.dp))
    }
}
