package org.dpdns.bnai.anime.ui.episode
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodePlannerScreen(onStartProduction: (String) -> Unit = {}) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        AlertDialog(onDismissRequest = { showDialog = false }, title = { Text("Start Production?") }, text = { Text("This will consume quota.") }, confirmButton = { Button(onClick = { showDialog = false; onStartProduction("ep_005") }) { Text("Confirm") } }, dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } })
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Episode Planner") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) { Text("Episodes List"); Button(onClick = { showDialog = true }) { Text("Generate Episode") } }
    }
}
