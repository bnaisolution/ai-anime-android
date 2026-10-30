package org.dpdns.bnai.anime.ui.owner
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.Resource
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogScreen() {
    val repository = remember { AnimeRepository() }
    val logs = remember { mutableStateListOf<Map<String, Any?>>() }
    LaunchedEffect(Unit) { when (val res = repository.fetchAuditLogs(repository.currentProjectId())) { is Resource.Success -> logs.addAll(res.data); else -> Unit } }
    Scaffold(topBar = { TopAppBar(title = { Text("Audit Logs") }) }) { p ->
        LazyColumn(modifier = Modifier.padding(p).padding(16.dp)) {
            items(logs) { log -> Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(text = log.toString(), modifier = Modifier.padding(12.dp)) } }
        }
    }
}
