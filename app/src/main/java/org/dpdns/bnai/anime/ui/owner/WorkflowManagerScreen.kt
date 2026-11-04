package org.dpdns.bnai.anime.ui.owner

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun WorkflowManagerScreen() {
    val context = LocalContext.current
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()

    var activeProjectId by remember { mutableStateOf(repository.currentProjectId()) }
    var searchQuery by remember { mutableStateOf("") }
    var workflowsState by remember { mutableStateOf<Resource<List<JsonMap>>>(Resource.Loading) }
    val enabledOverrides = remember { mutableStateMapOf<String, Boolean>() }

    fun loadWorkflows(pid: String) {
        if (pid.isBlank()) return
        scope.launch {
            workflowsState = Resource.Loading
            workflowsState = repository.fetchAdminWorkflows(pid)
        }
    }

    LaunchedEffect(activeProjectId) {
        loadWorkflows(activeProjectId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Fix #333 & #334: Clean header "Workflow Engine Status" with no dev disclaimer
        Text("Workflow Engine Status", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        ProjectSelectorBar(
            onProjectChanged = { newPid ->
                activeProjectId = newPid
                loadWorkflows(newPid)
            },
            onRefreshClick = { loadWorkflows(activeProjectId) }
        )

        // Fix #337: Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Search workflows by name or intent...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        ResourceStateBox(
            resource = workflowsState,
            emptyMessage = "No workflow definitions found.",
            onRetry = { loadWorkflows(activeProjectId) }
        ) { list ->
            val filtered = list.filter {
                searchQuery.isBlank() || it.toString().contains(searchQuery, ignoreCase = true)
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filtered) { wf ->
                    val wfKey = (wf["wf_key"] ?: wf["id"] ?: "WF").toString()
                    val name = (wf["name"] ?: wf["title"] ?: wfKey).toString()
                    val isEnabled = enabledOverrides[wfKey] ?: (wf["is_active"] != false)
                    val intentsRaw = (wf["intents"] as? List<*>)?.map { it.toString() }
                        ?: listOf(wf["intent"]?.toString() ?: "PIPELINE_STEP")

                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                    Text("Engine Key: $wfKey", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                                }
                                // Fix #336 & #338: Active/Disabled Status Badge + Enable/Disable Toggle
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    StatusBadge(if (isEnabled) "ACTIVE" else "ONLINE")
                                    Switch(
                                        checked = isEnabled,
                                        onCheckedChange = { enabledOverrides[wfKey] = it }
                                    )
                                }
                            }

                            // Fix #335: Render arrays as Chips/Tags instead of raw JSON brackets
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(intentsRaw) { tag ->
                                    AssistChip(onClick = {}, label = { Text(tag.replace("_", " ")) })
                                }
                            }

                            // Fix #339: Test Webhook Button
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        repository.submitStudioCommand(activeProjectId, CommandIntent.SYSTEM_HEALTH, payload = mapOf("test_wf" to wfKey))
                                        Toast.makeText(context, "Webhook ping sent to $name!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text("Test Webhook")
                            }
                        }
                    }
                }
            }
        }
    }
}
