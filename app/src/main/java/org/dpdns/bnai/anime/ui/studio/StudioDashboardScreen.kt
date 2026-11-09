package org.dpdns.bnai.anime.ui.studio

import org.dpdns.bnai.anime.ui.LocalAppContainer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.core.network.SystemPromptDto
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.components.StatusBadge
import org.dpdns.bnai.anime.ui.components.formatRelativeTime
import org.dpdns.bnai.anime.ui.components.friendlyPipelineStatus
import org.dpdns.bnai.anime.ui.testing.TestTags

@Composable
fun StudioDashboardScreen(
    onProjectClick: (String) -> Unit = {},
    onOpenReviews: () -> Unit = {},
    onOpenEpisodes: () -> Unit = {},
    viewModel: StudioViewModel = viewModel(factory = LocalAppContainer.current.viewModelFactory)
) {
    val projectsResource by viewModel.projectsState.collectAsState()
    val summary by viewModel.homeSummary.collectAsState()
    val repository = LocalAppContainer.current.repository
    var showCreateDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }
    var availablePrompts by remember { mutableStateOf<List<SystemPromptDto>>(emptyList()) }
    var selectedPromptIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showProdDialog by remember { mutableStateOf(false) }

    LaunchedEffect(showCreateDialog) {
        if (showCreateDialog) {
            try {
                when (val res = repository.fetchSystemPrompts()) {
                    is Resource.Success -> availablePrompts = res.data
                    else -> availablePrompts = emptyList()
                }
            } catch (_: Exception) {
                availablePrompts = emptyList()
            }
        }
    }

    // Fix #4: Create Project Dialog calling backend
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Project", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newProjectName,
                        onValueChange = { newProjectName = it },
                        label = { Text("Project Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag(TestTags.Studio.PROJECT_NAME_FIELD)
                    )
                    if (availablePrompts.isNotEmpty()) {
                        Text("Attach library prompts", fontWeight = FontWeight.SemiBold)
                        availablePrompts.take(12).forEach { prompt ->
                            FilterChip(
                                selected = prompt.id in selectedPromptIds,
                                onClick = {
                                    selectedPromptIds = if (prompt.id in selectedPromptIds) {
                                        selectedPromptIds - prompt.id
                                    } else {
                                        selectedPromptIds + prompt.id
                                    }
                                },
                                label = { Text("${prompt.prompt_category ?: "BIBLE"}: ${prompt.title}") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createNewProject(newProjectName, selectedPromptIds.toList())
                        newProjectName = ""
                        selectedPromptIds = emptySet()
                        showCreateDialog = false
                    },
                    enabled = newProjectName.isNotBlank(),
                    modifier = Modifier.testTag(TestTags.Studio.CREATE_CONFIRM)
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Fix #7: Clickable Production State AlertDialog
    if (showProdDialog) {
        AlertDialog(
            onDismissRequest = { showProdDialog = false },
            title = { Text("Pipeline Status Details", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Active Project: ${summary.activeProjectName}")
                    Text("Current Pipeline State: ${friendlyPipelineStatus(summary.activeProductionStatus)}")
                    Text("Pending Approvals: ${summary.pendingApprovalsCount}")
                }
            },
            confirmButton = {
                Button(onClick = { showProdDialog = false }) { Text("Close") }
            }
        )
    }

    Scaffold(
        modifier = Modifier.testTag(TestTags.Screen.STUDIO),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.testTag(TestTags.Studio.CREATE_PROJECT),
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
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))) {
                    Text(msg, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyLarge)
                }
            }

            // Fix #1, #5, #7: No WF-xx tags, Clickable Production State, Pending Approvals subtitle
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    modifier = Modifier.weight(1f).clickable { showProdDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Production State", style = MaterialTheme.typography.labelLarge)
                        Text("Tap for details", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        StatusBadge(friendlyPipelineStatus(summary.activeProductionStatus))
                    }
                }
                Card(
                    modifier = Modifier.weight(1f).clickable { onOpenReviews() },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Pending Approvals", style = MaterialTheme.typography.labelLarge)
                        Text("QC • Voice • Canon changes", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${summary.pendingApprovalsCount} awaiting", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Fix #6: Recent Episodes with project_name and formatted relative time ("2h ago")
            if (summary.recentEpisodes.isNotEmpty()) {
                Text("Recent Episodes", style = MaterialTheme.typography.headlineMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(summary.recentEpisodes, key = { it.id }) { ep ->
                        Card(
                            modifier = Modifier.width(220.dp).clickable { onOpenEpisodes() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(ep.projectName ?: summary.activeProjectName, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                Text("Ep ${ep.episodeNumber}: ${ep.title}", style = MaterialTheme.typography.labelLarge, maxLines = 1, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StatusBadge(friendlyPipelineStatus(ep.status))
                                    Text(formatRelativeTime(ep.updatedAt), fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }
                }
            }

            // Fix #8 & #9: Active project highlight border + Refresh control
            ResourceStateBox(
                resource = projectsResource,
                emptyMessage = "No projects found. Tap 'Create Project' to begin.",
                onRetry = { viewModel.loadProjects() }
            ) { projects ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Projects (${projects.size})", style = MaterialTheme.typography.headlineMedium)
                        IconButton(onClick = { viewModel.loadProjects() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Projects")
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.testTag(TestTags.Studio.PROJECT_LIST),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(projects, key = { it.id }) { project ->
                            val isActive = project.id == summary.activeProjectId
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag(TestTags.item(TestTags.Studio.PROJECT_ITEM_PREFIX, project.id)).clickable {
                                    viewModel.selectProject(project.id)
                                    onProjectClick(project.id)
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = if (isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(project.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                            if (isActive) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            }
                                        }
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
