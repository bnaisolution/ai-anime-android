package org.dpdns.bnai.anime.ui.production
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionScreen(id: String, vm: ProductionViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.start() }
    Scaffold(topBar = { TopAppBar(title = { Text("Production Center") }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (state.stage != ProductionStage.Completed) CircularProgressIndicator(Modifier.size(72.dp))
            Spacer(Modifier.height(16.dp))
            Text("Stage: ${state.stage.title}", style = MaterialTheme.typography.headlineLarge)
        }
    }
}
