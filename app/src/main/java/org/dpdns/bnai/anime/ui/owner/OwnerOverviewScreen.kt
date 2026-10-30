package org.dpdns.bnai.anime.ui.owner
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dpdns.bnai.anime.core.network.AnimeRepository
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerOverviewScreen(userRole: String = "owner", onOpenWorkflows: () -> Unit = {}, onOpenWorkers: () -> Unit = {}, onOpenDatabase: () -> Unit = {}, onOpenAuditLogs: () -> Unit = {}) {
    val repository = remember { AnimeRepository() }
    var isHealthy by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isHealthy = repository.checkBackendHealth() }
    Scaffold(topBar = { TopAppBar(title = { Text("Owner Console") }) }) { p ->
        Column(modifier = Modifier.padding(p).padding(16.dp)) {
            Text("DB Status: ${if(isHealthy) "ONLINE" else "OFFLINE"}", color = if(isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onOpenWorkflows, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) { Text("Workflows") }
            Button(onClick = onOpenWorkers, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) { Text("Workers") }
            Button(onClick = onOpenDatabase, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) { Text("Database") }
            Button(onClick = onOpenAuditLogs, modifier = Modifier.fillMaxWidth()) { Text("Audit Logs") }
        }
    }
}
