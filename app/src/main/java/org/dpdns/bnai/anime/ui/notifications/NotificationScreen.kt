package org.dpdns.bnai.anime.ui.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.KeyValueMapList
import org.dpdns.bnai.anime.ui.components.ResourceStateBox

@Composable
fun NotificationScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var feedState by remember {
        mutableStateOf<Resource<List<JsonMap>>>(Resource.Loading)
    }

    fun load() {
        scope.launch {
            feedState = Resource.Loading
            feedState = repository.fetchAuditLogs(repository.currentProjectId())
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Project Activity & Event Feed",
            style = MaterialTheme.typography.headlineLarge
        )
        ResourceStateBox(
            resource = feedState,
            emptyMessage = "No activity events recorded for this project yet.",
            onRetry = { load() }
        ) { events ->
            KeyValueMapList(events)
        }
    }
}
