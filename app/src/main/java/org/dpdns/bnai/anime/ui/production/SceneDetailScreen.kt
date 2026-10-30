package org.dpdns.bnai.anime.ui.production
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.Resource
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SceneDetailScreen() {
    val repository = remember { AnimeRepository() }
    val scenes = remember { mutableStateListOf<org.dpdns.bnai.anime.core.network.SceneDto>() }
    LaunchedEffect(Unit) { when (val res = repository.fetchScenes(repository.currentProjectId(), 1)) { is Resource.Success -> { if (res.data.isNotEmpty()) { scenes.clear(); scenes.addAll(res.data) } } else -> Unit } }
    Scaffold(topBar = { TopAppBar(title = { Text("Scene Inspector") }) }) { p ->
        LazyColumn(modifier = Modifier.padding(p).padding(16.dp)) {
            items(scenes, key = { it.id }) { sc -> Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Column(modifier = Modifier.padding(16.dp)) { Text("Scene ${sc.scene_no}"); Text(sc.description ?: "No description") } } }
        }
    }
}
