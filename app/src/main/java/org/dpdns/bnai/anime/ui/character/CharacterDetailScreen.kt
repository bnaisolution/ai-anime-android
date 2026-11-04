package org.dpdns.bnai.anime.ui.character

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dpdns.bnai.anime.core.model.Character
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun CharacterDetailScreen(
    characterId: String = "",
    initialCharacter: Character? = null,
    onOpenDeepDive: () -> Unit = {},
    viewModel: CharacterViewModel = viewModel()
) {
    val context = LocalContext.current
    val voices by viewModel.voiceCatalog.collectAsState()
    var selectedProvider by remember { mutableStateOf("Svara TTS") }
    var selectedVoice by remember { mutableStateOf(initialCharacter?.voiceId ?: voices.firstOrNull().orEmpty()) }
    var providerExpanded by remember { mutableStateOf(false) }
    var voiceExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadCharacters()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(initialCharacter?.name ?: characterId.ifBlank { "Character Profile" }, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("Role: ${initialCharacter?.role ?: "Main Cast"}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
            }
            StatusBadge(initialCharacter?.status?.uppercase() ?: "ALIVE")
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Visual DNA Anchor", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(initialCharacter?.visualDna ?: "11-field Visual DNA locked in Canon.")
            }
        }

        // Fix #36 & #103: Voice Provider (Svara, Supertonic) & Real Voice ID Dropdowns
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Voice Configuration", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

                Box {
                    OutlinedButton(onClick = { providerExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Voice Engine: $selectedProvider")
                    }
                    DropdownMenu(expanded = providerExpanded, onDismissRequest = { providerExpanded = false }) {
                        listOf("Svara TTS", "Supertonic TTS", "Edge TTS Fallback").forEach { prov ->
                            DropdownMenuItem(
                                text = { Text(prov) },
                                onClick = {
                                    selectedProvider = prov
                                    providerExpanded = false
                                }
                            )
                        }
                    }
                }

                Box {
                    OutlinedButton(onClick = { voiceExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Voice Model ID: ${selectedVoice.ifBlank { "Select Voice" }}")
                    }
                    DropdownMenu(expanded = voiceExpanded, onDismissRequest = { voiceExpanded = false }) {
                        voices.forEach { vId ->
                            DropdownMenuItem(
                                text = { Text(vId) },
                                onClick = {
                                    selectedVoice = vId
                                    voiceExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            viewModel.previewVoice(initialCharacter?.id ?: characterId, selectedVoice) { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Preview Voice")
                    }
                    Button(
                        onClick = {
                            viewModel.updateCharacterVoice(initialCharacter?.id ?: characterId, selectedVoice) { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Voice")
                    }
                }
            }
        }

        Button(onClick = onOpenDeepDive, modifier = Modifier.fillMaxWidth()) {
            Text("Open Character Deep Dive (11-Field DNA, Arc, Powers & Relations)")
        }
    }
}
