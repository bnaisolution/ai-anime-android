package org.dpdns.bnai.anime.ui.tools
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickToolsScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var output by remember { mutableStateOf("Ready to generate...") }
    Scaffold(topBar = { TopAppBar(title = { Text("Quick Tools (S10)") }) }) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp)) {
            Text(output, modifier = Modifier.padding(bottom = 16.dp))
            Button(onClick = { scope.launch { val res = repository.triggerQuickMedia(repository.currentProjectId(), false, "Anime keyframe"); output = "Image Generation Status: ${(res as? org.dpdns.bnai.anime.core.network.Resource.Success)?.data?.status ?: "Queued"}" } }, modifier = Modifier.fillMaxWidth()) { Text("Generate Image") }
        }
    }
}
