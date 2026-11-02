package org.dpdns.bnai.anime.ui.advanced

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
fun LoreExplorerScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    val tabs = listOf(
        "Mysteries" to "mysteries",
        "Character Arcs" to "arcs",
        "Powers" to "powers",
        "Locations" to "locations",
        "Artifacts" to "artifacts",
        "Story Rules" to "rules",
        "Foreshadowing" to "foreshadowing"
    )
    var selectedIndex by remember { mutableStateOf(0) }
    var itemsState by remember {
        mutableStateOf<Resource<List<JsonMap>>>(Resource.Loading)
    }

    fun loadCategory(slug: String) {
        scope.launch {
            itemsState = Resource.Loading
            itemsState = repository.fetchCategoryItems(
                repository.currentProjectId(),
                slug
            )
        }
    }

    LaunchedEffect(selectedIndex) {
        loadCategory(tabs[selectedIndex].second)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Lore Explorer", style = MaterialTheme.typography.headlineLarge)
        ScrollableTabRow(
            selectedTabIndex = selectedIndex,
            edgePadding = 0.dp
        ) {
            tabs.forEachIndexed { idx, pair ->
                Tab(
                    selected = selectedIndex == idx,
                    onClick = { selectedIndex = idx },
                    text = { Text(pair.first) }
                )
            }
        }
        ResourceStateBox(
            resource = itemsState,
            emptyMessage = "No ${tabs[selectedIndex].first} records persisted for this project yet.",
            onRetry = { loadCategory(tabs[selectedIndex].second) }
        ) { list ->
            KeyValueMapList(list)
        }
    }
}
