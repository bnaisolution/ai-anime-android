package org.dpdns.bnai.anime.ui.publish

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.Resource

@Composable
fun PublishScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Publish Studio (WF-07)", style = MaterialTheme.typography.headlineLarge)
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Release Title") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description / Tags") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                scope.launch {
                    val res = repository.submitStudioCommand(
                        projectId = repository.currentProjectId(),
                        intent = CommandIntent.PUBLISH_NOW,
                        mode = "SAVE",
                        payload = mapOf(
                            "title" to title,
                            "description" to description
                        )
                    )
                    statusText = when (res) {
                        is Resource.Success -> "PUBLISH_NOW Command Dispatched (${res.data.command_id ?: res.data.status})"
                        is Resource.Error -> "Publish failed: ${res.message}"
                        else -> null
                    }
                }
            },
            enabled = title.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Dispatch PUBLISH_NOW (WF-07)")
        }
        statusText?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
    }
}
