package org.dpdns.bnai.anime.ui.publish
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
fun PublishScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("Episode 5: The Crimson Eclipse") }
    Scaffold(topBar = { TopAppBar(title = { Text("Publish Studio") }) }) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("YouTube Title") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { scope.launch { repository.submitStudioCommand(repository.currentProjectId(), CommandIntent.PUBLISH_NOW, "SAVE", payload = mapOf("title" to title)) } }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("Publish Now") }
        }
    }
}
