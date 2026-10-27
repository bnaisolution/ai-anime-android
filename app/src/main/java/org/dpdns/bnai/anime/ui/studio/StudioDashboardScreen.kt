package org.dpdns.bnai.anime.ui.studio
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dpdns.bnai.anime.core.network.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioDashboardScreen(onProjectClick: (String) -> Unit = {}, vm: StudioViewModel = viewModel()) {
    val state by vm.projectsState.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("Anime Studio") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            Text("Active Productions", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            when (val s = state) {
                is Resource.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is Resource.Success -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(s.data) { p ->
                        Card(Modifier.fillMaxWidth().clickable { onProjectClick(p.id) }) {
                            Column(Modifier.padding(16.dp)) { Text(p.name, style = MaterialTheme.typography.headlineMedium); Text(p.activeEpisode) }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}
