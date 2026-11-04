package org.dpdns.bnai.anime.ui.owner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminQueuesScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()

    var activeProjectId by remember { mutableStateOf(repository.currentProjectId()) }
    var refreshIntervalSec by remember { mutableIntStateOf(0) } // 0 = Off, 5, 10, 30
    var queuesState by remember { mutableStateOf<Resource<JsonMap>>(Resource.Loading) }
    var selectedQueueTitle by remember { mutableStateOf<String?>(null) }

    fun loadQueues(pid: String) {
        if (pid.isBlank()) return
        scope.launch {
            queuesState = repository.fetchAdminQueues(pid)
        }
    }

    LaunchedEffect(activeProjectId) {
        loadQueues(activeProjectId)
    }

    // Fix #354: Auto-refresh loop (5s, 10s, 30s)
    LaunchedEffect(refreshIntervalSec, activeProjectId) {
        if (refreshIntervalSec > 0 && activeProjectId.isNotBlank()) {
            while (true) {
                delay(refreshIntervalSec * 1000L)
                loadQueues(activeProjectId)
            }
        }
    }

    // Fix #352 & #353: Clickable Queue Card opens Bottom Sheet with details
    selectedQueueTitle?.let { qTitle ->
        ModalBottomSheet(onDismissRequest = { selectedQueueTitle = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("$qTitle — Queue Inspector", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Outbox dispatcher processes queued events every 5 seconds with idempotency locks and exponential backoff.")
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Fix #355: Header + Info Tooltip
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Pipeline Queues", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            InfoTooltipButton(
                "Pipeline Queues",
                "Outbox Events hold commands waiting for n8n dispatch. Pending Actions track scene renders. Dead Letter holds jobs that exceeded max retries."
            )
        }

        ProjectSelectorBar(
            onProjectChanged = { newPid ->
                activeProjectId = newPid
                loadQueues(newPid)
            },
            onRefreshClick = { loadQueues(activeProjectId) }
        )

        // Fix #354: Auto-Refresh Toggle Chips
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Auto-Refresh:", style = MaterialTheme.typography.labelLarge)
            listOf(0 to "Off", 5 to "5s", 10 to "10s", 30 to "30s").forEach { (sec, label) ->
                FilterChip(
                    selected = refreshIntervalSec == sec,
                    onClick = { refreshIntervalSec = sec },
                    label = { Text(label) }
                )
            }
        }

        ResourceStateBox(
            resource = queuesState,
            emptyMessage = "All queues are clear (0 pending items).",
            onRetry = { loadQueues(activeProjectId) }
        ) { data ->
            val outboxCount = (data["outbox_pending"] as? Number)?.toInt() ?: (data["pending_count"] as? Number)?.toInt() ?: 0
            val activeCmds = (data["commands_running"] as? Number)?.toInt() ?: (data["running_count"] as? Number)?.toInt() ?: 0
            val failedCount = (data["dead_letter_count"] as? Number)?.toInt() ?: (data["failed_count"] as? Number)?.toInt() ?: 0

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Fix #349, #350, #351, #352: Integer formatting, Progress Bars, Red color for Failed/Dead Letter, Clickable
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { selectedQueueTitle = "Transactional Outbox Queue" },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Transactional Outbox Queue", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("${formatInt(outboxCount)} Queued Events", style = MaterialTheme.typography.headlineLarge)
                        LinearProgressIndicator(progress = { (outboxCount / 20f).coerceIn(0.05f, 1f) }, modifier = Modifier.fillMaxWidth().height(6.dp))
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { selectedQueueTitle = "Running Pipeline Commands" },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Active Pipeline Commands", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("${formatInt(activeCmds)} Running", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                        LinearProgressIndicator(progress = { (activeCmds / 10f).coerceIn(0.05f, 1f) }, modifier = Modifier.fillMaxWidth().height(6.dp))
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { selectedQueueTitle = "Failed & Dead-Letter Queue" },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Failed / Dead-Letter Queue", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${formatInt(failedCount)} Failed Items",
                            style = MaterialTheme.typography.headlineLarge,
                            color = if (failedCount > 0) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                        )
                        LinearProgressIndicator(
                            progress = { (failedCount / 10f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }
        }
    }
}
