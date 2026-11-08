package org.dpdns.bnai.anime.ui.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.*

data class StoryBibleData(
    val projectId: String,
    val universeTitle: String,
    val worldRules: String,
    val magicSystem: String,
    val toneAndTheme: String
)

class ProjectViewModel(private val repository: AnimeRepositoryApi) : ViewModel() {
    private val _overviewState = MutableStateFlow<Resource<ProjectOverviewDto>>(Resource.Loading)
    val overviewState: StateFlow<Resource<ProjectOverviewDto>> = _overviewState.asStateFlow()

    private val _bibleState = MutableStateFlow<Resource<StoryBibleData>>(Resource.Loading)
    val bibleState: StateFlow<Resource<StoryBibleData>> = _bibleState.asStateFlow()

    fun fetchOverview(projectId: String) {
        val targetId = projectId.ifBlank { repository.currentProjectId() }
        if (targetId.isBlank()) {
            _overviewState.value = Resource.Error("No active project selected.")
            return
        }
        viewModelScope.launch {
            _overviewState.value = Resource.Loading
            _overviewState.value = repository.fetchProjectOverview(targetId)
        }
    }

    fun updateSettings(
        projectId: String,
        visualMedium: String,
        genre: String,
        autoApprove: Boolean,
        coverUrl: String?,
        onDone: (String) -> Unit
    ) {
        val targetId = projectId.ifBlank { repository.currentProjectId() }
        viewModelScope.launch {
            when (val res = repository.updateProjectSettings(targetId, visualMedium, genre, autoApprove, coverUrl)) {
                is Resource.Success -> {
                    onDone("Project settings saved!")
                    fetchOverview(targetId)
                }
                is Resource.Error -> onDone("Failed: ${res.message}")
                else -> Unit
            }
        }
    }

    fun quickGenerateEpisode(projectId: String, onDone: (String) -> Unit) {
        val targetId = projectId.ifBlank { repository.currentProjectId() }
        viewModelScope.launch {
            when (val res = repository.submitStudioCommand(targetId, CommandIntent.GENERATE_EPISODE, "SAVE")) {
                is Resource.Success -> onDone("Episode generation queued (${res.data.status ?: "QUEUED"})")
                is Resource.Error -> onDone("Failed: ${res.message}")
                else -> Unit
            }
        }
    }

    fun fetchBible(projectId: String) {
        val targetId = projectId.ifBlank { repository.currentProjectId() }
        if (targetId.isBlank()) {
            _bibleState.value = Resource.Error("No active project selected.")
            return
        }
        viewModelScope.launch {
            _bibleState.value = Resource.Loading
            when (val res = repository.fetchBible(targetId)) {
                is Resource.Success -> {
                    val data = res.data
                    val rawBible = data["story_bible"] as? Map<*, *> ?: data
                    val title = (rawBible["title"] ?: data["name"] ?: "Project Story Bible").toString()
                    val overview = (rawBible["world_overview"] ?: rawBible["logline"] ?: "").toString()
                    val powers = (rawBible["magic_system"] ?: "").toString()
                    val tone = (rawBible["tone"] ?: "").toString()
                    _bibleState.value = Resource.Success(StoryBibleData(targetId, title, overview, powers, tone))
                }
                is Resource.Empty -> _bibleState.value = Resource.Empty
                is Resource.Error -> _bibleState.value = res
                else -> Unit
            }
        }
    }

    fun proposeBibleChange(projectId: String, summary: String, onDone: (String) -> Unit) {
        val targetId = projectId.ifBlank { repository.currentProjectId() }
        viewModelScope.launch {
            when (val res = repository.submitStudioCommand(targetId, CommandIntent.SAVE_BIBLE, "SAVE", payload = mapOf("user_prompt" to summary))) {
                is Resource.Success -> onDone("Story Bible saved (${res.data.status ?: "QUEUED"})")
                is Resource.Error -> onDone("Failed: ${res.message}")
                else -> Unit
            }
        }
    }
}
