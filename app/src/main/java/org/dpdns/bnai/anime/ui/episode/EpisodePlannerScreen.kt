package org.dpdns.bnai.anime.ui.episode
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.model.Episode
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodePlannerScreen(onStartProduction: (String) -> Unit = {}) {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var showConfirmDialog by remember { mutableStateOf(false) }
    val episodes = remember { mutableStateListOf<Episode>() }
    LaunchedEffect(Unit) { when (val res = repository.fetchEpisodes(repository.currentProjectId())) { is org.dpdns.bnai.anime.core.network.Resource.Success -> { if (res.data.isNotEmpty()) { episodes.clear(); episodes.addAll(res.data) } } else -> Unit } }
    if (showConfirmDialog) {
        AlertDialog(onDismissRequest = { showConfirmDialog = false }, title = { Text("Confirm") }, text = { Text("Start Production?") }, confirmButton = { Button(onClick = { showConfirmDialog = false; scope.launch { repository.submitStudioCommand(repository.currentProjectId(), CommandIntent.GENERATE_EPISODE, "SAVE"); onStartProduction("ep_005") } }) { Text("Yes") } }, dismissButton = { TextButton(onClick = { showConfirmDialog = false }) { Text("No") } })
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Episode Planner") }) }) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp)) {
            LazyColumn(modifier = Modifier.weight(1f)) { items(episodes, key = { it.id }) { ep -> Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("Ep ${ep.episodeNumber}: ${ep.title}"); Text(ep.status) } } } }
            Button(onClick = { showConfirmDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Generate Episode") }
        }
    }
}
