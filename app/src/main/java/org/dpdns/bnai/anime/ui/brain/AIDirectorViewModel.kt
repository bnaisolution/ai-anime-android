package org.dpdns.bnai.anime.ui.brain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.auth.PersistedChatMessage
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.network.AnimeRepositoryApi
import org.dpdns.bnai.anime.core.network.DirectorActionDto
import org.dpdns.bnai.anime.core.network.DirectorCapabilityDto
import org.dpdns.bnai.anime.core.network.DirectorContextDto
import org.dpdns.bnai.anime.core.network.DirectorReadinessDto
import org.dpdns.bnai.anime.core.network.DirectorTimelineDto
import org.dpdns.bnai.anime.core.network.DirectorPlanDto
import org.dpdns.bnai.anime.core.network.RequestedDirectorActionDto
import org.dpdns.bnai.anime.core.network.Resource

data class AIDirectorUiState(
    val projectId: String = "",
    val providerBadge: String = "BYOD: checking",
    val messages: List<PersistedChatMessage> = emptyList(),
    val input: String = "",
    val isSending: Boolean = false,
    val activeCommandId: String? = null,
    val activeCommandStatus: String? = null,
    val actions: List<DirectorActionDto> = emptyList(),
    val context: DirectorContextDto? = null,
    val capabilities: List<DirectorCapabilityDto> = emptyList(),
    val readiness: DirectorReadinessDto? = null,
    val timeline: DirectorTimelineDto? = null,
    val plan: DirectorPlanDto? = null,
    val isPlanning: Boolean = false,
    val error: String? = null
)

class AIDirectorViewModel(
    private val repository: AnimeRepositoryApi,
    private val tokenManager: TokenManager
) : ViewModel() {
    private val _state = MutableStateFlow(AIDirectorUiState())
    val state: StateFlow<AIDirectorUiState> = _state.asStateFlow()

    private var pollJob: Job? = null
    private var timelineJob: Job? = null

    init {
        setProject(repository.currentProjectId())
    }

    fun setProject(projectId: String) {
        pollJob?.cancel()
        val pid = projectId.ifBlank { "global_studio" }
        _state.update {
            it.copy(
                projectId = projectId,
                messages = tokenManager.getProjectChatHistory(pid),
                input = "",
                activeCommandId = tokenManager.getActiveCommandId(pid, "ai_director"),
                activeCommandStatus = null,
                actions = emptyList(),
                context = null,
                capabilities = emptyList(),
                readiness = null,
                timeline = null,
                plan = null,
                isPlanning = false,
                error = null
            )
        }
        viewModelScope.launch {
            val badge = repository.resolveConfiguredAiProviderBadge()
            _state.update { it.copy(providerBadge = badge) }
        }
        if (projectId.isNotBlank()) {
            viewModelScope.launch {
                when (val res = repository.loadDirectorReadiness(projectId)) {
                    is Resource.Success -> _state.update { it.copy(readiness = res.data) }
                    is Resource.Error -> _state.update { it.copy(readiness = null) }
                    else -> Unit
                }
            }
        }
        viewModelScope.launch {
            when (val res = repository.loadDirectorCapabilities()) {
                is Resource.Success -> _state.update { it.copy(capabilities = res.data.capabilities) }
                else -> Unit
            }
        }
        tokenManager.getActiveCommandId(pid, "ai_director")?.let(::pollCommand)
        if (projectId.isNotBlank()) refreshTimeline()
    }

    fun refreshTimeline(commandId: String? = _state.value.activeCommandId) {
        val pid = _state.value.projectId
        if (pid.isBlank()) return
        timelineJob?.cancel()
        timelineJob = viewModelScope.launch {
            when (val res = repository.loadDirectorTimeline(pid, commandId)) {
                is Resource.Success -> _state.update { it.copy(timeline = res.data) }
                else -> Unit
            }
        }
    }

    fun requestPlan(goal: String? = null) {
        val current = _state.value
        if (current.projectId.isBlank() || current.isPlanning) return
        _state.update { it.copy(isPlanning = true, error = null) }
        viewModelScope.launch {
            when (val res = repository.buildDirectorPlan(current.projectId, goal)) {
                is Resource.Success -> {
                    val readyActions = res.data.steps.mapNotNull { it.action }
                    _state.update {
                        it.copy(
                            isPlanning = false,
                            plan = res.data,
                            actions = if (readyActions.isEmpty()) it.actions else readyActions,
                            readiness = it.readiness?.copy(overall = res.data.overall)
                        )
                    }
                }
                is Resource.Error -> _state.update { it.copy(isPlanning = false, error = res.message) }
                else -> _state.update { it.copy(isPlanning = false) }
            }
        }
    }

    fun clearPlan() {
        _state.update { it.copy(plan = null) }
    }

    fun updateInput(value: String) {
        _state.update { it.copy(input = value) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun sendMessage(text: String = _state.value.input, currentScreen: String = "AI_BRAIN") {
        val clean = text.trim()
        val current = _state.value
        if (clean.isEmpty() || current.isSending || current.projectId.isBlank()) return
        val pid = current.projectId
        val history = tokenManager.appendProjectChatMessage(
            pid,
            PersistedChatMessage(role = "user", content = clean)
        )
        _state.update {
            it.copy(
                input = "",
                messages = history,
                isSending = true,
                actions = emptyList(),
                error = null
            )
        }

        viewModelScope.launch {
            val conversationId = tokenManager.getProjectConversationId(pid)
            val target = current.context?.target
            val seasonNo = intValue(target?.get("seasonNo") ?: target?.get("season_no"))
            val episodeNo = intValue(target?.get("episodeNo") ?: target?.get("episode_no"))
            val targetSec = intValue(target?.get("targetDurationSec") ?: target?.get("target_duration_sec"))
            when (val res = repository.askAiDirector(
                message = clean,
                currentScreen = currentScreen,
                conversationId = conversationId,
                projectId = pid,
                seasonNo = seasonNo,
                episodeNo = episodeNo,
                targetDurationSec = targetSec
            )) {
                is Resource.Success -> {
                    res.data.conversation_id?.takeIf { it.isNotBlank() }?.let {
                        tokenManager.saveProjectConversationId(pid, it)
                    }
                    val replyText = res.data.reply ?: res.data.message ?: "Done."
                    val updated = tokenManager.appendProjectChatMessage(
                        pid,
                        PersistedChatMessage(role = "assistant", content = replyText)
                    )
                    _state.update {
                        it.copy(
                            messages = updated,
                            isSending = false,
                            actions = res.data.actions,
                            context = res.data.context ?: it.context,
                            providerBadge = res.data.provider?.let { provider -> "BYOD: $provider" } ?: it.providerBadge
                        )
                    }
                }
                is Resource.Error -> {
                    val updated = tokenManager.appendProjectChatMessage(
                        pid,
                        PersistedChatMessage(role = "assistant", content = "**Error:** ${res.message}")
                    )
                    _state.update {
                        it.copy(messages = updated, isSending = false, error = res.message)
                    }
                }
                else -> _state.update { it.copy(isSending = false) }
            }
        }
    }

    fun proposeAction(intent: String, currentScreen: String = "AI_BRAIN") {
        val current = _state.value
        if (current.isSending) return
        val prompt = "Prepare this production action for my confirmation: ${intent.replace('_', ' ').lowercase()}."
        val pid = current.projectId
        val history = tokenManager.appendProjectChatMessage(
            pid,
            PersistedChatMessage(role = "user", content = prompt)
        )
        _state.update { it.copy(input = "", messages = history, isSending = true, actions = emptyList(), error = null) }
        viewModelScope.launch {
            val target = current.context?.target
            val seasonNo = intValue(target?.get("seasonNo") ?: target?.get("season_no"))
            val episodeNo = intValue(target?.get("episodeNo") ?: target?.get("episode_no"))
            val targetSec = intValue(target?.get("targetDurationSec") ?: target?.get("target_duration_sec"))
            val res = repository.askAiDirector(
                message = prompt,
                currentScreen = currentScreen,
                conversationId = tokenManager.getProjectConversationId(pid),
                projectId = pid,
                seasonNo = seasonNo,
                episodeNo = episodeNo,
                targetDurationSec = targetSec,
                requestedAction = RequestedDirectorActionDto(
                    intent = intent,
                    season_no = seasonNo,
                    episode_no = episodeNo,
                    target_duration_sec = targetSec
                )
            )
            if (res is Resource.Success) {
                res.data.conversation_id?.takeIf { it.isNotBlank() }?.let {
                    tokenManager.saveProjectConversationId(pid, it)
                }
                val replyText = res.data.reply ?: res.data.message ?: "Action prepared."
                val updated = tokenManager.appendProjectChatMessage(
                    pid,
                    PersistedChatMessage(role = "assistant", content = replyText)
                )
                _state.update {
                    it.copy(
                        messages = updated,
                        isSending = false,
                        actions = res.data.actions,
                        context = res.data.context ?: it.context
                    )
                }
            } else if (res is Resource.Error) {
                _state.update { it.copy(isSending = false, error = res.message) }
            } else {
                _state.update { it.copy(isSending = false) }
            }
        }
    }

    fun confirmAction(action: DirectorActionDto) {
        val current = _state.value
        if (action.action_id.isBlank() || current.projectId.isBlank()) return
        _state.update { it.copy(isSending = true, error = null) }
        viewModelScope.launch {
            when (val res = repository.executeDirectorAction(current.projectId, action.action_id)) {
                is Resource.Success -> {
                    val commandId = res.data.command_id.orEmpty()
                    if (commandId.isNotBlank()) {
                        tokenManager.saveActiveCommandId(current.projectId, "ai_director", commandId)
                        _state.update {
                            it.copy(
                                isSending = false,
                                actions = emptyList(),
                                activeCommandId = commandId,
                                activeCommandStatus = res.data.status ?: "QUEUED"
                            )
                        }
                        pollCommand(commandId)
                        refreshTimeline(commandId)
                    } else {
                        _state.update { it.copy(isSending = false, error = "Action accepted without a command id.") }
                    }
                }
                is Resource.Error -> _state.update { it.copy(isSending = false, error = res.message) }
                else -> _state.update { it.copy(isSending = false) }
            }
        }
    }

    fun clearConversation() {
        val pid = _state.value.projectId
        pollJob?.cancel()
        tokenManager.clearProjectChatHistory(pid)
        tokenManager.saveActiveCommandId(pid, "ai_director", null)
        tokenManager.saveProjectConversationId(pid, null)
        _state.update {
            it.copy(
                messages = emptyList(),
                actions = emptyList(),
                activeCommandId = null,
                activeCommandStatus = null,
                timeline = null,
                plan = null,
                error = null
            )
        }
        viewModelScope.launch { repository.clearDirectorConversation(pid) }
    }

    private fun pollCommand(commandId: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            var waitMs = 1500L
            val terminal = setOf("SUCCEEDED", "COMPLETED", "FAILED", "ERROR", "CANCELLED", "DEAD_LETTER", "MANUAL_REVIEW")
            while (true) {
                when (val res = repository.fetchCommandStatus(commandId)) {
                    is Resource.Success -> {
                        val status = res.data.status?.uppercase() ?: "QUEUED"
                        _state.update { it.copy(activeCommandStatus = status) }
                        refreshTimeline(commandId)
                        if (status in terminal) {
                            tokenManager.saveActiveCommandId(_state.value.projectId, "ai_director", null)
                            _state.update { it.copy(activeCommandId = null) }
                            break
                        }
                    }
                    is Resource.Error -> {
                        _state.update { it.copy(error = "Command status unavailable: ${res.message}") }
                        break
                    }
                    else -> Unit
                }
                delay(waitMs)
                waitMs = (waitMs * 2).coerceAtMost(15_000L)
            }
        }
    }

    private fun intValue(value: Any?): Int? = when (value) {
        is Int -> value
        is Long -> value.toInt()
        is Double -> value.toInt()
        is Float -> value.toInt()
        is String -> value.toIntOrNull()
        else -> null
    }
}
