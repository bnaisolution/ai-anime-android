package org.dpdns.bnai.anime.ui.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.Resource

data class StoryBibleData(
    val projectId: String,
    val universeTitle: String,
    val worldRules: String,
    val magicSystem: String,
    val toneAndTheme: String
)

class ProjectViewModel : ViewModel() {

    private val _bibleState = MutableStateFlow<Resource<StoryBibleData>>(Resource.Loading)
    val bibleState: StateFlow<Resource<StoryBibleData>> = _bibleState.asStateFlow()

    fun fetchBible(projectId: String) {
        viewModelScope.launch {
            _bibleState.value = Resource.Loading
            delay(400)
            val bible = StoryBibleData(
                projectId = projectId,
                universeTitle = "Universe Workspace ($projectId)",
                worldRules = "1. Three Floating Citadels govern the mortal realm.\n" +
                    "2. No mortal may cross the Obsidian Rift without a Soul Anchor.\n" +
                    "3. Time flows twice as fast inside the Astral Sanctum.",
                magicSystem = "Resonance Weaving: Practitioners bind elemental frequencies to ancestral weapons. " +
                    "Overuse causes crystalline corruption of the nervous system (Rule 47 Power Cap enforced).",
                toneAndTheme = "Tone: Dark Cinematic Seinen, High-Stakes Political Intrigue.\n" +
                    "Theme: Memory vs. Destiny, and the cost of rewriting immutable Canon."
            )
            _bibleState.value = Resource.Success(bible)
        }
    }
}
