package org.dpdns.bnai.anime.ui.advanced

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
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.friendlyPipelineStatus
import org.dpdns.bnai.anime.ui.designsystem.*

@Composable
fun VisionAnalysisScreen() {
    val context = LocalContext.current
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()

    var imageUrl by rememberSaveable { mutableStateOf("") }
    var characterCode by rememberSaveable { mutableStateOf("CHAR_001") }
    var mode by rememberSaveable { mutableStateOf("QUICK") } // QUICK | SAVE

    var activeCommandId by rememberSaveable { mutableStateOf<String?>(null) }
    var commandStatus by remember { mutableStateOf<String?>(null) }
    var extractedDna by remember { mutableStateOf>(emptyMap()) }
    var providerUsed by remember { mutableStateOf<String?>(null) }
    var errorOrReviewMsg by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    // BUG-009 FIX: Poll actual command status and NEVER invent fake fallback DNA!
    LaunchedEffect(activeCommandId) {
        val cid = activeCommandId ?: return@LaunchedEffect
        while (true) {
            when (val res = repository.fetchCommandStatus(cid)) {
                is Resource.Success -> {
                    val st = res.data.status.uppercase()
                    commandStatus = st
                    val resultObj = res.data.result ?: emptyMap()
                    val rawDna = (resultObj["visual_dna"] as? Map<*, *>)
                        ?: ((resultObj["n8n_response"] as? Map<*, *>)?.get("visual_dna") as? Map<*, *>)

                    if (rawDna != null && rawDna.isNotEmpty()) {
                        extractedDna = rawDna.entries.associate { (k, v) ->
                            k.toString() to (v?.toString() ?: "")
                        }
                        providerUsed = (resultObj["vision_provider_used"] ?: "Configured Vision Provider").toString()
                    }

                    if (st in listOf("SUCCEEDED", "COMPLETED", "VISION_EXTRACTION_SAVED", "QUICK_VISION_PREVIEW_READY")) {
                        if (extractedDna.isEmpty()) {
                            errorOrReviewMsg = "Vision workflow finished without returning structured DNA fields."
                        }
                        break
                    } else if (st in listOf("FAILED", "ERROR", "MANUAL_REVIEW", "MANUAL_REVIEW_QUEUED", "CANCELLED")) {
                        errorOrReviewMsg = res.data.error
                            ?: "Vision analysis ended with status: $st. No visual attributes were fabricated."
                        break
                    }
                }
                is Resource.Error -> {
                    errorOrReviewMsg = res.message
                    break
                }
                else -> {}
            }
            delay(3000)
        }
    }

    val isProcessing = activeCommandId != null &&
        commandStatus !in listOf("SUCCEEDED", "COMPLETED", "VISION_EXTRACTION_SAVED", "QUICK_VISION_PREVIEW_READY", "FAILED", "ERROR", "MANUAL_REVIEW", "MANUAL_REVIEW_QUEUED", "CANCELLED")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProjectSelectorBar()

        ScreenHeader(
            title = "Vision DNA Analyzer",
            subtitle = "Extract verified 11-field Character Visual DNA via WF-05 (Zero fabricated attributes)"
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == "QUICK",
                onClick = { mode = "QUICK" },
                label = { Text("QUICK Preview (Zero DB Writes)") }
            )
            FilterChip(
                selected = mode == "SAVE",
                onClick = { mode = "SAVE" },
                label = { Text("SAVE to Character & R4 History") }
            )
        }

        StandardCard {
            OutlinedTextField(
                value = imageUrl,
                onValueChange = { imageUrl = it },
                label = { Text("Source Image / Reference URL (https://...)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = characterCode,
                onValueChange = { characterCode = it },
                label = { Text("Target Character Code (e.g. CHAR_001)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (imageUrl.isNotBlank()) {
                AsyncMediaPreview(
                    mediaUrl = imageUrl,
                    contentDescription = "Source reference image",
                    height = 150.dp
                )
            }

            PrimaryButton(
                text = "Start Vision DNA Analysis",
                isLoading = isSubmitting || isProcessing,
                enabled = imageUrl.startsWith("http") && !isSubmitting && !isProcessing,
                onClick = {
                    val pid = repository.currentProjectId()
                    if (pid.isBlank()) return@PrimaryButton
                    isSubmitting = true
                    extractedDna = emptyMap()
                    errorOrReviewMsg = null
                    scope.launch {
                        val res = repository.submitVisionAnalysis(
                            projectId = pid,
                            imageUrl = imageUrl.trim(),
                            analysisType = mode,
                            characterId = characterCode.trim().ifBlank { "CHAR_001" }
                        )
                        isSubmitting = false
                        if (res is Resource.Success && res.data.command_id.isNotBlank()) {
                            activeCommandId = res.data.command_id
                            commandStatus = res.data.status
                            Toast.makeText(context, "Vision job queued (#${res.data.command_id.take(6)})", Toast.LENGTH_SHORT).show()
                        } else if (res is Resource.Error) {
                            errorOrReviewMsg = res.message
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
                    Text("Command #${activeCommandId?.take(8)}", fontWeight = FontWeight.Bold)
                    StatusChip(label = commandStatus ?: "QUEUED")
                }
                if (isProcessing) {
                    Text(
                        text = "Vision pipeline is processing (${friendlyPipelineStatus(commandStatus)}). Waiting for real provider response...",
                        fontSize = 12.sp
                    )
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }

        if (errorOrReviewMsg != null) {
            StandardCard {
                Text("Vision Analysis Status Notice", fontWeight = FontWeight.Bold, color = StudioTokens.Error)
                Text(errorOrReviewMsg ?: "", fontSize = 13.sp)
            }
        }

        if (extractedDna.isNotEmpty()) {
            SectionHeader(
                title = "Verified Visual DNA Output",
                badgeText = providerUsed ?: mode
            )
            extractedDna.forEach { (fieldKey, fieldVal) ->
                StandardCard {
                    Text(
                        text = fieldKey.replace("_", " ").uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioTokens.Primary
                    )
                    Text(text = fieldVal.ifBlank { "Not specified by provider" }, fontSize = 14.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(64.dp))
    }
}
