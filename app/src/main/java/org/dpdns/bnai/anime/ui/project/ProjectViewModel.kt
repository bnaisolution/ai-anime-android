package org.dpdns.bnai.anime.ui.project
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.Resource
data class StoryBibleData(val projectId: String, val universeTitle: String, val worldRules: String, val magicSystem: String, val toneAndTheme: String)
class ProjectViewModel : ViewModel() {
    private val _bibleState = MutableStateFlow<Resource<StoryBibleData>>(Resource.Loading)
    val bibleState: StateFlow<Resource<StoryBibleData>> = _bibleState
    fun fetchBible(projectId: String) {
        viewModelScope.launch { _bibleState.value = Resource.Loading; delay(400); _bibleState.value = Resource.Success(StoryBibleData(projectId, "Universe", "1. Three Floating Citadels\n2. No mortal may cross Rift", "Resonance Weaving", "Dark Cinematic Seinen")) }
    }
}
