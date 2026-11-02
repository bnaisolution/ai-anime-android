package org.dpdns.bnai.anime.ui.advanced

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.JobDto
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.KeyValueMapList
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun JobsMonitorScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    var jobsState by remember {
        mutableStateOf<Resource<List<JobDto>>>(Resource.Loading)
    }
    var retriesState by remember {
        mutableStateOf<Resource<List<JsonMap>>>(Resource.Loading)
    }

    fun loadAll() {
        val pid = repository.currentProjectId()
        scope.launch {
            jobsState = Resource.Loading
            jobsState = repository.fetchJobs(pid)
        }
        scope.launch {
            retriesState = Resource.Loading
            retriesState = repository.fetchRetries(pid)
        }
    }

    LaunchedEffect(Unit) { loadAll() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Jobs & Retry Monitor (WF-10 + WF-13)",
            style = MaterialTheme.typography.headlineLarge
        )
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("State Machine (WF-10)") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Retry Manager (WF-13)") }
            )
        }

        if (selectedTab == 0) {
            ResourceStateBox(resource = jobsState, onRetry = { loadAll() }) { jobs ->
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(jobs, key = { it.id }) { job ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        job.intent ?: job.id,
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                    StatusBadge(job.status ?: "UNKNOWN")
                                }
                                Text("Job ID: ${job.id} • Ep: ${job.episode_no ?: 1}")
                                Text(
                                    "Created: ${job.created_at ?: "N/A"} • " +
                                        "Pending Actions: ${job.pending_actions?.size ?: 0}"
                                )
                                job.error_message?.let {
                                    Text("Error: $it", color = MaterialTheme.colorScheme.error)
                                }
                                if (job.status?.uppercase() in listOf("FAILED", "MANUAL_REVIEW")) {
                                    OutlinedButton(onClick = {
                                        scope.launch {
                                            repository.submitStudioCommand(
                                                repository.currentProjectId(),
                                                CommandIntent.RETRY_FAILED,
                                                "QUICK",
                                                payload = mapOf("job_id" to job.id)
                                            )
                                            loadAll()
                                        }
                                    }) { Text("Dispatch WF-13 Retry") }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            ResourceStateBox(
                resource = retriesState,
                emptyMessage = "No WF-13 retry records returned by backend for this project.",
                onRetry = { loadAll() }
            ) { retries ->
                KeyValueMapList(retries)
            }
        }
    }
}
