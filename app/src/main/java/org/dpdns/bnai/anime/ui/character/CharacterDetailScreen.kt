package org.dpdns.bnai.anime.ui.character

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.model.Character
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.Resource

@Composable
fun CharacterDetailScreen(
    characterId: String = "",
    initialCharacter: Character? = null,
    onOpenDeepDive: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { AnimeRepository() }
    var character by remember { mutableStateOf(initialCharacter) }
    var isLoading by remember { mutableStateOf(initialCharacter == null) }

    LaunchedEffect(characterId) {
        if (character == null) {
            isLoading = true
            val res = repository.fetchCharacters(repository.currentProjectId())
            if (res is Resource.Success) {
                character = res.data.find { it.id == characterId } ?: res.data.firstOrNull()
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (character == null) {
            Text(
                "Character not found in current project.",
                color = MaterialTheme.colorScheme.error
            )
        } else {
            val ch = character!!
            Text(ch.name, style = MaterialTheme.typography.headlineLarge)
            Text(
                "Code: ${ch.id} • Role: ${ch.role}",
                style = MaterialTheme.typography.bodyLarge
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Visual DNA", style = MaterialTheme.typography.headlineMedium)
                    Text(ch.visualDna, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Assigned Voice ID: ${ch.voiceId}",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            Button(
                onClick = {
                    scope.launch {
                        val res = repository.submitStudioCommand(
                            projectId = repository.currentProjectId(),
                            intent = CommandIntent.PREVIEW_VOICE,
                            mode = "QUICK",
                            payload = mapOf("character_code" to ch.id)
                        )
                        val msg = if (res is Resource.Success)
                            "WF-04 Voice Preview queued (${res.data.command_id ?: "OK"})"
                        else
                            "Voice preview request failed"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Preview Voice (WF-04)")
            }
            OutlinedButton(
                onClick = { onOpenDeepDive(ch.id) },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Open Character Deep Dive (Knowledge, Voice, Status, Ref History)")
            }
        }
    }
}
