package org.dpdns.bnai.anime.ui.episode

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.EpisodeDto
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.core.network.SystemPromptDto
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.designsystem.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodePlannerScreen(
    onStartProduction: (String) -> Unit = {},
    onOpenScenes: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()

    var episodes by remember { mutableStateOf<List<EpisodeDto>>(emptyList()) }
    var selectedEpNo by remember { mutableStateOf(1) }
    var epDropdownExpanded by remember { mutableStateOf(false) }

    var editTitle by remember { mutableStateOf("") }
    var editSynopsis by remember { mutableStateOf("") }
    var editScriptText by remember { mutableStateOf("") }
    var currentStatus by remember { mutableStateOf("PENDING") }
    var currentQcStatus by remember { mutableStateOf("PENDING") }

    var episodePrompts by remember { mutableStateOf<List<SystemPromptDto>>(emptyList()) }
    var selectedPrompt by remember { mutableStateOf<SystemPromptDto?>(null) }
    var promptDropdownExpanded by remember { mutableStateOf(false) }
    var showPromptPreview by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(true) }
    var isSavingMetadata by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }
    var showRegenerateConfirm by remember { mutableStateOf(false) }

    fun hydrateEpisodeFields(dto: EpisodeDto?) {
        if (dto == null) {
            editTitle = "Episode $selectedEpNo"
            editSynopsis = ""
            editScriptText = ""
            currentStatus = "UNSET"
            currentQcStatus = "UNCHECKED"
            return
        }
        editTitle = dto.title?.takeIf { it.isNotBlank() } ?: "Episode ${dto.episode_no}"
        editSynopsis = dto.synopsis.orEmpty()
        val scriptMap = dto.script_json as? Map<*, *>
        editScriptText = scriptMap?.get("raw_script")?.toString()
            ?: dto.script_json?.toString().orEmpty()
        currentStatus = dto.current_state ?: dto.status ?: "PENDING"
        currentQcStatus = dto.qc_status ?: "PENDING"
    }

    fun loadPlannerData() {
        val pid = repository.currentProjectId()
        if (pid.isBlank()) {
            isLoading = false
            return
        }
        scope.launch {
            isLoading = true
            val epRes = repository.fetchEpisodesRaw(pid)
            val prRes = repository.fetchSystemPrompts(pid)

            if (epRes is Resource.Success) {
                episodes = epRes.data
                val matched = epRes.data.find { it.episode_no == selectedEpNo } ?: epRes.data.firstOrNull()
                if (matched != null) {
                    selectedEpNo = matched.episode_no
                }
                hydrateEpisodeFields(matched)
            }
            if (prRes is Resource.Success) {
                episodePrompts = prRes.data.filter {
                    val cat = (it.prompt_category ?: "EPISODE").uppercase()
                    cat == "EPISODE" || cat == "BIBLE"
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadPlannerData()
    }

    fun dispatchEpisodeGeneration(isRegeneration: Boolean) {
        val pid = repository.currentProjectId()
        if (pid.isBlank() || isGenerating) return
        isGenerating = true
        scope.launch {
            val payload = mutableMapOf<String, Any?>(
                "episode_no" to selectedEpNo,
                "title" to editTitle.trim(),
                "synopsis" to editSynopsis.trim(),
                "force_regenerate" to isRegeneration
            )
            selectedPrompt?.let { sp ->
                payload["prompt_id"] = sp.id
                payload["custom_system_prompt"] = sp.prompt_text
                payload["prompt_category"] = sp.prompt_category ?: "EPISODE"
            }

            val res = repository.submitStudioCommand(
                projectId = pid,
                intent = if (isRegeneration) CommandIntent.CONTINUE_EPISODE else CommandIntent.GENERATE_EPISODE,
                episodeNo = selectedEpNo,
                payload = payload
            )
            isGenerating = false
            if (res is Resource.Success) {
                val cid = res.data.command_id.orEmpty()
                if (cid.isNotBlank()) {
                    Toast.makeText(context, "Episode $selectedEpNo production queued!", Toast.LENGTH_SHORT).show()
                    onStartProduction(cid)
                }
            } else if (res is Resource.Error) {
                Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    if (showRegenerateConfirm) {
        ConfirmationDialog(
            title = "Regenerate Episode $selectedEpNo?",
            message = "This will start a new AI story/production run for Episode $selectedEpNo. Ordinary script edits should use 'Save Script & Metadata' instead.",
            confirmText = "Regenerate Episode",
            onConfirm = {
                showRegenerateConfirm = false
                dispatchEpisodeGeneration(isRegeneration = true)
            },
            onDismiss = { showRegenerateConfirm = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProjectSelectorBar(
            onProjectChanged = { loadPlannerData() },
            onRefreshClick = { loadPlannerData() }
        )

        ScreenHeader(
            title = "Episode Planner",
            subtitle = "Edit episode metadata/script via PATCH or launch preflight-checked production",
            trailingContent = { StatusChip(label = currentStatus) }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExposedDropdownMenuBox(
                expanded = epDropdownExpanded,
                onExpandedChange = { epDropdownExpanded = !epDropdownExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = "Episode $selectedEpNo: $editTitle",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Episode") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = epDropdownExpanded) },
                    singleLine = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = epDropdownExpanded,
                    onDismissRequest = { epDropdownExpanded = false }
                ) {
                    val epNums = (episodes.map { it.episode_no } + listOf(1, 2, 3, 4, 5)).distinct().sorted()
                    epNums.forEach { num ->
                        val match = episodes.find { it.episode_no == num }
                        DropdownMenuItem(
                            text = { Text("Episode $num: ${match?.title ?: "New Episode"}") },
                            onClick = {
                                selectedEpNo = num
                                hydrateEpisodeFields(match)
                                epDropdownExpanded = false
                            }
                        )
                    }
                }
            }
            SecondaryButton(
                text = "Scenes",
                onClick = { onOpenScenes(selectedEpNo) }
            )
        }

        StandardCard {
            SectionHeader(
                title = "Episode Prompt Selection",
                badgeText = selectedPrompt?.prompt_category ?: "PROJECT DEFAULT"
            )
            ExposedDropdownMenuBox(
                expanded = promptDropdownExpanded,
                onExpandedChange = { promptDropdownExpanded = !promptDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedPrompt?.let { "${it.title} (${it.prompt_category ?: "EPISODE"})" } ?: "Use Project Default Episode Prompt",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Director Prompt Override") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = promptDropdownExpanded) },
                    singleLine = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = promptDropdownExpanded,
                    onDismissRequest = { promptDropdownExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Use Project Default Episode Prompt") },
                        onClick = {
                            selectedPrompt = null
                            promptDropdownExpanded = false
                        }
                    )
                    episodePrompts.forEach { pr ->
                        DropdownMenuItem(
                            text = { Text("${pr.title} [${pr.prompt_category ?: "EPISODE"}]") },
                            onClick = {
                                selectedPrompt = pr
                                promptDropdownExpanded = false
                            }
                        )
                    }
                }
            }
            if (selectedPrompt != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Category: ${selectedPrompt?.prompt_category ?: "EPISODE"}",
                        fontSize = 12.sp,
                        color = StudioTokens.Primary
                    )
                    TextButton(onClick = { showPromptPreview = !showPromptPreview }) {
                        Text(if (showPromptPreview) "Hide Preview" else "Preview Prompt")
                    }
                }
                if (showPromptPreview) {
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = selectedPrompt?.prompt_text.orEmpty(),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        StandardCard {
            SectionHeader(title = "Episode Metadata & Script Editor", badgeText = "QC: $currentQcStatus")
            OutlinedTextField(
                value = editTitle,
                onValueChange = { editTitle = it },
                label = { Text("Episode Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = editSynopsis,
                onValueChange = { editSynopsis = it },
                label = { Text("Episode Synopsis") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = editScriptText,
                onValueChange = { editScriptText = it },
                label = { Text("Episode Script / Narrative Outline") },
                minLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            PrimaryButton(
                text = "Save Script & Metadata (No Generation)",
                isLoading = isSavingMetadata,
                onClick = {
                    val pid = repository.currentProjectId()
                    if (pid.isBlank()) return@PrimaryButton
                    isSavingMetadata = true
                    scope.launch {
                        val updates = mutableMapOf<String, Any?>(
                            "title" to editTitle.trim(),
                            "synopsis" to editSynopsis.trim(),
                            "script_text" to editScriptText
                        )
                        selectedPrompt?.id?.let { updates["prompt_id"] = it }
                        when (val res = repository.updateEpisodeSync(pid, selectedEpNo, updates)) {
                            is Resource.Success -> {
                                Toast.makeText(context, "Episode $selectedEpNo saved via PATCH", Toast.LENGTH_SHORT).show()
                                loadPlannerData()
                            }
                            is Resource.Error -> {
                                Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                            }
                            else -> {}
                        }
                        isSavingMetadata = false
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        StandardCard {
            SectionHeader(title = "AI Production Pipeline (Preflight Gated)")
            Text(
                text = "Runs WF-12 Quota -> WF-09 Canon -> WF-06 Continuity preflight checks before WF-02 Story & WF-03 Scene Production.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrimaryButton(
                    text = "Generate Episode",
                    isLoading = isGenerating,
                    enabled = !isGenerating,
                    onClick = { dispatchEpisodeGeneration(isRegeneration = false) },
                    modifier = Modifier.weight(1f)
                )
                SecondaryButton(
                    text = "Regenerate...",
                    enabled = !isGenerating,
                    onClick = { showRegenerateConfirm = true },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(64.dp))
    }
}
