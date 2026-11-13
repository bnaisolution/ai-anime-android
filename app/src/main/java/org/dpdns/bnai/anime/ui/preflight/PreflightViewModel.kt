package org.dpdns.bnai.anime.ui.preflight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepositoryApi
import org.dpdns.bnai.anime.core.network.Resource

data class PreflightUiState(
    val isLoading: Boolean = true,
    val report: ReadinessReport? = null,
    val episodeCount: Int = 0,
    val error: String? = null
)

class PreflightViewModel(
    private val repository: AnimeRepositoryApi
) : ViewModel() {
    private val _state = MutableStateFlow(PreflightUiState())
    val state: StateFlow<PreflightUiState> = _state.asStateFlow()

    fun load(projectId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val episodesRes = try {
                repository.fetchEpisodes(projectId)
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Could not load episodes")
            }
            @Suppress("UNCHECKED_CAST")
            val episodes = (episodesRes as? Resource.Success<List<org.dpdns.bnai.anime.core.model.Episode>>)?.data.orEmpty()
            val episodeCount = episodes.maxOfOrNull { it.episodeNumber } ?: 0
            val report = loadReadiness(repository, projectId, episodeCount)
            _state.value = PreflightUiState(
                isLoading = false,
                report = report,
                episodeCount = episodeCount,
                error = null
            )
        }
    }
}
