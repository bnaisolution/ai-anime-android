package org.dpdns.bnai.anime.ui.project

import org.dpdns.bnai.anime.ui.LocalAppContainer

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.StatusBadge
import org.dpdns.bnai.anime.ui.components.formatInt
import org.dpdns.bnai.anime.ui.components.friendlyPipelineStatus
import org.dpdns.bnai.anime.ui.designsystem.*
import org.dpdns.bnai.anime.ui.preflight.ReadinessReport
import org.dpdns.bnai.anime.ui.preflight.loadReadiness
import org.dpdns.bnai.anime.ui.testing.TestTags

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: String = "",
    onOpenBible: () -> Unit = {},
    onOpenCharacters: () -> Unit = {},
    onOpenEpisodes: () -> Unit = {},
    onOpenProduction: () -> Unit = {},
    onOpenScenes: () -> Unit = {},
    onOpenQC: () -> Unit = {},
    onOpenPublish: () -> Unit = {},
    onOpenReviews: () -> Unit = {},
    onOpenReviewInbox: () -> Unit = onOpenReviews,
    onOpenAiBrain: () -> Unit = {},
    onOpenQuickTools: () -> Unit = {},
    onOpenVision: () -> Unit = {},
    onOpenCanon: () -> Unit = {},
    onOpenContinuity: () -> Unit = {},
    onOpenLore: () -> Unit = {},
    onOpenCharDeepDive: () -> Unit = {},
    onOpenMediaLibrary: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {},
    onOpenJobsMonitor: () -> Unit = {},
    onOpenQuota: () -> Unit = {},
    onOpenBudget: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenPromptStudio: () -> Unit = {},
    onOpenPreflight: (String) -> Unit = {},
    viewModel: ProjectViewModel = viewModel(factory = LocalAppContainer.current.viewModelFactory)
) {
    val context = LocalContext.current
    val tokenManager = LocalAppContainer.current.tokenManager
    val repository = LocalAppContainer.current.repository
    val scope = rememberCoroutineScope()

    val activePid = projectId.ifBlank { repository.currentProjectId() }
    var overviewMap by remember { mutableStateOf<JsonMap>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    var activeCmdId by remember(activePid) { mutableStateOf(tokenManager.getActiveCommandId(activePid, "quick_gen_episode")) }
    var activeCmdStatus by remember { mutableStateOf<String?>(null) }
    var readinessReport by remember { mutableStateOf<ReadinessReport?>(null) }
    var isCheckingReadiness by remember { mutableStateOf(false) }
    var isUploadingCover by remember { mutableStateOf(false) }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editGenre by remember { mutableStateOf("") }
    var editMedium by remember { mutableStateOf("ANIME") }
    var editCoverUrl by remember { mutableStateOf("") }
    var editAutoApprove by remember { mutableStateOf(false) }
    var isSavingSettings by remember { mutableStateOf(false) }
    var settingsDirty by remember { mutableStateOf(false) }
    val builtInGenres = listOf("Action", "Thriller", "Horror", "Comedy", "Dark Fantasy", "Fantasy", "Sci-Fi", "Romance", "Drama", "Slice of Life")
    val mediumOptions = listOf("ANIME", "MANHWA", "3D", "2D ANIMATION")
    var selectedGenres by remember { mutableStateOf<Set<String>>(emptySet()) }

    fun refreshReadiness(pid: String) {
        if (pid.isBlank()) return
        scope.launch {
            isCheckingReadiness = true
            readinessReport = loadReadiness(repository, pid)
            isCheckingReadiness = false
        }
    }

    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val pid = activePid.ifBlank { repository.currentProjectId() }
        if (pid.isBlank()) return@rememberLauncherForActivityResult
        isUploadingCover = true
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
            if (bytes == null || bytes.isEmpty()) {
                isUploadingCover = false
                Toast.makeText(context, "Could not read selected image", Toast.LENGTH_LONG).show()
                return@launch
            }
            if (bytes.size > 12 * 1024 * 1024) {
                isUploadingCover = false
                Toast.makeText(context, "Image is larger than 12 MB. Choose a smaller file.", Toast.LENGTH_LONG).show()
                return@launch
            }
            val contentType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val filename = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "cover.jpg"
            val key = "projects/$pid/cover/${System.currentTimeMillis()}-${filename.replace(Regex("[^A-Za-z0-9._-]"), "_")}"
            when (val res = repository.uploadStorageObject(key, filename, contentType, bytes)) {
                is Resource.Success -> {
                    val url = (res.data["url"] ?: res.data["public_url"] ?: res.data["download_url"])
                        ?.toString()?.takeIf { it.startsWith("http") }
                    if (url != null) {
                        editCoverUrl = url
                        settingsDirty = true
                        showSettingsDialog = true
                        Toast.makeText(context, "Cover uploaded. Save settings to apply.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Uploaded, but storage returned no public URL. Paste a URL manually.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                is Resource.Error -> Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                else -> Unit
            }
            isUploadingCover = false
        }
    }

    fun loadProjectOverviewData(targetPid: String = repository.currentProjectId()) {
        if (targetPid.isBlank()) {
            isLoading = false
            loadError = "No active project selected."
            return
        }
        scope.launch {
            isLoading = true
            loadError = null
            when (val res = repository.fetchProjectOverviewRaw(targetPid)) {
                is Resource.Success -> {
                    overviewMap = res.data
                    val projObj = (res.data["project"] as? Map<*, *>) ?: res.data
                    val bibleObj = (projObj["story_bible"] as? Map<*, *>) ?: emptyMap<String, Any?>()

                    editName = (projObj["name"] ?: res.data["name"] ?: "Studio Project").toString()
                    editGenre = (res.data["genre"] ?: projObj["genre"] ?: bibleObj["genre"] ?: "Action / Fantasy").toString()
                    editMedium = (res.data["visual_medium"] ?: projObj["visual_medium"] ?: bibleObj["visual_medium"] ?: "ANIME").toString()
                    editCoverUrl = (res.data["cover_image_url"] ?: projObj["cover_image_url"] ?: bibleObj["cover_image_url"] ?: "").toString()
                        .let { if (it == "null") "" else it }
                    editAutoApprove = (res.data["auto_approve"] ?: projObj["auto_approve"] ?: bibleObj["auto_approve"]) == true

                    val backendActiveCmd = (res.data["status"] as? Map<*, *>)?.get("active_command_id")?.toString()
                    if (!backendActiveCmd.isNullOrBlank() && backendActiveCmd != "null") {
                        activeCmdId = backendActiveCmd
                        tokenManager.saveActiveCommandId(targetPid, "quick_gen_episode", backendActiveCmd)
                    }
                    isLoading = false
                }
                is Resource.Error -> {
                    loadError = res.message
                    isLoading = false
                }
                else -> {
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(activePid) {
        loadProjectOverviewData(activePid)
    }

    LaunchedEffect(activePid, activeCmdId) {
        refreshReadiness(activePid)
    }

    LaunchedEffect(activeCmdId, activePid) {
        val cid = activeCmdId ?: return@LaunchedEffect
        while (true) {
            when (val st = repository.fetchCommandStatus(cid)) {
                is Resource.Success -> {
                    val statusUpper = st.data.status?.uppercase() ?: "QUEUED"
                    activeCmdStatus = statusUpper
                    if (statusUpper in listOf("SUCCEEDED", "COMPLETED", "FAILED", "ERROR", "CANCELLED")) {
                        tokenManager.saveActiveCommandId(activePid, "quick_gen_episode", null)
                        activeCmdId = null
                        loadProjectOverviewData(activePid)
                        break
                    }
                }
                else -> break
            }
            delay(3000)
        }
    }

    val projName = ((overviewMap["project"] as? Map<*, *>)?.get("name") ?: overviewMap["name"] ?: "Project Detail").toString()
    val coverUrl = (overviewMap["cover_image_url"] ?: (overviewMap["project"] as? Map<*, *>)?.get("cover_image_url"))
        ?.toString()?.takeIf { it.isNotBlank() && it != "null" }

    val canonStatus = (overviewMap["canon_status"] ?: (overviewMap["status"] as? Map<*, *>)?.get("canon_status"))
        ?.toString()?.takeIf { it.isNotBlank() && it != "null" } ?: "UNSET"
    val continuityStatus = (overviewMap["continuity_status"] ?: (overviewMap["status"] as? Map<*, *>)?.get("continuity_status"))
        ?.toString()?.takeIf { it.isNotBlank() && it != "null" } ?: "UNCHECKED"

    val countsObj = (overviewMap["counts"] as? Map<*, *>) ?: overviewMap
    val episodesCount = formatInt(countsObj["episodes_count"] ?: overviewMap["episodes_count"] ?: 0)
    val charactersCount = formatInt(countsObj["characters_count"] ?: overviewMap["characters_count"] ?: 0)
    val scenesCount = formatInt(countsObj["scenes_count"] ?: overviewMap["scenes_count"] ?: 0)
    val activeJobsCount = formatInt(countsObj["active_jobs_count"] ?: overviewMap["active_jobs_count"] ?: 0)
    val pendingApprovalsCount = formatInt(countsObj["pending_approvals_count"] ?: overviewMap["pending_approvals_count"] ?: 0)
    val pendingActionsCount = formatInt(countsObj["pending_actions_count"] ?: overviewMap["pending_actions_count"] ?: 0)

    val nextEpisodeNo = (episodesCount.toIntOrNull() ?: 0) + 1
    val generationLabel = if (nextEpisodeNo == 1) "Create 1st Episode" else "Create Episode $nextEpisodeNo"
    val readinessReady = readinessReport?.allReady == true
    val readinessBadge = when {
        !activeCmdId.isNullOrBlank() -> friendlyPipelineStatus(activeCmdStatus ?: "RUNNING")
        isCheckingReadiness -> "CHECKING"
        readinessReady -> "READY"
        else -> "PENDING"
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSavingSettings) showSettingsDialog = false },
            title = { Text("Project Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it; settingsDirty = true },
                        label = { Text("Project Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCoverUrl,
                        onValueChange = { editCoverUrl = it; settingsDirty = true },
                        label = { Text("Cover Image URL (https://...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    SecondaryButton(
                        text = if (isUploadingCover) "Uploading..." else "Upload / Change Cover Image",
                        enabled = !isUploadingCover,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { coverPicker.launch("image/*") }
                    )
                    Text("Genre (select multiple)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        builtInGenres.forEach { genre ->
                            val selected = selectedGenres.contains(genre)
                            if (selected) {
                                Button(
                                    onClick = {
                                        selectedGenres = selectedGenres - genre
                                        settingsDirty = true
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp)
                                ) {
                                    Text(genre, fontSize = 11.sp)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        selectedGenres = selectedGenres + genre
                                        settingsDirty = true
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp)
                                ) {
                                    Text(genre, fontSize = 11.sp)
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                showSettingsDialog = false
                                onOpenPromptStudio()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("+ Add more genre", fontSize = 11.sp)
                        }
                    }
                    Text("Visual Medium", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        mediumOptions.forEach { medium ->
                            val selected = editMedium.equals(medium, ignoreCase = true)
                            if (selected) {
                                Button(
                                    onClick = { editMedium = medium; settingsDirty = true },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp)
                                ) {
                                    Text(medium, fontSize = 11.sp)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { editMedium = medium; settingsDirty = true },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp)
                                ) {
                                    Text(medium, fontSize = 11.sp)
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                showSettingsDialog = false
                                onOpenPromptStudio()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("+ Add more medium", fontSize = 11.sp)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Approve Policy", fontWeight = FontWeight.SemiBold)
                            Text("Automatically approve QC & Canon gates when checks pass", fontSize = 11.sp)
                        }
                        Switch(
                            checked = editAutoApprove,
                            onCheckedChange = { editAutoApprove = it; settingsDirty = true },
                            modifier = Modifier.testTag(TestTags.ProjectDetail.AUTO_APPROVE)
                        )
                    }
                }
            },
            confirmButton = {
                PrimaryButton(
                    text = "Save Settings",
                    isLoading = isSavingSettings,
                    enabled = !isSavingSettings && settingsDirty,
                    modifier = Modifier.testTag(TestTags.ProjectDetail.SAVE_SETTINGS),
                    onClick = {
                        val pid = repository.currentProjectId()
                        if (pid.isBlank()) return@PrimaryButton
                        isSavingSettings = true
                        scope.launch {
                            val res = repository.updateProjectSettings(
                                projectId = pid,
                                updates = mapOf(
                                    "name" to editName.trim(),
                                    "genre" to (if (selectedGenres.isNotEmpty()) selectedGenres.joinToString(", ") else editGenre.trim()),
                                    "visual_medium" to editMedium.trim(),
                                    "cover_image_url" to editCoverUrl.trim(),
                                    "auto_approve" to editAutoApprove
                                )
                            )
                            isSavingSettings = false
                            if (res is Resource.Success) {
                                settingsDirty = false
                                showSettingsDialog = false
                                loadProjectOverviewData(pid)
                                Toast.makeText(context, "Project settings saved", Toast.LENGTH_SHORT).show()
                            } else if (res is Resource.Error) {
                                Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { showSettingsDialog = false },
                    enabled = !isSavingSettings
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(TestTags.Screen.PROJECT_DETAIL)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ProjectSelectorBar(
            onProjectChanged = { newPid -> loadProjectOverviewData(newPid) },
            onRefreshClick = { loadProjectOverviewData(repository.currentProjectId()) }
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = StudioTokens.Primary)
            }
        } else if (loadError != null) {
            StandardCard {
                Text("Error Loading Project", color = StudioTokens.Error, fontWeight = FontWeight.Bold)
                Text(loadError ?: "Unknown error", fontSize = 13.sp)
                SecondaryButton(text = "Retry", onClick = { loadProjectOverviewData(repository.currentProjectId()) })
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isUploadingCover) { coverPicker.launch("image/*") }
            ) {
                AsyncMediaPreview(
                    mediaUrl = coverUrl,
                    contentDescription = "$projName Cover Banner",
                    height = 150.dp,
                    emptyLabel = "$projName • $editMedium ($editGenre)"
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { coverPicker.launch("image/*") },
                        enabled = !isUploadingCover,
                        modifier = Modifier.height(34.dp)
                    ) {
                        if (isUploadingCover) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Text(if (coverUrl == null) "Upload cover" else "Change cover", fontSize = 11.sp)
                        }
                    }
                }
            }

            ScreenHeader(
                title = projName,
                subtitle = "Medium: $editMedium • Genre: $editGenre • Policy: ${if (editAutoApprove) "Auto-Approve" else "Review Gate"}",
                trailingContent = {
                    IconButton(
                        onClick = {
                            selectedGenres = editGenre.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
                            settingsDirty = false
                            showSettingsDialog = true
                        },
                        modifier = Modifier.testTag(TestTags.ProjectDetail.SETTINGS_BUTTON)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Project Settings")
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StandardCard(modifier = Modifier.weight(1f), onClick = onOpenCanon) {
                    Text("Canon Status", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    StatusChip(label = canonStatus)
                }
                StandardCard(modifier = Modifier.weight(1f), onClick = onOpenContinuity) {
                    Text("Continuity Check", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    StatusChip(label = continuityStatus)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard(label = "Episodes", value = episodesCount, modifier = Modifier.weight(1f), onClick = onOpenEpisodes)
                MetricCard(label = "Scenes", value = scenesCount, modifier = Modifier.weight(1f), onClick = onOpenScenes)
                MetricCard(label = "Characters", value = charactersCount, modifier = Modifier.weight(1f), onClick = onOpenCharacters)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard(
                    label = "Active Jobs",
                    value = activeJobsCount,
                    accentColor = if (activeJobsCount != "0") StudioTokens.Warning else StudioTokens.Primary,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenJobsMonitor
                )
                MetricCard(
                    label = "Pending Approvals",
                    value = pendingApprovalsCount,
                    accentColor = if (pendingApprovalsCount != "0") StudioTokens.Warning else StudioTokens.Success,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenReviewInbox
                )
                MetricCard(
                    label = "Queued Actions",
                    value = pendingActionsCount,
                    accentColor = StudioTokens.Secondary,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenJobsMonitor
                )
            }

            SectionHeader(title = "Studio Modules")
            val modules = listOf(
                "Story Bible" to onOpenBible,
                "Episode Planner" to onOpenEpisodes,
                "Scene Inspector" to onOpenScenes,
                "Characters" to onOpenCharacters,
                "Character Deep Dive" to onOpenCharDeepDive,
                "World Wiki" to onOpenLore,
                "Vision DNA" to onOpenVision,
                "Quick Generate" to onOpenQuickTools,
                "Media Library" to onOpenMediaLibrary,
                "Quality Control (QC)" to onOpenQC,
                "Review Inbox" to onOpenReviewInbox,
                "Publish Studio" to onOpenPublish,
                "Analytics" to onOpenAnalytics,
                "Quota Dashboard" to onOpenQuota,
                "Budget Ledger" to onOpenBudget
            )

            modules.chunked(2).forEach { pair ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { (label, action) ->
                        OutlinedButton(
                            onClick = action,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(label, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }

            StandardCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Episode Production Control",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier.clickable { if (activePid.isNotBlank()) onOpenPreflight(activePid) }
                    ) {
                        StatusBadge(readinessBadge)
                    }
                }
                if (!activeCmdId.isNullOrBlank()) {
                    Text(
                        text = "Active Job #${activeCmdId?.take(8)} is currently ${friendlyPipelineStatus(activeCmdStatus ?: "RUNNING")}. Duplicate submission is locked.",
                        fontSize = 12.sp,
                        color = StudioTokens.Warning
                    )
                    SecondaryButton(
                        text = "Open Active Job in Pipeline Monitor",
                        onClick = onOpenProduction,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    val pending = readinessReport?.pending.orEmpty()
                    if (!readinessReady) {
                        Text(
                            if (isCheckingReadiness) {
                                "Checking production readiness..."
                            } else {
                                "${pending.size} readiness check(s) pending: ${pending.take(3).joinToString(", ") { it.label }}"
                            },
                            fontSize = 12.sp,
                            color = StudioTokens.Warning
                        )
                    }
                    PrimaryButton(
                        text = generationLabel,
                        enabled = readinessReady && !isCheckingReadiness,
                        onClick = { if (activePid.isNotBlank()) onOpenPreflight(activePid) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    SecondaryButton(
                        text = if (readinessReady) "Review Readiness" else "View Pending Checks",
                        onClick = { if (activePid.isNotBlank()) onOpenPreflight(activePid) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}
