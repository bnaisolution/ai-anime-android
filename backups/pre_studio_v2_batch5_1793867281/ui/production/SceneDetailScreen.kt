package org.dpdns.bnai.anime.ui.production

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.model.Episode
import org.dpdns.bnai.anime.core.network.*
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun SceneDetailScreen(episodeNo: Int = 1) {
    val context = LocalContext.current
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()

    var currentEpisode by remember { mutableStateOf(episodeNo) }
    var episodes by remember { mutableStateOf<List<Episode>>(emptyList()) }
    var epDropdownExpanded by remember { mutableStateOf(false) }

    var scenesState by remember { mutableStateOf<Resource<List<SceneDto>>>(Resource.Loading) }
    var selectedFilter by remember { mutableStateOf("All") }
    var editingScene by remember { mutableStateOf<SceneDto?>(null) }
    var editedDescription by remember { mutableStateOf("") }

    fun loadScenes(ep: Int) {
        val pid = repository.currentProjectId()
        scope.launch {
            scenesState = Resource.Loading
            val epRes = repository.fetchEpisodes(pid)
            episodes = (epRes as? Resource.Success)?.data.orEmpty()
            scenesState = repository.fetchScenes(pid, ep)
        }
    }

    LaunchedEffect(currentEpisode) { loadScenes(currentEpisode) }

    // Fix #60: Edit Scene Description Dialog
    editingScene?.let { sc ->
        AlertDialog(
            onDismissRequest = { editingScene = null },
            title = { Text("Edit Scene #${sc.scene_no ?: sc.scene_order ?: 1}") },
            text = {
                OutlinedTextField(
                    value = editedDescription,
                    onValueChange = { editedDescription = it },
                    label = { Text("Scene Action / Visual Prompt") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        repository.submitStudioCommand(
                            projectId = repository.currentProjectId(),
                            intent = CommandIntent.CONTINUE_EPISODE,
                            mode = "SAVE",
                            episodeNo = currentEpisode,
                            payload = mapOf("scene_id" to sc.id, "description" to editedDescription)
                        )
                        Toast.makeText(context, "Scene updated!", Toast.LENGTH_SHORT).show()
                        editingScene = null
                        loadScenes(currentEpisode)
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingScene = null }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProjectSelectorBar(
            onProjectChanged = { loadScenes(currentEpisode) },
            onRefreshClick = { loadScenes(currentEpisode) }
        )

        // Fix #57: Episode Dropdown Selector instead of ugly +/- buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Scene Inspector", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Box {
                OutlinedButton(onClick = { epDropdownExpanded = true }) {
                    Text("Episode $currentEpisode")
                }
                DropdownMenu(expanded = epDropdownExpanded, onDismissRequest = { epDropdownExpanded = false }) {
                    val epNums = if (episodes.isNotEmpty()) episodes.map { it.episodeNumber } else (1..5).toList()
                    epNums.forEach { num ->
                        DropdownMenuItem(
                            text = { Text("Episode $num") },
                            onClick = {
                                currentEpisode = num
                                epDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Fix #62: FilterChips (All, Pending, Rendered)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("All", "Pending", "Rendered")) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) }
                )
            }
        }

        ResourceStateBox(
            resource = scenesState,
            emptyMessage = "No scenes generated for Episode $currentEpisode yet.",
            onRetry = { loadScenes(currentEpisode) }
        ) { scenes ->
            val filtered = scenes.filter { sc ->
                val isRendered = sc.status?.uppercase() in setOf("RENDERED", "COMPLETED", "SUCCEEDED") || !sc.clips.isNullOrEmpty()
                when (selectedFilter) {
                    "Rendered" -> isRendered
                    "Pending" -> !isRendered
                    else -> true
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                itemsIndexed(filtered, key = { _, s -> s.id }) { idx, sc ->
                    // GAP #5.3: Use scene_no or scene_order, never non-existent title
                    val resolvedSceneNo = sc.scene_no ?: sc.scene_order ?: (idx + 1)
                    val isRendered = sc.status?.uppercase() in setOf("RENDERED", "COMPLETED", "SUCCEEDED") || !sc.clips.isNullOrEmpty()

                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Scene #$resolvedSceneNo", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Fix #58: Color-coded Badges (Green = Ready, Orange = Pending)
                                    StatusBadge(if (isRendered) "COMPLETED" else "PENDING")
                                    // Fix #60: Edit Icon
                                    IconButton(onClick = {
                                        editedDescription = sc.description ?: sc.visual_prompt ?: ""
                                        editingScene = sc
                                    }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Scene", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            Text(sc.description ?: "No scene description.")

                            if (!sc.visual_prompt.isNullOrBlank()) {
                                Text("Prompt: ${sc.visual_prompt}", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                            }

                            // Fix #63: Dialogue Speaker & Voice Text clearly displayed
                            if (!sc.dialogue_speaker_id.isNullOrBlank() || !sc.voice_text.isNullOrBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("🎙️ Speaker: ${sc.dialogue_speaker_id ?: "Narrator"}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        Text("\"${sc.voice_text ?: "..."}\"", style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                            }

                            // Fix #64: Audio tags (bgm_hint and sfx_hint) as Chips
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!sc.bgm_hint.isNullOrBlank()) {
                                    AssistChip(onClick = {}, label = { Text("🎵 BGM: ${sc.bgm_hint}", fontSize = 11.sp) })
                                }
                                if (!sc.sfx_hint.isNullOrBlank()) {
                                    AssistChip(onClick = {}, label = { Text("💥 SFX: ${sc.sfx_hint}", fontSize = 11.sp) })
                                }
                            }

                            // Fix #59: Lightweight clickable URLs instead of heavy embedded video players
                            sc.clips?.forEach { clip ->
                                val url = clip.clip_url.orEmpty()
                                if (url.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open Rendered Clip (${clip.duration_sec?.toInt() ?: 5}s • ${clip.provider ?: "GPU"})")
                                    }
                                }
                            }

                            // Fix #61: Regenerate Scene Button
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        repository.submitStudioCommand(
                                            projectId = repository.currentProjectId(),
                                            intent = CommandIntent.RETRY_FAILED,
                                            mode = "QUICK",
                                            episodeNo = currentEpisode,
                                            payload = mapOf("scene_no" to resolvedSceneNo, "scene_id" to sc.id)
                                        )
                                        Toast.makeText(context, "Regenerating Scene #$resolvedSceneNo...", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Regenerate Scene")
                            }
                        }
                    }
                }
            }
        }
    }
}
