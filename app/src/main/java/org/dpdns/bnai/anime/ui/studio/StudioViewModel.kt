package org.dpdns.bnai.anime.ui.studio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.model.Episode
import org.dpdns.bnai.anime.core.model.Project
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.Resource

data class HomeSummaryUi(
    val activeProjectId: String = "",
    val recentEpisodes: List<Episode> = emptyList(),
    val pendingApprovalsCount: Int = 0,
    val activeProductionStatus: String = "IDLE",
    val createStatusMessage: String? = null
)

class StudioViewModel(
    private val repository: AnimeRepository = AnimeRepository()
) : ViewModel() {

    private val _projectsState = MutableStateFlow<Resource<List<Project>>>(Resource.Loading)
    val projectsState: StateFlow<Resource<List<Project>>> = _projectsState.asStateFlow()

    private val _homeSummary = MutableStateFlow(HomeSummaryUi())
    val homeSummary: StateFlow<HomeSummaryUi> = _homeSummary.asStateFlow()

    init {
        loadProjects()
    }

    fun loadProjects() {
        viewModelScope.launch {
            _projectsState.value = Resource.Loading
            val res = repository.fetchProjects()
            _projectsState.value = res
            if (res is Resource.Success && res.data.isNotEmpty()) {
                val activeId = repository.currentProjectId().ifBlank { res.data.first().id }
                loadProjectHomeMetrics(activeId)
            }
        }
    }

    fun selectProject(projectId: String) {
        TokenManager.getInstance()?.saveActiveProjectId(projectId)
        loadProjectHomeMetrics(projectId)
    }

    fun loadProjectHomeMetrics(projectId: String) {
        if (projectId.isBlank()) return
        viewModelScope.launch {
            val episodesRes = repository.fetchEpisodes(projectId)
            val approvalsRes = repository.fetchApprovals(projectId)
            val jobsRes = repository.fetchJobs(projectId)

            val recentEps = (episodesRes as? Resource.Success)?.data?.take(4).orEmpty()
            val pendingCount = (approvalsRes as? Resource.Success)?.data?.count {
                it.status == null || it.status.equals("PENDING", ignoreCase = true)
            } ?: 0
            val latestJobState = (jobsRes as? Resource.Success)?.data?.firstOrNull()?.status ?: "IDLE"

            _homeSummary.value = _homeSummary.value.copy(
                activeProjectId = projectId,
                recentEpisodes = recentEps,
                pendingApprovalsCount = pendingCount,
                activeProductionStatus = latestJobState
            )
        }
    }

    fun createNewProject(projectName: String) {
        val cleanName = projectName.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch {
            val baseProjectId = repository.currentProjectId().ifBlank {
                "00000000-0000-0000-0000-000000000000"
            }
            when (
                val cmd = repository.submitStudioCommand(
                    projectId = baseProjectId,
                    intent = CommandIntent.CREATE_PROJECT,
                    mode = "SAVE",
                    payload = mapOf("name" to cleanName)
                )
            ) {
                is Resource.Success -> {
                    _homeSummary.value = _homeSummary.value.copy(
                        createStatusMessage = "Create Project command dispatched (${cmd.data.status ?: "QUEUED"})"
                    )
                    loadProjects()
                }
                is Resource.Error -> {
                    _homeSummary.value = _homeSummary.value.copy(
                        createStatusMessage = "Create failed: ${cmd.message}"
                    )
                }
                else -> Unit
            }
        }
    }
}
