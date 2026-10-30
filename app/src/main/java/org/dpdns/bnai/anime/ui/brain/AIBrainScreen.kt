package org.dpdns.bnai.anime.ui.brain
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.Resource
data class ChatMsg(val isUser: Boolean, val text: String)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIBrainScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    val messages = remember { mutableStateListOf(ChatMsg(false, "AI Brain ready. Ask me anything.")) }
    var input by remember { mutableStateOf("") }
    Scaffold(topBar = { TopAppBar(title = { Text("AI Brain (S16)") }) }) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp)) {
            LazyColumn(modifier = Modifier.weight(1f)) { items(messages) { msg -> Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(msg.text, modifier = Modifier.padding(12.dp)) } } }
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f))
                Button(onClick = { if(input.isNotBlank()) { val q = input; messages.add(ChatMsg(true, q)); input = ""; scope.launch { val res = repository.chatWithBrain(q); messages.add(ChatMsg(false, (res as? Resource.Success)?.data?.reply ?: "Error")) } } }, modifier = Modifier.padding(start = 8.dp)) { Text("Send") }
            }
        }
    }
}
