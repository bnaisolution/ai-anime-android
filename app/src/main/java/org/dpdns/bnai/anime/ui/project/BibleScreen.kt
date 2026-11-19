package org.dpdns.bnai.anime.ui.project

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.BiblePatchDto
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.core.network.SystemPromptDto
import org.dpdns.bnai.anime.ui.LocalAppContainer
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.friendlyPipelineStatus
import org.dpdns.bnai.anime.ui.designsystem.PrimaryButton
import org.dpdns.bnai.anime.ui.designsystem.ScreenHeader
import org.dpdns.bnai.anime.ui.designsystem.SectionHeader
import org.dpdns.bnai.anime.ui.designsystem.StandardCard
import org.dpdns.bnai.anime.ui.designsystem.StatusChip
import org.dpdns.bnai.anime.ui.designsystem.StudioTokens
import org.dpdns.bnai.anime.ui.designsystem.UnsavedChangesDialog
import org.dpdns.bnai.anime.ui.navigation.RegisterNavigationExitGuard
import org.dpdns.bnai.anime.ui.testing.TestTags

private enum class BibleAiMode { Discuss, Structure }

private fun friendlyBibleError(raw: String?): String {
    val message = raw.orEmpty().trim()
    val lower = message.lowercase()
    return when {
        lower.contains("unknownhost") || lower.contains("unable to resolve host") ||
            lower.contains("failed to connect") || lower.contains("connect timed out") ||
            lower.contains("timeout") || lower.contains("network is unreachable") ||
            lower.contains("no address associated") ->
            "Cannot reach the server. Check your internet connection and try again."
        lower.contains("401") || lower.contains("unauthorized") ->
            "Your session expired. Please sign in again."
        lower.contains("403") || lower.contains("forbidden") ->
            "You do not have permission to edit this Story Bible."
        lower.contains("409") || lower.contains("conflict") ->
            "This Story Bible changed after the preview. Refresh and preview again."
        lower.contains("invalid json") || lower.contains("unexpected end of input") ->
            "The server returned an invalid response. Please retry."
        message.isBlank() -> "Something went wrong. Please retry."
        else -> message
    }
}

private fun displayValue(value: Any?): String {
    return when (value) {
        null -> "—"
        is String -> value.ifBlank { "—" }
        is Map<*, *>, is List<*> -> value.toString()
        else -> value.toString()
    }
}

@Composable
fun BibleScreen(
    onOpenCanon: () -> Unit = {},
    onOpenSeasons: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val tokenManager = container.tokenManager
    val repository = container.repository
    val scope = rememberCoroutineScope()

    var projectId by remember { mutableStateOf(repository.currentProjectId()) }
    var selectorEpoch by remember { mutableStateOf(0) }
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf(
        "Overview & Tone",
        "World & Rules",
        "Power System",
        "Characters & Timeline"
    )

    var premise by rememberSaveable(projectId) { mutableStateOf("") }
    var tone by rememberSaveable(projectId) { mutableStateOf("") }
    var worldSetting by rememberSaveable(projectId) { mutableStateOf("") }
    var worldRules by rememberSaveable(projectId) { mutableStateOf("") }
    var powerSystem by rememberSaveable(projectId) { mutableStateOf("") }
    var characterFoundations by rememberSaveable(projectId) { mutableStateOf("") }
    var timelineFoundation by rememberSaveable(projectId) { mutableStateOf("") }
    var totalSeasons by rememberSaveable(projectId) { mutableStateOf<Int?>(null) }
    var episodesPerSeason by rememberSaveable(projectId) { mutableStateOf<Int?>(null) }
    var episodeDurationMin by rememberSaveable(projectId) { mutableStateOf<Int?>(null) }

    var projectGenre by remember { mutableStateOf("") }
    var visualMedium by remember { mutableStateOf("") }
    var bibleHash by remember { mutableStateOf("") }
    var bibleSnapshot by remember { mutableStateOf<JsonMap>(emptyMap()) }
    var livingCanonCount by remember { mutableStateOf<Int?>(null) }
    var savedSnapshot by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSavingDraft by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var baselineLoaded by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var pendingAfterSave by remember { mutableStateOf<(() -> Unit)?>(null) }

    var activeWf01CmdId by remember(projectId) { mutableStateOf(tokenManager.getActiveCommandId(projectId, "wf01_bible_gen")) }
    var activeWf01Status by remember { mutableStateOf<String?>(null) }
    var isSubmittingAi by remember { mutableStateOf(false) }
    var structureInstruction by rememberSaveable(projectId) { mutableStateOf("") }
    var aiModeName by rememberSaveable(projectId) { mutableStateOf(BibleAiMode.Structure.name) }
    val aiMode = runCatching { BibleAiMode.valueOf(aiModeName) }.getOrDefault(BibleAiMode.Structure)
    var biblePrompts by remember { mutableStateOf<List<SystemPromptDto>>(emptyList()) }
    var activeBiblePromptId by remember(projectId) { mutableStateOf<String?>(null) }
    var patches by remember { mutableStateOf<List<BiblePatchDto>>(emptyList()) }
    var isLoadingPatches by remember { mutableStateOf(false) }
    var patchActionId by remember { mutableStateOf<String?>(null) }

    fun currentEditorSnapshot(): String {
        return listOf(
            premise,
            tone,
            worldSetting,
            worldRules,
            powerSystem,
            characterFoundations,
            timelineFoundation,
            totalSeasons,
            episodesPerSeason,
            episodeDurationMin
        ).joinToString("||")
    }

    val hasUnsavedChanges = !isLoading &&
        (baselineLoaded || savedSnapshot.isNotEmpty()) &&
        currentEditorSnapshot() != savedSnapshot

    fun requestExit(proceed: () -> Unit) {
        if (hasUnsavedChanges) {
            pendingAfterSave = proceed
            showDiscardDialog = true
        } else {
            proceed()
        }
    }

    fun switchProject(nextProjectId: String) {
        tokenManager.saveActiveProjectId(nextProjectId)
        projectId = nextProjectId
    }

    RegisterNavigationExitGuard { proceed -> requestExit(proceed) }

    BackHandler {
        requestExit(onNavigateBack)
    }

    fun hydrateFromBibleMap(map: JsonMap) {
        val bible = (map["story_bible"] as? Map<*, *>) ?: map
        val cleanBible = bible.entries
            .filter { it.key is String }
            .associate { it.key as String to it.value }
        premise = (cleanBible["premise"] ?: cleanBible["logline"] ?: cleanBible["overview"] ?: "").toString()
        tone = (cleanBible["tone"] ?: cleanBible["tone_mood"] ?: "").toString()
        worldSetting = (cleanBible["world_setting"] ?: cleanBible["world"] ?: cleanBible["setting"] ?: "").toString()
        worldRules = (cleanBible["world_rules"] ?: cleanBible["rules"] ?: "").toString()
        powerSystem = (cleanBible["power_system"] ?: cleanBible["magic_system"] ?: "").toString()
        characterFoundations = (cleanBible["character_foundations"] ?: cleanBible["main_cast_notes"] ?: "").toString()
        timelineFoundation = (cleanBible["timeline"] ?: cleanBible["season_arc"] ?: "").toString()
        totalSeasons = (cleanBible["total_seasons"] as? Number)?.toInt() ?: 1
        episodesPerSeason = (cleanBible["episodes_per_season"] as? Number)?.toInt()
            ?: (cleanBible["target_episode_count"] as? Number)?.toInt()
            ?: 12
        episodeDurationMin = (cleanBible["episode_duration_min"] as? Number)?.toInt() ?: 24
        projectGenre = (map["genre"] ?: cleanBible["genre"] ?: "").toString()
        visualMedium = (map["visual_medium"] ?: cleanBible["visual_medium"] ?: "").toString()
        bibleHash = map["bible_hash"]?.toString().orEmpty()
        bibleSnapshot = cleanBible
        activeBiblePromptId = map["active_system_prompt_id"]?.toString()?.takeIf { it.isNotBlank() }
        savedSnapshot = currentEditorSnapshot()
        baselineLoaded = true
    }

    fun loadBibleFromServer(targetPid: String = repository.currentProjectId()) {
        if (targetPid.isBlank()) {
            isLoading = false
            return
        }
        scope.launch {
            isLoading = true
            errorMessage = null
            when (val res = repository.fetchBible(targetPid)) {
                is Resource.Success -> {
                    hydrateFromBibleMap(res.data)
                    isLoading = false
                }
                is Resource.Error -> {
                    errorMessage = friendlyBibleError(res.message)
                    isLoading = false
                }
                else -> {
                    isLoading = false
                }
            }
        }
    }

    fun loadBiblePrompts(targetPid: String) {
        if (targetPid.isBlank()) return
        scope.launch {
            biblePrompts = emptyList()
            when (val res = repository.fetchSystemPrompts(targetPid, "BIBLE")) {
                is Resource.Success -> biblePrompts = res.data
                is Resource.Empty -> biblePrompts = emptyList()
                else -> {}
            }
        }
    }

    fun loadLivingCanonCount(targetPid: String) {
        if (targetPid.isBlank()) return
        livingCanonCount = null
        scope.launch {
            when (val res = repository.fetchCanon(targetPid)) {
                is Resource.Success -> livingCanonCount = res.data.size
                is Resource.Empty -> livingCanonCount = 0
                else -> livingCanonCount = null
            }
        }
    }

    fun loadPatches(targetPid: String) {
        if (targetPid.isBlank()) {
            patches = emptyList()
            return
        }
        scope.launch {
            isLoadingPatches = true
            when (val res = repository.fetchBiblePatches(targetPid)) {
                is Resource.Success -> patches = res.data
                is Resource.Empty -> patches = emptyList()
                is Resource.Error -> errorMessage = friendlyBibleError(res.message)
                else -> {}
            }
            isLoadingPatches = false
        }
    }

    LaunchedEffect(projectId) {
        loadBibleFromServer(projectId)
        loadBiblePrompts(projectId)
        loadLivingCanonCount(projectId)
        loadPatches(projectId)
    }

    LaunchedEffect(activeWf01CmdId, projectId) {
        val cmdId = activeWf01CmdId ?: return@LaunchedEffect
        while (true) {
            when (val st = repository.fetchCommandStatus(cmdId)) {
                is Resource.Success -> {
                    val statusUpper = st.data.status?.uppercase() ?: "QUEUED"
                    activeWf01Status = statusUpper
                    if (statusUpper in listOf("SUCCEEDED", "COMPLETED", "BIBLE_SAVED", "PATCH_READY")) {
                        tokenManager.saveActiveCommandId(projectId, "wf01_bible_gen", null)
                        activeWf01CmdId = null
                        Toast.makeText(context, "AI response received. Review the proposed patch below.", Toast.LENGTH_SHORT).show()
                        loadBibleFromServer(projectId)
                        loadPatches(projectId)
                        delay(1500)
                        loadPatches(projectId)
                        break
                    } else if (statusUpper in listOf("FAILED", "ERROR", "CANCELLED", "DEAD_LETTER")) {
                        tokenManager.saveActiveCommandId(projectId, "wf01_bible_gen", null)
                        activeWf01CmdId = null
                        errorMessage = friendlyBibleError(st.data.error_message ?: "Story Bible AI request failed.")
                        break
                    }
                }
                is Resource.Error -> {
                    errorMessage = friendlyBibleError(st.message)
                    break
                }
                else -> {}
            }
            delay(3000)
        }
    }

    val pendingPatches = patches.filter {
        it.status?.uppercase() in setOf("PATCH_READY", "PENDING_APPROVAL", "CONFLICT", "APPLYING")
    }

    if (showDiscardDialog) {
        UnsavedChangesDialog(
            onSave = {
                showDiscardDialog = false
                isSavingDraft = true
                scope.launch {
                    when (val res = repository.updateProjectBible(projectId, mapOf(
                        "premise" to premise,
                        "tone" to tone,
                        "world_setting" to worldSetting,
                        "world_rules" to worldRules,
                        "power_system" to powerSystem,
                        "character_foundations" to characterFoundations,
                        "timeline" to timelineFoundation
                    ))) {
                        is Resource.Success -> {
                            savedSnapshot = currentEditorSnapshot()
                            baselineLoaded = true
                            isSavingDraft = false
                            val nextAction = pendingAfterSave
                            pendingAfterSave = null
                            nextAction?.invoke()
                        }
                        is Resource.Error -> {
                            errorMessage = friendlyBibleError(res.message)
                            isSavingDraft = false
                            pendingAfterSave = null
                        }
                        else -> {
                            isSavingDraft = false
                            pendingAfterSave = null
                        }
                    }
                }
            },
            onDiscard = {
                showDiscardDialog = false
                savedSnapshot = currentEditorSnapshot()
                baselineLoaded = true
                val nextAction = pendingAfterSave
                pendingAfterSave = null
                nextAction?.invoke()
            },
            onKeepEditing = {
                showDiscardDialog = false
                pendingAfterSave = null
                selectorEpoch += 1
            }
        )
    }

    Column(
        modifier = Modifier
            .testTag(TestTags.Screen.BIBLE)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        key(projectId, selectorEpoch) {
            ProjectSelectorBar(
                selectedProjectIdOverride = projectId,
                persistSelection = false,
                onProjectChanged = { newPid ->
                    if (newPid == projectId) return@ProjectSelectorBar
                    requestExit { switchProject(newPid) }
                },
                onRefreshClick = { loadBibleFromServer(projectId); loadPatches(projectId) }
            )
        }

        ScreenHeader(
            title = "Story Bible",
            subtitle = "Master universe foundation, world rules, power scaling & character anchors",
            trailingContent = {
                when {
                    isLoading -> StatusChip(label = "LOADING")
                    hasUnsavedChanges -> StatusChip(label = "UNSAVED EDITS")
                    else -> StatusChip(label = "SAVED")
                }
            }
        )

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (errorMessage != null) {
            StandardCard {
                Text(
                    text = errorMessage ?: "",
                    color = StudioTokens.Error,
                    fontSize = 13.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { loadBibleFromServer(projectId); loadPatches(projectId) },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry")
                    }
                    OutlinedButton(onClick = { errorMessage = null }) {
                        Text("Dismiss")
                    }
                }
            }
        }

        StandardCard {
            SectionHeader(title = "Project Context", badgeText = "READ ONLY")
            ReadOnlyContextRow("Genre", projectGenre.ifBlank { "Not set" })
            ReadOnlyContextRow("Visual Medium", visualMedium.ifBlank { "Not set" })
            ReadOnlyContextRow("Total Seasons", displayValue(totalSeasons))
            ReadOnlyContextRow("Episodes per Season", displayValue(episodesPerSeason))
            ReadOnlyContextRow("Episode Duration", episodeDurationMin?.let { "$it minutes" } ?: "—")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Living Canon: ${livingCanonCount?.let { "$it facts" } ?: "Unavailable"}",
                    fontSize = 13.sp
                )
                TextButton(onClick = onOpenCanon) {
                    Text("Open Living Canon")
                }
            }
        }

        StandardCard {
            SectionHeader(title = "Seasons", badgeText = "ONE BIBLE")
            Text(
                text = "${totalSeasons ?: 1} season${if ((totalSeasons ?: 1) == 1) "" else "s"} planned. Season arcs, episode ranges, season genres, and prompt overrides are stored here in the single project Story Bible.",
                fontSize = 13.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Full episode scripts remain inside each episode.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onOpenSeasons) {
                    Text("Open Seasons")
                }
            }
        }

        StandardCard(modifier = Modifier.testTag(TestTags.Bible.PROMPT_LIBRARY)) {
            SectionHeader(title = "Active Bible Prompt", badgeText = "BIBLE")
            val activePrompt = biblePrompts.find { it.id == activeBiblePromptId }
            Text(
                text = activePrompt?.title ?: "No BIBLE prompt active yet.",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text(
                text = activePrompt?.let { "${it.genre ?: "Any genre"} | ${it.format ?: "Any format"} | v${it.version}" }
                    ?: "Choose an owned BIBLE prompt below. It will be used for the next AI request.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (biblePrompts.isEmpty()) {
                Text(
                    text = "No saved BIBLE prompts yet. Create one in Prompt Studio.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                biblePrompts.forEach { prompt ->
                    val isActive = prompt.id == activeBiblePromptId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prompt.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                text = "${prompt.genre ?: "Any genre"} | ${prompt.format ?: "Any format"} | v${prompt.version}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            enabled = !isActive,
                            onClick = {
                                scope.launch {
                                    when (val activated = repository.activateSystemPrompt(projectId, prompt.id, "BIBLE")) {
                                        is Resource.Success -> {
                                            activeBiblePromptId = prompt.id
                                            Toast.makeText(context, "Bible prompt activated", Toast.LENGTH_SHORT).show()
                                        }
                                        is Resource.Error -> errorMessage = friendlyBibleError(activated.message)
                                        else -> {}
                                    }
                                }
                            },
                            modifier = Modifier.testTag(TestTags.item(TestTags.Bible.PROMPT_OPTION_PREFIX, prompt.id))
                        ) {
                            Text(if (isActive) "Active" else "Use")
                        }
                    }
                }
            }
        }

        StandardCard {
            SectionHeader(title = "AI Workspace", badgeText = "SAFE PATCH")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = aiMode == BibleAiMode.Discuss,
                    onClick = { aiModeName = BibleAiMode.Discuss.name },
                    label = { Text("Discuss") }
                )
                FilterChip(
                    selected = aiMode == BibleAiMode.Structure,
                    onClick = { aiModeName = BibleAiMode.Structure.name },
                    label = { Text("Structure") }
                )
            }
            Text(
                text = if (aiMode == BibleAiMode.Discuss) {
                    "Ask questions without changing the Story Bible. The AI uses the current project context and active BIBLE prompt."
                } else {
                    "Paste an outline or instructions. The AI returns a preview patch; nothing is written until you approve it."
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Context: ${projectGenre.ifBlank { "genre not set" }} | ${visualMedium.ifBlank { "medium not set" }} | " +
                    "${totalSeasons ?: "—"} seasons | ${episodesPerSeason ?: "—"} eps/season | " +
                    "prompt: ${biblePrompts.find { it.id == activeBiblePromptId }?.title ?: "none"}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = structureInstruction,
                onValueChange = { structureInstruction = it },
                label = {
                    Text(if (aiMode == BibleAiMode.Discuss) "Question for AI" else "Structure Instruction (required for AI Expand)")
                },
                minLines = 3,
                modifier = Modifier.fillMaxWidth().testTag(TestTags.Bible.STRUCTURE_INSTRUCTION)
            )
            if (!activeWf01CmdId.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Bible Architect Active (#${activeWf01CmdId?.take(8)})",
                            fontWeight = FontWeight.Bold,
                            color = StudioTokens.Primary
                        )
                        Text(
                            text = "Status: ${friendlyPipelineStatus(activeWf01Status ?: "QUEUED")}",
                            fontSize = 12.sp
                        )
                    }
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                }
            }
            OutlinedButton(
                onClick = {
                    if (projectId.isBlank() || isSubmittingAi || !activeWf01CmdId.isNullOrBlank()) return@OutlinedButton
                    if (structureInstruction.isBlank()) {
                        Toast.makeText(
                            context,
                            if (aiMode == BibleAiMode.Discuss) "Please enter a question first" else "Please enter Structure Instruction first",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@OutlinedButton
                    }
                    isSubmittingAi = true
                    scope.launch {
                        val sharedPayload: Map<String, Any?> = mapOf(
                            "premise" to premise,
                            "tone" to tone,
                            "world_setting" to worldSetting,
                            "world_rules" to worldRules,
                            "power_system" to powerSystem,
                            "character_foundations" to characterFoundations,
                            "timeline" to timelineFoundation,
                            "total_seasons" to totalSeasons,
                            "episodes_per_season" to episodesPerSeason,
                            "episode_duration_min" to episodeDurationMin,
                            "userPrompt" to structureInstruction,
                            "user_prompt" to structureInstruction,
                            "base_hash" to bibleHash,
                            "operation" to if (aiMode == BibleAiMode.Discuss) "DISCUSS" else "STRUCTURE"
                        )
                        val intent = if (aiMode == BibleAiMode.Discuss) CommandIntent.BIBLE_CHAT else CommandIntent.SAVE_BIBLE
                        val res = repository.submitStudioCommand(
                            projectId = projectId,
                            intent = intent,
                            payload = sharedPayload
                        )
                        isSubmittingAi = false
                        if (res is Resource.Success) {
                            val cid = res.data.command_id.orEmpty()
                            if (cid.isNotBlank()) {
                                activeWf01CmdId = cid
                                activeWf01Status = res.data.status ?: "QUEUED"
                                tokenManager.saveActiveCommandId(projectId, "wf01_bible_gen", cid)
                                Toast.makeText(context, "AI request queued (#${cid.take(6)})", Toast.LENGTH_SHORT).show()
                                loadPatches(projectId)
                            }
                        } else if (res is Resource.Error) {
                            errorMessage = friendlyBibleError(res.message)
                        }
                    }
                },
                enabled = activeWf01CmdId.isNullOrBlank() && !isSubmittingAi,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (aiMode == BibleAiMode.Discuss) "Ask AI" else "AI Expand Bible")
            }
        }

        StandardCard {
            SectionHeader(title = "Proposed Changes", badgeText = "APPROVAL REQUIRED")
            if (isLoadingPatches) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (pendingPatches.isEmpty()) {
                Text(
                    text = "No proposed changes. AI output will appear here for review before it is applied.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                pendingPatches.forEach { patch ->
                    val status = patch.status?.uppercase() ?: "PATCH_READY"
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = patch.summary?.takeIf { it.isNotBlank() } ?: "AI Story Bible update",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            StatusChip(label = status)
                        }
                        Text("${patch.fields.size} proposed field(s)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val entityCount = patch.entities.values.sumOf { value ->
                            (value as? List<*>)?.size ?: 0
                        }
                        if (entityCount > 0) {
                            Text(
                                text = "$entityCount proposed canon record(s): " +
                                    patch.entities.entries
                                        .filter { (it.value as? List<*>)?.isNotEmpty() == true }
                                        .joinToString { (key, value) -> "$key=${(value as List<*>).size}" },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        patch.fields.forEach { (field, newValue) ->
                            val oldValue = bibleSnapshot[field]
                            Text(
                                text = "$field: ${displayValue(oldValue)} → ${displayValue(newValue)}",
                                fontSize = 12.sp
                            )
                        }
                        if (status == "CONFLICT") {
                            Text(
                                text = "This preview is stale. Reject it and create a fresh preview.",
                                fontSize = 12.sp,
                                color = StudioTokens.Warning
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PrimaryButton(
                                text = "Approve",
                                enabled = status in setOf("PATCH_READY", "PENDING_APPROVAL") && patchActionId == null,
                                isLoading = patchActionId == patch.id,
                                modifier = Modifier.weight(1f).testTag(TestTags.item(TestTags.Bible.PATCH_APPROVE_PREFIX, patch.id)),
                                onClick = {
                                    patchActionId = patch.id
                                    scope.launch {
                                        when (val res = repository.approveBiblePatch(projectId, patch.id, patch.base_hash ?: bibleHash)) {
                                            is Resource.Success -> {
                                                Toast.makeText(context, "Story Bible patch applied", Toast.LENGTH_SHORT).show()
                                                loadBibleFromServer(projectId)
                                                loadPatches(projectId)
                                            }
                                            is Resource.Error -> {
                                                errorMessage = friendlyBibleError(res.message)
                                                loadPatches(projectId)
                                            }
                                            else -> {}
                                        }
                                        patchActionId = null
                                    }
                                }
                            )
                            OutlinedButton(
                                onClick = {
                                    patchActionId = patch.id
                                    scope.launch {
                                        when (val res = repository.rejectBiblePatch(projectId, patch.id, "Rejected by user")) {
                                            is Resource.Success -> loadPatches(projectId)
                                            is Resource.Error -> errorMessage = friendlyBibleError(res.message)
                                            else -> {}
                                        }
                                        patchActionId = null
                                    }
                                },
                                enabled = patchActionId == null,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reject")
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }

        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
            tabs.forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(label) }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                StandardCard {
                    SectionHeader(title = "Series Premise & Tone")
                    OutlinedTextField(
                        value = premise,
                        onValueChange = { premise = it },
                        label = { Text("Master Premise / Logline") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth().testTag(TestTags.Bible.PREMISE_FIELD)
                    )
                    OutlinedTextField(
                        value = tone,
                        onValueChange = { tone = it },
                        label = { Text("Tone & Mood") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag(TestTags.Bible.TONE_GENRE_FIELD)
                    )
                    Text("Genre: ${projectGenre.ifBlank { "Not set" }}", fontSize = 13.sp)
                    Text(
                        "Genre is read-only here. Edit it in Project Settings.",
                        fontSize = 12.sp,
                        color = StudioTokens.Warning
                    )
                    Text(
                        "Series Structure: ${totalSeasons ?: "—"} seasons | ${episodesPerSeason ?: "—"} episodes/season | ${episodeDurationMin ?: "—"} minutes",
                        fontSize = 12.sp
                    )
                    Text(
                        "Series Structure is read-only here. Edit it in Project Settings.",
                        fontSize = 12.sp,
                        color = StudioTokens.Warning
                    )
                }
            }
            1 -> {
                StandardCard {
                    SectionHeader(title = "World Building & Immutable Laws")
                    OutlinedTextField(
                        value = worldSetting,
                        onValueChange = { worldSetting = it },
                        label = { Text("World Setting & Geography") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth().testTag(TestTags.Bible.WORLD_SETTING_FIELD)
                    )
                    OutlinedTextField(
                        value = worldRules,
                        onValueChange = { worldRules = it },
                        label = { Text("Immutable World Rules (Enforced by Canon)") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth().testTag(TestTags.Bible.WORLD_RULES_FIELD)
                    )
                }
            }
            2 -> {
                StandardCard {
                    SectionHeader(title = "Power System & Combat Limits")
                    OutlinedTextField(
                        value = powerSystem,
                        onValueChange = { powerSystem = it },
                        label = { Text("Power System Rules, Costs & Ceilings") },
                        minLines = 6,
                        modifier = Modifier.fillMaxWidth().testTag(TestTags.Bible.POWER_SYSTEM_FIELD)
                    )
                }
            }
            3 -> {
                StandardCard {
                    SectionHeader(title = "Character Foundations & Story Timeline")
                    OutlinedTextField(
                        value = characterFoundations,
                        onValueChange = { characterFoundations = it },
                        label = { Text("Core Cast Foundations & Factions") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth().testTag(TestTags.Bible.CHARACTER_FOUNDATIONS_FIELD)
                    )
                    OutlinedTextField(
                        value = timelineFoundation,
                        onValueChange = { timelineFoundation = it },
                        label = { Text("Timeline & Season Arc Structure") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth().testTag(TestTags.Bible.TIMELINE_FIELD)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PrimaryButton(
                text = "Save Bible Draft",
                isLoading = isSavingDraft,
                enabled = hasUnsavedChanges,
                onClick = {
                    if (projectId.isBlank()) return@PrimaryButton
                    isSavingDraft = true
                    scope.launch {
                        when (val res = repository.updateProjectBible(projectId, mapOf(
                            "premise" to premise,
                            "tone" to tone,
                            "world_setting" to worldSetting,
                            "world_rules" to worldRules,
                            "power_system" to powerSystem,
                            "character_foundations" to characterFoundations,
                            "timeline" to timelineFoundation
                        ))) {
                            is Resource.Success -> {
                                savedSnapshot = currentEditorSnapshot()
                                baselineLoaded = true
                                Toast.makeText(context, "Story Bible saved to database", Toast.LENGTH_SHORT).show()
                                loadBibleFromServer(projectId)
                            }
                            is Resource.Error -> errorMessage = friendlyBibleError(res.message)
                            else -> {}
                        }
                        isSavingDraft = false
                    }
                },
                modifier = Modifier.weight(1f).testTag(TestTags.Bible.SAVE_DRAFT)
            )
            if (hasUnsavedChanges) {
                OutlinedButton(
                    onClick = { loadBibleFromServer(projectId) },
                    enabled = !isSavingDraft,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text("Discard Changes")
                }
            }
        }

        Spacer(modifier = Modifier.height(64.dp))
    }
}

@Composable
private fun ReadOnlyContextRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
