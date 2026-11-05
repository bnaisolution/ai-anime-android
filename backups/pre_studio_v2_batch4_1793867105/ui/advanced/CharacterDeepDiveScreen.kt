package org.dpdns.bnai.anime.ui.advanced

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.*
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun CharacterDeepDiveScreen(characterId: String = "") {
    val context = LocalContext.current
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()

    var characters by remember { mutableStateOf<List<CharacterDto>>(emptyList()) }
    var selectedChar by remember { mutableStateOf<CharacterDto?>(null) }
    var charDropdownExpanded by remember { mutableStateOf(false) }

    // 4 Clean Tabs covering all 9 deep-dive areas without mobile clutter:
    // 0: Visual DNA (11 fields) & Canon Lock
    // 1: Knowledge & Character Arc
    // 2: Powers, Status & Relationships
    // 3: Voice & Reference History
    var selectedTab by remember { mutableStateOf(0) }
    var tabState by remember { mutableStateOf<Resource<List<JsonMap>>>(Resource.Loading) }

    val dnaFields = listOf(
        "face_shape" to "Face & Jawline",
        "eye_color" to "Eyes & Iris",
        "hair_style" to "Hair Style & Color",
        "skin_tone" to "Skin Tone",
        "body_build" to "Physique & Height",
        "primary_outfit" to "Signature Outfit",
        "accessories" to "Accessories / Weapons",
        "scars_marks" to "Scars / Markings",
        "posture" to "Posture & Aura",
        "color_palette" to "Color Palette",
        "art_style_anchor" to "Studio Style Anchor"
    )

    fun loadTab(charDto: CharacterDto?, tabIdx: Int) {
        val pid = repository.currentProjectId()
        val resolvedCode = charDto?.character_code ?: charDto?.id ?: characterId
        // Fix GAP 4.2: Never call reference-history with "default" placeholder if character ID is blank
        if (pid.isBlank() || resolvedCode.isBlank()) {
            tabState = Resource.Empty
            return
        }
        scope.launch {
            tabState = Resource.Loading
            tabState = when (tabIdx) {
                0 -> {
                    val dnaMap = charDto?.visual_dna ?: emptyMap()
                    Resource.Success(listOf(dnaMap))
                }
                1 -> {
                    val know = (repository.fetchCategoryItems(pid, "character-knowledge") as? Resource.Success)?.data.orEmpty()
                    val arcs = (repository.fetchCategoryItems(pid, "arcs") as? Resource.Success)?.data.orEmpty()
                    val combined = know + arcs
                    if (combined.isEmpty()) Resource.Empty else Resource.Success(combined)
                }
                2 -> {
                    val status = (repository.fetchCategoryItems(pid, "character-status") as? Resource.Success)?.data.orEmpty()
                    val powers = (repository.fetchCategoryItems(pid, "powers") as? Resource.Success)?.data.orEmpty()
                    val rels = (repository.fetchCategoryItems(pid, "relationships") as? Resource.Success)?.data.orEmpty()
                    val combined = status + powers + rels
                    if (combined.isEmpty()) Resource.Empty else Resource.Success(combined)
                }
                else -> repository.fetchCharacterReferenceHistory(pid, resolvedCode)
            }
        }
    }

    fun loadCharactersList() {
        val pid = repository.currentProjectId()
        if (pid.isBlank()) {
            tabState = Resource.Empty
            return
        }
        scope.launch {
            val res = repository.fetchCharactersRaw(pid)
            if (res is Resource.Success && res.data.isNotEmpty()) {
                characters = res.data
                selectedChar = res.data.find { it.character_code == characterId || it.id == characterId } ?: res.data.first()
                loadTab(selectedChar, selectedTab)
            } else {
                characters = emptyList()
                selectedChar = null
                tabState = Resource.Empty
            }
        }
    }

    LaunchedEffect(characterId) { loadCharactersList() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Fix #162: Project & Character Selectors at top
        ProjectSelectorBar(
            onProjectChanged = { loadCharactersList() },
            onRefreshClick = { loadCharactersList() }
        )

        // Fix #161 & #180: Header with Avatar, Name, Small Code (CHAR_001) & 🔒 Canon Lock Status
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text((selectedChar?.name ?: "C").take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(selectedChar?.name ?: "Select Character", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Code: ${selectedChar?.character_code ?: characterId.ifBlank { "CHAR_001" }}", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                    StatusBadge("🔒 ${selectedChar?.canon_lock_status?.uppercase() ?: "LOCKED"}")
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(onClick = { charDropdownExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedChar?.name ?: "Switch Character", maxLines = 1)
                        }
                        DropdownMenu(expanded = charDropdownExpanded, onDismissRequest = { charDropdownExpanded = false }) {
                            characters.forEach { ch ->
                                DropdownMenuItem(
                                    text = { Text("${ch.name ?: ch.character_code}") },
                                    onClick = {
                                        selectedChar = ch
                                        charDropdownExpanded = false
                                        loadTab(ch, selectedTab)
                                    }
                                )
                            }
                        }
                    }
                    // Fix #169: Generate New Reference button
                    Button(onClick = {
                        scope.launch {
                            val pid = repository.currentProjectId()
                            repository.submitStudioCommand(pid, CommandIntent.VISION_ANALYSIS, "SAVE", payload = mapOf("character_code" to (selectedChar?.character_code ?: "CHAR_001")))
                            Toast.makeText(context, "✨ Generating new reference sheet...", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Ref")
                    }
                }
            }
        }

        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
            listOf("Visual DNA (11)", "Knowledge & Arc", "Powers & Relations", "Voice & Ref History").forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = {
                        selectedTab = idx
                        loadTab(selectedChar, idx)
                    },
                    text = { Text(title) }
                )
            }
        }

        if (selectedTab == 0) {
            // Fix #174 & #180: 11-Field Visual DNA Cards with icons + Canon Lock status
            val dnaMap = selectedChar?.visual_dna.orEmpty()
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                items(dnaFields) { (key, label) ->
                    val value = dnaMap[key]?.toString()?.takeIf { it.isNotBlank() } ?: "Standard Anime Anchor (Locked)"
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                Text(value, style = MaterialTheme.typography.bodyLarge)
                            }
                            Icon(Icons.Default.Lock, contentDescription = "Canon Locked", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        } else {
            ResourceStateBox(
                resource = tabState,
                emptyMessage = "No records in this category yet.",
                onRetry = { loadTab(selectedChar, selectedTab) }
            ) { itemsList ->
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                    items(itemsList) { item ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Fix #164, #166, #178, #179: Smart badges for Secrets, Timeline, Powers & Trust Level
                                val title = (item["fact"] ?: item["arc_name"] ?: item["power_name"] ?: item["status"] ?: item["relationship_type"] ?: "Character Record").toString()
                                val epLearned = item["learned_in_episode"] ?: item["status_changed_ep"]
                                val isSecret = item["is_secret"] == true
                                val trustLevel = (item["trust_level"] as? Number)?.toFloat()

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    if (isSecret) StatusBadge("🔒 SECRET")
                                    epLearned?.let { StatusBadge("Ep $it") }
                                }

                                trustLevel?.let { trust ->
                                    Text("Trust Level: ${(trust * 10).toInt()}%")
                                    LinearProgressIndicator(progress = { (trust / 10f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                                }

                                item.entries.forEach { (k, v) ->
                                    if (k !in setOf("id", "project_id", "character_id", "created_at", "updated_at") && v != null) {
                                        Text("${k.replace("_", " ").uppercase()}: $v", style = MaterialTheme.typography.bodyLarge)
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
