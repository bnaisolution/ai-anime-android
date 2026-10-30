package org.dpdns.bnai.anime.ui.review
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.Resource
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewInboxScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    val approvals = remember { mutableStateListOf<org.dpdns.bnai.anime.core.network.ApprovalDecisionDto>() }
    LaunchedEffect(Unit) { when (val res = repository.fetchApprovals(repository.currentProjectId())) { is Resource.Success -> { if (res.data.isNotEmpty()) { approvals.clear(); approvals.addAll(res.data) } } else -> Unit } }
    Scaffold(topBar = { TopAppBar(title = { Text("Review Inbox") }) }) { p ->
        LazyColumn(modifier = Modifier.padding(p).padding(16.dp)) {
            items(approvals, key = { it.id }) { item ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(item.type, style = MaterialTheme.typography.headlineMedium); Text("Resource: ${item.resource_id}")
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { scope.launch { repository.submitApprovalDecision(repository.currentProjectId(), item.id, "APPROVED", "Approved") } }, modifier = Modifier.weight(1f)) { Text("Approve") }
                            Button(onClick = { scope.launch { repository.submitApprovalDecision(repository.currentProjectId(), item.id, "REJECTED", "Rejected") } }, modifier = Modifier.weight(1f)) { Text("Reject") }
                        }
                    }
                }
            }
        }
    }
}
