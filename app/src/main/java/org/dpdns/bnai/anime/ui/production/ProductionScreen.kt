package org.dpdns.bnai.anime.ui.production

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun ProductionScreen(
    episodeId: String = "",
    onViewVideoClick: () -> Unit = {},
    onOpenSceneInspector: () -> Unit = {},
    onOpenMediaLibrary: () -> Unit = {},
    viewModel: ProductionViewModel = viewModel()
) {
    val state by viewModel.productionState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var pollTrigger by remember { mutableStateOf(0) }

    // Patch 1: Lifecycle-aware polling — pauses in background
    LaunchedEffect(episodeId, pollTrigger, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.pollWhileStarted(episodeId)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Production Command Center (WF-03)", style = MaterialTheme.typography.headlineLarge)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Target Command / Job ID: ${episodeId.ifBlank { "Latest Project Job" }}")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Live Status:", style = MaterialTheme.typography.headlineMedium)
                    StatusBadge(state.backendStatus)
                }
                if (state.isPolling) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Polling backend every 4s (pauses automatically in background)...")
                }
                state.errorMessage?.let { err ->
                    Text(err, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { pollTrigger++ },
                modifier = Modifier.weight(1f)
            ) { Text("Refresh / Retry Poll") }

            OutlinedButton(
                onClick = onOpenSceneInspector,
                modifier = Modifier.weight(1f)
            ) { Text("Scene Inspector") }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onOpenMediaLibrary,
                modifier = Modifier.weight(1f)
            ) { Text("Media Library") }

            Button(
                onClick = onViewVideoClick,
                modifier = Modifier.weight(1f)
            ) { Text("QC & Approvals") }
        }
    }
}
