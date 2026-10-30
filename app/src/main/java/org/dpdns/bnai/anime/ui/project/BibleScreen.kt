package org.dpdns.bnai.anime.ui.project
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dpdns.bnai.anime.core.network.Resource
@Composable
fun BibleScreen(projectId: String = "11111111-1111-1111-1111-111111111111", viewModel: ProjectViewModel = viewModel()) {
    val context = LocalContext.current
    val bibleResource by viewModel.bibleState.collectAsState()
    var worldRules by remember { mutableStateOf("") }
    var magicSystem by remember { mutableStateOf("") }
    var toneAndTheme by remember { mutableStateOf("") }
    LaunchedEffect(projectId) { viewModel.fetchBible(projectId) }
    LaunchedEffect(bibleResource) {
        if (bibleResource is Resource.Success) {
            val data = (bibleResource as Resource.Success<StoryBibleData>).data
            worldRules = data.worldRules; magicSystem = data.magicSystem; toneAndTheme = data.toneAndTheme
        }
    }
    when (val state = bibleResource) {
        is Resource.Loading, is Resource.Idle -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
        is Resource.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text = state.message, color = MaterialTheme.colorScheme.error) }
        is Resource.Success -> {
            val bible = state.data
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = "Master Story Bible", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                Text(text = bible.universeTitle, style = MaterialTheme.typography.bodyLarge)
                OutlinedTextField(value = worldRules, onValueChange = { worldRules = it }, label = { Text("World Rules") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = magicSystem, onValueChange = { magicSystem = it }, label = { Text("Magic System") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = toneAndTheme, onValueChange = { toneAndTheme = it }, label = { Text("Tone/Theme") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { viewModel.proposeBibleChange(projectId, "Save Bible: $worldRules | Magic: $magicSystem | Tone: $toneAndTheme") { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() } }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Propose & Save (WF-01)") }
            }
        }
    }
}
