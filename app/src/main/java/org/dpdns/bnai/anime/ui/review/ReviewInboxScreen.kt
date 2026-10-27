package org.dpdns.bnai.anime.ui.review
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
data class ApprovalRequestItem(val id: String, val type: String, val summary: String, val status: String = "PENDING")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewInboxScreen() {
    val approvals = remember { mutableStateListOf(ApprovalRequestItem("APR_01", "Canon Change", "Update Commander Meera", "PENDING")) }
    Scaffold(topBar = { TopAppBar(title = { Text("Review Inbox") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            approvals.forEach { item ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Column(Modifier.padding(16.dp)) { Text(item.type); Text(item.summary); Button(onClick = {}) { Text("Approve") } } }
            }
        }
    }
}
