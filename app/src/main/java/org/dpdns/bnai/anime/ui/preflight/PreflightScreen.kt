package org.dpdns.bnai.anime.ui.preflight

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.*
import org.dpdns.bnai.anime.ui.LocalAppContainer
import org.dpdns.bnai.anime.ui.components.StatusBadge
import org.dpdns.bnai.anime.ui.designsystem.PrimaryButton
import org.dpdns.bnai.anime.ui.designsystem.SecondaryButton
import org.dpdns.bnai.anime.ui.designsystem.StandardCard
import org.dpdns.bnai.anime.ui.designsystem.StudioTokens
import org.dpdns.bnai.anime.ui.testing.TestTags

data class ReadinessCheck(
    val key: String,
    val label: String,
    val ready: Boolean,
    val detail: String,
    val fixRoute: String? = null,
    val aiFix: AiFixKind = AiFixKind.None
)

enum class AiFixKind { None, StoryBible, EpisodeScenes }

data class ReadinessReport(val checks: List<ReadinessCheck>) {
    val allReady: Boolean get() = checks.isNotEmpty() && checks.all { it.ready }
    val readyCount: Int get() = checks.count { it.ready }
    val pending: List<ReadinessCheck> get() = checks.filter { !it.ready }
}

private fun JsonMap.isTruthy(key: String): Boolean {
    val value = this[key]
    return value == true || value?.toString()?.equals("true", true) == true
}

private suspend fun <T> safeResource(block: suspend () -> Resource<T>): Resource<T> = try {
    block()
} catch (e: Exception) {
    Resource.Error(e.localizedMessage ?: "Check unavailable")
}

@Suppress("UNCHECKED_CAST")
private fun <T> Resource<T>.dataOrNull(): T? = (this as? Resource.Success<T>)?.data

suspend fun loadReadiness(
    repository: AnimeRepositoryApi,
    projectId: String,
    episodeNo: Int = 0
): ReadinessReport {
    val checks = mutableListOf<ReadinessCheck>()

    val backendUp = try { repository.checkBackendHealth() } catch (_: Exception) { false }
    checks += ReadinessCheck(
        key = "backend",
        label = "Backend API",
        ready = backendUp,
        detail = if (backendUp) "Backend health check passed." else "Backend health check failed.",
        fixRoute = "settings"
    )

    val readyRes = safeResource { repository.fetchReadiness() }
    val readyData = readyRes.dataOrNull().orEmpty()
    val dbUp = readyRes is Resource.Success &&
        readyData["ok"] != false &&
        !readyData["status"]?.toString().equals("not_ready", true)
    val dbDetail = when (readyRes) {
        is Resource.Success -> "Database: ${(readyData["dependencies"] as? Map<*, *>)?.get("database") ?: "up"}"
        is Resource.Error -> readyRes.message
        else -> "Database readiness unavailable."
    }
    checks += ReadinessCheck(
        key = "database",
        label = "Live Database",
        ready = dbUp,
        detail = dbDetail,
        fixRoute = "database"
    )

    val bibleRes = safeResource { repository.fetchBible(projectId) }
    val bibleMap = bibleRes.dataOrNull().orEmpty()
    val bibleReady = bibleRes is Resource.Success && bibleMap.isNotEmpty() &&
        (bibleMap["story_bible"] != null || bibleMap["title"] != null || bibleMap["world_overview"] != null)
    checks += ReadinessCheck(
        key = "bible",
        label = "Story Bible",
        ready = bibleReady,
        detail = if (bibleReady) "Story bible is present." else "Story bible is missing or empty.",
        fixRoute = "bible",
        aiFix = AiFixKind.StoryBible
    )

    val charsRes = safeResource { repository.fetchCharacters(projectId) }
    val charCount = charsRes.dataOrNull()?.size ?: 0
    checks += ReadinessCheck(
        key = "characters",
        label = "Characters",
        ready = charCount > 0,
        detail = if (charCount > 0) "$charCount character(s) available." else "No characters created yet.",
        fixRoute = "characters"
    )

    val epsRes = safeResource { repository.fetchEpisodes(projectId) }
    val episodes = epsRes.dataOrNull().orEmpty()
    val sceneEpisodeNo = episodes.maxOfOrNull { it.episodeNumber } ?: episodeNo
    val sceneCount = if (sceneEpisodeNo > 0) {
        (safeResource { repository.fetchScenes(projectId, sceneEpisodeNo) }.dataOrNull())?.size ?: 0
    } else 0
    checks += ReadinessCheck(
        key = "scenes",
        label = "Episode & Scene Plan",
        ready = episodes.isNotEmpty() && sceneCount > 0,
        detail = if (episodes.isEmpty()) {
            "No episode or scene plan exists yet."
        } else if (sceneCount == 0) {
            "Episode plan exists but no scenes are generated."
        } else {
            "$sceneCount scene(s) ready in episode $sceneEpisodeNo."
        },
        fixRoute = "episodes",
        aiFix = AiFixKind.EpisodeScenes
    )

    val jobsRes = safeResource { repository.fetchJobs(projectId) }
    val jobs = jobsRes.dataOrNull().orEmpty()
    val busy = jobs.any {
        val s = it.status?.uppercase()
        s in listOf("QUEUED", "DISPATCHED", "ACKNOWLEDGED", "RUNNING", "RECONCILING")
    }
    checks += ReadinessCheck(
        key = "pipeline",
        label = "Pipeline Idle",
        ready = !busy,
        detail = if (busy) "A pipeline job is currently active. Wait for it to finish." else "No active pipeline job.",
        fixRoute = "jobs_monitor"
    )

    val gpuRes = safeResource { repository.fetchGpuInfrastructure() }
    val gpuMap = gpuRes.dataOrNull().orEmpty()
    val workerConfigured = gpuMap.isNotEmpty() && (
        gpuMap["preferred_mode"] != null ||
            gpuMap["gpu_routing"] != null ||
            gpuMap["worker_url"] != null ||
            gpuMap["github_worker"] != null
        )
    checks += ReadinessCheck(
        key = "workers",
        label = "GPU / CPU Workers",
        ready = workerConfigured,
        detail = if (workerConfigured) "Worker routing configuration is present." else "No GPU/CPU worker configuration found.",
        fixRoute = "workers"
    )

    val storageRes = safeResource { repository.fetchStorageStatus() }
    val storageMap = storageRes.dataOrNull().orEmpty()
    val storageReady = storageRes is Resource.Success && storageMap.isTruthy("configured")
    checks += ReadinessCheck(
        key = "storage",
        label = "Telegram / Media Storage",
        ready = storageReady,
        detail = if (storageReady) {
            "Storage provider: ${storageMap["provider"] ?: "configured"}"
        } else {
            (storageRes as? Resource.Error)?.message ?: "No user-owned storage provider configured."
        },
        fixRoute = "settings"
    )

    return ReadinessReport(checks)
}

@Composable
fun PreflightScreen(
    projectId: String,
    onNavigate: (String) -> Unit = {},
    onGenerated: () -> Unit = {},
    viewModel: PreflightViewModel = viewModel(factory = LocalAppContainer.current.viewModelFactory)
) {
    val tokenManager = LocalAppContainer.current.tokenManager
    val repository = LocalAppContainer.current.repository
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsState()
    var isGenerating by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(projectId) {
        viewModel.load(projectId)
    }

    val report = state.report
    val nextEpisode = (state.episodeCount + 1).coerceAtLeast(1)
    val generateLabel = if (nextEpisode == 1) "Generate 1st Episode" else "Generate Episode $nextEpisode"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(TestTags.Screen.PREFLIGHT)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Episode Readiness", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Review every dependency before generating an episode. Pending items can be opened directly or fixed with AI.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = StudioTokens.Primary)
            }
        } else if (state.error != null) {
            StandardCard {
                Text("Could not load readiness", fontWeight = FontWeight.Bold, color = StudioTokens.Error)
                Text(state.error ?: "", fontSize = 13.sp)
                SecondaryButton(text = "Retry", onClick = { viewModel.load(projectId) })
            }
        } else if (report != null) {
            StandardCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${report.readyCount} / ${report.checks.size} checks ready", fontWeight = FontWeight.Bold)
                    StatusBadge(if (report.allReady) "READY" else "PENDING")
                }
                LinearProgressIndicator(
                    progress = { report.readyCount.toFloat() / report.checks.size.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (report.allReady) StudioTokens.Success else StudioTokens.Warning
                )
            }

            report.checks.forEach { check ->
                StandardCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(check.label, fontWeight = FontWeight.SemiBold)
                        StatusBadge(if (check.ready) "READY" else "PENDING")
                    }
                    Text(check.detail, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!check.ready) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (check.fixRoute != null) {
                                SecondaryButton(
                                    text = "Open",
                                    modifier = Modifier.weight(1f),
                                    onClick = { check.fixRoute?.let { onNavigate(it) } }
                                )
                            }
                            if (check.aiFix != AiFixKind.None) {
                                SecondaryButton(
                                    text = "Generate with AI",
                                    modifier = Modifier.weight(1f),
                                    enabled = !isGenerating,
                                    onClick = {
                                        if (isGenerating) return@SecondaryButton
                                        isGenerating = true
                                        scope.launch {
                                            val res = when (check.aiFix) {
                                                AiFixKind.StoryBible -> repository.submitStudioCommand(
                                                    projectId = projectId,
                                                    intent = CommandIntent.SAVE_BIBLE,
                                                    payload = mapOf(
                                                        "user_prompt" to "Generate a complete, production-ready story bible for this project."
                                                    )
                                                )
                                                AiFixKind.EpisodeScenes -> repository.submitStudioCommand(
                                                    projectId = projectId,
                                                    intent = CommandIntent.GENERATE_EPISODE,
                                                    episodeNo = nextEpisode,
                                                    payload = mapOf(
                                                        "idempotency_key" to "preflight_${projectId.take(8)}_${nextEpisode}"
                                                    )
                                                )
                                                AiFixKind.None -> null
                                            }
                                            isGenerating = false
                                            message = when (res) {
                                                is Resource.Success -> "AI job queued. Refresh readiness after it finishes."
                                                is Resource.Error -> res.message
                                                else -> "AI job could not be queued."
                                            }
                                            viewModel.load(projectId)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            message?.let {
                Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
            }

            PrimaryButton(
                text = generateLabel,
                isLoading = isGenerating,
                enabled = report.allReady && !isGenerating,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (!report.allReady || isGenerating) return@PrimaryButton
                    isGenerating = true
                    scope.launch {
                        val res = repository.submitStudioCommand(
                            projectId = projectId,
                            intent = CommandIntent.GENERATE_EPISODE,
                            episodeNo = nextEpisode,
                            payload = mapOf(
                                "idempotency_key" to "ep_gen_${projectId.take(8)}_${System.currentTimeMillis() / 60000}"
                            )
                        )
                        isGenerating = false
                        when (res) {
                            is Resource.Success -> {
                                val cid = res.data.command_id.orEmpty()
                                if (cid.isNotBlank()) {
                                    tokenManager.saveActiveCommandId(projectId, "quick_gen_episode", cid)
                                }
                                message = "Episode $nextEpisode queued."
                                onGenerated()
                            }
                            is Resource.Error -> message = res.message
                            else -> message = "Episode could not be queued."
                        }
                    }
                }
            )
            if (!report.allReady) {
                Text(
                    "Resolve all pending checks to enable episode generation.",
                    fontSize = 12.sp,
                    color = StudioTokens.Warning
                )
            }
        }
    }
}
