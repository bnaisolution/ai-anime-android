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
fun CharacterDeepDiveScreen(characterId: String = "") {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    val tabs = listOf(
        "Knowledge Graph",
        "Voice Catalog",
        "Status History",
        "Reference History"
    )
    var selectedTab by remember { mutableStateOf(0) }
    var tabState by remember {
        mutableStateOf<Resource<List<JsonMap>>>(Resource.Loading)
    }
    var resolvedCharId by remember { mutableStateOf(characterId) }

    fun loadTab(index: Int) {
        val pid = repository.currentProjectId()
        scope.launch {
            tabState = Resource.Loading
            if (resolvedCharId.isBlank()) {
                val chars = repository.fetchCharacters(pid)
                if (chars is Resource.Success && chars.data.isNotEmpty()) {
                    resolvedCharId = chars.data.first().id
                }
            }
            tabState = when (index) {
                0 -> repository.fetchCategoryItems(pid, "character-knowledge")
                1 -> repository.fetchCategoryItems(pid, "voices")
                2 -> repository.fetchCategoryItems(pid, "character-status")
                else -> {
                    // GAP #4.2: Never call reference-history with placeholder
                    if (resolvedCharId.isBlank()) {
                        Resource.Empty
                    } else {
                        repository.fetchCharacterReferenceHistory(pid, resolvedCharId)
                    }
                }
            }
        }
    }

    LaunchedEffect(selectedTab, characterId) { loadTab(selectedTab) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Character Deep Dive " +
                (if (resolvedCharId.isNotBlank()) "($resolvedCharId)" else ""),
            style = MaterialTheme.typography.headlineLarge
        )
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title) }
                )
            }
        }
        ResourceStateBox(
            resource = tabState,
            emptyMessage = "No ${tabs[selectedTab]} records found for this character/project.",
            onRetry = { loadTab(selectedTab) }
        ) { items ->
            KeyValueMapList(items)
        }
    }
}
