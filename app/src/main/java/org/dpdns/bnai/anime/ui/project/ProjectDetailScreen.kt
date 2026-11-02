package org.dpdns.bnai.anime.ui.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.components.StatusBadge

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
    val overviewState by viewModel.overviewState.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.fetchOverview(projectId)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Project Overview", style = MaterialTheme.typography.headlineLarge)
        ResourceStateBox(
            resource = overviewState,
            onRetry = { viewModel.fetchOverview(projectId) }
        ) { overview ->
            val stats = overview.stats.orEmpty()
            val totalEps = stats["total_episodes"] ?: overview.episodes_count ?: 0
            val totalScenes = stats["total_scenes"] ?: 0
            val pendingActs = stats["pending_actions_count"] ?: overview.pending_approvals ?: 0
            val activeJobs = stats["active_jobs_count"] ?: overview.active_jobs ?: 0

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            overview.project?.name ?: "Project $projectId",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // GAP #5.2: status derived from is_active only
                            StatusBadge(
                                if (overview.project?.is_active == true) "ACTIVE" else "INACTIVE"
                            )
                            overview.canon_status?.let { StatusBadge("Canon: $it") }
                            overview.continuity_status?.let { StatusBadge("Continuity: $it") }
                        }
                        Text("Episodes: $totalEps • Scenes: $totalScenes")
                        Text("Active Jobs: $activeJobs • Pending Actions/Approvals: $pendingActs")
                    }
                }
                Button(
                    onClick = onOpenBible,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open Story Bible (WF-01)") }
                Button(
                    onClick = onOpenCharacters,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open Characters (WF-04)") }
                Button(
                    onClick = onOpenEpisodes,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open Episode Planner (WF-02)") }
                OutlinedButton(
                    onClick = onOpenReviews,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open Review & Approval Inbox (WF-11)") }
                OutlinedButton(
                    onClick = onOpenAnalytics,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open Analytics (WF-08)") }
            }
        }
    }
}
