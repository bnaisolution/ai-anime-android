package org.dpdns.bnai.anime.ui.production
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
enum class ProductionStage(val title: String) { Queued("Queued"), Scripting("Scripting"), Rendering("Rendering"), Completed("Completed") }
data class ProductionState(val stage: ProductionStage = ProductionStage.Queued)
class ProductionViewModel : ViewModel() {
    private val _state = MutableStateFlow(ProductionState())
    val state: StateFlow<ProductionState> = _state
    fun start() {
        viewModelScope.launch {
            for (s in listOf(ProductionStage.Queued, ProductionStage.Scripting, ProductionStage.Rendering, ProductionStage.Completed)) {
                _state.value = ProductionState(s); if (s != ProductionStage.Completed) delay(3000)
            }
        }
    }
}
