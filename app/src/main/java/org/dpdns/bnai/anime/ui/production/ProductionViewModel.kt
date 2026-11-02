package org.dpdns.bnai.anime.ui.production

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.Resource

data class ProductionState(
    val targetId: String = "",
    val backendStatus: String = "IDLE",
    val isPolling: Boolean = false,
    val errorMessage: String? = null,
    val retryCount: Int = 0
)

class ProductionViewModel(
    private val repository: AnimeRepository = AnimeRepository()
) : ViewModel() {

    private val _productionState = MutableStateFlow(ProductionState())
    val productionState: StateFlow<ProductionState> = _productionState.asStateFlow()

    companion object {
        val NON_TERMINAL_STATES = setOf(
            "QUEUED", "DISPATCHED", "ACKNOWLEDGED", "RECONCILING", "RUNNING"
        )
        val TERMINAL_STATES = setOf(
            "SUCCEEDED", "FAILED", "MANUAL_REVIEW", "COMPLETED"
        )
    }

    // Patch 1: Called from repeatOnLifecycle(Lifecycle.State.STARTED)
    // - Poll interval: 4000ms (3-5s spec)
    // - Stops on terminal state
    // - Exponential backoff on network failure (3s, 6s, 12s)
    // - Max 150 iterations cap
    suspend fun pollWhileStarted(commandOrJobId: String) {
        val projectId = repository.currentProjectId()
        var consecutiveFailures = 0
        var pollIterations = 0
        val maxIterations = 150

        while (pollIterations < maxIterations) {
            pollIterations++
            _productionState.value = _productionState.value.copy(
                targetId = commandOrJobId,
                isPolling = true
            )

            val resolvedStatus: String? = if (commandOrJobId.length == 36) {
                when (val cmdRes = repository.fetchCommandStatus(commandOrJobId)) {
                    is Resource.Success -> {
                        consecutiveFailures = 0
                        cmdRes.data.status?.uppercase() ?: "QUEUED"
                    }
                    is Resource.Error -> {
                        consecutiveFailures++
                        null
                    }
                    else -> "QUEUED"
                }
            } else {
                when (val jobsRes = repository.fetchJobs(projectId)) {
                    is Resource.Success -> {
                        consecutiveFailures = 0
                        jobsRes.data.firstOrNull()?.status?.uppercase() ?: "IDLE"
                    }
                    is Resource.Error -> {
                        consecutiveFailures++
                        null
                    }
                    is Resource.Empty -> {
                        consecutiveFailures = 0
                        "IDLE"
                    }
                    else -> "IDLE"
                }
            }

            if (resolvedStatus == null) {
                if (consecutiveFailures >= 3) {
                    _productionState.value = ProductionState(
                        targetId = commandOrJobId,
                        backendStatus = "ERROR",
                        isPolling = false,
                        errorMessage = "Network failure after 3 retries. Tap Retry to poll again.",
                        retryCount = consecutiveFailures
                    )
                    return
                }
                val backoffMs = 3000L * (1 shl (consecutiveFailures - 1))
                delay(backoffMs)
                continue
            }

            _productionState.value = ProductionState(
                targetId = commandOrJobId,
                backendStatus = resolvedStatus,
                isPolling = resolvedStatus in NON_TERMINAL_STATES,
                errorMessage = if (resolvedStatus == "FAILED")
                    "Command/Job reported FAILED." else null,
                retryCount = consecutiveFailures
            )

            if (resolvedStatus in TERMINAL_STATES ||
                resolvedStatus !in NON_TERMINAL_STATES
            ) {
                return
            }

            delay(4000L)
        }

        _productionState.value = _productionState.value.copy(
            isPolling = false,
            errorMessage = "Polling paused due to inactivity timeout. Tap Retry to refresh."
        )
    }
}
