package org.dpdns.bnai.anime.ui.studio
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dpdns.bnai.anime.core.network.Resource
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioDashboardScreen(onProjectClick: (String) -> Unit = {}, vm: StudioViewModel = viewModel()) {
    val state by vm.projectsState.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("Anime Studio") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text("Compute Engine", style = MaterialTheme.typography.headlineMedium); Text("SDXL + AnimateDiff + MuseTalk", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF4CAF50))); Spacer(Modifier.width(8.dp)); Text("Kaggle Status: ONLINE", color = Color(0xFF4CAF50), style = MaterialTheme.typography.labelLarge) }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Active Productions", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            when (val s = state) {
                is Resource.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is Resource.Success -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(s.data) { p -> Card(Modifier.fillMaxWidth().clickable { onProjectClick(p.id) }) { Row(Modifier.padding(16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) { Column { Text(p.name, style = MaterialTheme.typography.headlineMedium); Text(p.activeEpisode) }; Surface(color = Color(0xFF3F51B5).copy(alpha = 0.2f), shape = RoundedCornerShape(50)) { Text(p.status, Modifier.padding(8.dp, 4.dp), color = Color(0xFF3F51B5)) } } } }
                }
                else -> {}
            }
        }
    }
}
