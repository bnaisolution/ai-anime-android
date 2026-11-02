package org.dpdns.bnai.anime.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.ApprovalDecisionDto
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun ReviewInboxScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var approvalsState by remember {
        mutableStateOf<Resource<List<ApprovalDecisionDto>>>(Resource.Loading)
    }
    var reasonInput by remember { mutableStateOf("") }

    fun load() {
        scope.launch {
            approvalsState = Resource.Loading
            approvalsState = repository.fetchApprovals(repository.currentProjectId())
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Review & Approval Inbox (WF-11)", style = MaterialTheme.typography.headlineLarge)
        OutlinedTextField(
            value = reasonInput,
            onValueChange = { reasonInput = it },
            label = { Text("Decision Reason / Reviewer Note") },
            modifier = Modifier.fillMaxWidth()
        )
        ResourceStateBox(
            resource = approvalsState,
            emptyMessage = "No pending approvals in this project's inbox.",
            onRetry = { load() }
        ) { items ->
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items, key = { it.id }) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${item.type} • ${item.resource_id ?: item.id}",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                StatusBadge(item.status ?: "PENDING")
                            }
                            item.risk?.let {
                                Text("Risk Level: $it", style = MaterialTheme.typography.labelLarge)
                            }
                            item.exact_diff?.let {
                                Text("Diff: $it", style = MaterialTheme.typography.bodyLarge)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    scope.launch {
                                        repository.submitApprovalDecision(
                                            repository.currentProjectId(),
                                            item.id,
                                            "APPROVED",
                                            reasonInput.ifBlank { "Approved from Review Inbox" },
                                            item.resource_version
                                        )
                                        load()
                                    }
                                }) { Text("Approve") }
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        repository.submitApprovalDecision(
                                            repository.currentProjectId(),
                                            item.id,
                                            "REJECTED",
                                            reasonInput.ifBlank { "Rejected from Review Inbox" },
                                            item.resource_version
                                        )
                                        load()
                                    }
                                }) { Text("Reject") }
                            }
                        }
                    }
                }
            }
        }
    }
}
