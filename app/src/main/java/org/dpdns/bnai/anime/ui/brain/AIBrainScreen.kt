package org.dpdns.bnai.anime.ui.brain
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
data class ChatMessage(val isUser: Boolean, val text: String)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIBrainScreen() {
    val messages = remember { mutableStateListOf(ChatMessage(true, "Can Aarav cross the Rift?"), ChatMessage(false, "No. Rule #2 prevents it.")) }
    var input by remember { mutableStateOf("") }
    Scaffold(topBar = { TopAppBar(title = { Text("AI Brain") }) }, bottomBar = {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("Ask...") })
            Spacer(Modifier.width(8.dp))
            Button(onClick = { if (input.isNotBlank()) { messages.add(ChatMessage(true, input)); input = "" } }) { Text("Send") }
        }
    }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages) { msg -> 
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start) {
                    Card(colors = CardDefaults.cardColors(containerColor = if (msg.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)) { 
                        Text(msg.text, Modifier.padding(12.dp)) 
                    }
                }
            }
        }
    }
}
