package org.dpdns.bnai.anime.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ResourceStateBox

@Composable
fun AnalyticsScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var analyticsState by remember {
        mutableStateOf<Resource<JsonMap>>(Resource.Loading)
    }

    fun load() {
        scope.launch {
            analyticsState = Resource.Loading
            analyticsState = repository.fetchAnalytics(repository.currentProjectId())
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Project Analytics (WF-08)", style = MaterialTheme.typography.headlineLarge)
        ResourceStateBox(resource = analyticsState, onRetry = { load() }) { data ->
            val episodes = data["episodes"] as? Map<*, *>
            val jobs = data["jobs"] as? Map<*, *>
            val ledger = data["ledger"] as? Map<*, *>

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Episodes Breakdown",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            "Total: ${episodes?.get("total") ?: 0} • " +
                                "Published: ${episodes?.get("published") ?: 0} • " +
                                "QC Passed: ${episodes?.get("qc_passed") ?: 0}"
                        )
                    }
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Commands & Pipeline Jobs",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            "Total: ${jobs?.get("total") ?: 0} • " +
                                "Succeeded: ${jobs?.get("succeeded") ?: 0} • " +
                                "Failed: ${jobs?.get("failed") ?: 0} • " +
                                "Manual Review: ${jobs?.get("manual_review") ?: 0}"
                        )
                    }
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Ledger Cost Summary",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            "Reserved: ${ledger?.get("reserved_cost") ?: 0} • " +
                                "Consumed: ${ledger?.get("consumed_cost") ?: 0} • " +
                                "Total Committed: ${ledger?.get("total_committed_cost") ?: 0}"
                        )
                    }
                }
            }
        }
    }
}
