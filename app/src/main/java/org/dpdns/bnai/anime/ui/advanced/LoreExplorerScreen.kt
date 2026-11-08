package org.dpdns.bnai.anime.ui.advanced
import androidx.compose.ui.platform.testTag
import org.dpdns.bnai.anime.ui.testing.TestTags

import org.dpdns.bnai.anime.ui.LocalAppContainer

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.*
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.StatusBadge
import org.dpdns.bnai.anime.ui.components.cleanDisplayTitle

@Composable
fun LoreExplorerScreen() {
    val context = LocalContext.current
    val repository = LocalAppContainer.current.repository
    val scope = rememberCoroutineScope()

    // Fix #149 & #157: 8 World Wiki categories including Relationships
    val categories = listOf(
        "Mysteries" to "mysteries",
        "Arcs" to "arcs",
        "Powers" to "powers",
        "Locations" to "locations",
        "Artifacts" to "artifacts",
        "Rules" to "rules",
        "Foreshadowing" to "foreshadowing",
        "Relationships" to "relationships"
    )

    var selectedIndex by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<Resource<List<JsonMap>>>(Resource.Loading) }
    var showAddDialog by remember { mutableStateOf(false) }
    var aiLorePrompt by remember { mutableStateOf("") }

    fun loadCategory(index: Int) {
        val pid = repository.currentProjectId()
        if (pid.isBlank()) {
            state = Resource.Error("Select an active project first.")
            return
        }
        scope.launch {
            state = Resource.Loading
            state = repository.fetchCategoryItems(pid, categories[index].second)
        }
    }

    LaunchedEffect(selectedIndex) { loadCategory(selectedIndex) }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("✨ Add / Edit World Wiki via AI") },
            text = {
                OutlinedTextField(
                    value = aiLorePrompt,
                    onValueChange = { aiLorePrompt = it },
                    label = { Text("Describe new ${categories[selectedIndex].first} entry") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.submitStudioCommand(
                                repository.currentProjectId(),
                                CommandIntent.SAVE_BIBLE,
                                "SAVE",
                                payload = mapOf("user_prompt" to aiLorePrompt, "category" to categories[selectedIndex].second)
                            )
                            Toast.makeText(context, "Lore update dispatched!", Toast.LENGTH_SHORT).show()
                            aiLorePrompt = ""
                            showAddDialog = false
                            loadCategory(selectedIndex)
                        }
                    },
                    enabled = aiLorePrompt.isNotBlank()
                ) { Text("Generate & Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(modifier = Modifier.testTag(TestTags.Screen.LORE_EXPLORER),
        // Fix #160: FAB (+) to Add via AI
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Lore") },
                text = { Text("Add via AI") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Fix #147 & #148: Renamed to "World Wiki" + Project Selector
            ProjectSelectorBar(
                onProjectChanged = { loadCategory(selectedIndex) },
                onRefreshClick = { loadCategory(selectedIndex) }
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("World Wiki", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                // Fix #155: AI Edit button
                OutlinedButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Edit")
                }
            }

            // Fix #152: Search Bar to filter current tab
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter ${categories[selectedIndex].first}...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Fix #149: Clean FilterChip row for the 8 World Wiki categories
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories.indices.toList()) { idx ->
                    FilterChip(
                        selected = selectedIndex == idx,
                        onClick = { selectedIndex = idx },
                        label = { Text(categories[idx].first) }
                    )
                }
            }

            when (val res = state) {
                is Resource.Idle, is Resource.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is Resource.Empty, is Resource.Error -> {
                    // Fix #151: Inspiring Empty State + "Generate Story Bible" button
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Your world is a blank slate. 🪄", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("No ${categories[selectedIndex].first} entries yet.", color = MaterialTheme.colorScheme.secondary)
                            Button(onClick = { showAddDialog = true }) {
                                Text("Generate ${categories[selectedIndex].first} with AI")
                            }
                        }
                    }
                }
                is Resource.Success -> {
                    val filtered = res.data.filter { map ->
                        searchQuery.isBlank() || map.values.any { v -> v?.toString()?.contains(searchQuery, ignoreCase = true) == true }
                    }
                    // Fix #150, #156, #158: Smart Custom Cards with Danger Level / Open-Resolved Badges
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                        items(filtered) { item ->
                            val title = cleanDisplayTitle(
                                (item["name"] ?: item["title"] ?: item["power_name"] ?: item["arc_name"] ?: item["location_code"] ?: item["rule_code"] ?: "Wiki Entry").toString()
                            )
                            val badge = (item["danger_level"] ?: item["status"] ?: item["tier"] ?: item["relationship_type"])?.toString()

                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        if (!badge.isNullOrBlank()) StatusBadge(badge)
                                    }
                                    item.entries.forEach { (k, v) ->
                                        if (k !in setOf("id", "project_id", "name", "title", "status", "danger_level", "created_at", "updated_at") && v != null) {
                                            Text("${k.replace("_", " ").replaceFirstChar { it.uppercase() }}: $v", style = MaterialTheme.typography.bodyLarge)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
