package org.dpdns.bnai.anime.ui.project
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(id: String) {
    Scaffold(topBar = { TopAppBar(title = { Text("Project: $id") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) { Text("Overview / Bible / Characters Tabs here") }
    }
}
