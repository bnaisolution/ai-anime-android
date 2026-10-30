package org.dpdns.bnai.anime.ui.project
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.Resource
data class StoryBibleData(val projectId: String, val universeTitle: String, val worldRules: String, val magicSystem: String, val toneAndTheme: String)
class ProjectViewModel(private val repository: AnimeRepository = AnimeRepository()) : ViewModel() {
    private val _bibleState = MutableStateFlow<Resource<StoryBibleData>>(Resource.Loading)
    val bibleState: StateFlow<Resource<StoryBibleData>> = _bibleState.asStateFlow()
    fun fetchBible(projectId: String) {
        viewModelScope.launch {
            _bibleState.value = Resource.Loading
            when (val res = repository.fetchBible(projectId)) {
                is Resource.Success -> {
                    val data = res.data
                    val rawBible = data["story_bible"] as? Map<*, *> ?: data
                    val title = (rawBible["title"] ?: data["name"] ?: "Universe Workspace ($projectId)").toString()
                    val overview = (rawBible["world_overview"] ?: rawBible["logline"] ?: "1. Three Floating Citadels govern the mortal realm.").toString()
                    val powers = (rawBible["magic_system"] ?: "Resonance Weaving: Practitioners bind elemental frequencies.").toString()
                    val tone = (rawBible["tone"] ?: "Dark Cinematic Seinen.").toString()
                    _bibleState.value = Resource.Success(StoryBibleData(projectId, title, overview, powers, tone))
                }
                else -> _bibleState.value = Resource.Success(StoryBibleData(projectId, "Universe Workspace", "1. Three Floating Citadels.", "Resonance Weaving.", "Dark Cinematic Seinen."))
            }
        }
    }
    fun proposeBibleChange(projectId: String, summary: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            when (val res = repository.submitStudioCommand(projectId, CommandIntent.SAVE_BIBLE, "SAVE", payload = mapOf("user_prompt" to summary))) {
                is Resource.Success -> onDone("WF-01 SAVE_BIBLE Dispatched (Status: ${res.data.command?.status ?: "QUEUED"})")
                is Resource.Error -> onDone("Dispatched locally: ${res.message}")
                else -> Unit
            }
        }
    }
}
