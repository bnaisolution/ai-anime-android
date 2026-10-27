package org.dpdns.bnai.anime.ui.project
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(id: String) {
    val nav = rememberNavController()
    var tabIndex by remember { mutableIntStateOf(0) }
    Scaffold(topBar = { TopAppBar(title = { Text("Project: $id") }) }) { pad ->
        Column(Modifier.padding(pad)) {
            TabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0; nav.navigate("overview") }, text = { Text("Overview") })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1; nav.navigate("bible") }, text = { Text("Bible") })
            }
            NavHost(nav, startDestination = "overview") {
                composable("overview") { Column(Modifier.padding(16.dp)) { Text("Universe Snapshot"); Button(onClick = {}) { Text("Open Episode Planner") } } }
                composable("bible") { BibleScreen(projectId = id) }
            }
        }
    }
}
