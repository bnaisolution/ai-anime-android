package org.dpdns.bnai.anime.ui.production

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
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
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.core.network.SceneDto
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.designsystem.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SceneDetailScreen(episodeNo: Int = 1) {
    val context = LocalContext.current
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()

    var currentEpisodeNo by remember { mutableStateOf(episodeNo.coerceAtLeast(1)) }
    var epDropdownExpanded by remember { mutableStateOf(false) }
    var scenesState by remember { mutableStateOf<Resource<List<SceneDto>>>(Resource.Loading) }

    // BUG-021: Dedicated Scene PATCH dialog state
    var editingScene by remember { mutableStateOf<SceneDto?>(null) }
    var editDescription by remember { mutableStateOf("") }
    var editVisualPrompt by remember { mutableStateOf("") }
    var isSavingScene by remember { mutableStateOf(false) }

    fun loadScenesForEpisode(ep: Int = currentEpisodeNo) {
        val pid = repository.currentProjectId()
        if (pid.isBlank()) {
            scenesState = Resource.Empty
            return
        }
        scope.launch {
            scenesState = Resource.Loading
            scenesState = repository.fetchScenesRaw(pid, ep)
        }
    }

    LaunchedEffect(currentEpisodeNo) {
        loadScenesForEpisode(currentEpisodeNo)
    }

    editingScene?.let { sc ->
        AlertDialog(
            onDismissRequest = { if (!isSavingScene) editingScene = null },
            title = { Text("Edit Scene #${sc.scene_order ?: sc.scene_no ?: 1}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text("Scene Description / Action") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editVisualPrompt,
                        onValueChange = { editVisualPrompt = it },
                        label = { Text("Visual Prompt") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                PrimaryButton(
                    text = "Save Scene (PATCH)",
                    isLoading = isSavingScene,
                    onClick = {
                        val pid = repository.currentProjectId()
                        val sceneIdOrOrder = sc.id ?: (sc.scene_order ?: sc.scene_no ?: 1).toString()
                        isSavingScene = true
                        scope.launch {
                            // BUG-021 FIX: Call dedicated scene PATCH route instead of CONTINUE_EPISODE!
                            val res = repository.updateSceneSync(
                                projectId = pid,
                                episodeNo = currentEpisodeNo,
                                sceneId = sceneIdOrOrder,
                                updates = mapOf(
                                    "description" to editDescription,
                                    "visual_prompt" to editVisualPrompt
                                )
                            )
                            isSavingScene = false
                            if (res is Resource.Success) {
                                editingScene = null
                                Toast.makeText(context, "Scene updated via PATCH", Toast.LENGTH_SHORT).show()
                                loadScenesForEpisode(currentEpisodeNo)
                            } else if (res is Resource.Error) {
                                Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { editingScene = null }, enabled = !isSavingScene) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProjectSelectorBar(
            onProjectChanged = { loadScenesForEpisode(currentEpisodeNo) },
            onRefreshClick = { loadScenesForEpisode(currentEpisodeNo) }
        )

        ScreenHeader(
            title = "Scene Inspector",
            subtitle = "Inspect scenes & rendered clips, edit scene prompts via PATCH, or regenerate a scene"
        )

        ExposedDropdownMenuBox(
            expanded = epDropdownExpanded,
            onExpandedChange = { epDropdownExpanded = !epDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = "Episode $currentEpisodeNo",
                onValueChange = {},
                readOnly = true,
                label = { Text("Episode") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = epDropdownExpanded) },
                singleLine = true,
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = epDropdownExpanded,
                onDismissRequest = { epDropdownExpanded = false }
            ) {
                (1..12).forEach { epNum ->
                    DropdownMenuItem(
                        text = { Text("Episode $epNum") },
                        onClick = {
                            currentEpisodeNo = epNum
                            epDropdownExpanded = false
                        }
                    )
                }
            }
        }

        org.dpdns.bnai.anime.ui.components.ResourceStateBox(
            resource = scenesState,
            emptyMessage = "No scenes generated for Episode $currentEpisodeNo yet.",
            onRetry = { loadScenesForEpisode(currentEpisodeNo) }
        ) { scenesList ->
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(scenesList, key = { it.id ?: "${it.scene_order}" }) { scene ->
                    val sceneNo = scene.scene_order ?: scene.scene_no ?: 1
                    val firstClipUrl = scene.clips?.firstOrNull()?.clip_url?.takeIf {
                        it.startsWith("http://") || it.startsWith("https://")
                    }
                    StandardCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Scene #$sceneNo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            StatusChip(label = scene.status ?: "PENDING")
                        }

                        if (!firstClipUrl.isNullOrBlank()) {
                            AsyncMediaPreview(
                                mediaUrl = firstClipUrl,
                                contentDescription = "Scene $sceneNo preview",
                                height = 130.dp
                            )
                        }

                        Text(
                            text = scene.description?.ifBlank { "No scene description" } ?: "No scene description",
                            fontSize = 14.sp
                        )
                        if (!scene.visual_prompt.isNullOrBlank()) {
                            Text(
                                text = "Prompt: ${scene.visual_prompt}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    editDescription = scene.description.orEmpty()
                                    editVisualPrompt = scene.visual_prompt.orEmpty()
                                    editingScene = scene
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit (PATCH)", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        val pid = repository.currentProjectId()
                                        val res = repository.submitStudioCommand(
                                            projectId = pid,
                                            intent = CommandIntent.CONTINUE_EPISODE,
                                            episodeNo = currentEpisodeNo,
                                            payload = mapOf("scene_order" to sceneNo, "regenerate_scene_only" to true)
                                        )
                                        if (res is Resource.Success) {
                                            Toast.makeText(context, "Scene #$sceneNo regeneration queued", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Regenerate", fontSize = 12.sp)
                            }
                        }

                        if (!firstClipUrl.isNullOrBlank()) {
                            TextButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(firstClipUrl)))
                                    }
                                }
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open Rendered Clip Stream")
                            }
                        }
                    }
                }
            }
        }
    }
}
