package org.dpdns.bnai.anime.ui.production

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepositoryApi
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.Resource

data class SceneProgressItem(
    val sceneNo: Int,
    val title: String,
    val imageDone: Boolean,
    val audioDone: Boolean,
    val videoDone: Boolean
)

data class ProductionState(
    val episodeId: String = "",
    val episodeTitle: String = "Loading Episode...",
    val status: String = "IDLE",
    val progressPercent: Int = 0,
    val imageProgress: Float = 0f,
    val audioProgress: Float = 0f,
    val videoProgress: Float = 0f,
    val workerName: String = "Kaggle GPU Pool",
    val qcStatusText: String = "QC: Waiting for render",
    val logs: List<String> = listOf("Pipeline ready. Waiting for command dispatch..."),
    val sceneChecklist: List<SceneProgressItem> = emptyList(),
    val errorMessage: String? = null
)

class ProductionViewModel(
    private val repository: AnimeRepositoryApi
) : ViewModel() {

    private val _state = MutableStateFlow(ProductionState())
    val state: StateFlow<ProductionState> = _state.asStateFlow()

    private var pollingJob: Job? = null

    fun startMonitoring(commandOrEpisodeId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            val activePid = repository.currentProjectId()

            // [MEDIUM-1 FIX] Resolve real episode title from project episodes list first
            var resolvedTitle = "Episode Pipeline"
            if (activePid.isNotBlank()) {
                val epsRes = repository.fetchEpisodes(activePid)
                if (epsRes is Resource.Success && epsRes.data.isNotEmpty()) {
                    val matchedEp = epsRes.data.find { it.id == commandOrEpisodeId } ?: epsRes.data.first()
                    resolvedTitle = "Episode ${matchedEp.episodeNumber}: ${matchedEp.title}"
                }
            }

            _state.value = _state.value.copy(
                episodeId = commandOrEpisodeId,
                episodeTitle = resolvedTitle
            )

            val isCommandUuid = commandOrEpisodeId.length == 36 && commandOrEpisodeId.count { it == '-' } == 4
            if (!isCommandUuid) {
                if (activePid.isNotBlank()) {
                    val jobsRes = repository.fetchJobs(activePid)
                    if (jobsRes is Resource.Success && jobsRes.data.isNotEmpty()) {
                        val latestJob = jobsRes.data.first()
                        val epNum = latestJob.episode_no ?: 1
                        val st = latestJob.status ?: "IDLE"
                        updateFromStatus(st, "Episode $epNum Pipeline", latestJob.error_message)
                        return@launch
                    }
                }
                updateFromStatus("IDLE", resolvedTitle, null)
                return@launch
            }

            var ticks = 0
            while (isActive && ticks < 120) {
                when (val res = repository.fetchCommandStatus(commandOrEpisodeId)) {
                    is Resource.Success -> {
                        val cmd = res.data
                        val st = (cmd.status ?: "QUEUED").uppercase()
                        val payloadEpTitle = cmd.payload?.get("title")?.toString()
                        val epNo = (cmd.payload?.get("episode_no") as? Number)?.toInt()
                        val dynamicTitle = when {
                            !payloadEpTitle.isNullOrBlank() && epNo != null -> "Episode $epNo: $payloadEpTitle"
                            !payloadEpTitle.isNullOrBlank() -> payloadEpTitle
                            epNo != null -> "Episode $epNo Production"
                            else -> _state.value.episodeTitle
                        }
                        updateFromStatus(st, dynamicTitle, cmd.error_message)
                        if (st in listOf("SUCCEEDED", "COMPLETED", "FAILED", "MANUAL_REVIEW")) {
                            break
                        }
                    }
                    is Resource.Error -> {
                        _state.value = _state.value.copy(errorMessage = res.message)
                        break
                    }
                    else -> Unit
                }
                ticks++
                delay(3000)
            }
        }
    }

    private fun updateFromStatus(rawStatus: String, title: String, errorMsg: String?) {
        val upper = rawStatus.uppercase()
        val (pct, imgP, audP, vidP) = when (upper) {
            "QUEUED" -> listOf(15f, 0.2f, 0.1f, 0.05f)
            "DISPATCHED", "ACKNOWLEDGED" -> listOf(40f, 0.6f, 0.4f, 0.2f)
            "RECONCILING", "RUNNING" -> listOf(75f, 1.0f, 0.85f, 0.65f)
            "SUCCEEDED", "COMPLETED" -> listOf(100f, 1.0f, 1.0f, 1.0f)
            else -> listOf(0f, 0f, 0f, 0f)
        }

        val checklist = (1..4).map { sNo ->
            SceneProgressItem(
                sceneNo = sNo,
                title = "Scene $sNo",
                imageDone = imgP >= (sNo * 0.25f),
                audioDone = audP >= (sNo * 0.25f),
                videoDone = vidP >= (sNo * 0.25f)
            )
        }

        val newLog = when (upper) {
            "QUEUED" -> "⏳ Queued in transactional outbox..."
            "DISPATCHED", "ACKNOWLEDGED" -> "🎨 Generating keyframes & Svara voice stems..."
            "RECONCILING", "RUNNING" -> "🎬 Rendering scene clips on Kaggle GPU worker..."
            "SUCCEEDED", "COMPLETED" -> "✅ All scenes rendered & muxed! Ready for QC."
            "FAILED" -> "❌ Pipeline error: ${errorMsg ?: "Worker timeout"}"
            else -> "Pipeline status: $upper"
        }

        _state.value = _state.value.copy(
            episodeTitle = title,
            status = upper,
            progressPercent = pct.toInt(),
            imageProgress = imgP,
            audioProgress = audP,
            videoProgress = vidP,
            qcStatusText = if (upper in listOf("SUCCEEDED", "COMPLETED")) "QC: Ready for Inspection" else "QC: Auto-Retry Guard Active",
            logs = (_state.value.logs + newLog).distinct().takeLast(8),
            sceneChecklist = checklist,
            errorMessage = errorMsg
        )
    }

    fun pausePipeline() {
        pollingJob?.cancel()
        _state.value = _state.value.copy(
            status = "PAUSED",
            logs = (_state.value.logs + "⏸️ Pipeline polling paused by user.").takeLast(8)
        )
    }

    fun cancelPipeline() {
        pollingJob?.cancel()
        viewModelScope.launch {
            val pid = repository.currentProjectId()
            if (pid.isNotBlank()) {
                repository.submitStudioCommand(pid, CommandIntent.ADMIN_STOP_KAGGLE)
            }
            _state.value = _state.value.copy(
                status = "CANCELLED",
                logs = (_state.value.logs + "🛑 Stop signal sent to GPU worker.").takeLast(8)
            )
        }
    }
}
