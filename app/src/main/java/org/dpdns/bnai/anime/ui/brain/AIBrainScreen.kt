package org.dpdns.bnai.anime.ui.brain

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dpdns.bnai.anime.core.network.DirectorActionDto
import org.dpdns.bnai.anime.core.network.DirectorContextDto
import org.dpdns.bnai.anime.core.network.DirectorPlanDto
import org.dpdns.bnai.anime.core.network.DirectorTimelineDto
import org.dpdns.bnai.anime.core.runtime.RuntimeContractCalculator
import org.dpdns.bnai.anime.ui.LocalAppContainer
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.friendlyPipelineStatus
import org.dpdns.bnai.anime.ui.designsystem.*
import org.dpdns.bnai.anime.ui.testing.TestTags

@Composable
fun AIBrainScreen(
    onNavigate: ((String) -> Unit)? = null,
    viewModel: AIDirectorViewModel = rememberSharedAIDirectorViewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    var draft by rememberSaveable { mutableStateOf("") }
    var showCreateProject by rememberSaveable { mutableStateOf(false) }
    var createName by rememberSaveable { mutableStateOf("") }
    var createPremise by rememberSaveable { mutableStateOf("") }
    var createGenre by rememberSaveable { mutableStateOf("Dark Fantasy") }
    var createSeasons by rememberSaveable { mutableStateOf("1") }
    var createEpisodes by rememberSaveable { mutableStateOf("12") }
    var createDuration by rememberSaveable { mutableStateOf("24") }

    if (showCreateProject) {
        AlertDialog(
            onDismissRequest = { showCreateProject = false },
            title = { Text("Create project with Director") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = createName,
                        onValueChange = { createName = it },
                        label = { Text("Project title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = createPremise,
                        onValueChange = { createPremise = it },
                        label = { Text("Premise (optional)") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = createGenre,
                        onValueChange = { createGenre = it },
                        label = { Text("Genre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = createSeasons,
                            onValueChange = { createSeasons = it.filter(Char::isDigit) },
                            label = { Text("Seasons") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = createEpisodes,
                            onValueChange = { createEpisodes = it.filter(Char::isDigit) },
                            label = { Text("Episodes/season") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = createDuration,
                            onValueChange = { createDuration = it.filter(Char::isDigit) },
                            label = { Text("Minutes (1–30)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = createName.isNotBlank() && !state.isSending,
                    onClick = {
                        val seasons = createSeasons.toIntOrNull()?.coerceAtLeast(1) ?: 1
                        val episodesPerSeason = createEpisodes.toIntOrNull()?.coerceAtLeast(1) ?: 12
                        viewModel.createProjectWithDirector(
                            name = createName,
                            premise = createPremise,
                            genre = createGenre,
                            totalSeasons = seasons,
                            episodesPerSeason = episodesPerSeason,
                            episodeDurationMin = (createDuration.toIntOrNull() ?: 24).coerceIn(RuntimeContractCalculator.MIN_EPISODE_MINUTES, RuntimeContractCalculator.MAX_EPISODE_MINUTES),
                            targetEpisodeCount = seasons * episodesPerSeason
                        ) {
                            showCreateProject = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateProject = false }) { Text("Cancel") }
            }
        )
    }

    LaunchedEffect(state.projectId, state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .testTag(TestTags.Screen.AI_BRAIN)
            .fillMaxSize()
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ProjectSelectorBar(
            onProjectChanged = { newPid ->
                draft = ""
                viewModel.setProject(newPid)
            }
        )

        ScreenHeader(
            title = "AI Studio Director",
            subtitle = "Project context • BYOD AI • server-approved actions",
            trailingContent = {
                StatusChip(label = state.providerBadge)
                if (state.messages.isNotEmpty()) {
                    IconButton(onClick = viewModel::clearConversation) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear project chat")
                    }
                }
            }
        )

        state.context?.let { DirectorContextBar(it) }

        DirectorReadinessStrip(
            readiness = state.readiness,
            onOpenSetup = { route -> onNavigate?.invoke(route) },
            onCreateProject = if (state.projectId.isBlank()) {
                { showCreateProject = true }
            } else null
        )

        state.timeline?.let { timeline ->
            DirectorTimelineCard(
                timeline = timeline,
                isRetrying = state.isSending,
                onRefresh = { viewModel.refreshTimeline() },
                onRetry = viewModel::retryTimeline
            )
        }

        state.plan?.let { plan ->
            DirectorPlanCard(plan = plan, onDismiss = viewModel::clearPlan)
        }

        if (!state.activeCommandId.isNullOrBlank()) {
            StandardCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Command #${state.activeCommandId?.take(8)}: ${friendlyPipelineStatus(state.activeCommandStatus ?: "QUEUED")}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    StatusChip(label = state.activeCommandStatus ?: "QUEUED")
                }
            }
        }

        if (state.messages.isEmpty()) {
            val hasProject = state.projectId.isNotBlank()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (hasProject) {
                    SuggestionChip(
                        onClick = { viewModel.sendMessage("Audit this project's Story Bible and tell me what is missing.") },
                        label = { Text("Audit Story Bible", fontSize = 12.sp) }
                    )
                    SuggestionChip(
                        onClick = { viewModel.proposeAction("CHECK_CONTINUITY") },
                        label = { Text("Check Continuity", fontSize = 12.sp) }
                    )
                    SuggestionChip(
                        onClick = { viewModel.proposeAction("CONTINUE_EPISODE") },
                        label = { Text("Plan Next Episode", fontSize = 12.sp) }
                    )
                    SuggestionChip(
                        onClick = { viewModel.requestPlan() },
                        label = { Text("Build Episode Plan", fontSize = 12.sp) }
                    )
                } else {
                    SuggestionChip(
                        onClick = { viewModel.sendMessage("Help me create a new anime project. Ask me for the essential details.") },
                        label = { Text("Start a new project", fontSize = 12.sp) }
                    )
                    SuggestionChip(
                        onClick = { viewModel.sendMessage("What setup do I need before creating a project?") },
                        label = { Text("Check setup", fontSize = 12.sp) }
                    )
                    SuggestionChip(
                        onClick = { viewModel.sendMessage("Suggest a strong first anime project brief.") },
                        label = { Text("Suggest a brief", fontSize = 12.sp) }
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.messages, key = { it.id }) { msg ->
                val isUser = msg.role == "user"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUser) StudioTokens.Primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isUser) "You" else "AI Studio Director",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUser) StudioTokens.Primary else StudioTokens.Secondary
                        )
                        MarkdownText(markdown = msg.content)
                    }
                }
            }

            if (state.actions.isNotEmpty()) {
                items(state.actions) { action ->
                    val capabilityRoute = directorDeepLinkRoute(
                        state.capabilities
                            .firstOrNull { it.intent == action.intent || it.key == action.intent }
                            ?.deep_link
                    )
                    DirectorActionCard(
                        action = action,
                        enabled = !state.isSending,
                        onConfirm = { viewModel.confirmAction(action) },
                        onOpenScreen = if (capabilityRoute != null && onNavigate != null) {
                            { onNavigate?.invoke(capabilityRoute) }
                        } else null
                    )
                }
            }

            state.error?.let { errorText ->
                item("director_error") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StudioTokens.Error.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(errorText, fontSize = 12.sp, color = StudioTokens.Error, modifier = Modifier.weight(1f))
                            TextButton(onClick = viewModel::clearError) { Text("Dismiss") }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = {
                    draft = it
                    viewModel.updateInput(it)
                },
                placeholder = {
                    Text(
                        if (state.projectId.isBlank()) "Tell the Director what you want to make..."
                        else "Ask AI Director about this project..."
                    )
                },
                modifier = Modifier.weight(1f),
                maxLines = 4
            )
            IconButton(
                onClick = {
                    viewModel.sendMessage(draft)
                    draft = ""
                },
                enabled = draft.isNotBlank() && !state.isSending,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (draft.isNotBlank() && !state.isSending) StudioTokens.Primary else Color.Gray.copy(alpha = 0.3f))
            ) {
                if (state.isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun DirectorContextBar(context: DirectorContextDto) {
    val target = context.target.orEmpty()
    val project = context.project.orEmpty()
    val quota = context.quota.orEmpty()
    val targetMinutes = (target["targetDurationSec"] ?: target["target_duration_sec"])
        ?.toString()?.toDoubleOrNull()?.div(60.0)?.toInt()
    val status = target["duration_status"]?.toString() ?: target["durationStatus"]?.toString()
    val phase = project["phase"]?.toString() ?: "PROJECT_SETUP"
    StandardCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Phase: ${phase.replace('_', ' ')}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    buildString {
                        append("Target: ")
                        append(targetMinutes?.let { "${it}m" } ?: "project default")
                        status?.let { append(" • $it") }
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusChip(label = "RPM ${quota["rpm_used"] ?: 0}/${quota["rpm_limit"] ?: 0}")
        }
    }
}

@Composable
fun DirectorActionCard(
    action: DirectorActionDto,
    enabled: Boolean,
    onConfirm: () -> Unit,
    onOpenScreen: (() -> Unit)? = null
) {
    val preview = action.preview
    val targetMinutes = preview?.target_duration_sec?.let { (it / 60.0).toInt() }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (action.risk) {
                "destructive" -> StudioTokens.Error.copy(alpha = 0.12f)
                "external_side_effect" -> StudioTokens.Primary.copy(alpha = 0.10f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(action.label.ifBlank { action.intent }, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        "Season ${preview?.season_no ?: 1} • Episode ${preview?.episode_no ?: 1} • ${targetMinutes ?: "?"} min",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusChip(label = action.risk.replace('_', ' '))
            }
            Text(action.description, fontSize = 12.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onConfirm,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (action.requires_confirmation) "Confirm & Execute" else "Execute")
                }
                if (onOpenScreen != null) {
                    OutlinedButton(onClick = onOpenScreen, enabled = enabled) {
                        Text("Open Screen")
                    }
                }
            }
        }
    }
}

@Composable
fun DirectorTimelineCard(
    timeline: DirectorTimelineDto,
    isRetrying: Boolean = false,
    onRefresh: () -> Unit,
    onRetry: () -> Unit
) {
    StandardCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Production Timeline", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = timeline.command_status?.let { status ->
                        if (timeline.stages.isEmpty()) {
                            friendlyPipelineStatus(status)
                        } else {
                            "Stage ${timeline.stage_index + 1}/${timeline.stages.size} • ${friendlyPipelineStatus(status)}"
                        }
                    } ?: "No active production command",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onRefresh) { Text("Refresh", fontSize = 11.sp) }
                if (timeline.job?.retryable == true) {
                    TextButton(onClick = onRetry, enabled = !isRetrying) {
                        Text(if (isRetrying) "Retrying..." else "Retry", fontSize = 11.sp)
                    }
                }
            }
        }
        timeline.job?.failure_reason?.takeIf { it.isNotBlank() }?.let { reason ->
            Text(
                text = "Last failure: $reason",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.error
            )
        }
        if (timeline.stages.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                timeline.stages.forEach { stage ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(timelineStageColor(stage.status))
                        )
                        Text(
                            text = stage.label,
                            fontSize = 9.sp,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        timeline.episode?.let { episode ->
            Text(
                text = "S${episode.season_no}E${episode.episode_no} • ${episode.state.replace('_', ' ')} • QC ${episode.qc_status} • ${episode.clip_count}/${episode.scene_count} clips",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun DirectorPlanCard(plan: DirectorPlanDto, onDismiss: () -> Unit) {
    val ready = plan.steps.count { it.ready }
    StandardCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Director Plan", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    "$ready/${plan.steps.size} steps ready • ${plan.overall}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onDismiss) { Text("Dismiss", fontSize = 11.sp) }
        }
        plan.steps.forEach { step ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("${step.order}.", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Column(modifier = Modifier.weight(1f)) {
                    Text(step.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (step.ready) "Ready" else (step.blocked_reason ?: "Blocked"),
                        fontSize = 10.sp,
                        color = if (step.ready) StudioTokens.Success else StudioTokens.Warning
                    )
                }
                StatusChip(label = step.risk.replace('_', ' '))
            }
        }
    }
}

private fun timelineStageColor(status: String): Color = when (status.lowercase()) {
    "done" -> StudioTokens.Success
    "active" -> StudioTokens.Primary
    "failed" -> StudioTokens.Error
    "skipped" -> StudioTokens.Neutral.copy(alpha = 0.5f)
    else -> StudioTokens.Neutral.copy(alpha = 0.35f)
}

private fun directorDeepLinkRoute(deepLink: String?): String? = when (deepLink?.lowercase()) {
    "bible" -> "bible"
    "production" -> "production"
    "quick_tools" -> "quick_tools"
    "library" -> "media_library"
    "vision" -> "vision_analysis"
    "characters" -> "characters"
    "qc" -> "qc"
    "publish" -> "publish"
    "continuity" -> "continuity"
    "canon" -> "canon"
    "review_inbox" -> "review_inbox"
    "jobs" -> "jobs_monitor"
    "project_detail" -> "project_detail"
    "projects" -> "projects"
    else -> null
}
