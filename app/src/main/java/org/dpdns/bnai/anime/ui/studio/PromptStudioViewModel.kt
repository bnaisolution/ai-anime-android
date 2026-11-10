package org.dpdns.bnai.anime.ui.studio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.network.AnimeApiService
import org.dpdns.bnai.anime.core.network.CreateSystemPromptRequest
import org.dpdns.bnai.anime.core.network.RetrofitClient
import org.dpdns.bnai.anime.core.network.SystemPromptDto

data class PromptStudioUiState(
    val isLoading: Boolean = false,
    val prompts: List<SystemPromptDto> = emptyList(),
    val selectedVisualMedium: String = "ANIME",
    val selectedPrompt: SystemPromptDto? = null,
    val statusMessage: String? = null
)

class PromptStudioViewModel(
    private val api: AnimeApiService = RetrofitClient.getApiService(),
    private val activeProjectId: () -> String? = {
        TokenManager.getInstance().getActiveProjectId().ifBlank { null }
    }
) : ViewModel() {
    private val _uiState = MutableStateFlow(PromptStudioUiState())
    val uiState: StateFlow<PromptStudioUiState> = _uiState.asStateFlow()

    init {
        loadPrompts()
    }

    fun loadPrompts() {
        val projectId = activeProjectId()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, statusMessage = null)
            try {
                val res = api.getSystemPrompts(projectId)
                if (res.isSuccessful) {
                    val list = res.body()?.data.orEmpty()
                    val active = list.firstOrNull { it.is_default == true } ?: list.firstOrNull()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        prompts = list,
                        selectedPrompt = active,
                        selectedVisualMedium = active?.visual_medium
                            ?: _uiState.value.selectedVisualMedium
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        statusMessage = "Failed to load prompts (HTTP ${res.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = e.localizedMessage ?: "Network error"
                )
            }
        }
    }

    fun setVisualMedium(medium: String) {
        _uiState.value = _uiState.value.copy(selectedVisualMedium = medium)
    }

    fun selectPrompt(prompt: SystemPromptDto) {
        _uiState.value = _uiState.value.copy(
            selectedPrompt = prompt,
            selectedVisualMedium = prompt.visual_medium
        )
    }

    fun createAndActivatePrompt(title: String, promptText: String) {
        if (title.isBlank() || promptText.isBlank()) return
        val projectId = activeProjectId()
        val medium = _uiState.value.selectedVisualMedium
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, statusMessage = null)
            try {
                val req = CreateSystemPromptRequest(
                    project_id = projectId,
                    title = title.trim(),
                    visual_medium = medium,
                    prompt_category = "BIBLE",
                    prompt_text = promptText.trim(),
                    set_active = true
                )
                val res = api.createSystemPrompt(req)
                val created = res.body()?.data
                if (res.isSuccessful && created != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        prompts = listOf(created) + _uiState.value.prompts,
                        selectedPrompt = created,
                        statusMessage = "Saved & Activated: ${created.title}"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        statusMessage = "Save failed (HTTP ${res.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = e.localizedMessage ?: "Error saving prompt"
                )
            }
        }
    }
}
