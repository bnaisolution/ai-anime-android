package org.dpdns.bnai.anime.ui.owner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.formatInt
import org.dpdns.bnai.anime.ui.components.formatRelativeTime
import org.dpdns.bnai.anime.ui.designsystem.*

private data class CanonicalWfInfo(
    val code: String,
    val name: String,
    val responsibility: String
)

private val FROZEN_14_WORKFLOWS = listOf(
    CanonicalWfInfo("WF-00", "Main Router", "Central request ingestion, preflight gate & sub-workflow dispatch"),
    CanonicalWfInfo("WF-01", "Bible Architect", "Master Story Bible, world rules & character foundations"),
    CanonicalWfInfo("WF-02", "Story Generator", "Episode narrative outline & multi-scene screenplay generation"),
    CanonicalWfInfo("WF-03", "Production Pipeline", "Scene batching, image/audio/video synthesis & FFmpeg assembly"),
    CanonicalWfInfo("WF-04", "Admin Panel", "Operational inspection & administrative overrides"),
    CanonicalWfInfo("WF-05", "Vision Pipeline", "11-field Character Visual DNA extraction & R4 reference history"),
    CanonicalWfInfo("WF-06", "Continuity Engine", "Cross-episode plot, character status & timeline continuity checks"),
    CanonicalWfInfo("WF-07", "Publisher", "Idempotent multi-platform publishing & metadata packaging"),
    CanonicalWfInfo("WF-08", "Analytics", "Production throughput & published episode telemetry aggregation"),
    CanonicalWfInfo("WF-09", "Canon Engine", "Authoritative Canon validation, proposals & lock enforcement"),
    CanonicalWfInfo("WF-10", "State Machine Manager", "Episode & scene lifecycle state transitions"),
    CanonicalWfInfo("WF-11", "QC Gate", "Automated quality-control inspection & approval gating"),
    CanonicalWfInfo("WF-12", "Quota Governor", "Provider quota verification & project budget ceiling guard"),
    CanonicalWfInfo("WF-13", "Retry Manager", "Exponential backoff retry scheduling & dead-letter recovery")
)

@Composable
fun WorkflowManagerScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()

    var wfSummaryRes by remember { mutableStateOf<Resource<List<JsonMap>>>(Resource.Loading) }
    var lastCheckedIso by remember { mutableStateOf<String?>(null) }

    fun loadWorkflowSummary() {
        scope.launch {
            wfSummaryRes = Resource.Loading
            wfSummaryRes = repository.fetchAdminWorkflows()
            lastCheckedIso = java.time.Instant.now().toString()
        }
    }

    LaunchedEffect(Unit) {
        loadWorkflowSummary()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(
            title = "Workflow Manager (14 Frozen Workflows)",
            subtitle = "Separates Backend Command Activity from Live n8n Activation Status",
            trailingContent = {
                SecondaryButton(text = "Refresh", onClick = { loadWorkflowSummary() })
            }
        )

        StandardCard {
            Text(
                text = "Source Disclosure: Command totals below reflect backend api_commands activity. Live n8n activation state is shown only when verified directly from the n8n API.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Last checked: ${formatRelativeTime(lastCheckedIso)}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val backendList: List<JsonMap> = when (val res = wfSummaryRes) {
            is Resource.Success -> res.data
            else -> emptyList()
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(FROZEN_14_WORKFLOWS, key = { it.code }) { wf ->
                val matched = backendList.firstOrNull { row ->
                    val k = (row["wf_key"] ?: row["workflow_name"] ?: row["code"] ?: "").toString().uppercase()
                    k.contains(wf.code)
                }
                val totalCmds = formatInt(matched?.get("total_commands") ?: matched?.get("count") ?: 0)
                val failedCmds = formatInt(matched?.get("failed_commands") ?: matched?.get("failed") ?: 0)
                val verifiedLiveStatus = matched?.get("n8n_live_active")?.toString()
                val liveBadge = when (verifiedLiveStatus?.lowercase()) {
                    "true", "active" -> "N8N: ACTIVE"
                    "false", "inactive" -> "N8N: INACTIVE"
                    else -> "N8N STATE: UNKNOWN"
                }

                StandardCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${wf.code} — ${wf.name}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        StatusChip(label = liveBadge)
                    }
                    Text(
                        text = wf.responsibility,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Backend Command Activity: $totalCmds", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = "Recent Failures: $failedCmds",
                            fontSize = 12.sp,
                            color = if (failedCmds != "0") StudioTokens.Error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
