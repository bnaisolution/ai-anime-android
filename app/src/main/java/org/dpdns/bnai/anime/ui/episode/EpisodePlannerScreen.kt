package org.dpdns.bnai.anime.ui.episode

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.model.Character
import org.dpdns.bnai.anime.core.model.Episode
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.components.StatusBadge
import org.dpdns.bnai.anime.ui.components.formatRelativeTime
import org.dpdns.bnai.anime.ui.components.friendlyPipelineStatus

@Composable
fun EpisodePlannerScreen(
    onStartProduction: (String) -> Unit = {},
    onOpenScenes: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()

    var episodesState by remember { mutableStateOf<Resource<List<Episode>>>(Resource.Loading) }
    var characters by remember { mutableStateOf<List<Character>>(emptyList()) }
    var hasBible by remember { mutableStateOf(true) }
    var gpuOnline by remember { mutableStateOf(true) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    // Pre-flight & Script Editor states
    var showPreflightDialog by remember { mutableStateOf(false) }
    var autoApprovePolicy by remember { mutableStateOf(true) }
    var selectedCastIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showCastOverride by remember { mutableStateOf(false) }

    var editingEpisode by remember { mutableStateOf<Episode?>(null) }
    var editedScriptText by remember { mutableStateOf("") }
    var readyForProductionEp by remember { mutableStateOf<Episode?>(null) }

    fun loadAll() {
        val pid = repository.currentProjectId()
        scope.launch {
            episodesState = Resource.Loading
            episodesState = repository.fetchEpisodes(pid)
            val charRes = repository.fetchCharacters(pid)
            characters = (charRes as? Resource.Success)?.data.orEmpty()
            selectedCastIds = characters.map { it.id }.toSet()
            val bibleRes = repository.fetchBible(pid)
            hasBible = bibleRes is Resource.Success
            val workersRes = repository.fetchAdminWorkers(pid)
            gpuOnline = (workersRes as? Resource.Success)?.data?.isNotEmpty() ?: true
        }
    }

    LaunchedEffect(Unit) { loadAll() }

    // Fix #38, #45, #213, #216, #217, #218: Pre-Flight Checklist & Policy Dialog before Episode Generation
    if (showPreflightDialog) {
        AlertDialog(
            onDismissRequest = { showPreflightDialog = false },
            title = { Text("Pre-Flight Episode Check", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Verify studio readiness before generating a new episode:")
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("1. Story Bible:")
                        StatusBadge(if (hasBible) "READY" else "MISSING")
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("2. Project Characters (${characters.size}):")
                        StatusBadge(if (characters.isNotEmpty()) "READY" else "BLOCKED (0)")
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("3. Compute Worker (GPU):")
                        StatusBadge(if (gpuOnline) "ONLINE" else "STANDBY")
                    }

                    // Fix #216: Block generation if project has 0 characters
                    if (characters.isEmpty()) {
                        Text(
                            "⚠️ Cannot generate episode with 0 characters. Please add or auto-generate characters first.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    // Fix #45: Start Kaggle option if GPU is offline
                    if (!gpuOnline) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    repository.submitStudioCommand(repository.currentProjectId(), CommandIntent.ADMIN_START_KAGGLE, "QUICK")
                                    Toast.makeText(context, "Starting Kaggle GPU worker...", Toast.LENGTH_SHORT).show()
                                    gpuOnline = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("⚡ Start Kaggle GPU Now") }
                    }

                    HorizontalDivider()

                    // Fix #213: Approval Policy choice
                    Text("Approval Policy", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = autoApprovePolicy,
                            onClick = { autoApprovePolicy = true },
                            label = { Text("Auto-Approve") }
                        )
                        FilterChip(
                            selected = !autoApprovePolicy,
                            onClick = { autoApprovePolicy = false },
                            label = { Text("Pause & Ask") }
                        )
                    }

                    // Fix #217: Smart Casting & Manual Override
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cast: AI Smart Pick (${selectedCastIds.size})", fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = { showCastOverride = !showCastOverride }) {
                            Text(if (showCastOverride) "Hide Cast" else "Override Cast")
                        }
                    }
                    if (showCastOverride) {
                        characters.forEach { ch ->
                            val checked = ch.id in selectedCastIds
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        selectedCastIds = if (isChecked) selectedCastIds + ch.id else selectedCastIds - ch.id
                                    }
                                )
                                Text("${ch.name} (${ch.role})")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPreflightDialog = false
                        scope.launch {
                            val payload = mapOf(
                                "auto_approve" to autoApprovePolicy,
                                "cast_ids" to selectedCastIds.toList()
                            )
                            val res = repository.submitStudioCommand(
                                repository.currentProjectId(),
                                CommandIntent.GENERATE_EPISODE,
                                "SAVE",
                                payload = payload
                            )
                            if (res is Resource.Success) {
                                val cmdId = res.data.command?.id ?: res.data.command_id ?: ""
                                statusBanner = "Episode generation queued! Tap 'Start Video Production' when script is ready."
                                if (cmdId.length == 36) onStartProduction(cmdId) else loadAll()
                            } else if (res is Resource.Error) {
                                statusBanner = "Error: ${res.message}"
                            }
                        }
                    },
                    enabled = characters.isNotEmpty()
                ) { Text("Confirm & Generate") }
            },
            dismissButton = {
                TextButton(onClick = { showPreflightDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Fix #44 & #46: Full-screen Script Viewer / Editor Dialog
    editingEpisode?.let { ep ->
        AlertDialog(
            onDismissRequest = { editingEpisode = null },
            title = { Text("Script Viewer & Editor • Ep ${ep.episodeNumber}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Synopsis: ${ep.synopsis ?: "No synopsis available"}")
                    OutlinedTextField(
                        value = editedScriptText,
                        onValueChange = { editedScriptText = it },
                        label = { Text("Episode Script / Screenplay JSON") },
                        minLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        repository.submitStudioCommand(
                            projectId = repository.currentProjectId(),
                            intent = CommandIntent.CONTINUE_EPISODE,
                            mode = "SAVE",
                            episodeNo = ep.episodeNumber,
                            payload = mapOf("script_override" to editedScriptText)
                        )
                        Toast.makeText(context, "Script updated for Episode ${ep.episodeNumber}", Toast.LENGTH_SHORT).show()
                        editingEpisode = null
                        readyForProductionEp = ep
                    }
                }) { Text("Save Script") }
            },
            dismissButton = {
                TextButton(onClick = { editingEpisode = null }) { Text("Close") }
            }
        )
    }

    // Fix #219: 2-Step Flow Prompt ("🚀 Start Video Production")
    readyForProductionEp?.let { ep ->
        AlertDialog(
            onDismissRequest = { readyForProductionEp = null },
            title = { Text("🚀 Start Video Production?", fontWeight = FontWeight.Bold) },
            text = { Text("Script for Episode ${ep.episodeNumber} (${ep.title}) is ready. Launch GPU/CPU rendering pipeline now?") },
            confirmButton = {
                Button(onClick = {
                    val targetEp = ep
                    readyForProductionEp = null
                    scope.launch {
                        val res = repository.submitStudioCommand(
                            projectId = repository.currentProjectId(),
                            intent = CommandIntent.CONTINUE_EPISODE,
                            mode = "SAVE",
                            episodeNo = targetEp.episodeNumber,
                            payload = mapOf("stage" to "production", "auto_approve" to autoApprovePolicy)
                        )
                        if (res is Resource.Success) {
                            val cmdId = res.data.command?.id ?: res.data.command_id ?: ""
                            if (cmdId.length == 36) onStartProduction(cmdId)
                        }
                    }
                }) { Text("🚀 Start Video Production") }
            },
            dismissButton = {
                TextButton(onClick = { readyForProductionEp = null }) { Text("Later") }
            }
        )
    }

    Scaffold(
        // Fix #38 & #43: Bottom "Generate New Episode" primary button
        bottomBar = {
            Surface(tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showPreflightDialog = true },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate New Episode")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Fix #37: Project Selector in Header
            ProjectSelectorBar(
                onProjectChanged = { loadAll() },
                onRefreshClick = { loadAll() }
            )

            Text("Episodes & Scripts", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)

            statusBanner?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))) {
                    Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.secondary)
                }
            }

            ResourceStateBox(
                resource = episodesState,
                emptyMessage = "No episodes created yet. Tap 'Generate New Episode' below to begin.",
                onRetry = { loadAll() }
            ) { list ->
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(list, key = { it.id }) { ep ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Fix #39 & #40: Clean non-duplicate Episode title + non-clickable status badge
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Ep ${ep.episodeNumber}: ${ep.title}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Updated ${formatRelativeTime(ep.updatedAt)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    StatusBadge(friendlyPipelineStatus(ep.status))
                                }

                                // Fix #42: Episode Synopsis
                                Text(
                                    text = ep.synopsis?.takeIf { it.isNotBlank() } ?: "Synopsis: AI screenplay and scene breakdown ready for inspection.",
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                // Fix #44: Inline Script Preview snippet
                                if (!ep.scriptText.isNullOrBlank()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Script Preview: ${ep.scriptText.take(140)}...",
                                            modifier = Modifier.padding(10.dp),
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                // Fix #41, #43, #46 & GAP #1/#4.1: Clean buttons for Inspect Scenes, Script Editor & Run Production (Existing)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onOpenScenes(ep.episodeNumber) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Inspect Scenes", fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            editedScriptText = ep.scriptText ?: ep.synopsis ?: ""
                                            editingEpisode = ep
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Script Editor", fontSize = 12.sp)
                                    }
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            val res = repository.submitStudioCommand(
                                                projectId = repository.currentProjectId(),
                                                intent = CommandIntent.CONTINUE_EPISODE,
                                                mode = "SAVE",
                                                episodeNo = ep.episodeNumber,
                                                payload = mapOf("stage" to "production", "auto_approve" to autoApprovePolicy)
                                            )
                                            when (res) {
                                                is Resource.Success -> {
                                                    val cmdId = res.data.command?.id ?: res.data.command_id ?: ""
                                                    if (cmdId.length == 36) {
                                                        onStartProduction(cmdId)
                                                    } else {
                                                        statusBanner = "Production queued for Ep ${ep.episodeNumber}."
                                                    }
                                                }
                                                is Resource.Error -> {
                                                    statusBanner = "Failed to start production: ${res.message}"
                                                }
                                                else -> Unit
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Run Production (Existing)")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
