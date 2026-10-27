package org.dpdns.bnai.anime.ui.production

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ProductionStage(val displayTitle: String, val detailMessage: String) {
    Queued("Queued", "Job registered in WF-10 State Machine & Admission Control..."),
    Scripting("Scripting", "WF-02 generating Hindi/Hinglish screenplay & Section 14A flags..."),
    Rendering("Rendering", "Rendering Scene 3... (SDXL Lightning + Svara TTS + MuseTalk)"),
    QC("QC", "Running WF-11 Automated Quality & Audio/Video Sync Gate..."),
    Completed("Completed", "Master MP4 verified and ready for review & delivery."),
    Failed("Failed", "Worker timeout on Scene 4 GPU dispatch. Ready for WF-13 stage retry.")
}

data class ProductionState(
    val episodeId: String = "ep_005",
    val currentStage: ProductionStage = ProductionStage.Queued,
    val errorMessage: String? = null
)

class ProductionViewModel : ViewModel() {

    private val _productionState = MutableStateFlow(ProductionState())
    val productionState: StateFlow<ProductionState> = _productionState.asStateFlow()

    private var pollingJob: Job? = null

    fun startPolling(episodeId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            val sequence = listOf(
                ProductionStage.Queued,
                ProductionStage.Scripting,
                ProductionStage.Rendering,
                ProductionStage.QC,
                ProductionStage.Completed
            )
            for (stage in sequence) {
                _productionState.value = ProductionState(
                    episodeId = episodeId,
                    currentStage = stage,
                    errorMessage = null
                )
                if (stage != ProductionStage.Completed) {
                    delay(5000)
                }
            }
        }
    }

    fun triggerSimulatedFailure(episodeId: String) {
        pollingJob?.cancel()
        _productionState.value = ProductionState(
            episodeId = episodeId,
            currentStage = ProductionStage.Failed,
            errorMessage = ProductionStage.Failed.detailMessage
        )
    }
}
