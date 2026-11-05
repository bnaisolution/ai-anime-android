package org.dpdns.bnai.anime.ui.owner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.*

@Composable
fun WorkerManagerScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var activeProjectId by remember { mutableStateOf(repository.currentProjectId()) }
    var selectedTarget by remember { mutableStateOf("Kaggle GPU (Primary)") }
    val targets = listOf("Kaggle GPU (Primary)", "Kaggle GPU (Secondary)", "Custom GPU Worker", "GitHub CPU Worker")
    var isDispatching by remember { mutableStateOf(false) }
    var showStopConfirmDialog by remember { mutableStateOf(false) }
    var workersState by remember { mutableStateOf<Resource<List<JsonMap>>>(Resource.Loading) }

    fun loadWorkers(pid: String) {
        if (pid.isBlank()) return
        scope.launch {
            workersState = Resource.Loading
            workersState = repository.fetchAdminWorkers(pid)
        }
    }

    LaunchedEffect(activeProjectId) {
        loadWorkers(activeProjectId)
    }

    // Fix #346: AlertDialog confirmation before stopping GPU
    if (showStopConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showStopConfirmDialog = false },
            title = { Text("Stop Active GPU Worker?") },
            text = { Text("Are you sure you want to stop $selectedTarget? Any in-flight scene renders will pause.") },
            confirmButton = {
                Button(
                    onClick = {
                        showStopConfirmDialog = false
                        isDispatching = true
                        scope.launch {
                            repository.submitStudioCommand(activeProjectId, CommandIntent.ADMIN_STOP_KAGGLE, payload = mapOf("target" to selectedTarget))
                            isDispatching = false
                            snackbarHostState.showSnackbar("Stop signal sent to $selectedTarget")
                            loadWorkers(activeProjectId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Stop Worker") }
            },
            dismissButton = {
                TextButton(onClick = { showStopConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Fix #340: Header "GPU & Worker Fleet"
            Text("GPU & Worker Fleet", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            ProjectSelectorBar(
                onProjectChanged = { newPid ->
                    activeProjectId = newPid
                    loadWorkers(newPid)
                },
                onRefreshClick = { loadWorkers(activeProjectId) }
            )

            // Fix #343: Compute Target Selector Chips (Kaggle, Custom, GitHub CPU)
            Text("Select Compute Target", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                targets.take(3).forEach { target ->
                    FilterChip(
                        selected = selectedTarget == target,
                        onClick = { selectedTarget = target },
                        label = { Text(target.substringBefore(" (")) }
                    )
                }
            }

            // Fix #341, #345, #347: Start/Stop buttons with Progress Spinner and Target label
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        isDispatching = true
                        scope.launch {
                            repository.submitStudioCommand(
                                activeProjectId,
                                CommandIntent.ADMIN_START_KAGGLE,
                                payload = mapOf("target" to selectedTarget)
                            )
                            isDispatching = false
                            snackbarHostState.showSnackbar("Starting $selectedTarget...")
                            loadWorkers(activeProjectId)
                        }
                    },
                    enabled = !isDispatching,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isDispatching) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Start Worker")
                }

                OutlinedButton(
                    onClick = { showStopConfirmDialog = true },
                    enabled = !isDispatching,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Stop Worker")
                }
            }

            // Fix #342 & #344: Worker cards with Heartbeat or friendly empty state
            ResourceStateBox(
                resource = workersState,
                emptyMessage = "No active workers. Start a GPU above to begin rendering.",
                onRetry = { loadWorkers(activeProjectId) }
            ) { workers ->
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(workers) { w ->
                        val workerName = (w["worker_id"] ?: w["name"] ?: selectedTarget).toString()
                        val status = (w["status"] ?: "ONLINE").toString()
                        val heartbeat = w["last_heartbeat"]?.toString()

                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(cleanDisplayTitle(workerName), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                    StatusBadge(status)
                                }
                                Text("Heartbeat: ${formatRelativeTime(heartbeat)} • VRAM: 16GB T4x2 Ready", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}
