package org.dpdns.bnai.anime.ui.production
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.RetrofitClient
enum class ProductionStage(val displayTitle: String, val detailMessage: String) {
    Queued("Queued", "Job registered in WF-10..."),
    Scripting("Scripting", "WF-02 generating screenplay..."),
    Rendering("Rendering", "Rendering Scene 3..."),
    QC("QC", "Running WF-11 Automated Quality Gate..."),
    Completed("Completed", "Master MP4 verified."),
    Failed("Failed", "Worker timeout.")
}
data class ProductionState(val episodeId: String = "ep_005", val currentStage: ProductionStage = ProductionStage.Queued, val errorMessage: String? = null)
class ProductionViewModel(private val repository: AnimeRepository = AnimeRepository()) : ViewModel() {
    private val _productionState = MutableStateFlow(ProductionState())
    val productionState: StateFlow<ProductionState> = _productionState.asStateFlow()
    private var pollingJob: Job? = null
    fun startPolling(episodeId: String) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            val api = RetrofitClient.getApiService()
            val sequence = listOf(ProductionStage.Queued, ProductionStage.Scripting, ProductionStage.Rendering, ProductionStage.QC, ProductionStage.Completed)
            for (stage in sequence) {
                if (episodeId.length == 36) {
                    try {
                        val cmdRes = api.getCommandStatus(episodeId)
                        val backendStatus = cmdRes.body()?.data?.status?.uppercase()
                        if (backendStatus == "FAILED") { triggerSimulatedFailure(episodeId); return@launch }
                        else if (backendStatus == "SUCCEEDED") { _productionState.value = ProductionState(episodeId, ProductionStage.Completed, null); return@launch }
                    } catch (_: Exception) {}
                }
                _productionState.value = ProductionState(episodeId, stage, null)
                if (stage != ProductionStage.Completed) delay(4000)
            }
        }
    }
    fun triggerSimulatedFailure(episodeId: String) {
        pollingJob?.cancel()
        _productionState.value = ProductionState(episodeId, ProductionStage.Failed, ProductionStage.Failed.detailMessage)
    }
}
