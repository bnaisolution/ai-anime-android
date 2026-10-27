package org.dpdns.bnai.anime.ui.project
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dpdns.bnai.anime.core.network.Resource
@Composable
fun BibleScreen(projectId: String = "proj_001", vm: ProjectViewModel = viewModel()) {
    val state by vm.bibleState.collectAsState()
    LaunchedEffect(projectId) { vm.fetchBible(projectId) }
    when (val s = state) {
        is Resource.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is Resource.Success -> {
            val b = s.data
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Master Story Bible", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(value = b.worldRules, onValueChange = {}, readOnly = true, label = { Text("World Rules") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = b.magicSystem, onValueChange = {}, readOnly = true, label = { Text("Magic System") }, modifier = Modifier.fillMaxWidth())
            }
        }
        else -> {}
    }
}
