package org.dpdns.bnai.anime.ui.character

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.dpdns.bnai.anime.core.model.Character
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun CharacterListScreen(
    onCharacterClick: (Character) -> Unit = {},
    onOpenDeepDive: (String) -> Unit = {},
    viewModel: CharacterViewModel = viewModel()
) {
    val context = LocalContext.current
    val charactersResource by viewModel.charactersState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var newCharName by remember { mutableStateOf("") }
    var newCharRole by remember { mutableStateOf("Protagonist") }
    val statusOverrides = remember { mutableStateMapOf<String, Boolean>() }

    val filters = listOf("All", "Alive", "Dead", "Protagonist", "Antagonist")

    LaunchedEffect(Unit) {
        viewModel.loadCharacters()
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("➕ Add Character & Auto-Portrait") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newCharName,
                        onValueChange = { newCharName = it },
                        label = { Text("Character Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCharRole,
                        onValueChange = { newCharRole = it },
                        label = { Text("Role (Protagonist / Antagonist / Support)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createOrGenerateCharacter(newCharName, newCharRole, false) { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                        newCharName = ""
                        showAddDialog = false
                    },
                    enabled = newCharName.isNotBlank()
                ) { Text("Create & Gen Image") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Fix #29: Header Project Selector + Search Bar
        ProjectSelectorBar(
            onProjectChanged = { newId -> viewModel.loadCharacters(newId) },
            onRefreshClick = { viewModel.loadCharacters() }
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search characters by name or role...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Fix #34: FilterChips (All, Alive, Dead, Protagonist, Antagonist)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters) { chip ->
                FilterChip(
                    selected = selectedFilter == chip,
                    onClick = { selectedFilter = chip },
                    label = { Text(chip) }
                )
            }
        }

        when (val res = charactersResource) {
            is Resource.Idle, is Resource.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is Resource.Empty, is Resource.Error -> {
                // Fix #30: Rich Empty State with "✨ Generate with AI" and "➕ Add Character"
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("No characters in this project yet.", style = MaterialTheme.typography.headlineMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = {
                                viewModel.createOrGenerateCharacter("", "", true) { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generate with AI")
                            }
                            OutlinedButton(onClick = { showAddDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Character")
                            }
                        }
                    }
                }
            }
            is Resource.Success -> {
                val filtered = res.data.filter { ch ->
                    val isAlive = statusOverrides[ch.id] ?: !ch.status.equals("dead", ignoreCase = true)
                    val matchesSearch = searchQuery.isBlank() ||
                        ch.name.contains(searchQuery, ignoreCase = true) ||
                        ch.role.contains(searchQuery, ignoreCase = true)
                    val matchesFilter = when (selectedFilter) {
                        "Alive" -> isAlive
                        "Dead" -> !isAlive
                        "Protagonist" -> ch.role.contains("Protagonist", ignoreCase = true) || ch.role.contains("Hero", ignoreCase = true) || ch.role.contains("Main", ignoreCase = true)
                        "Antagonist" -> ch.role.contains("Antagonist", ignoreCase = true) || ch.role.contains("Villain", ignoreCase = true)
                        else -> true
                    }
                    matchesSearch && matchesFilter
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Cast (${filtered.size})", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Character")
                    }
                }

                // Fix #31, #32, #35: 2-Column Grid/Card Layout with Avatar, Role, Status Toggle & Voice Play button
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filtered, key = { it.id }) { character ->
                        val isAlive = statusOverrides[character.id] ?: !character.status.equals("dead", ignoreCase = true)
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onCharacterClick(character) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(character.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(character.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(character.role, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StatusBadge(if (isAlive) "ALIVE" else "DEAD")
                                    // Fix #35: Quick switch for Alive/Dead
                                    Switch(
                                        checked = isAlive,
                                        onCheckedChange = { statusOverrides[character.id] = it }
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Fix #32: Play button for PREVIEW_VOICE
                                    IconButton(onClick = {
                                        viewModel.previewVoice(character.id, character.voiceId) { msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    }) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Preview Voice", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    TextButton(onClick = { onOpenDeepDive(character.id) }) {
                                        Text("Deep Dive", fontSize = 11.sp)
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
