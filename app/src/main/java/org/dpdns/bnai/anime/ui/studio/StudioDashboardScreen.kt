package org.dpdns.bnai.anime.ui.studio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun StudioDashboardScreen(
    onProjectClick: (String) -> Unit = {},
    onOpenReviews: () -> Unit = {},
    onOpenEpisodes: () -> Unit = {},
    viewModel: StudioViewModel = viewModel()
) {
    val projectsResource by viewModel.projectsState.collectAsState()
    val summary by viewModel.homeSummary.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }
    var projectDropdownExpanded by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Anime Project") },
            text = {
                OutlinedTextField(
                    value = newProjectName,
                    onValueChange = { newProjectName = it },
                    label = { Text("Project Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.createNewProject(newProjectName)
                    newProjectName = ""
                    showCreateDialog = false
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Create Project") },
                text = { Text("Create Project") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            summary.createStatusMessage?.let { msg ->
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                )) {
                    Text(msg, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyLarge)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Production State", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(6.dp))
                        StatusBadge(summary.activeProductionStatus)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f).clickable { onOpenReviews() },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Pending Approvals", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "${summary.pendingApprovalsCount} awaiting review",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
            }

            if (summary.recentEpisodes.isNotEmpty()) {
                Text("Recent Episodes", style = MaterialTheme.typography.headlineMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(summary.recentEpisodes, key = { it.id }) { ep ->
                        Card(
                            modifier = Modifier.width(200.dp).clickable { onOpenEpisodes() },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Ep ${ep.episodeNumber}: ${ep.title}",
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                StatusBadge(ep.status)
                            }
                        }
                    }
                }
            }

            ResourceStateBox(
                resource = projectsResource,
                emptyMessage = "No projects found in Anime Supabase. Tap 'Create Project' to begin.",
                onRetry = { viewModel.loadProjects() }
            ) { projects ->
                val activeProject = projects.find { it.id == summary.activeProjectId }
                    ?: projects.firstOrNull()
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Projects (${projects.size})", style = MaterialTheme.typography.headlineMedium)
                        Box {
                            OutlinedButton(onClick = { projectDropdownExpanded = true }) {
                                Text("Active: ${activeProject?.name ?: "Select"}")
                            }
                            DropdownMenu(
                                expanded = projectDropdownExpanded,
                                onDismissRequest = { projectDropdownExpanded = false }
                            ) {
                                projects.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text(p.name) },
                                        onClick = {
                                            viewModel.selectProject(p.id)
                                            projectDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(projects, key = { it.id }) { project ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    viewModel.selectProject(project.id)
                                    onProjectClick(project.id)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(project.name, style = MaterialTheme.typography.headlineMedium)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(project.activeEpisode, style = MaterialTheme.typography.bodyLarge)
                                    }
                                    StatusBadge(project.status)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
