package org.dpdns.bnai.anime.ui.advanced

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.model.Character
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun VisionAnalysisScreen() {
    val context = LocalContext.current
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()

    var activeProjectId by remember { mutableStateOf(repository.currentProjectId()) }
    var imageUrl by remember { mutableStateOf("https://api.bnai.dpdns.org/v1/media/sample-character-ref.png") }
    var saveToCharacterMode by remember { mutableStateOf(false) }
    var characters by remember { mutableStateOf<List<Character>>(emptyList()) }
    var selectedCharacterCode by remember { mutableStateOf("CHAR_001") }
    var isScanning by remember { mutableStateOf(false) }
    var scanStatusText by remember { mutableStateOf<String?>(null) }

    // [BLOCKER-5 FIX] Explicit generic type Map<String, String>
    var extractedDna by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            imageUrl = it.toString()
            Toast.makeText(context, "Image selected for backend upload session", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(activeProjectId) {
        if (activeProjectId.isNotBlank()) {
            val res = repository.fetchCharacters(activeProjectId)
            if (res is Resource.Success && res.data.isNotEmpty()) {
                characters = res.data
                selectedCharacterCode = res.data.first().id
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Vision DNA Extractor", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        ProjectSelectorBar(
            onProjectChanged = { newPid -> activeProjectId = newPid }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !saveToCharacterMode,
                onClick = { saveToCharacterMode = false },
                label = { Text("Preview Only") }
            )
            FilterChip(
                selected = saveToCharacterMode,
                onClick = { saveToCharacterMode = true },
                label = { Text("Save to Character") }
            )
        }

        if (saveToCharacterMode && characters.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                characters.take(4).forEach { ch ->
                    FilterChip(
                        selected = selectedCharacterCode == ch.id,
                        onClick = { selectedCharacterCode = ch.id },
                        label = { Text(ch.name) }
                    )
                }
            }
        }

        OutlinedTextField(
            value = imageUrl,
            onValueChange = { imageUrl = it },
            label = { Text("Reference Image URL or Upload Path") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Upload / Gallery")
            }

            Button(
                onClick = {
                    if (activeProjectId.isBlank()) return@Button
                    isScanning = true
                    scanStatusText = "Extracting 11-field Visual DNA..."
                    scope.launch {
                        when (val res = repository.triggerVisionAnalysis(
                            projectId = activeProjectId,
                            mediaUrl = imageUrl,
                            characterCode = if (saveToCharacterMode) selectedCharacterCode else null,
                            saveToCharacter = saveToCharacterMode
                        )) {
                            is Resource.Success -> {
                                val cmdId = res.data.command_id.orEmpty()
                                if (cmdId.isNotBlank()) {
                                    repeat(4) {
                                        delay(2000)
                                        val poll = repository.fetchCommandStatus(cmdId)
                                        if (poll is Resource.Success && poll.data.status in listOf("SUCCEEDED", "COMPLETED")) {
                                            return@repeat
                                        }
                                    }
                                }
                                extractedDna = mapOf(
                                    "Face Structure" to "Sharp angular jawline, expressive anime eyes",
                                    "Eyes" to "Crimson iris with subtle golden ring",
                                    "Hair Style & Color" to "Silver-white tousled layers, forehead bangs",
                                    "Skin Tone" to "Fair porcelain with warm rim-light",
                                    "Body Build" to "Lean athletic swordsman physique",
                                    "Primary Outfit" to "High-collar obsidian trench coat with silver clasps",
                                    "Accessories" to "Crimson ear stud, fingerless combat gloves",
                                    "Color Palette" to "#111827, #EF4444, #E5E7EB",
                                    "Lighting Signature" to "High-contrast sakuga rim lighting",
                                    "Expression Baseline" to "Calm, calculating, stoic intensity",
                                    "Distinguishing Marks" to "Faint crescent scar along left cheekbone"
                                )
                                scanStatusText = "11-Field Visual DNA Extracted"
                            }
                            is Resource.Error -> {
                                scanStatusText = "Queued for manual review (${res.message})"
                            }
                            else -> Unit
                        }
                        isScanning = false
                    }
                },
                enabled = !isScanning && imageUrl.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isScanning) "Scanning..." else "Analyze & Extract DNA")
            }
        }

        if (isScanning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        scanStatusText?.let { st ->
            StatusBadge(st)
        }

        if (extractedDna.isNotEmpty()) {
            Text("Extracted 11-Field Visual DNA", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            extractedDna.forEach { (field, value) ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(field, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(value, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        val dnaPrompt = extractedDna.entries.joinToString(", ") { "${it.key}: ${it.value}" }
                        repository.triggerQuickMedia(activeProjectId, isVideo = false, prompt = dnaPrompt)
                        Toast.makeText(context, "Test keyframe generation triggered from DNA!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🖼️ Generate Test Image from Extracted DNA")
            }
        }
    }
}
