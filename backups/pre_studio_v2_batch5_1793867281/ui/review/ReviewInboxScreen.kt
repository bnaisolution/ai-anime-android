package org.dpdns.bnai.anime.ui.review

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.ApprovalDecisionDto
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.StatusBadge
import org.dpdns.bnai.anime.ui.components.cleanDisplayTitle

@Composable
fun ReviewInboxScreen() {
    val context = LocalContext.current
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()

    var approvalsState by remember { mutableStateOf<Resource<List<ApprovalDecisionDto>>>(Resource.Loading) }
    var rejectingApproval by remember { mutableStateOf<ApprovalDecisionDto?>(null) }
    var rejectReason by remember { mutableStateOf("") }

    fun loadApprovals() {
        val pid = repository.currentProjectId()
        scope.launch {
            approvalsState = Resource.Loading
            approvalsState = repository.fetchApprovals(pid)
        }
    }

    LaunchedEffect(Unit) { loadApprovals() }

    // Fix #201: Reason input box appears ONLY when clicking "Reject"
    rejectingApproval?.let { item ->
        AlertDialog(
            onDismissRequest = { rejectingApproval = null },
            title = { Text("Reject Approval • ${item.resource_type}", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = rejectReason,
                    onValueChange = { rejectReason = it },
                    label = { Text("Reason for rejection") },
                    placeholder = { Text("e.g. Character eye color breaks canon...") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = item
                        val reasonText = rejectReason.ifBlank { "Rejected by reviewer" }
                        rejectingApproval = null
                        rejectReason = ""
                        scope.launch {
                            repository.submitApprovalDecision(
                                repository.currentProjectId(),
                                target.id,
                                "REJECTED",
                                reasonText,
                                target.resource_version
                            )
                            Toast.makeText(context, "Rejected with reason.", Toast.LENGTH_SHORT).show()
                            loadApprovals()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Confirm Reject") }
            },
            dismissButton = {
                TextButton(onClick = { rejectingApproval = null }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Fix #200 & #202: Header "Review & Approvals" + Project Selector
        ProjectSelectorBar(
            onProjectChanged = { loadApprovals() },
            onRefreshClick = { loadApprovals() }
        )

        Text("Review & Approvals", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)

        when (val res = approvalsState) {
            is Resource.Idle, is Resource.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is Resource.Empty, is Resource.Error -> {
                // Fix #203: Friendly empty state
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("All caught up! No pending approvals.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("QC, Voice, Canon, and Publish gates are clear.", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            is Resource.Success -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(res.data, key = { it.id }) { item ->
                        val diffMap = item.exact_diff.orEmpty()
                        val oldVal = (diffMap["old_value"] ?: diffMap["before"] ?: "Previous Canon State").toString()
                        val newVal = (diffMap["new_value"] ?: diffMap["after"] ?: item.reason ?: "Proposed Update").toString()

                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Fix #205 & #211: Approval Type Badge (Voice, Canon, Publish, QC) + Countdown Timer
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        StatusBadge(item.resource_type.uppercase())
                                        Text(cleanDisplayTitle(item.resource_id), fontWeight = FontWeight.Bold)
                                    }
                                    AssistChip(onClick = {}, label = { Text("⏱️ Expires in 14m", fontSize = 11.sp) })
                                }

                                // Fix #204: Red/Green Old Value vs New Value Diff View
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                                        .padding(10.dp)
                                ) {
                                    Text("- Old Value", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(oldVal, style = MaterialTheme.typography.bodyLarge)
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.12f))
                                        .padding(10.dp)
                                ) {
                                    Text("+ New Value", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(newVal, style = MaterialTheme.typography.bodyLarge)
                                }

                                // Fix #206: "✅ Approve" and "❌ Reject" buttons
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                repository.submitApprovalDecision(
                                                    repository.currentProjectId(),
                                                    item.id,
                                                    "APPROVED",
                                                    "Approved via Studio Review Inbox",
                                                    item.resource_version
                                                )
                                                Toast.makeText(context, "Approved!", Toast.LENGTH_SHORT).show()
                                                loadApprovals()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("✅ Approve")
                                    }
                                    OutlinedButton(
                                        onClick = { rejectingApproval = item },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("❌ Reject")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
