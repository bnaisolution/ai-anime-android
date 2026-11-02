package org.dpdns.bnai.anime.ui.owner

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
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun WorkflowManagerScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var wfState by remember {
        mutableStateOf<Resource<List<JsonMap>>>(Resource.Loading)
    }

    fun load() {
        scope.launch {
            wfState = Resource.Loading
            wfState = repository.fetchAdminWorkflows(repository.currentProjectId())
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Workflow Registry & Activity (WF-00–WF-13)",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            "Shows configured intent mappings and command execution counts recorded in Anime Supabase (does not claim live n8n toggle state without webhook execution).",
            style = MaterialTheme.typography.bodyLarge
        )
        ResourceStateBox(resource = wfState, onRetry = { load() }) { list ->
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list) { wf ->
                    val wfKey = (wf["wf_key"] ?: "WF").toString()
                    val intents = wf["intents"]?.toString() ?: "[]"
                    val totalCmds = (wf["total_commands"] as? Number)?.toInt() ?: 0
                    val activeCmds = (wf["active_commands"] as? Number)?.toInt() ?: 0
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(wfKey, style = MaterialTheme.typography.headlineMedium)
                                StatusBadge(
                                    if (activeCmds > 0) "RUNNING ($activeCmds)" else "CONFIGURED"
                                )
                            }
                            Text("Mapped Intents: $intents")
                            Text("Total Dispatched Commands: $totalCmds • Currently Active: $activeCmds")
                        }
                    }
                }
            }
        }
    }
}
