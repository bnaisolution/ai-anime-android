package org.dpdns.bnai.anime.ui.episode
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable
fun EpisodePlannerScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Episode Planner") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) { Text("Episodes List Here"); Button(onClick = {}) { Text("Generate Episode") } }
    }
}
