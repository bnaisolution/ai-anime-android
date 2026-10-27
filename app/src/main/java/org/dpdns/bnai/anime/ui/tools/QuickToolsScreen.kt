package org.dpdns.bnai.anime.ui.tools
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable
fun QuickToolsScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Quick Tools") }) }) { pad -> Text("Vision Tools Here", modifier = Modifier.padding(pad).padding(16.dp)) }
}
