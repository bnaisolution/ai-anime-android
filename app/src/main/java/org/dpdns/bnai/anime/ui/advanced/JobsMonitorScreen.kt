package org.dpdns.bnai.anime.ui.advanced
import androidx.compose.ui.platform.testTag
import org.dpdns.bnai.anime.ui.testing.TestTags

import org.dpdns.bnai.anime.ui.LocalAppContainer

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.*
import org.dpdns.bnai.anime.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsMonitorScreen() {
    val context = LocalContext.current
    val repository = LocalAppContainer.current.repository
    val scope = rememberCoroutineScope()

    var activeProjectId by remember { mutableStateOf(repository.currentProjectId()) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Active Jobs, 1: Retries
    var jobsState by remember { mutableStateOf<Resource<List<JobDto>>>(Resource.Loading) }
    var retriesState by remember { mutableStateOf<Resource<List<JsonMap>>>(Resource.Loading) }
    var selectedJobDetail by remember { mutableStateOf<JobDto?>(null) }

    fun loadAll(pid: String) {
        if (pid.isBlank()) return
        scope.launch {
            jobsState = Resource.Loading
            retriesState = Resource.Loading
            jobsState = repository.fetchJobs(pid)
            retriesState = repository.fetchRetries(pid)
        }
    }

    LaunchedEffect(activeProjectId) {
        loadAll(activeProjectId)
    }

    // Fix #139 & #142: Job Detail ModalBottomSheet populated via fetchJobDetail
    selectedJobDetail?.let { job ->
        ModalBottomSheet(onDismissRequest = { selectedJobDetail = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Pipeline Job Inspector", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Action: ${job.intent ?: "STUDIO_JOB"}", style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(friendlyPipelineStatus(job.status))
                    Text("Attempts: ${formatInt(job.attempt_count ?: 1)} / ${formatInt(job.max_attempts ?: 3)}")
                }
                job.episode_state?.let { st ->
                    Text("Pipeline Stage: ${st["current_state"] ?: st["stage"] ?: "Processing"}", fontWeight = FontWeight.SemiBold)
                }
                if (!job.error_message.isNullOrBlank()) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(
                            text = "Error: ${job.error_message}",
                            modifier = Modifier.padding(10.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(TestTags.Screen.JOBS_MONITOR)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Fix #132 & #140: Header "Pipeline Jobs" + Refresh
        Text("Pipeline Jobs", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        ProjectSelectorBar(
            onProjectChanged = { newPid ->
                activeProjectId = newPid
                loadAll(newPid)
            },
            onRefreshClick = { loadAll(activeProjectId) }
        )

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Active Jobs") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Retries") })
        }

        if (selectedTab == 0) {
            ResourceStateBox(
                resource = jobsState,
                emptyMessage = "No active pipeline jobs right now. Everything running smoothly!",
                onRetry = { loadAll(activeProjectId) }
            ) { jobs ->
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(jobs, key = { it.id }) { job ->
                        val pendingCount = job.pending_actions?.size ?: 0
                        val isProjectLevel = job.intent in listOf("CREATE_PROJECT", "SAVE_BIBLE", "CHECK_CANON", "CHECK_CONTINUITY")
                        val stageName = job.episode_state?.get("current_state")?.toString()
                            ?: job.episode_state?.get("stage")?.toString()

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        val detail = repository.fetchJobDetail(activeProjectId, job.id)
                                        selectedJobDetail = (detail as? Resource.Success)?.data ?: job
                                    }
                                },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Fix #133: Hide raw UUIDs, show clean intent title
                                    Text(
                                        text = (job.intent ?: "Pipeline Task").replace("_", " "),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    // Fix #141: Standard color badges
                                    StatusBadge(friendlyPipelineStatus(job.status))
                                }

                                // Fix #135 & #143: Hide Ep: 1 for project-level commands; show pipeline stage
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (!isProjectLevel && job.episode_no != null && job.episode_no > 0) {
                                        Text("Episode ${job.episode_no}", style = MaterialTheme.typography.bodyLarge)
                                    }
                                    if (!stageName.isNullOrBlank()) {
                                        Text("Stage: $stageName", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Text(formatRelativeTime(job.created_at), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                                }

                                // Fix #146: Supervisor auto-healing indicator for RECONCILING jobs
                                if (job.status.equals("RECONCILING", ignoreCase = true)) {
                                    Text(
                                        "⚡ Supervisor is auto-healing & syncing worker outputs...",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Fix #136: Only show Pending Actions chip if > 0
                                if (pendingCount > 0) {
                                    AssistChip(
                                        onClick = {},
                                        leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        label = { Text("$pendingCount Pending Actions") }
                                    )
                                }

                                // Fix #137 & GAP #1: Force Retry using CommandIntent.RETRY_FAILED
                                if (job.status in listOf("FAILED", "RECONCILING", "MANUAL_REVIEW")) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                scope.launch {
                                                    val res = repository.submitStudioCommand(
                                                        projectId = activeProjectId,
                                                        intent = CommandIntent.RETRY_FAILED,
                                                        episodeNo = job.episode_no,
                                                        payload = mapOf("job_id" to job.id)
                                                    )
                                                    val msg = if (res is Resource.Success) "Retry dispatched!" else "Retry failed"
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    loadAll(activeProjectId)
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Force Retry")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Fix #138 & #144: Retries Tab with friendly empty state and retry schedule
            ResourceStateBox(
                resource = retriesState,
                emptyMessage = "No failed jobs. Everything running smoothly!",
                onRetry = { loadAll(activeProjectId) }
            ) { retries ->
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(retries) { item ->
                        val actionName = (item["action_type"] ?: item["intent"] ?: "Scene Render Task").toString()
                        val status = (item["status"] ?: "RETRY_SCHEDULED").toString()
                        val attempts = formatInt(item["attempt_count"] ?: 1)
                        val maxAttempts = formatInt(item["max_attempts"] ?: 3)
                        val readyAt = item["ready_at"]?.toString()

                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(cleanDisplayTitle(actionName), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                    StatusBadge(status)
                                }
                                Text("Next retry scheduled (Attempt $attempts/$maxAttempts) • ${formatRelativeTime(readyAt)}", style = MaterialTheme.typography.bodyLarge)
                                item["last_error"]?.toString()?.takeIf { it.isNotBlank() }?.let { err ->
                                    Text("Reason: $err", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
                                }
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            repository.submitStudioCommand(
                                                projectId = activeProjectId,
                                                intent = CommandIntent.RETRY_FAILED
                                            )
                                            Toast.makeText(context, "Manual retry triggered!", Toast.LENGTH_SHORT).show()
                                            loadAll(activeProjectId)
                                        }
                                    }
                                ) {
                                    Text("Trigger Immediate Retry")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
