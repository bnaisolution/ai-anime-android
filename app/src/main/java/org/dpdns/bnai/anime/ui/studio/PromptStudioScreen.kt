package org.dpdns.bnai.anime.ui.studio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PromptStudioScreen(viewModel: PromptStudioViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var title by remember(state.selectedPrompt) {
        mutableStateOf(state.selectedPrompt?.title ?: "")
    }
    var promptText by remember(state.selectedPrompt) {
        mutableStateOf(state.selectedPrompt?.prompt_text ?: "")
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("System Prompt Studio", style = MaterialTheme.typography.headlineLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ANIME", "REAL_HUMAN").forEach { medium ->
                FilterChip(
                    selected = state.selectedVisualMedium == medium,
                    onClick = { viewModel.setVisualMedium(medium) },
                    label = { Text(medium) }
                )
            }
        }
        state.statusMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.secondary)
        }
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Prompt Title") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = promptText,
            onValueChange = { promptText = it },
            label = { Text("System Prompt Instructions") },
            minLines = 6,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { viewModel.createAndActivatePrompt(title, promptText) },
            enabled = !state.isLoading && title.isNotBlank() && promptText.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(if (state.isLoading) "Saving..." else "Save & Set Active Prompt")
        }
    }
}
