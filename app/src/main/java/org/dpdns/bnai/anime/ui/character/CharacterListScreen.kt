package org.dpdns.bnai.anime.ui.character

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import org.dpdns.bnai.anime.core.model.Character
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ResourceStateBox

@Composable
fun CharacterListScreen(
    onCharacterClick: (Character) -> Unit = {},
    onOpenDeepDive: (String) -> Unit = {}
) {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<Resource<List<Character>>>(Resource.Loading) }

    fun load() {
        scope.launch {
            state = Resource.Loading
            state = repository.fetchCharacters(repository.currentProjectId())
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Characters", style = MaterialTheme.typography.headlineLarge)
        ResourceStateBox(resource = state, onRetry = { load() }) { list ->
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { ch ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onCharacterClick(ch) }
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "${ch.name} (${ch.id})",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                "Role: ${ch.role} • Voice: ${ch.voiceId}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onCharacterClick(ch) }) {
                                    Text("Details & Voice Preview")
                                }
                                Button(onClick = { onOpenDeepDive(ch.id) }) {
                                    Text("Deep Dive (4 Tabs)")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
