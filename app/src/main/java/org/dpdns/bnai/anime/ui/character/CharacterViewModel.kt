package org.dpdns.bnai.anime.ui.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.model.Character
import org.dpdns.bnai.anime.core.network.*

class CharacterViewModel(private val repository: AnimeRepositoryApi) : ViewModel() {
    private val _charactersState = MutableStateFlow<Resource<List<Character>>>(Resource.Loading)
    val charactersState: StateFlow<Resource<List<Character>>> = _charactersState.asStateFlow()

    private val _voiceCatalog = MutableStateFlow<List<String>>(
        listOf("svara_hi_male_hero_01", "svara_hi_female_lead_01", "supertonic_ja_villain_01", "supertonic_en_narrator_01")
    )
    val voiceCatalog: StateFlow<List<String>> = _voiceCatalog.asStateFlow()

    fun loadCharacters(projectId: String = repository.currentProjectId()) {
        if (projectId.isBlank()) {
            _charactersState.value = Resource.Error("Please select an active project first.")
            return
        }
        viewModelScope.launch {
            _charactersState.value = Resource.Loading
            _charactersState.value = repository.fetchCharacters(projectId)
            // Fix #103: Fetch real Voice Catalog from /voices endpoint
            val voicesRes = repository.fetchCategoryItems(projectId, "voices")
            if (voicesRes is Resource.Success && voicesRes.data.isNotEmpty()) {
                val ids = voicesRes.data.mapNotNull {
                    (it["voice_id"] ?: it["id"] ?: it["name"])?.toString()
                }.distinct()
                if (ids.isNotEmpty()) _voiceCatalog.value = ids
            }
        }
    }

    fun previewVoice(characterId: String, voiceId: String, onResult: (String) -> Unit) {
        val projectId = repository.currentProjectId()
        viewModelScope.launch {
            val payload = mapOf("character_code" to characterId, "voice_id" to voiceId, "sample_text" to "This is my studio voice preview.")
            when (val res = repository.submitStudioCommand(projectId, CommandIntent.PREVIEW_VOICE, "PREVIEW", payload = payload)) {
                is Resource.Success -> onResult("Voice preview queued for $voiceId")
                is Resource.Error -> onResult("Voice preview failed: ${res.message}")
                else -> Unit
            }
        }
    }

    fun updateCharacterVoice(characterId: String, newVoiceId: String, onResult: (String) -> Unit) {
        val projectId = repository.currentProjectId()
        viewModelScope.launch {
            val payload = mapOf("character_code" to characterId, "voice_id" to newVoiceId)
            when (val res = repository.submitStudioCommand(projectId, CommandIntent.CHANGE_CHARACTER_VOICE, "SAVE", payload = payload)) {
                is Resource.Success -> {
                    onResult("Voice updated to $newVoiceId")
                    loadCharacters(projectId)
                }
                is Resource.Error -> onResult("Voice update failed: ${res.message}")
                else -> Unit
            }
        }
    }

    fun createOrGenerateCharacter(name: String, role: String, autoGenerateAi: Boolean, onResult: (String) -> Unit) {
        val projectId = repository.currentProjectId()
        viewModelScope.launch {
            val prompt = if (autoGenerateAi) {
                "Auto-generate anime character cast and visual DNA for project"
            } else {
                "Add character $name with role $role and trigger reference portrait"
            }
            when (val res = repository.submitStudioCommand(projectId, CommandIntent.SAVE_BIBLE, "SAVE", payload = mapOf("user_prompt" to prompt))) {
                is Resource.Success -> {
                    onResult("Character generation dispatched!")
                    loadCharacters(projectId)
                }
                is Resource.Error -> onResult("Failed: ${res.message}")
                else -> Unit
            }
        }
    }
}
