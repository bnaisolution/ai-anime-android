package org.dpdns.bnai.anime.ui.studio
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.model.Project
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.Resource
class StudioViewModel(private val repository: AnimeRepository = AnimeRepository()) : ViewModel() {
    private val _projectsState = MutableStateFlow<Resource<List<Project>>>(Resource.Loading)
    val projectsState: StateFlow<Resource<List<Project>>> = _projectsState.asStateFlow()
    init { loadProjects() }
    fun loadProjects() {
        viewModelScope.launch {
            _projectsState.value = Resource.Loading
            when (val apiResult = repository.fetchProjects()) {
                is Resource.Success -> { if (apiResult.data.isNotEmpty()) _projectsState.value = apiResult else _projectsState.value = Resource.Success(defaultSeededProjects()) }
                else -> _projectsState.value = Resource.Success(defaultSeededProjects())
            }
        }
    }
    fun createNewProject(projectName: String) {
        viewModelScope.launch {
            val currentList = (_projectsState.value as? Resource.Success)?.data ?: defaultSeededProjects()
            val targetProjectId = currentList.firstOrNull()?.id ?: repository.currentProjectId()
            val cleanName = projectName.ifBlank { "Untitled Anime Universe ${currentList.size + 1}" }
            repository.submitStudioCommand(targetProjectId, CommandIntent.CREATE_PROJECT, "SAVE", payload = mapOf("name" to cleanName))
            val created = Project(targetProjectId, cleanName, "Season 1 • Episode 1: Pilot Script", "QUEUED")
            TokenManager.getInstance()?.saveActiveProjectId(targetProjectId)
            _projectsState.value = Resource.Success(listOf(created) + currentList)
        }
    }
    private fun defaultSeededProjects(): List<Project> = listOf(
        Project("11111111-1111-1111-1111-111111111111", "Shadows of Aethelgard", "Episode 5: The Crimson Eclipse", "RUNNING"),
        Project("22222222-2222-4222-8222-222222222222", "Cyber Samurai: Neon Karma", "Episode 2: Ghost in the Spire", "REVIEW")
    )
}
