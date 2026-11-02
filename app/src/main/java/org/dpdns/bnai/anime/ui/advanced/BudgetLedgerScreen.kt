package org.dpdns.bnai.anime.ui.advanced

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ResourceStateBox

@Composable
fun BudgetLedgerScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var ledgerState by remember {
        mutableStateOf<Resource<JsonMap>>(Resource.Loading)
    }

    fun load() {
        scope.launch {
            ledgerState = Resource.Loading
            ledgerState = repository.fetchLedgerSummary(repository.currentProjectId())
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Budget & Resource Ledger",
            style = MaterialTheme.typography.headlineLarge
        )
        ResourceStateBox(resource = ledgerState, onRetry = { load() }) { data ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Active Project Cost Ledger",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text("Reserved Cost: ${data["reserved_cost"] ?: 0}")
                    Text("Consumed Cost: ${data["consumed_cost"] ?: 0}")
                    Text("Total Committed Cost: ${data["total_committed_cost"] ?: 0}")
                }
            }
        }
    }
}
