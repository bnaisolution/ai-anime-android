package org.dpdns.bnai.anime.ui.studio

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.*
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun PromptStudioScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiService = remember { RetrofitClient.getApiService(context) }
    val repository = remember { AnimeRepository(apiService) }

    var activeProjectId by remember { mutableStateOf(repository.currentProjectId()) }
    var selectedCategoryTab by remember { mutableIntStateOf(0) }
    val categories = listOf(
        "BIBLE" to "Story Bible",
        "EPISODE" to "Episode Script",
        "IMAGE" to "Visual / Image",
        "AI_DIRECTOR" to "AI Director"
    )

    var promptsList by remember { mutableStateOf<List<SystemPromptDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("Dark Shonen Master Prompt") }
    var visualMedium by remember { mutableStateOf("ANIME") }
    var promptText by remember { mutableStateOf("Generate cinematic 2D cel-shaded anime scenes with high-contrast sakuga lighting, expressive character keyframes, and tight pacing.") }
    var savedBaselineText by remember { mutableStateOf(promptText) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var isTestingPrompt by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    val hasUnsavedChanges = promptText != savedBaselineText

    // Fix #248: BackHandler warning for unsaved prompt edits
    BackHandler(enabled = hasUnsavedChanges) {
        showUnsavedDialog = true
    }

    fun loadPrompts(pid: String) {
        scope.launch {
            isLoading = true
            try {
                val res = apiService.getSystemPrompts(pid.ifBlank { null })
                if (res.isSuccessful) {
                    promptsList = res.body()?.data.orEmpty()
                }
            } catch (_: Exception) {
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(activeProjectId) {
        loadPrompts(activeProjectId)
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Discard Unsaved Prompt Changes?") },
            text = { Text("You have edited the system prompt without saving. Discard your edits?") },
            confirmButton = {
                Button(
                    onClick = {
                        promptText = savedBaselineText
                        showUnsavedDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedDialog = false }) { Text("Keep Editing") }
            }
        )
    }

    val presets = listOf(
        "Dark Fantasy" to "Dark fantasy seinen style, intricate gothic worldbuilding, grim atmospheric lighting, high stakes.",
        "Cyberpunk" to "Neon-drenched cyberpunk anime, rain-slicked megacity reflections, kinetic synthwave camera cuts.",
        "Peak Shonen" to "High-energy shonen battle choreography, dynamic speedlines, impact frames, emotional inner monologues.",
        "Photoreal Cinema" to "Photorealistic 35mm anamorphic lens cinema, natural skin texture, volumetric fog, dramatic shadows."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Prompt Studio Control Center", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // Fix #247: Project Context Selector
        ProjectSelectorBar(
            onProjectChanged = { newPid ->
                activeProjectId = newPid
                loadPrompts(newPid)
            },
            onRefreshClick = { loadPrompts(activeProjectId) }
        )

        // Fix #251 & #257: Control Center Category Tabs (Story Bible, Episode, Image, AI Director)
        ScrollableTabRow(selectedTabIndex = selectedCategoryTab, edgePadding = 0.dp) {
            categories.forEachIndexed { index, (_, label) ->
                Tab(
                    selected = selectedCategoryTab == index,
                    onClick = { selectedCategoryTab = index },
                    text = { Text(label) }
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Fix #256 & #258: Smart Inheritance Hierarchy Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Smart Prompt Inheritance Tree", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Global Default ➔ Project Override (${categories[selectedCategoryTab].second}) ➔ Episode Payload",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            // Fix #249: Visual Medium Selector with Icons
            Text("Visual Medium", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = visualMedium == "ANIME",
                    onClick = { visualMedium = "ANIME" },
                    label = { Text("🎨 Anime (2D Cel)") }
                )
                FilterChip(
                    selected = visualMedium == "REAL_HUMAN",
                    onClick = { visualMedium = "REAL_HUMAN" },
                    label = { Text("📷 Realistic Cinema") }
                )
            }

            // Fix #246: Preset Chips
            Text("Quick Genre Presets", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presets) { (presetName, presetBody) ->
                    AssistChip(
                        onClick = {
                            title = "$presetName (${categories[selectedCategoryTab].second})"
                            promptText = presetBody
                        },
                        label = { Text("✨ $presetName") }
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Prompt Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = promptText,
                onValueChange = { promptText = it },
                label = { Text("${categories[selectedCategoryTab].second} System Instructions") },
                minLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            // Fix #250 & #255: Test Prompt & Apply to Current Project Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        isTestingPrompt = true
                        scope.launch {
                            val res = repository.chatWithBrain(
                                message = "Test this ${categories[selectedCategoryTab].second} system prompt and give a 2-line sample output:\n$promptText",
                                projectId = activeProjectId,
                                currentScreen = "prompt_studio"
                            )
                            testResultText = when (res) {
                                is Resource.Success -> res.data.reply ?: res.data.message ?: "Prompt verified."
                                is Resource.Error -> "Test error: ${res.message}"
                                else -> "No response."
                            }
                            isTestingPrompt = false
                        }
                    },
                    enabled = !isTestingPrompt && promptText.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isTestingPrompt) "Testing..." else "✨ Test Prompt")
                }

                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val categoryCode = categories[selectedCategoryTab].first
                                val res = apiService.createSystemPrompt(
                                    CreateSystemPromptRequest(
                                        project_id = activeProjectId.ifBlank { null },
                                        title = title.trim(),
                                        visual_medium = visualMedium,
                                        prompt_category = categoryCode,
                                        prompt_text = promptText.trim(),
                                        set_active = true
                                    )
                                )
                                if (res.isSuccessful) {
                                    savedBaselineText = promptText
                                    Toast.makeText(context, "Applied & Saved to Project!", Toast.LENGTH_SHORT).show()
                                    loadPrompts(activeProjectId)
                                } else {
                                    Toast.makeText(context, "Failed (HTTP ${res.code()})", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, e.localizedMessage ?: "Error saving prompt", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apply to Project")
                }
            }

            testResultText?.let { sample ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("AI Director Test Output", fontWeight = FontWeight.Bold)
                        Text(sample, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            HorizontalDivider()

            // Fix #243, #244, #245, #254: Saved Prompts List with Active Badge & Delete Icon
            Text("Saved Prompts (${promptsList.size})", style = MaterialTheme.typography.headlineMedium)
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else if (promptsList.isEmpty()) {
                Text("No saved custom prompts yet. Save one above to override defaults.", style = MaterialTheme.typography.bodyLarge)
            } else {
                promptsList.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, style = MaterialTheme.typography.headlineMedium)
                                    Text(
                                        "${if (item.visual_medium == "ANIME") "🎨 Anime" else "📷 Realistic"} • Category: ${item.prompt_category ?: "BIBLE"}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (item.is_default == true) {
                                        StatusBadge("ACTIVE")
                                    }
                                    IconButton(onClick = {
                                        title = item.title
                                        visualMedium = item.visual_medium
                                        promptText = item.prompt_text
                                        savedBaselineText = item.prompt_text
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Load into Editor")
                                    }
                                    IconButton(onClick = {
                                        scope.launch {
                                            repository.deletePrompt(item.id)
                                            loadPrompts(activeProjectId)
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Prompt", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            Text(item.prompt_text, maxLines = 2, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}
