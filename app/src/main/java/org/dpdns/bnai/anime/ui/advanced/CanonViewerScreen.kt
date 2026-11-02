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
fun CanonViewerScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var canonState by remember {
        mutableStateOf<Resource<List<JsonMap>>>(Resource.Loading)
    }

    fun load() {
        scope.launch {
            canonState = Resource.Loading
            canonState = repository.fetchCanon(repository.currentProjectId())
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
                "Canon Engine Viewer (WF-09)",
                style = MaterialTheme.typography.headlineLarge
            )
            Button(onClick = {
                scope.launch {
                    repository.submitStudioCommand(
                        repository.currentProjectId(),
                        CommandIntent.APPROVE_CANON_CHANGE,
                        "SAVE"
                    )
                    load()
                }
            }) { Text("Commit Canon") }
        }
        ResourceStateBox(
            resource = canonState,
            emptyMessage = "No persisted canon facts found in canon_facts table for this project.",
            onRetry = { load() }
        ) { facts ->
            KeyValueMapList(facts)
        }
    }
}
