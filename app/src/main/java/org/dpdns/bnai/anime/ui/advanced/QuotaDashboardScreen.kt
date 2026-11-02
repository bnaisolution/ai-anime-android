package org.dpdns.bnai.anime.ui.advanced

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
fun QuotaDashboardScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var quotaState by remember {
        mutableStateOf<Resource<JsonMap>>(Resource.Loading)
    }

    fun load() {
        scope.launch {
            quotaState = Resource.Loading
            quotaState = repository.fetchQuota(repository.currentProjectId())
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Quota & Usage Governor (WF-12)",
            style = MaterialTheme.typography.headlineLarge
        )
        ResourceStateBox(resource = quotaState, onRetry = { load() }) { map ->
            KeyValueMapList(listOf(map))
        }
    }
}
