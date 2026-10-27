package org.dpdns.bnai.anime.ui.settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    var darkMode by remember { mutableStateOf(true) }
    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Dark Mode"); Switch(checked = darkMode, onCheckedChange = { darkMode = it }) }
        }
    }
}
