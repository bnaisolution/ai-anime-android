package org.dpdns.bnai.anime.ui.advanced

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.KeyValueMapList
import org.dpdns.bnai.anime.ui.components.ResourceStateBox

@Composable
fun ContinuityCheckerScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var continuityState by remember {
        mutableStateOf<Resource<List<JsonMap>>>(Resource.Loading)
    }
    var banner by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            continuityState = Resource.Loading
            continuityState = repository.fetchContinuity(repository.currentProjectId())
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Continuity Checker (WF-06)",
                style = MaterialTheme.typography.headlineLarge
            )
            Button(onClick = {
                scope.launch {
                    val res = repository.submitStudioCommand(
                        repository.currentProjectId(),
                        CommandIntent.CHECK_CONTINUITY,
                        "SAVE"
                    )
                    banner = if (res is Resource.Success)
                        "WF-06 Continuity check dispatched (${res.data.command_id ?: "QUEUED"})"
                    else
                        "Dispatch failed"
                }
            }) { Text("Run Check") }
        }
        banner?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
        ResourceStateBox(
            resource = continuityState,
            emptyMessage = "No executed continuity check records found. Unexecuted checks are never labeled as passed.",
            onRetry = { load() }
        ) { list ->
            KeyValueMapList(list)
        }
    }
}
