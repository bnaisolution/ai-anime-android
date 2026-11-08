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

data class HomeSummaryUi(
    val activeProjectId: String = "",
    val activeProjectName: String = "Active Project",
    val recentEpisodes: List<Episode> = emptyList(),
    val pendingApprovalsCount: Int = 0,
    val activeProductionStatus: String = "IDLE",
    val createStatusMessage: String? = null
)

class StudioViewModel(private val repository: AnimeRepository = AnimeRepository()) : ViewModel() {
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
                    TokenManager.getInstance()?.saveActiveProjectId(activeId)
                }
                loadProjectHomeMetrics(activeId, activeName)
            }
        }
    }

    fun selectProject(projectId: String) {
        TokenManager.getInstance()?.saveActiveProjectId(projectId)
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
                it.copy(projectName = projectName)
            }.orEmpty()
            val pendingCount = (approvalsRes as? Resource.Success)?.data?.count {
                it.status == null || it.status.equals("PENDING", ignoreCase = true)
            } ?: 0
            val latestJobState = (jobsRes as? Resource.Success)?.data?.firstOrNull()?.status ?: "IDLE"

            _homeSummary.value = _homeSummary.value.copy(
                activeProjectId = projectId,
                activeProjectName = projectName,
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
            when (val created = repository.createProjectSync(cleanName)) {
                is Resource.Success -> {
                    TokenManager.getInstance()?.saveActiveProjectId(created.data.id)
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
