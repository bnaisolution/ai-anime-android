package org.dpdns.bnai.anime.ui.owner
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerOverviewScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Owner Console") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), Arrangement.SpaceBetween) { Text("DB Status"); Text("ONLINE", color = androidx.compose.ui.graphics.Color.Green) } }
            Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), Arrangement.SpaceBetween) { Text("n8n Status"); Text("ONLINE", color = androidx.compose.ui.graphics.Color.Green) } }
            Button(onClick = {}) { Text("Workflows (WF-00 to 13)") }
        }
    }
}
