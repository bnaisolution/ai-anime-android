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
import org.dpdns.bnai.anime.core.network.*

data class EpisodeSummary(
    val id: String = "",
    val episodeNumber: Int = 1,
    val title: String = "",
    val projectName: String = "",
    val status: String = "",
    val updatedAt: Long = 0L
)

data class LatestProduction(
    val projectName: String = "",
    val episodeNumber: Int = 1,
    val jobStatus: String = "IDLE",
    val statusText: String = ""
)

data class HomeSummaryUi(
    val activeProjectId: String = "",
    val activeProjectName: String = "Active Project",
    val recentEpisodes: List<EpisodeSummary> = emptyList(),
    val pendingApprovalsCount: Int = 0,
    val activeProductionStatus: String = "IDLE",
    val createStatusMessage: String? = null,
    val latestProduction: LatestProduction = LatestProduction(),
    val debugTag: String = "debug"
)

class StudioViewModel(
    private val repository: AnimeRepositoryApi,
    private val tokenManager: TokenManager
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
                val activeName = res.data.find { it.id == activeId }?.name ?: res.data.first().name
                if (repository.currentProjectId().isBlank()) {
                    tokenManager.saveActiveProjectId(activeId)
                }
                loadProjectHomeMetrics(activeId, activeName)
            }
        }
    }

    fun selectProject(projectId: String) {
        tokenManager.saveActiveProjectId(projectId)
        val projName = (_projectsState.value as? Resource.Success)?.data?.find { it.id == projectId }?.name ?: "Active Project"
        loadProjectHomeMetrics(projectId, projName)
    }

    fun loadProjectHomeMetrics(projectId: String, projectName: String = "Active Project") {
        if (projectId.isBlank()) return
        viewModelScope.launch {
            val episodesRes = repository.fetchEpisodes(projectId)
            val approvalsRes = repository.fetchApprovals(projectId)
            val jobsRes = repository.fetchJobs(projectId)

            val recentEps = (episodesRes as? Resource.Success)?.data?.take(5)?.map {
                EpisodeSummary(
                    id = it.id,
                    episodeNumber = it.episodeNumber,
                    title = it.title ?: "",
                    projectName = projectName,
                    status = it.current_state ?: "",
                    updatedAt = it.updated_at?.toLong() ?: 0L
                )
            }.orEmpty()
            val pendingCount = (approvalsRes as? Resource.Success)?.data?.count {
                it.status == null || it.status.equals("PENDING", ignoreCase = true)
            } ?: 0
            val latestJobState = (jobsRes as? Resource.Success)?.data?.firstOrNull()?.status ?: "IDLE"

            // Determine latest production from jobs
            val activeStatuses = setOf("QUEUED", "DISPATCHED", "ACKNOWLEDGED", "RUNNING", "RECONCILING")
            val activeJobs = (jobsRes as? Resource.Success)?.data?.filter { activeStatuses.contains((it.status ?? "").toUpperCase()) } ?: emptyList()
            val latestProduction = latestJobState.run {
                if (activeJobs.isNotEmpty()) {
                    val latest = activeJobs.first()
                    LatestProduction(
                        projectName = projectName,
                        episodeNumber = (latest.payload?.episode_no ?? latest.payload?.episode_number ?: 1),
                        jobStatus = latest.status ?: "IDLE",
                        statusText = when {
                            latest.status?.equals("FAILED", ignoreCase = true) -> "FAILED"
                            latest.status?.equals("RUNNING", ignoreCase = true) -> "Running"
                            latest.status?.equals("DISPATCHED", ignoreCase = true) -> "Queued"
                            else -> latest.status
                        }
                    )
                } else {
                    LatestProduction(
                        projectName = projectName,
                        jobStatus = latestJobState,
                        statusText = when (latestJobState.equals("FAILED", ignoreCase = true)) { "FAILED" } else { latestJobState }
                    )
                }
            }

            _homeSummary.value = _homeSummary.value.copy(
                activeProjectId = projectId,
                activeProjectName = projectName,
                recentEpisodes = recentEps,
                pendingApprovalsCount = pendingCount,
                activeProductionStatus = latestJobState,
                latestProduction = latestProduction
            )
        }
    }

    fun deleteProject(projectId: String) {
        if (projectId.isBlank()) return
        viewModelScope.launch {
            when (val res = repository.deleteProject(projectId)) {
                is Resource.Success -> {
                    _homeSummary.value = _homeSummary.value.copy(createStatusMessage = "Project deleted.")
                    loadProjects()
                }
                is Resource.Error -> {
                    _homeSummary.value = _homeSummary.value.copy(createStatusMessage = "Delete failed: ${res.message}")
                }
                else -> Unit
            }
        }
    }

    fun createNewProject(projectName: String, promptIds: List<String> = emptyList()) {
        val cleanName = projectName.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch {
            when (val created = repository.createProjectSync(cleanName, promptIds)) {
                is Resource.Success -> {
                    tokenManager.saveActiveProjectId(created.data.id)
                    _homeSummary.value = _homeSummary.value.copy(createStatusMessage = "Project '$cleanName' created!")
                    loadProjects()
                }
                is Resource.Error -> {
                    _homeSummary.value = _homeSummary.value.copy(createStatusMessage = "Create failed: ${created.message}")
                }
                else -> Unit
            }
        }
    }
}
