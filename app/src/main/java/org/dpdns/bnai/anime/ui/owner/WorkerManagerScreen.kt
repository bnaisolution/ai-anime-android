package org.dpdns.bnai.anime.ui.owner
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerManagerScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    Scaffold(topBar = { TopAppBar(title = { Text("Kaggle Workers") }) }) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp)) {
            Button(onClick = { scope.launch { repository.submitStudioCommand(repository.currentProjectId(), CommandIntent.ADMIN_START_KAGGLE, "SAVE") } }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) { Text("Start Kaggle") }
            Button(onClick = { scope.launch { repository.submitStudioCommand(repository.currentProjectId(), CommandIntent.ADMIN_STOP_KAGGLE, "SAVE") } }, modifier = Modifier.fillMaxWidth()) { Text("Stop Kaggle") }
        }
    }
}
