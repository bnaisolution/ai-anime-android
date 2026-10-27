package org.dpdns.bnai.anime.ui.studio
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.model.Project
import org.dpdns.bnai.anime.core.network.Resource
class StudioViewModel : ViewModel() {
    private val _projectsState = MutableStateFlow<Resource<List<Project>>>(Resource.Loading)
    val projectsState: StateFlow<Resource<List<Project>>> = _projectsState
    init { loadProjects() }
    fun loadProjects() {
        viewModelScope.launch {
            _projectsState.value = Resource.Loading; delay(500)
            _projectsState.value = Resource.Success(listOf(Project("proj_001", "Shadows of Aethelgard", "Episode 5: Crimson Eclipse", "RUNNING"), Project("proj_002", "Cyber Samurai", "Episode 2", "REVIEW")))
        }
    }
}
