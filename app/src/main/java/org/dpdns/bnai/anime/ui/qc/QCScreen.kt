package org.dpdns.bnai.anime.ui.qc

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
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
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()

    var episodes by remember { mutableStateOf<List<EpisodeDto>>(emptyList()) }
    var selectedEpNo by remember { mutableStateOf(1) }
    var epDropdownExpanded by remember { mutableStateOf(false) }
    var isUpdating by remember { mutableStateOf(false) }

    fun loadEpisodes() {
        val pid = repository.currentProjectId()
        if (pid.isBlank()) return
        scope.launch {
            val res = repository.fetchEpisodesRaw(pid)
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
    val isApproved = rawQcStatus == "APPROVED" || rawQcStatus == "PASSED"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProjectSelectorBar(
            onProjectChanged = { loadEpisodes() },
            onRefreshClick = { loadEpisodes() }
        )

        ScreenHeader(
            title = "Quality Control (QC Gate)",
            subtitle = "Inspect script, scene & media QC before unlocking publishing",
            trailingContent = { StatusChip(label = rawQcStatus) }
        )

        ExposedDropdownMenuBox(
            expanded = epDropdownExpanded,
            onExpandedChange = { epDropdownExpanded = !epDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
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

        StandardCard {
            SectionHeader(title = "Episode $selectedEpNo QC Summary", badgeText = rawQcStatus)
            Text(
                text = when (rawQcStatus) {
                    "APPROVED", "PASSED" -> "All mandatory QC gates passed. Publishing is unlocked."
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
                                episodeNo = selectedEpNo
                            )
                            if (res is Resource.Success) {
                                val cid = res.data.command_id.orEmpty()
                                Toast.makeText(context, "QC job queued (#${cid.take(6)})", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                PrimaryButton(
                    text = "Approve QC Gate",
                    isLoading = isUpdating,
                    onClick = {
                        val pid = repository.currentProjectId()
                        isUpdating = true
                        scope.launch {
                            val res = repository.updateEpisodeSync(
                                projectId = pid,
                                episodeNo = selectedEpNo,
                                updates = mapOf("qc_status" to "APPROVED")
                            )
                            isUpdating = false
                            if (res is Resource.Success) {
                                loadEpisodes()
                                Toast.makeText(context, "Episode $selectedEpNo QC marked APPROVED", Toast.LENGTH_SHORT).show()
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
