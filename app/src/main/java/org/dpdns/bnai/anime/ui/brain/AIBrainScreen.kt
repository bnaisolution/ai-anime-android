package org.dpdns.bnai.anime.ui.brain
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable
fun AIBrainScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("AI Brain") }) }) { pad -> Text("Chat UI Here", modifier = Modifier.padding(pad).padding(16.dp)) }
}
