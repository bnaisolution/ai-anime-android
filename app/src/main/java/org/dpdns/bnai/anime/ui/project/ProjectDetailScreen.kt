package org.dpdns.bnai.anime.ui.project

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.components.StatusBadge
import org.dpdns.bnai.anime.ui.components.formatInt

@Composable
fun ProjectDetailScreen(
    projectId: String = TokenManager.getInstance()?.getActiveProjectId().orEmpty(),
    onOpenBible: () -> Unit = {},
    onOpenCharacters: () -> Unit = {},
    onOpenEpisodes: () -> Unit = {},
    onOpenReviews: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {},
    viewModel: ProjectViewModel = viewModel()
) {
    val context = LocalContext.current
    val overviewState by viewModel.overviewState.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }
    var visualMedium by remember { mutableStateOf("ANIME") }
    var genre by remember { mutableStateOf("Dark Fantasy") }
    var autoApprove by remember { mutableStateOf(false) }
    var coverUrl by remember { mutableStateOf("") }

    LaunchedEffect(projectId) {
        viewModel.fetchOverview(projectId)
    }

    // Fix #15 & #17: Project Settings Dialog (Visual Medium, Genre, Auto-Approve, Cover Banner URL)
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Project Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Visual Medium", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ANIME", "REAL_HUMAN").forEach { med ->
                            FilterChip(
                                selected = visualMedium == med,
                                onClick = { visualMedium = med },
                                label = { Text(if (med == "ANIME") "🎨 Anime" else "📷 Realistic") }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = genre,
                        onValueChange = { genre = it },
                        label = { Text("Genre (e.g. Dark Fantasy, Cyberpunk)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = coverUrl,
                        onValueChange = { coverUrl = it },
                        label = { Text("Project Cover Banner URL (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto-Approve Pipeline Changes")
                        Switch(checked = autoApprove, onCheckedChange = { autoApprove = it })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateSettings(projectId, visualMedium, genre, autoApprove, coverUrl.ifBlank { null }) { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                    showSettingsDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ProjectSelectorBar(
            onProjectChanged = { newId -> viewModel.fetchOverview(newId) },
            onRefreshClick = { viewModel.fetchOverview(projectId) }
        )

        ResourceStateBox(
            resource = overviewState,
            onRetry = { viewModel.fetchOverview(projectId) }
        ) { overview ->
            val stats = overview.stats.orEmpty()
            // Fix #10: Format all numeric stats as integers (no 1.0)
            val totalEps = formatInt(stats["total_episodes"] ?: overview.episodes_count ?: 0)
            val totalScenes = formatInt(stats["total_scenes"] ?: 0)
            val pendingActs = formatInt(stats["pending_actions_count"] ?: overview.pending_approvals ?: 0)
            val activeJobs = formatInt(stats["active_jobs_count"] ?: overview.active_jobs ?: 0)

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Fix #13, #17, #101: Active Project Cover Banner + Canon/Continuity/Jobs Badges
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(125.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("ACTIVE PROJECT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(overview.project?.name ?: "Anime Studio Project", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusBadge(if (overview.project?.is_active == true) "ACTIVE" else "INACTIVE")
                            StatusBadge("Canon: ${overview.canon_status ?: "LOCKED"}")
                            StatusBadge("Continuity: ${overview.continuity_status ?: "OK"}")
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Episodes: $totalEps", fontWeight = FontWeight.SemiBold)
                        Text("Scenes: $totalScenes", fontWeight = FontWeight.SemiBold)
                        Text("Jobs: $activeJobs", fontWeight = FontWeight.SemiBold)
                        Text("Reviews: $pendingActs", fontWeight = FontWeight.SemiBold)
                    }
                }

                // Fix #16: Primary Quick Generate Button
                Button(
                    onClick = {
                        viewModel.quickGenerateEpisode(projectId) { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Quick Generate Next Episode")
                }

                // Fix #11 & #14: 2x2 Grid/Card Layout without WF-xx technical labels
                Text("Studio Modules", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        modifier = Modifier.weight(1f).clickable { onOpenBible() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Story Bible", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("Universe rules & lore", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f).clickable { onOpenCharacters() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Characters", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("Cast & Voice DNA", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        modifier = Modifier.weight(1f).clickable { onOpenEpisodes() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Episodes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("Scripts & production", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f).clickable { onOpenReviews() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Review Inbox", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("$pendingActs pending", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onOpenAnalytics, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Analytics")
                    }
                    OutlinedButton(onClick = { showSettingsDialog = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Project Settings")
                    }
                }
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}
