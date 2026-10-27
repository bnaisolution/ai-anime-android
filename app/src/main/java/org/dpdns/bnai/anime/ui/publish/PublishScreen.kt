package org.dpdns.bnai.anime.ui.publish
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishScreen() {
    var title by remember { mutableStateOf("Shadows of Aethelgard - Episode 5") }
    Scaffold(topBar = { TopAppBar(title = { Text("Publish Studio") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("YouTube Title") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Button(onClick = {}) { Text("Publish Now") }
        }
    }
}
