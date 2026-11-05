package org.dpdns.bnai.anime.ui.tools

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.friendlyPipelineStatus
import org.dpdns.bnai.anime.ui.designsystem.*

@Composable
fun QuickToolsScreen(
    onOpenProductionPoll: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()

    var mode by rememberSaveable { mutableStateOf("IMAGE") } // IMAGE | VIDEO
    var promptText by rememberSaveable { mutableStateOf("") }
    var stylePreset by rememberSaveable { mutableStateOf("Cinematic Anime") }
    var aspectRatio by rememberSaveable { mutableStateOf("16:9") }

    var activeCommandId by rememberSaveable { mutableStateOf<String?>(null) }
    var activeStatus by remember { mutableStateOf<String?>(null) }
    var resultMediaUrl by remember { mutableStateOf<String?>(null) }
    var elapsedSeconds by remember { mutableStateOf(0) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isCancelling by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // BUG-023: Poll real backend command status without fake percentages or fixed 45s ETA
    LaunchedEffect(activeCommandId) {
        val cid = activeCommandId ?: return@LaunchedEffect
        elapsedSeconds = 0
        while (true) {
            when (val res = repository.fetchCommandStatus(cid)) {
                is Resource.Success -> {
                    val st = res.data.status.uppercase()
                    activeStatus = st
                    val outUrl = (res.data.result?.get("media_url") ?: res.data.result?.get("url") ?: res.data.result?.get("working_storage_url"))?.toString()
                    if (!outUrl.isNullOrBlank() && outUrl != "null") {
                        resultMediaUrl = outUrl
                    }
                    if (st in listOf("SUCCEEDED", "COMPLETED", "FAILED", "ERROR", "CANCELLED")) {
                        break
                    }
                }
                is Resource.Error -> {
                    errorMessage = res.message
                    break
                }
                else -> {}
            }
            delay(3000)
            elapsedSeconds += 3
        }
    }

    val isJobRunning = activeCommandId != null && activeStatus !in listOf("SUCCEEDED", "COMPLETED", "FAILED", "ERROR", "CANCELLED")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProjectSelectorBar()

        ScreenHeader(
            title = "Quick Tools (Ad-Hoc Preview)",
            subtitle = "Runs in QUICK zero-persistence mode until you explicitly click Save to Project"
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == "IMAGE",
                onClick = { mode = "IMAGE" },
                label = { Text("Image Generation") }
            )
            FilterChip(
                selected = mode == "VIDEO",
                onClick = { mode = "VIDEO" },
                label = { Text("Video Clip Generation") }
            )
        }

        StandardCard {
            OutlinedTextField(
                value = promptText,
                onValueChange = { promptText = it },
                label = { Text("Visual Prompt") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = stylePreset,
                    onValueChange = { stylePreset = it },
                    label = { Text("Style Preset") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = aspectRatio,
                    onValueChange = { aspectRatio = it },
                    label = { Text("Aspect Ratio") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            PrimaryButton(
                text = if (mode == "IMAGE") "Generate Ad-Hoc Image" else "Generate Ad-Hoc Video",
                isLoading = isSubmitting,
                enabled = promptText.isNotBlank() && !isSubmitting && !isJobRunning,
                onClick = {
                    val pid = repository.currentProjectId()
                    if (pid.isBlank()) return@PrimaryButton
                    isSubmitting = true
                    errorMessage = null
                    resultMediaUrl = null
                    scope.launch {
                        val res = if (mode == "IMAGE") {
                            repository.submitQuickImage(pid, promptText.trim(), stylePreset)
                        } else {
                            repository.submitQuickVideo(pid, promptText.trim(), 5)
                        }
                        isSubmitting = false
                        if (res is Resource.Success && res.data.command_id.isNotBlank()) {
                            activeCommandId = res.data.command_id
                            activeStatus = res.data.status
                        } else if (res is Resource.Error) {
                            errorMessage = res.message
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (activeCommandId != null) {
            StandardCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Job #${activeCommandId?.take(8)}", fontWeight = FontWeight.Bold)
                    StatusChip(label = activeStatus ?: "QUEUED")
                }
                Text(
                    text = "Stage: ${friendlyPipelineStatus(activeStatus)} • Elapsed: ${elapsedSeconds}s",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isJobRunning) {
                    // BUG-023: Indeterminate progress bar when exact worker percentage is unknown
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

                    // BUG-022: Real backend job cancellation via POST /jobs/:jobId/cancel
                    DangerButton(
                        text = if (isCancelling) "Cancelling on Server..." else "Cancel Remote Job",
                        enabled = !isCancelling,
                        onClick = {
                            val pid = repository.currentProjectId()
                            val cid = activeCommandId ?: return@DangerButton
                            isCancelling = true
                            scope.launch {
                                when (val cancelRes = repository.cancelJobSync(pid, cid)) {
                                    is Resource.Success -> {
                                        activeStatus = "CANCELLED"
                                        Toast.makeText(context, "Job cancelled on backend", Toast.LENGTH_SHORT).show()
                                    }
                                    is Resource.Error -> {
                                        errorMessage = "Remote cancellation unconfirmed: ${cancelRes.message}"
                                    }
                                    else -> {}
                                }
                                isCancelling = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (!resultMediaUrl.isNullOrBlank()) {
                    AsyncMediaPreview(
                        mediaUrl = resultMediaUrl,
                        contentDescription = "Quick generation output",
                        height = 180.dp
                    )
                    PrimaryButton(
                        text = "Save Asset to Project",
                        onClick = {
                            val pid = repository.currentProjectId()
                            scope.launch {
                                val saveRes = repository.submitStudioCommand(
                                    projectId = pid,
                                    intent = CommandIntent.SAVE_TO_PROJECT,
                                    mode = "SAVE",
                                    payload = mapOf(
                                        "source_command_id" to activeCommandId,
                                        "media_url" to resultMediaUrl,
                                        "prompt" to promptText
                                    )
                                )
                                if (saveRes is Resource.Success) {
                                    Toast.makeText(context, "Saved asset to project!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (errorMessage != null) {
            StandardCard {
                Text(errorMessage ?: "", color = StudioTokens.Error, fontSize = 13.sp)
            }
        }
    }
}
