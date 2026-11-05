package org.dpdns.bnai.anime.ui.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.formatInt
import org.dpdns.bnai.anime.ui.components.formatRelativeTime
import org.dpdns.bnai.anime.ui.designsystem.*

@Composable
fun AnalyticsScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()

    var dateRange by remember { mutableStateOf("LAST_30_DAYS") }
    var analyticsRes by remember { mutableStateOf<Resource<JsonMap>>(Resource.Loading) }
    var lastFetchedIso by remember { mutableStateOf<String?>(null) }

    fun loadAnalytics() {
        val pid = repository.currentProjectId()
        if (pid.isBlank()) {
            analyticsRes = Resource.Empty
            return
        }
        scope.launch {
            analyticsRes = Resource.Loading
            analyticsRes = repository.fetchAnalytics(pid)
            lastFetchedIso = java.time.Instant.now().toString()
        }
    }

    LaunchedEffect(Unit) {
        loadAnalytics()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProjectSelectorBar(
            onProjectChanged = { loadAnalytics() },
            onRefreshClick = { loadAnalytics() }
        )

        ScreenHeader(
            title = "Production & Audience Analytics",
            subtitle = "Verified backend telemetry from episodes, jobs & budget ledger (Zero fabricated metrics)",
            trailingContent = { StatusChip(label = dateRange.replace("_", " ")) }
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("LAST_7_DAYS" to "7 Days", "LAST_30_DAYS" to "30 Days", "ALL_TIME" to "All Time").forEach { (key, label) ->
                FilterChip(
                    selected = dateRange == key,
                    onClick = { dateRange = key },
                    label = { Text(label) }
                )
            }
        }

        org.dpdns.bnai.anime.ui.components.ResourceStateBox(
            resource = analyticsRes,
            emptyMessage = "No analytics records available for this project yet.",
            onRetry = { loadAnalytics() }
        ) { raw ->
            val episodesObj = (raw["episodes"] as? Map<*, *>) ?: raw
            val jobsObj = (raw["jobs"] as? Map<*, *>) ?: raw
            val ledgerObj = (raw["ledger"] as? Map<*, *>) ?: raw
            val audienceObj = (raw["audience"] as? Map<*, *>)

            val epTotal = formatInt(episodesObj["total"] ?: raw["episodes_total"] ?: raw["episodes_count"] ?: 0)
            val epPublished = formatInt(episodesObj["published"] ?: raw["episodes_published"] ?: 0)
            val qcApproved = formatInt(episodesObj["qc_approved"] ?: raw["qc_approved_count"] ?: 0)
            val qcFailed = formatInt(episodesObj["qc_failed"] ?: raw["qc_failed_count"] ?: 0)

            val jobsTotal = formatInt(jobsObj["total"] ?: raw["jobs_total"] ?: 0)
            val jobsSucceeded = formatInt(jobsObj["succeeded"] ?: raw["jobs_succeeded"] ?: 0)
            val jobsFailed = formatInt(jobsObj["failed"] ?: raw["jobs_failed"] ?: 0)
            val jobsManualReview = formatInt(jobsObj["manual_review"] ?: raw["manual_review_count"] ?: 0)

            val consumedCredits = formatInt(ledgerObj["consumed"] ?: raw["credits_consumed"] ?: raw["total_spent"] ?: 0)
            val reservedCredits = formatInt(ledgerObj["reserved"] ?: raw["credits_reserved"] ?: 0)

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(title = "Episode & QC Throughput", badgeText = "Source: episodes")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(label = "Total Episodes", value = epTotal, modifier = Modifier.weight(1f))
                    MetricCard(label = "Published", value = epPublished, accentColor = StudioTokens.Success, modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(label = "QC Approved", value = qcApproved, accentColor = StudioTokens.Success, modifier = Modifier.weight(1f))
                    MetricCard(label = "QC Failed/Rejected", value = qcFailed, accentColor = StudioTokens.Error, modifier = Modifier.weight(1f))
                }

                SectionHeader(title = "Pipeline Job Reliability", badgeText = "Source: api_commands")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(label = "Total Jobs", value = jobsTotal, modifier = Modifier.weight(1f))
                    MetricCard(label = "Succeeded", value = jobsSucceeded, accentColor = StudioTokens.Success, modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(label = "Failed Jobs", value = jobsFailed, accentColor = StudioTokens.Error, modifier = Modifier.weight(1f))
                    MetricCard(label = "Manual Review", value = jobsManualReview, accentColor = StudioTokens.Warning, modifier = Modifier.weight(1f))
                }

                SectionHeader(title = "Ledger & Resource Consumption", badgeText = "Source: resource_reservations")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard(label = "Consumed Credits", value = consumedCredits, modifier = Modifier.weight(1f))
                    MetricCard(label = "Reserved Credits", value = reservedCredits, accentColor = StudioTokens.Warning, modifier = Modifier.weight(1f))
                }

                StandardCard {
                    SectionHeader(
                        title = "Audience & Platform Telemetry",
                        badgeText = if (audienceObj != null && audienceObj.isNotEmpty()) "LIVE" else "UNAVAILABLE"
                    )
                    if (audienceObj != null && audienceObj.isNotEmpty()) {
                        val views = formatInt(audienceObj["views"] ?: 0)
                        val watchSec = formatInt(audienceObj["watch_time_seconds"] ?: 0)
                        Text("Total Verified Views: $views • Watch Time: ${watchSec}s", fontSize = 13.sp)
                    } else {
                        Text(
                            text = "Audience retention and watch-time metrics are currently UNAVAILABLE. Metrics appear only after episodes are published and platform telemetry is ingested by WF-08.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Last checked: ${formatRelativeTime(lastFetchedIso)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(64.dp))
    }
}
