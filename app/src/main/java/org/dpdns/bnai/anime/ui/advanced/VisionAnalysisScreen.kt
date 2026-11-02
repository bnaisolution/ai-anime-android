package org.dpdns.bnai.anime.ui.advanced

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
import androidx.compose.material3.OutlinedButton
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
import org.dpdns.bnai.anime.core.network.ApiCommandRecordDto
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun VisionAnalysisScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var mediaUrl by remember { mutableStateOf("") }
    var prompt by remember {
        mutableStateOf("Extract 11-field Visual DNA and character consistency anchors")
    }
    var submissionState by remember {
        mutableStateOf<Resource<ApiCommandRecordDto>?>(null)
    }
    var lastCommandId by remember { mutableStateOf<String?>(null) }

    fun pollCommand(cmdId: String) {
        scope.launch {
            submissionState = Resource.Loading
            submissionState = repository.fetchCommandStatus(cmdId)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Vision DNA Analyzer (WF-05)", style = MaterialTheme.typography.headlineLarge)
        OutlinedTextField(
            value = mediaUrl,
            onValueChange = { mediaUrl = it },
            label = { Text("Reference Image / Frame URL") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Analysis Instruction") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                scope.launch {
                    submissionState = Resource.Loading
                    when (
                        val res = repository.triggerVisionAnalysis(
                            repository.currentProjectId(),
                            mediaUrl.trim(),
                            prompt.trim()
                        )
                    ) {
                        is Resource.Success -> {
                            val cmdId = res.data.command_id ?: res.data.command?.id
                            lastCommandId = cmdId
                            if (cmdId != null) pollCommand(cmdId)
                        }
                        is Resource.Error -> {
                            submissionState = Resource.Error(res.message, res.code)
                        }
                        else -> Unit
                    }
                }
            },
            enabled = mediaUrl.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Dispatch WF-05 Vision Analysis")
        }

        submissionState?.let { st ->
            ResourceStateBox(
                resource = st,
                onRetry = { lastCommandId?.let { pollCommand(it) } }
            ) { record ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Command: ${record.id}",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            StatusBadge(record.status ?: "QUEUED")
                        }
                        record.result?.let { Text("Result: $it") }
                        record.error_message?.let {
                            Text("Error: $it", color = MaterialTheme.colorScheme.error)
                        }
                        OutlinedButton(onClick = { pollCommand(record.id) }) {
                            Text("Refresh Result")
                        }
                    }
                }
            }
        }
    }
}
