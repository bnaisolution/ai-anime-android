package org.dpdns.bnai.anime.ui.studio

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptStudioScreen(
    viewModel: PromptStudioViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    val mediumOptions = listOf(
        "ANIME" to "Anime (2D Cel-Shaded)",
        "REAL_HUMAN" to "Real Human (Cinematic 8K)"
    )
    var mediumDropdownExpanded by remember { mutableStateOf(false) }
    var promptDropdownExpanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    var newTitle by remember { mutableStateOf("") }
    var newPromptBody by remember { mutableStateOf("") }
    var editablePromptText by remember(state.selectedPrompt) {
        mutableStateOf(state.selectedPrompt?.prompt_text ?: "")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Dynamic System Prompt Studio",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1. Select Visual Medium",
                        style = MaterialTheme.typography.titleMedium
                    )

                    ExposedDropdownMenuBox(
                        expanded = mediumDropdownExpanded,
                        onExpandedChange = { mediumDropdownExpanded = !mediumDropdownExpanded }
                    ) {
                        val currentLabel = mediumOptions.firstOrNull { it.first == state.selectedVisualMedium }?.second
                            ?: "Anime (2D Cel-Shaded)"
                        OutlinedTextField(
                            value = currentLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Visual Medium (Anime vs Real Human)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mediumDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = mediumDropdownExpanded,
                            onDismissRequest = { mediumDropdownExpanded = false }
                        ) {
                            mediumOptions.forEach { (code, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        viewModel.setVisualMedium(code)
                                        mediumDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Text(
                        text = "2. Select or Add Story System Prompt",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = promptDropdownExpanded,
                            onExpandedChange = { promptDropdownExpanded = !promptDropdownExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = state.selectedPrompt?.title ?: "Tap + to add custom genre prompt",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Story Persona / Genre Title") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = promptDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = promptDropdownExpanded,
                                onDismissRequest = { promptDropdownExpanded = false }
                            ) {
                                state.prompts.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text("${item.title} [${item.visual_medium}]") },
                                        onClick = {
                                            viewModel.selectPrompt(item)
                                            editablePromptText = item.prompt_text
                                            promptDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        FilledIconButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add New System Prompt")
                        }
                    }

                    OutlinedTextField(
                        value = editablePromptText,
                        onValueChange = { editablePromptText = it },
                        label = { Text("Active System Instructions for AI (WF-01 & WF-02)") },
                        minLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val titleToSave = state.selectedPrompt?.title ?: "Custom Story Prompt"
                            if (editablePromptText.isNotBlank()) {
                                viewModel.createAndActivatePrompt(titleToSave, editablePromptText)
                            }
                        },
                        enabled = !state.isLoading && editablePromptText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.isLoading) "Saving..." else "Save & Activate For Current Project")
                    }

                    state.statusMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New Genre / System Prompt") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Title (e.g. Comedy Horror)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPromptBody,
                        onValueChange = { newPromptBody = it },
                        label = { Text("System Prompt Instructions") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank() && newPromptBody.isNotBlank()) {
                            viewModel.createAndActivatePrompt(newTitle, newPromptBody)
                            newTitle = ""
                            newPromptBody = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Add & Activate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
