package org.dpdns.bnai.anime.ui.qc
import androidx.compose.ui.platform.testTag
import org.dpdns.bnai.anime.ui.testing.TestTags

import org.dpdns.bnai.anime.ui.LocalAppContainer

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.EpisodeDto
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.designsystem.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QCScreen(
    onProceedToPublish: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = LocalAppContainer.current.repository
    val scope = rememberCoroutineScope()

    var episodes by remember { mutableStateOf<List<EpisodeDto>>(emptyList()) }
    var selectedSeason by remember { mutableStateOf(1) }
    var totalSeasons by remember { mutableStateOf(5) }
    var seasonInitialized by remember { mutableStateOf(false) }
    var selectedEpNo by remember { mutableStateOf(1) }
    var epDropdownExpanded by remember { mutableStateOf(false) }
    var seasonDropdownExpanded by remember { mutableStateOf(false) }
    var isUpdating by remember { mutableStateOf(false) }

    fun loadEpisodes() {
        val pid = repository.currentProjectId()
        if (pid.isBlank()) return
        scope.launch {
            if (!seasonInitialized) {
                when (val overview = repository.fetchProjectOverviewRaw(pid)) {
                    is Resource.Success -> {
                        val projObj = (overview.data["project"] as? Map<*, *>) ?: overview.data
                        val bibleObj = (projObj["story_bible"] as? Map<*, *>) ?: emptyMap<String, Any?>()
                        totalSeasons = ((bibleObj["total_seasons"] as? Number)?.toInt() ?: 5).coerceAtLeast(1)
                        selectedSeason = ((projObj["current_season"] as? Number)?.toInt() ?: 1).coerceIn(1, totalSeasons)
                    }
                    else -> Unit
                }
                seasonInitialized = true
            }
            val res = repository.fetchEpisodesRaw(pid, selectedSeason)
            if (res is Resource.Success) {
                episodes = res.data
                if (res.data.isNotEmpty() && res.data.none { it.episode_no == selectedEpNo }) {
                    selectedEpNo = res.data.first().episode_no
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        loadEpisodes()
    }

    val activeEp = episodes.find { it.episode_no == selectedEpNo }
    val rawQcStatus = (activeEp?.qc_status ?: "UNCHECKED").uppercase()
    val rawApprovalStatus = (activeEp?.approval_status ?: "PENDING").uppercase()
    val isApproved = isQcPublishUnlocked(rawQcStatus, rawApprovalStatus)
    val canApprove = canApproveQc(rawQcStatus, rawApprovalStatus)
    val targetKey = "S${selectedSeason}E${selectedEpNo}"

    Column(
        modifier = Modifier.testTag(TestTags.Screen.QC)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProjectSelectorBar(
            onProjectChanged = {
                seasonInitialized = false
                loadEpisodes()
            },
            onRefreshClick = { loadEpisodes() }
        )

        ScreenHeader(
            title = "Quality Control (QC Gate)",
            subtitle = "Inspect script, scene & media QC before unlocking publishing",
            trailingContent = { StatusChip(label = rawQcStatus) }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExposedDropdownMenuBox(
                expanded = seasonDropdownExpanded,
                onExpandedChange = { seasonDropdownExpanded = !seasonDropdownExpanded },
                modifier = Modifier.weight(0.45f)
            ) {
                OutlinedTextField(
                    value = "Season $selectedSeason",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Season") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = seasonDropdownExpanded) },
                    singleLine = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = seasonDropdownExpanded,
                    onDismissRequest = { seasonDropdownExpanded = false }
                ) {
                    (1..totalSeasons).forEach { season ->
                        DropdownMenuItem(
                            text = { Text("Season $season") },
                            onClick = {
                                selectedSeason = season
                                selectedEpNo = 1
                                seasonDropdownExpanded = false
                                loadEpisodes()
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = epDropdownExpanded,
                onExpandedChange = { epDropdownExpanded = !epDropdownExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = "Episode $selectedEpNo: ${activeEp?.title ?: "Untitled"}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Episode for QC") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = epDropdownExpanded) },
                    singleLine = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = epDropdownExpanded,
                    onDismissRequest = { epDropdownExpanded = false }
                ) {
                    val epNums = (episodes.map { it.episode_no } + listOf(1, 2, 3)).distinct().sorted()
                    epNums.forEach { num ->
                        DropdownMenuItem(
                            text = { Text("Episode $num") },
                            onClick = {
                                selectedEpNo = num
                                epDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        StandardCard {
            SectionHeader(title = "$targetKey QC Summary", badgeText = if (isApproved) "APPROVED" else rawQcStatus)
            Text(
                text = when (rawQcStatus) {
                    "APPROVED", "PASS", "PASSED", "QC_PASSED", "PENDING_HUMAN_APPROVAL" ->
                        if (isApproved) "All mandatory QC gates passed and human approval is recorded. Publishing is unlocked."
                        else "Automated QC passed. Human approval is required before publishing."
                    "SKIPPED" -> "QC was marked SKIPPED. Skipped checks are never treated as passed."
                    "FAILED", "REJECTED" -> "QC reported critical issues. Fix scenes or re-run QC."
                    else -> "QC has not been verified for this episode yet."
                },
                fontSize = 13.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecondaryButton(
                    text = "Re-Run Automated QC",
                    enabled = !isUpdating,
                    onClick = {
                        val pid = repository.currentProjectId()
                        scope.launch {
                            val res = repository.submitStudioCommand(
                                projectId = pid,
                                intent = CommandIntent.RUN_QC,
                                episodeNo = selectedEpNo,
                                seasonNo = selectedSeason
                            )
                            if (res is Resource.Success) {
                                val cid = res.data.command_id.orEmpty()
                                Toast.makeText(context, "QC job queued for $targetKey (#${cid.take(6)})", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                PrimaryButton(
                    text = "Approve QC Gate",
                    isLoading = isUpdating,
                    enabled = canApprove && !isUpdating,
                    onClick = {
                        val pid = repository.currentProjectId()
                        if (!canApprove) {
                            Toast.makeText(context, "Automated QC must pass before approval.", Toast.LENGTH_SHORT).show()
                        } else {
                            isUpdating = true
                            scope.launch {
                                val res = repository.submitEpisodeApproval(
                                    projectId = pid,
                                    episodeNo = selectedEpNo,
                                    seasonNo = selectedSeason,
                                    decision = "APPROVED"
                                )
                                isUpdating = false
                                if (res is Resource.Success) {
                                    loadEpisodes()
                                    Toast.makeText(context, "$targetKey QC approved", Toast.LENGTH_SHORT).show()
                                } else if (res is Resource.Error) {
                                    Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        PrimaryButton(
            text = if (isApproved) "Proceed to Publish Studio" else "Publish Locked (Requires QC APPROVED)",
            enabled = isApproved,
            onClick = onProceedToPublish,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
