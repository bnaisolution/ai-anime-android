package org.dpdns.bnai.anime.ui.advanced

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
fun AdminQueuesScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var queuesState by remember {
        mutableStateOf<Resource<JsonMap>>(Resource.Loading)
    }

    fun load() {
        scope.launch {
            queuesState = Resource.Loading
            queuesState = repository.fetchAdminQueues(repository.currentProjectId())
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Outbox & Command Queues", style = MaterialTheme.typography.headlineLarge)
        ResourceStateBox(resource = queuesState, onRetry = { load() }) { data ->
            val outbox = data["outbox"] as? Map<*, *>
            val commands = data["commands"] as? Map<*, *>
            val pendingActions = data["pending_actions_count"] ?: 0

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Outbox Events Dispatcher",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            "Pending: ${outbox?.get("pending") ?: 0} • " +
                                "Processing: ${outbox?.get("processing") ?: 0}"
                        )
                        Text(
                            "Failed: ${outbox?.get("failed") ?: 0} • " +
                                "Dead Letter: ${outbox?.get("dead_letter") ?: 0}"
                        )
                    }
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "API Commands Queue",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            "Queued: ${commands?.get("queued") ?: 0} • " +
                                "Dispatched: ${commands?.get("dispatched") ?: 0}"
                        )
                        Text(
                            "Reconciling: ${commands?.get("reconciling") ?: 0} • " +
                                "Manual Review: ${commands?.get("manual_review") ?: 0}"
                        )
                    }
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Pending Actions Count: $pendingActions",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
            }
        }
    }
}
