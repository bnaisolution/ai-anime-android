package org.dpdns.bnai.anime.ui.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.KeyValueMapList
import org.dpdns.bnai.anime.ui.components.ResourceStateBox

@Composable
fun WorkerManagerScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var workersState by remember {
        mutableStateOf<Resource<List<JsonMap>>>(Resource.Loading)
    }
    var actionBanner by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            workersState = Resource.Loading
            workersState = repository.fetchAdminWorkers(repository.currentProjectId())
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Worker Fleet Manager (WF-04)", style = MaterialTheme.typography.headlineLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        val res = repository.submitStudioCommand(
                            repository.currentProjectId(),
                            CommandIntent.ADMIN_START_KAGGLE,
                            "QUICK"
                        )
                        actionBanner = if (res is Resource.Success)
                            "Start Kaggle command queued (${res.data.command_id ?: "OK"})"
                        else
                            "Start command failed"
                        load()
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text("Start Kaggle Worker") }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        val res = repository.submitStudioCommand(
                            repository.currentProjectId(),
                            CommandIntent.ADMIN_STOP_KAGGLE,
                            "QUICK"
                        )
                        actionBanner = if (res is Resource.Success)
                            "Stop Kaggle command queued (${res.data.command_id ?: "OK"})"
                        else
                            "Stop command failed"
                        load()
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text("Stop Worker") }
        }
        actionBanner?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
        ResourceStateBox(
            resource = workersState,
            emptyMessage = "No active worker heartbeats registered in the last 180 seconds for this project.",
            onRetry = { load() }
        ) { list ->
            KeyValueMapList(list)
        }
    }
}
