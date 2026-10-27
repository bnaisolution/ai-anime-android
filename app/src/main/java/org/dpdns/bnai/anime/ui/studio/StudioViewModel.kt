package org.dpdns.bnai.anime.ui.studio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.model.Project
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.Resource

class StudioViewModel(
    private val repository: AnimeRepository = AnimeRepository()
) : ViewModel() {

    private val _projectsState = MutableStateFlow<Resource<List<Project>>>(Resource.Loading)
    val projectsState: StateFlow<Resource<List<Project>>> = _projectsState.asStateFlow()

    init {
        loadProjects()
    }

    fun loadProjects() {
        viewModelScope.launch {
            _projectsState.value = Resource.Loading
            when (val apiResult = repository.fetchProjects()) {
                is Resource.Success -> {
                    if (apiResult.data.isNotEmpty()) {
                        _projectsState.value = apiResult
                    } else {
                        _projectsState.value = Resource.Success(defaultSeededProjects())
                    }
                }
                else -> {
                    _projectsState.value = Resource.Success(defaultSeededProjects())
                }
            }
        }
    }

    fun createNewProject(projectName: String) {
        viewModelScope.launch {
            val currentList = (_projectsState.value as? Resource.Success)?.data ?: defaultSeededProjects()
            val newId = "00000000-0000-4000-8000-00000000000${currentList.size + 1}"
            val cleanName = projectName.ifBlank { "Untitled Anime Universe ${currentList.size + 1}" }

            repository.submitStudioCommand(
                projectId = newId,
                intent = CommandIntent.CREATE_PROJECT,
                mode = "SAVE",
                payload = mapOf("name" to cleanName)
            )

            val created = Project(
                id = newId,
                name = cleanName,
                activeEpisode = "Season 1 • Episode 1: Pilot Script",
                status = "QUEUED"
            )
            _projectsState.value = Resource.Success(listOf(created) + currentList)
        }
    }

    private fun defaultSeededProjects(): List<Project> = listOf(
        Project(
            id = "11111111-1111-4111-8111-111111111111",
            name = "Shadows of Aethelgard",
            activeEpisode = "Episode 5: The Crimson Eclipse",
            status = "RUNNING"
        ),
        Project(
            id = "22222222-2222-4222-8222-222222222222",
            name = "Cyber Samurai: Neon Karma",
            activeEpisode = "Episode 2: Ghost in the Spire",
            status = "REVIEW"
        ),
        Project(
            id = "33333333-3333-4333-8333-333333333333",
            name = "Chronicles of Vayu",
            activeEpisode = "Episode 1: Awakening",
            status = "APPROVED"
        )
    )
}
