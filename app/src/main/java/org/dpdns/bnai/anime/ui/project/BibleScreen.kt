package org.dpdns.bnai.anime.ui.project

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.InfoTooltipButton
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.ResourceStateBox

@Composable
fun BibleScreen(
    projectId: String = TokenManager.getInstance()?.getActiveProjectId().orEmpty(),
    viewModel: ProjectViewModel = viewModel()
) {
    val context = LocalContext.current
    val bibleResource by viewModel.bibleState.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Draft, 1: Universe, 2: History
    var universeTitle by remember { mutableStateOf("") }
    var worldRules by remember { mutableStateOf("") }
    var magicSystem by remember { mutableStateOf("") }
    var toneAndTheme by remember { mutableStateOf("") }

    var savedWorldRules by remember { mutableStateOf("") }
    var savedMagicSystem by remember { mutableStateOf("") }
    var savedTone by remember { mutableStateOf("") }

    var showChatBox by remember { mutableStateOf(false) }
    var aiChatPrompt by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    val isDirty = worldRules != savedWorldRules || magicSystem != savedMagicSystem || toneAndTheme != savedTone

    // Fix #27: Warn on back press if there are unsaved changes
    BackHandler(enabled = isDirty) {
        showUnsavedDialog = true
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Unsaved Story Bible Changes") },
            text = { Text("You have unsaved edits in your Story Bible draft. Discard changes?") },
            confirmButton = {
                Button(
                    onClick = {
                        worldRules = savedWorldRules
                        magicSystem = savedMagicSystem
                        toneAndTheme = savedTone
                        showUnsavedDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedDialog = false }) { Text("Keep Editing") }
            }
        )
    }

    LaunchedEffect(projectId) { viewModel.fetchBible(projectId) }

    LaunchedEffect(bibleResource) {
        if (bibleResource is Resource.Success) {
            val data = (bibleResource as Resource.Success<StoryBibleData>).data
            universeTitle = data.universeTitle
            worldRules = data.worldRules
            magicSystem = data.magicSystem
            toneAndTheme = data.toneAndTheme
            savedWorldRules = data.worldRules
            savedMagicSystem = data.magicSystem
            savedTone = data.toneAndTheme
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Fix #19 & #25: Project Selector + Auto-Generate Universe button
        ProjectSelectorBar(
            onProjectChanged = { newId -> viewModel.fetchBible(newId) },
            onRefreshClick = { viewModel.fetchBible(projectId) }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Story Bible", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Fix #24: Chat with AI toggle
                OutlinedButton(onClick = { showChatBox = !showChatBox }) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chat AI")
                }
                // Fix #25: Auto-Generate Universe
                Button(onClick = {
                    isSaving = true
                    viewModel.proposeBibleChange(projectId, "✨ Auto-Generate complete anime universe, rules, and power system") { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        isSaving = false
                        viewModel.fetchBible(projectId)
                    }
                }) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto-Generate")
                }
            }
        }

        if (showChatBox) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Discuss Story Bible with AI Director", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = aiChatPrompt,
                        onValueChange = { aiChatPrompt = it },
                        placeholder = { Text("e.g. Expand the forbidden bloodline rule...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (aiChatPrompt.isNotBlank()) {
                                viewModel.proposeBibleChange(projectId, aiChatPrompt) { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    aiChatPrompt = ""
                                    viewModel.fetchBible(projectId)
                                }
                            }
                        },
                        enabled = aiChatPrompt.isNotBlank()
                    ) { Text("Apply via AI") }
                }
            }
        }

        // Fix #23: 3 Tabs (Draft, Universe Preview, History)
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Draft") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Universe") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("History") })
        }

        ResourceStateBox(
            resource = bibleResource,
            emptyMessage = "No Story Bible saved yet. Tap 'Auto-Generate' or fill out the Draft tab.",
            onRetry = { viewModel.fetchBible(projectId) }
        ) { _ ->
            when (selectedTab) {
                0 -> {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Fix #20: (i) Info Tooltip Icons next to field labels
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("World Rules & Premise", style = MaterialTheme.typography.labelLarge)
                            InfoTooltipButton("World Rules", "Core premise, setting laws, eras, and immutable facts of your anime world.")
                        }
                        OutlinedTextField(value = worldRules, onValueChange = { worldRules = it }, minLines = 3, modifier = Modifier.fillMaxWidth())

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Magic / Power System", style = MaterialTheme.typography.labelLarge)
                            InfoTooltipButton("Power System", "Energy sources, combat tiers, elemental affinities, and drawbacks/costs.")
                        }
                        OutlinedTextField(value = magicSystem, onValueChange = { magicSystem = it }, minLines = 3, modifier = Modifier.fillMaxWidth())

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Tone & Visual Theme", style = MaterialTheme.typography.labelLarge)
                            InfoTooltipButton("Tone & Theme", "Emotional atmosphere, lighting mood, and storytelling pacing.")
                        }
                        OutlinedTextField(value = toneAndTheme, onValueChange = { toneAndTheme = it }, modifier = Modifier.fillMaxWidth())

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Fix #28: Reset button to clear/revert fields
                            OutlinedButton(
                                onClick = {
                                    worldRules = ""
                                    magicSystem = ""
                                    toneAndTheme = ""
                                },
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) { Text("Clear / Reset") }

                            // Fix #21 & #22: Disabled if no changes + CircularProgressIndicator + state retention
                            Button(
                                onClick = {
                                    isSaving = true
                                    val combined = "World: $worldRules | Magic: $magicSystem | Tone: $toneAndTheme"
                                    viewModel.proposeBibleChange(projectId, combined) { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        savedWorldRules = worldRules
                                        savedMagicSystem = magicSystem
                                        savedTone = toneAndTheme
                                        isSaving = false
                                    }
                                },
                                enabled = isDirty && !isSaving,
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Text("Save Bible")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
                1 -> {
                    // Fix #26: Universe Preview Tab with Entity Edit/Delete action icons
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(universeTitle.ifBlank { "Universe Overview" }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                    Row {
                                        IconButton(onClick = { selectedTab = 0 }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Universe")
                                        }
                                        IconButton(onClick = {
                                            worldRules = ""
                                            Toast.makeText(context, "Draft cleared. Save to apply.", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Clear")
                                        }
                                    }
                                }
                                Text("World Rules: ${worldRules.ifBlank { "Not defined yet" }}")
                                Text("Power System: ${magicSystem.ifBlank { "Not defined yet" }}")
                                Text("Tone: ${toneAndTheme.ifBlank { "Standard Anime" }}")
                            }
                        }
                    }
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Active Snapshot • Story Bible v1", fontWeight = FontWeight.Bold)
                                Text("Status: Synced with Canon Engine", color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
