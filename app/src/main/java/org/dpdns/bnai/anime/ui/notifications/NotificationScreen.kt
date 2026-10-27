package org.dpdns.bnai.anime.ui.notifications
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Notifications") }) }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Card { Column(Modifier.padding(12.dp)) { Text("Lipsync Failed (WF-11)"); Text("Episode 5 failed MuseTalk alignment.") } } }
            item { Card { Column(Modifier.padding(12.dp)) { Text("Episode 4 Published"); Text("Receipt YT_VID_88219B live.") } } }
        }
    }
}
