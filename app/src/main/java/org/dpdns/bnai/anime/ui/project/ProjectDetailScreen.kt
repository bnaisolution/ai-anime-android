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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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

    var activePid by remember { mutableStateOf(projectId.ifBlank { repository.currentProjectId() }) }
    var overviewMap by remember { mutableStateOf<JsonMap>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var pendingSwitchPid by remember { mutableStateOf<String?>(null) }

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
    var editTargetEpisodeCount by remember { mutableStateOf<Int?>(null) }
    var editEpisodeDurationMin by remember { mutableStateOf<Int?>(null) }
    var editScriptWordCount by remember { mutableStateOf<Int?>(null) }
    var editTotalSeasons by remember { mutableStateOf("1") }
    var editEpisodesPerSeason by remember { mutableStateOf("12") }
    var editEpisodeDuration by remember { mutableStateOf("24") }
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

    fun loadProjectOverviewData(targetPid: String = activePid) {
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
                    editTargetEpisodeCount = (bibleObj["target_episode_count"] as? Number)?.toInt()
                    editEpisodeDurationMin = (bibleObj["episode_duration_min"] as? Number)?.toInt()
                    editScriptWordCount = (bibleObj["script_word_count"] as? Number)?.toInt()
                    editTotalSeasons = ((bibleObj["total_seasons"] as? Number)?.toInt() ?: 1).toString()
                    editEpisodesPerSeason = (
                        (bibleObj["episodes_per_season"] as? Number)?.toInt()
                            ?: (bibleObj["target_episode_count"] as? Number)?.toInt()
                            ?: 12
                        ).toString()
                    editEpisodeDuration = ((bibleObj["episode_duration_min"] as? Number)?.toInt() ?: 24).toString()

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

    val errorsObj = (overviewMap["errors"] as? Map<*, *>) ?: emptyMap<String, Any?>()
    val statusObj = overviewMap["status"] as? Map<*, *>
    val countsObj = overviewMap["counts"] as? Map<*, *>

    val canonStatus = when {
        isLoading -> "LOADING"
        errorsObj["canon"] != null -> "UNAVAILABLE"
        else -> (overviewMap["canon_status"] ?: statusObj?.get("canon_status"))
            ?.toString()?.takeIf { it.isNotBlank() && it != "null" } ?: "UNSET"
    }
    val continuityStatus = when {
        isLoading -> "LOADING"
        errorsObj["continuity"] != null -> "UNAVAILABLE"
        else -> (overviewMap["continuity_status"] ?: statusObj?.get("continuity_status"))
            ?.toString()?.takeIf { it.isNotBlank() && it != "null" } ?: "UNCHECKED"
    }

    fun metricValue(errorKey: String, countKey: String): String {
        if (isLoading) return "…"
        if (errorsObj[errorKey] != null) return "—"
        val value = countsObj?.get(countKey) ?: overviewMap[countKey]
        return if (value == null) "—" else formatInt(value)
    }

    val episodesCount = metricValue("episodes", "episodes_count")
    val charactersCount = metricValue("characters", "characters_count")
    val scenesCount = metricValue("scenes", "scenes_count")
    val activeJobsCount = metricValue("jobs", "active_jobs_count")
    val pendingApprovalsCount = metricValue("approvals", "pending_approvals_count")
    val pendingActionsCount = metricValue("pending_actions", "pending_actions_count")

    val seriesObj = overviewMap["series"] as? Map<*, *> ?: emptyMap<String, Any?>()
    val overviewTotalSeasons = (seriesObj["total_seasons"] as? Number)?.toInt()
    val overviewEpisodesPerSeason = (seriesObj["episodes_per_season"] as? Number)?.toInt()
    val overviewDuration = (seriesObj["episode_duration_min"] as? Number)?.toInt()

    val nextObj = overviewMap["next_episode"] as? Map<*, *>
    val nextEpisodeNo = (nextObj?.get("episode_no") as? Number)?.toInt()
        ?: ((countsObj?.get("episodes_count") as? Number)?.toInt() ?: 0) + 1
    val nextSeasonNo = (nextObj?.get("season_no") as? Number)?.toInt()
        ?: (overviewMap["project"] as? Map<*, *>)?.get("current_season")?.let { (it as? Number)?.toInt() }
        ?: 1
    val totalSeasonsForLabel = overviewTotalSeasons ?: 1

    val generationLabel = when {
        nextEpisodeNo <= 1 -> "Create 1st Episode"
        totalSeasonsForLabel > 1 || nextSeasonNo > 1 -> "Create S$nextSeasonNo E$nextEpisodeNo"
        else -> "Create Episode $nextEpisodeNo"
    }

    val totalSeasonsInput = editTotalSeasons.toIntOrNull()
    val episodesPerSeasonInput = editEpisodesPerSeason.toIntOrNull()
    val durationInput = editEpisodeDuration.toIntOrNull()
    val seriesError = when {
        totalSeasonsInput == null || episodesPerSeasonInput == null || durationInput == null ->
            "Series values must be whole numbers."
        (totalSeasonsInput ?: 0) !in 1..100 -> "Total Seasons must be between 1 and 100."
        (episodesPerSeasonInput ?: 0) !in 1..500 -> "Episodes per Season must be between 1 and 500."
        (durationInput ?: 0) !in 1..120 -> "Episode Duration must be between 1 and 120 minutes."
        (totalSeasonsInput ?: 0).toLong() * (episodesPerSeasonInput ?: 0).toLong() > 50_000L ->
            "Total planned episodes cannot exceed 50,000."
        else -> null
    }
    val seriesSummary = if (seriesError == null) {
        "${totalSeasonsInput} Season${if (totalSeasonsInput == 1) "" else "s"} · " +
            "${totalSeasonsInput!!.toLong() * episodesPerSeasonInput!!.toLong()} Episodes · " +
            "$durationInput Minutes/Episode"
    } else null

    val readinessReady = readinessReport?.allReady == true
    val readinessBadge = when {
        !activeCmdId.isNullOrBlank() -> friendlyPipelineStatus(activeCmdStatus ?: "RUNNING")
        isCheckingReadiness -> "CHECKING"
        readinessReady -> "READY"
        readinessReport == null -> "UNAVAILABLE"
        else -> "NEEDS ATTENTION"
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSavingSettings) showSettingsDialog = false },
            title = { Text("Project Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                    if (editCoverUrl.isNotBlank()) {
                        AsyncMediaPreview(
                            mediaUrl = editCoverUrl,
                            contentDescription = "Cover image preview",
                            height = 100.dp,
                            emptyLabel = "Cover preview"
                        )
                    }
                    Text("Genre (select multiple)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        (builtInGenres + selectedGenres).distinct().forEach { genre ->
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
                    Text("Series Configuration", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editTotalSeasons,
                            onValueChange = {
                                editTotalSeasons = it.filter(Char::isDigit)
                                settingsDirty = true
                            },
                            label = { Text("Total Seasons") },
                            singleLine = true,
                            isError = seriesError != null && (totalSeasonsInput ?: 0) !in 1..100,
                            modifier = Modifier.weight(1f).testTag(TestTags.ProjectDetail.TOTAL_SEASONS)
                        )
                        OutlinedTextField(
                            value = editEpisodesPerSeason,
                            onValueChange = {
                                editEpisodesPerSeason = it.filter(Char::isDigit)
                                settingsDirty = true
                            },
                            label = { Text("Episodes / Season") },
                            singleLine = true,
                            isError = seriesError != null && (episodesPerSeasonInput ?: 0) !in 1..500,
                            modifier = Modifier.weight(1f).testTag(TestTags.ProjectDetail.EPISODES_PER_SEASON)
                        )
                    }
                    OutlinedTextField(
                        value = editEpisodeDuration,
                        onValueChange = {
                            editEpisodeDuration = it.filter(Char::isDigit)
                            settingsDirty = true
                        },
                        label = { Text("Episode Duration (minutes)") },
                        singleLine = true,
                        isError = seriesError != null && (durationInput ?: 0) !in 1..120,
                        modifier = Modifier.fillMaxWidth().testTag(TestTags.ProjectDetail.EPISODE_DURATION)
                    )
                    if (seriesError != null) {
                        Text(seriesError, color = StudioTokens.Error, fontSize = 12.sp)
                    } else if (seriesSummary != null) {
                        Text("Summary: $seriesSummary", fontSize = 12.sp, color = StudioTokens.Success)
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
                    enabled = !isSavingSettings && settingsDirty && seriesError == null,
                    modifier = Modifier.testTag(TestTags.ProjectDetail.SAVE_SETTINGS),
                    onClick = {
                        val pid = activePid
                        if (pid.isBlank() || seriesError != null) return@PrimaryButton
                        isSavingSettings = true
                        scope.launch {
                            val updates = mutableMapOf<String, Any?>(
                                "name" to editName.trim(),
                                "genre" to (if (selectedGenres.isNotEmpty()) selectedGenres.joinToString(", ") else editGenre.trim()),
                                "visual_medium" to editMedium.trim(),
                                "cover_image_url" to editCoverUrl.trim(),
                                "auto_approve" to editAutoApprove
                            )
                            editTargetEpisodeCount?.let { updates["target_episode_count"] = it }
                            seriesSummary?.let {
                                updates["total_seasons"] = totalSeasonsInput!!
                                updates["episodes_per_season"] = episodesPerSeasonInput!!
                                updates["episode_duration_min"] = durationInput!!
                            }
                            editScriptWordCount?.let { updates["script_word_count"] = it }
                            val res = repository.updateProjectSettings(
                                projectId = pid,
                                updates = updates
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

    pendingSwitchPid?.let { targetPid ->
        AlertDialog(
            onDismissRequest = {
                pendingSwitchPid = null
                tokenManager.saveActiveProjectId(activePid)
            },
            title = { Text("Unsaved project changes", fontWeight = FontWeight.Bold) },
            text = { Text("Finish or discard the current Project Settings edits before switching projects.") },
            confirmButton = {
                Button(onClick = {
                    settingsDirty = false
                    showSettingsDialog = false
                    pendingSwitchPid = null
                    tokenManager.saveActiveProjectId(targetPid)
                    activePid = targetPid
                }) { Text("Discard & Switch") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingSwitchPid = null
                    tokenManager.saveActiveProjectId(activePid)
                }) { Text("Stay Here") }
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
            selectedProjectIdOverride = activePid,
            onProjectChanged = { newPid ->
                if (settingsDirty) {
                    pendingSwitchPid = newPid
                } else if (newPid != activePid) {
                    activePid = newPid
                }
            },
            onRefreshClick = { if (activePid.isNotBlank()) loadProjectOverviewData(activePid) }
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = StudioTokens.Primary)
            }
        } else if (loadError != null) {
            StandardCard {
                Text("Error Loading Project", color = StudioTokens.Error, fontWeight = FontWeight.Bold)
                Text(loadError ?: "Unknown error", fontSize = 13.sp)
                SecondaryButton(text = "Retry", onClick = { loadProjectOverviewData(activePid) })
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

            StandardCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Series Plan", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            seriesSummary ?: "Review series configuration in Project Settings",
                            fontSize = 12.sp,
                            color = if (seriesError == null) StudioTokens.Success else StudioTokens.Warning
                        )
                    }
                    TextButton(onClick = {
                        selectedGenres = editGenre.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
                        settingsDirty = false
                        showSettingsDialog = true
                    }) { Text("Configure") }
                }
            }

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
            var expandedGroups by remember { mutableStateOf<Set<String>>(emptySet()) }
            val moduleGroups = listOf(
                "Story & Planning" to listOf(
                    "Story Bible" to onOpenBible,
                    "Episode Planner" to onOpenEpisodes,
                    "Characters" to onOpenCharacters,
                    "Character Deep Dive" to onOpenCharDeepDive,
                    "World Wiki" to onOpenLore
                ),
                "Visual Development" to listOf(
                    "Vision DNA" to onOpenVision,
                    "Media Library" to onOpenMediaLibrary
                ),
                "Scene & Production" to listOf(
                    "Scene Inspector" to onOpenScenes,
                    "Quick Generate" to onOpenQuickTools
                ),
                "Quality & Review" to listOf(
                    "Quality Control (QC)" to onOpenQC,
                    "Review Inbox" to onOpenReviewInbox
                ),
                "Publishing & Insights" to listOf(
                    "Publish Studio" to onOpenPublish,
                    "Analytics" to onOpenAnalytics,
                    "Quota Dashboard" to onOpenQuota,
                    "Budget Ledger" to onOpenBudget
                )
            )

            moduleGroups.forEach { (groupName, modules) ->
                val expanded = groupName in expandedGroups
                StandardCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedGroups = if (expanded) expandedGroups - groupName else expandedGroups + groupName
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(groupName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse $groupName" else "Expand $groupName"
                        )
                    }
                }
                if (expanded) {
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
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
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
                            when {
                                isCheckingReadiness -> "Checking production readiness..."
                                readinessReport == null -> "Readiness unavailable. Open Preflight to retry."
                                else -> "${pending.size} readiness check(s) pending: ${pending.take(3).joinToString(", ") { it.label }}"
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
