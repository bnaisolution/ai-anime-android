package org.dpdns.bnai.anime.ui.production

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
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.core.network.SceneDto
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun SceneDetailScreen(initialEpisodeNo: Int = 1) {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var episodeNo by remember { mutableStateOf(initialEpisodeNo) }
    var scenesState by remember { mutableStateOf<Resource<List<SceneDto>>>(Resource.Loading) }

    fun load() {
        scope.launch {
            scenesState = Resource.Loading
            scenesState = repository.fetchScenes(repository.currentProjectId(), episodeNo)
        }
    }

    LaunchedEffect(episodeNo) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Scene Inspector (Ep $episodeNo)",
                style = MaterialTheme.typography.headlineLarge
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { if (episodeNo > 1) episodeNo-- }) {
                    Text("- Ep")
                }
                OutlinedButton(onClick = { episodeNo++ }) {
                    Text("+ Ep")
                }
            }
        }

        ResourceStateBox(resource = scenesState, onRetry = { load() }) { scenes ->
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(scenes, key = { it.id }) { sc ->
                    val resolvedSceneNo = sc.scene_no ?: sc.scene_order ?: 0
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // GAP #5.3: no phantom "title" field on scenes table
                                Text(
                                    "Scene #$resolvedSceneNo",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                sc.status?.let { StatusBadge(it) }
                            }
                            Text(sc.description ?: "No description")
                            sc.clips?.forEach { clip ->
                                val clipSceneNo = clip.scene_no
                                    ?: clip.scene_order
                                    ?: resolvedSceneNo
                                Text(
                                    "Clip (Scene #$clipSceneNo): ${clip.clip_url ?: "Pending render"} " +
                                        "• Provider: ${clip.provider ?: "N/A"} " +
                                        "• ${clip.duration_sec ?: 0.0}s",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
