package org.dpdns.bnai.anime.ui.settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable
fun SettingsScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { pad -> Text("Preferences", modifier = Modifier.padding(pad).padding(16.dp)) }
}
