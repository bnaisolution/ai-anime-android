package org.dpdns.bnai.anime.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun QuickToolsScreen(onOpenProductionPoll: (String) -> Unit = {}) {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    var lastCommandId by remember { mutableStateOf<String?>(null) }
    var lastStatus by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Quick Media Generator", style = MaterialTheme.typography.headlineLarge)
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Visual Prompt (Anime Scene / Character Shot)") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    isRunning = true
                    errorMsg = null
                    scope.launch {
                        when (
                            val res = repository.triggerQuickMedia(
                                projectId = repository.currentProjectId(),
                                isVideo = false,
                                prompt = prompt.trim()
                            )
                        ) {
                            is Resource.Success -> {
                                lastCommandId = res.data.command_id
                                    ?: res.data.command?.id
                                lastStatus = res.data.status ?: "QUEUED"
                            }
                            is Resource.Error -> errorMsg = res.message
                            else -> Unit
                        }
                        isRunning = false
                    }
                },
                enabled = !isRunning && prompt.isNotBlank(),
                modifier = Modifier.weight(1f).height(50.dp)
            ) { Text("Quick Image") }

            Button(
                onClick = {
                    isRunning = true
                    errorMsg = null
                    scope.launch {
                        when (
                            val res = repository.triggerQuickMedia(
                                projectId = repository.currentProjectId(),
                                isVideo = true,
                                prompt = prompt.trim()
                            )
                        ) {
                            is Resource.Success -> {
                                lastCommandId = res.data.command_id
                                    ?: res.data.command?.id
                                lastStatus = res.data.status ?: "QUEUED"
                            }
                            is Resource.Error -> errorMsg = res.message
                            else -> Unit
                        }
                        isRunning = false
                    }
                },
                enabled = !isRunning && prompt.isNotBlank(),
                modifier = Modifier.weight(1f).height(50.dp)
            ) { Text("Quick Video (5s)") }
        }

        errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        lastCommandId?.let { cmdId ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Command ID: $cmdId",
                            style = MaterialTheme.typography.labelLarge
                        )
                        StatusBadge(lastStatus ?: "QUEUED")
                    }
                    Button(
                        onClick = { onOpenProductionPoll(cmdId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Track Live Status in Production Monitor")
                    }
                }
            }
        }
    }
}
