package org.dpdns.bnai.anime.ui.project

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
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
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.friendlyPipelineStatus
import org.dpdns.bnai.anime.ui.designsystem.*

@Composable
fun BibleScreen() {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager.init(context) }
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()

    var projectId by remember { mutableStateOf(repository.currentProjectId()) }
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf(
        "Overview & Tone",
        "World & Rules",
        "Power System",
        "Characters & Timeline"
    )

    var premise by rememberSaveable(projectId) { mutableStateOf("") }
    var toneGenre by rememberSaveable(projectId) { mutableStateOf("") }
    var worldSetting by rememberSaveable(projectId) { mutableStateOf("") }
    var worldRules by rememberSaveable(projectId) { mutableStateOf("") }
    var powerSystem by rememberSaveable(projectId) { mutableStateOf("") }
    var characterFoundations by rememberSaveable(projectId) { mutableStateOf("") }
    var timelineFoundation by rememberSaveable(projectId) { mutableStateOf("") }

    var savedSnapshot by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSavingDraft by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    var activeWf01CmdId by remember(projectId) { mutableStateOf(tokenManager.getActiveCommandId(projectId, "wf01_bible_gen")) }
    var activeWf01Status by remember { mutableStateOf<String?>(null) }
    var isSubmittingWf01 by remember { mutableStateOf(false) }

    fun currentEditorSnapshot(): String {
        return listOf(
            premise,
            toneGenre,
            worldSetting,
            worldRules,
            powerSystem,
            characterFoundations,
            timelineFoundation
        ).joinToString("||")
    }

    val hasUnsavedChanges = !isLoading && savedSnapshot.isNotEmpty() && currentEditorSnapshot() != savedSnapshot

    BackHandler(enabled = hasUnsavedChanges) {
        showDiscardDialog = true
    }

    fun hydrateFromBibleMap(map: JsonMap) {
        val bible = (map["story_bible"] as? Map<*, *>) ?: map
        premise = (bible["premise"] ?: bible["logline"] ?: bible["overview"] ?: "").toString()
        toneGenre = (bible["genre"] ?: bible["tone"] ?: "Dark Fantasy / Action").toString()
        worldSetting = (bible["world_setting"] ?: bible["world"] ?: bible["setting"] ?: "").toString()
        worldRules = (bible["world_rules"] ?: bible["rules"] ?: "").toString()
        powerSystem = (bible["power_system"] ?: bible["magic_system"] ?: "").toString()
        characterFoundations = (bible["character_foundations"] ?: bible["main_cast_notes"] ?: "").toString()
        timelineFoundation = (bible["timeline"] ?: bible["season_arc"] ?: "").toString()
        savedSnapshot = currentEditorSnapshot()
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
                    errorMessage = res.message
                    isLoading = false
                }
                else -> {
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(projectId) {
        loadBibleFromServer(projectId)
    }

    LaunchedEffect(activeWf01CmdId, projectId) {
        val cmdId = activeWf01CmdId ?: return@LaunchedEffect
        while (true) {
            when (val st = repository.fetchCommandStatus(cmdId)) {
                is Resource.Success -> {
                    val statusUpper = st.data.status?.uppercase() ?: "QUEUED"
                    activeWf01Status = statusUpper
                    if (statusUpper in listOf("SUCCEEDED", "COMPLETED", "BIBLE_SAVED")) {
                        tokenManager.saveActiveCommandId(projectId, "wf01_bible_gen", null)
                        activeWf01CmdId = null
                        Toast.makeText(context, "Story Bible AI generation completed!", Toast.LENGTH_SHORT).show()
                        loadBibleFromServer(projectId)
                        break
                    } else if (statusUpper in listOf("FAILED", "ERROR", "CANCELLED", "DEAD_LETTER")) {
                        tokenManager.saveActiveCommandId(projectId, "wf01_bible_gen", null)
                        activeWf01CmdId = null
                        errorMessage = st.data.error_message ?: "Story Bible AI generation failed."
                        break
                    }
                }
                is Resource.Error -> {
                    break
                }
                else -> {}
            }
            delay(3000)
        }
    }

    if (showDiscardDialog) {
        UnsavedChangesDialog(
            onDiscard = {
                showDiscardDialog = false
                loadBibleFromServer(projectId)
            },
            onKeepEditing = { showDiscardDialog = false }
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
            onProjectChanged = { newPid ->
                projectId = newPid
                loadBibleFromServer(newPid)
            },
            onRefreshClick = { loadBibleFromServer(projectId) }
        )

        ScreenHeader(
            title = "Story Bible",
            subtitle = "Master universe foundation, world rules, power scaling & character anchors",
            trailingContent = {
                if (hasUnsavedChanges) {
                    StatusChip(label = "UNSAVED EDITS")
                } else {
                    StatusChip(label = "SYNCED")
                }
            }
        )

        if (!activeWf01CmdId.isNullOrBlank()) {
            StandardCard {
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
                            text = "Status: ${friendlyPipelineStatus(activeWf01Status ?: "QUEUED")} — Bible will refresh automatically upon completion.",
                            fontSize = 12.sp
                        )
                    }
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                }
            }
        }

        if (errorMessage != null) {
            StandardCard {
                Text(errorMessage ?: "", color = StudioTokens.Error, fontSize = 13.sp)
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
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = toneGenre,
                        onValueChange = { toneGenre = it },
                        label = { Text("Tone & Genre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
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
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = worldRules,
                        onValueChange = { worldRules = it },
                        label = { Text("Immutable World Rules (Enforced by Canon)") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
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
                        modifier = Modifier.fillMaxWidth()
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
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = timelineFoundation,
                        onValueChange = { timelineFoundation = it },
                        label = { Text("Timeline & Season Arc Structure") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
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
                onClick = {
                    if (projectId.isBlank()) return@PrimaryButton
                    isSavingDraft = true
                    scope.launch {
                        val payload: JsonMap = mapOf(
                            "premise" to premise,
                            "genre" to toneGenre,
                            "world_setting" to worldSetting,
                            "world_rules" to worldRules,
                            "power_system" to powerSystem,
                            "character_foundations" to characterFoundations,
                            "timeline" to timelineFoundation
                        )
                        when (val res = repository.updateProjectBible(projectId, payload)) {
                            is Resource.Success -> {
                                savedSnapshot = currentEditorSnapshot()
                                Toast.makeText(context, "Story Bible saved to database", Toast.LENGTH_SHORT).show()
                            }
                            is Resource.Error -> {
                                errorMessage = res.message
                            }
                            else -> {}
                        }
                        isSavingDraft = false
                    }
                },
                modifier = Modifier.weight(1f)
            )

            OutlinedButton(
                onClick = {
                    if (projectId.isBlank() || isSubmittingWf01 || !activeWf01CmdId.isNullOrBlank()) return@OutlinedButton
                    isSubmittingWf01 = true
                    scope.launch {
                        val res = repository.submitStudioCommand(
                            projectId = projectId,
                            intent = CommandIntent.SAVE_BIBLE,
                            payload = mapOf(
                                "premise" to premise,
                                "genre" to toneGenre,
                                "world_setting" to worldSetting,
                                "world_rules" to worldRules,
                                "power_system" to powerSystem
                            )
                        )
                        isSubmittingWf01 = false
                        if (res is Resource.Success) {
                            val cid = res.data.command_id.orEmpty()
                            if (cid.isNotBlank()) {
                                activeWf01CmdId = cid
                                activeWf01Status = res.data.status ?: "QUEUED"
                                tokenManager.saveActiveCommandId(projectId, "wf01_bible_gen", cid)
                                Toast.makeText(context, "AI Bible Architect queued (#${cid.take(6)})", Toast.LENGTH_SHORT).show()
                            }
                        } else if (res is Resource.Error) {
                            errorMessage = res.message
                        }
                    }
                },
                enabled = activeWf01CmdId.isNullOrBlank() && !isSubmittingWf01,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("AI Expand Bible")
            }
        }

        Spacer(modifier = Modifier.height(64.dp))
    }
}
