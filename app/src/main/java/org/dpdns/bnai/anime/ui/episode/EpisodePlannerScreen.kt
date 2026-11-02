package org.dpdns.bnai.anime.ui.episode

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.model.Episode
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun EpisodePlannerScreen(
    onStartProduction: (String) -> Unit = {},
    onOpenScenes: (Int) -> Unit = {}
) {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var episodesState by remember { mutableStateOf<Resource<List<Episode>>>(Resource.Loading) }
    var statusBanner by remember { mutableStateOf<String?>(null) }

    fun loadEpisodes() {
        scope.launch {
            episodesState = Resource.Loading
            episodesState = repository.fetchEpisodes(repository.currentProjectId())
        }
    }

    LaunchedEffect(Unit) { loadEpisodes() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Episode Planner (WF-02)", style = MaterialTheme.typography.headlineLarge)
            Button(onClick = {
                scope.launch {
                    val res = repository.submitStudioCommand(
                        projectId = repository.currentProjectId(),
                        intent = CommandIntent.GENERATE_EPISODE,
                        mode = "SAVE"
                    )
                    if (res is Resource.Success) {
                        val cmdId = res.data.command?.id ?: res.data.command_id ?: ""
                        statusBanner = "Episode generation queued ($cmdId)"
                        if (cmdId.length == 36) {
                            onStartProduction(cmdId)
                        } else {
                            loadEpisodes()
                        }
                    } else if (res is Resource.Error) {
                        statusBanner = "Error: ${res.message}"
                    }
                }
            }) {
                Text("Generate Episode")
            }
        }

        statusBanner?.let {
            Text(it, color = MaterialTheme.colorScheme.secondary)
        }

        ResourceStateBox(resource = episodesState, onRetry = { loadEpisodes() }) { list ->
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { ep ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { onOpenScenes(ep.episodeNumber) }
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Episode ${ep.episodeNumber}: ${ep.title}",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                StatusBadge(ep.status)
                            }
                            if (!ep.synopsis.isNullOrBlank()) {
                                Text(ep.synopsis, style = MaterialTheme.typography.bodyLarge)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onOpenScenes(ep.episodeNumber) }
                                ) { Text("Inspect Scenes") }

                                Button(onClick = {
                                    scope.launch {
                                        // GAP #1: use CONTINUE_EPISODE with stage=production
                                        // GAP #4.1: never fallback to ep.id
                                        val res = repository.submitStudioCommand(
                                            projectId = repository.currentProjectId(),
                                            intent = CommandIntent.CONTINUE_EPISODE,
                                            mode = "SAVE",
                                            episodeNo = ep.episodeNumber,
                                            payload = mapOf("stage" to "production")
                                        )
                                        when (res) {
                                            is Resource.Success -> {
                                                val cmdId = res.data.command?.id
                                                    ?: res.data.command_id ?: ""
                                                if (cmdId.length == 36) {
                                                    onStartProduction(cmdId)
                                                } else {
                                                    statusBanner = "Command queued but no valid UUID returned."
                                                }
                                            }
                                            is Resource.Error -> {
                                                statusBanner = "Failed to start production: ${res.message}"
                                            }
                                            else -> Unit
                                        }
                                    }
                                }) { Text("Run Production (WF-03)") }
                            }
                        }
                    }
                }
            }
        }
    }
}
