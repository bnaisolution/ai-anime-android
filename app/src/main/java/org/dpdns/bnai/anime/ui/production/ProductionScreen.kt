package org.dpdns.bnai.anime.ui.production

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.StatusBadge
import org.dpdns.bnai.anime.ui.components.friendlyPipelineStatus

@Composable
fun ProductionScreen(
    episodeId: String = "",
    onViewVideoClick: () -> Unit = {},
    onOpenSceneInspector: () -> Unit = {},
    onOpenMediaLibrary: () -> Unit = {},
    viewModel: ProductionViewModel = viewModel()
) {
    val state by viewModel.productionState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var pollTrigger by remember { mutableStateOf(0) }
    var showErrorDialog by remember { mutableStateOf(false) }

    LaunchedEffect(episodeId, pollTrigger, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.pollWhileStarted(episodeId)
        }
    }

    // Fix #53: Error Details Popup when GPU offline or job fails
    if (showErrorDialog && state.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Pipeline Error Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.errorMessage ?: "Unknown error")
                    Text("Worker: ${state.workerName}", fontSize = 12.sp)
                    Text("Auto-Retry Status: ${state.autoRetryText}", fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(onClick = {
                    showErrorDialog = false
                    viewModel.triggerAutoRetry()
                    pollTrigger++
                }) { Text("Auto-Retry Now") }
            },
            dismissButton = {
                TextButton(onClick = { showErrorDialog = false }) { Text("Close") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            ProjectSelectorBar(onRefreshClick = { pollTrigger++ })
        }

        // Fix #48, #49, #55, #56: Clean Header (no WF-03), Episode Context, Friendly Status, Worker & QC Tracker
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Production Pipeline", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                            Text("Active Target: ${state.episodeTitle}", color = MaterialTheme.colorScheme.secondary)
                        }
                        StatusBadge(friendlyPipelineStatus(state.backendStatus))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = {}, label = { Text("⚡ ${state.workerName}", fontSize = 11.sp) })
                    }
                    Text("${state.qcStatusText} • ${state.autoRetryText}", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)

                    state.errorMessage?.let { err ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(err, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                            TextButton(onClick = { showErrorDialog = true }) {
                                Text("View Error Details")
                            }
                        }
                    }
                }
            }
        }

        // Fix #54: Large "View Final Video" CTA when SUCCEEDED / COMPLETED
        if (state.backendStatus.uppercase() in setOf("SUCCEEDED", "COMPLETED")) {
            item {
                Button(
                    onClick = onViewVideoClick,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🎬 View Final Video & Quality Check", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        // Fix #51: Individual Progress Bars for Image, Audio, Video
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Stage Progress", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

                    Text("🖼️ Keyframe Images (${(state.imageProgress * 100).toInt()}%)")
                    LinearProgressIndicator(progress = { state.imageProgress }, modifier = Modifier.fillMaxWidth())

                    Text("🎙️ Voice & BGM Audio (${(state.audioProgress * 100).toInt()}%)")
                    LinearProgressIndicator(progress = { state.audioProgress }, modifier = Modifier.fillMaxWidth())

                    Text("🎬 Final Video Synthesis (${(state.videoProgress * 100).toInt()}%)")
                    LinearProgressIndicator(progress = { state.videoProgress }, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        // Fix #52: Pause, Cancel & Retry Controls
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { viewModel.togglePause() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(if (state.isPausedByUser) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (state.isPausedByUser) "Resume" else "Pause")
                }
                OutlinedButton(
                    onClick = { viewModel.cancelPipeline() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancel")
                }
                Button(
                    onClick = { pollTrigger++ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Sync")
                }
            }
        }

        // Fix #50: Scrolling Console-Style Live Logs Box
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Live Pipeline Console", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    state.liveLogs.forEach { logLine ->
                        Text(
                            text = logLine,
                            color = Color(0xFFE5E7EB),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Fix #105: Per-Scene Progress Checklist (Image / Audio / Video checkmarks)
        if (state.scenes.isNotEmpty()) {
            item {
                Text("Scene-by-Scene Checklist", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            itemsIndexed(state.scenes) { idx, sc ->
                val scNo = sc.scene_no ?: sc.scene_order ?: (idx + 1)
                val isDone = sc.status?.uppercase() in setOf("RENDERED", "COMPLETED", "SUCCEEDED") || !sc.clips.isNullOrEmpty()
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Scene #$scNo", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Image ✅", fontSize = 12.sp)
                            Text("Audio ${if (isDone) "✅" else "⏳"}", fontSize = 12.sp)
                            Text("Video ${if (isDone) "✅" else "⏳"}", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onOpenSceneInspector, modifier = Modifier.weight(1f)) {
                    Text("Scene Inspector")
                }
                OutlinedButton(onClick = onOpenMediaLibrary, modifier = Modifier.weight(1f)) {
                    Text("Media Library")
                }
            }
        }
    }
}
