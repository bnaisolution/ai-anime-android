package org.dpdns.bnai.anime.ui.library

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.*
import org.dpdns.bnai.anime.ui.components.*

data class MediaAssetItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val status: String,
    val streamUrl: String
)

@Composable
fun MediaLibraryScreen() {
    val context = LocalContext.current
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()

    var activeProjectId by remember { mutableStateOf(repository.currentProjectId()) }
    var selectedFilter by remember { mutableStateOf("ALL") }
    val filters = listOf(
        "ALL" to "All Assets",
        "VIDEO" to "Video Clips",
        "REFS" to "Character Refs",
        "SCENES" to "Scene Renders"
    )

    var mediaState by remember { mutableStateOf<Resource<List<MediaAssetItem>>>(Resource.Loading) }

    fun loadProjectMedia(pid: String) {
        if (pid.isBlank()) return
        scope.launch {
            mediaState = Resource.Loading
            when (val res = repository.fetchProjectMedia(pid)) {
                is Resource.Success -> {
                    val data = res.data
                    val items = mutableListOf<MediaAssetItem>()

                    val clips = data["clips"] as? List<*> ?: emptyList<Any?>()
                    clips.filterIsInstance<Map<*, *>>().forEachIndexed { idx, clip ->
                        val sceneNo = formatInt(clip["scene_no"] ?: clip["scene_order"] ?: (idx + 1))
                        val dur = formatInt(clip["duration_sec"] ?: 5)
                        val status = (clip["status"] ?: "READY").toString()
                        val url = (clip["clip_url"] ?: "https://api.bnai.dpdns.org/v1/media/telegram/clip_${idx + 1}.mp4").toString()
                        items.add(
                            MediaAssetItem(
                                id = "clip_$idx",
                                title = "Scene $sceneNo Render Clip",
                                subtitle = "Scene $sceneNo • ${dur}s • Video Clip",
                                category = "VIDEO",
                                status = status,
                                streamUrl = url
                            )
                        )
                    }

                    val charRefs = data["character_refs"] as? List<*> ?: emptyList<Any?>()
                    charRefs.filterIsInstance<Map<*, *>>().forEachIndexed { idx, ch ->
                        val name = (ch["name"] ?: ch["character_code"] ?: "Character ${idx + 1}").toString()
                        val ver = formatInt(ch["reference_version"] ?: 1)
                        val url = (ch["image_url"] ?: "").toString()
                        items.add(
                            MediaAssetItem(
                                id = "ref_$idx",
                                title = "$name (Ref v$ver)",
                                subtitle = "Character Visual DNA Reference • v$ver",
                                category = "REFS",
                                status = "LOCKED",
                                streamUrl = url
                            )
                        )
                    }

                    val scenes = data["scenes"] as? List<*> ?: emptyList<Any?>()
                    scenes.filterIsInstance<Map<*, *>>().forEachIndexed { idx, sc ->
                        val sNo = formatInt(sc["scene_no"] ?: sc["scene_order"] ?: (idx + 1))
                        val st = (sc["status"] ?: "READY").toString()
                        val url = (sc["media_url"] ?: sc["image_url"] ?: "https://api.bnai.dpdns.org/v1/media/telegram/scene_$sNo").toString()
                        items.add(
                            MediaAssetItem(
                                id = "scene_$idx",
                                title = "Scene $sNo Keyframe & Audio",
                                subtitle = "Scene $sNo • 5s • Keyframe Asset",
                                category = "SCENES",
                                status = st,
                                streamUrl = url
                            )
                        )
                    }

                    mediaState = if (items.isEmpty()) Resource.Empty else Resource.Success(items)
                }
                is Resource.Empty -> mediaState = Resource.Empty
                is Resource.Error -> mediaState = Resource.Error(res.message, res.code)
                else -> Unit
            }
        }
    }

    LaunchedEffect(activeProjectId) {
        loadProjectMedia(activeProjectId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Project Media Library", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        ProjectSelectorBar(
            onProjectChanged = { newPid ->
                activeProjectId = newPid
                loadProjectMedia(newPid)
            },
            onRefreshClick = { loadProjectMedia(activeProjectId) }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filters.forEach { (code, label) ->
                FilterChip(
                    selected = selectedFilter == code,
                    onClick = { selectedFilter = code },
                    label = { Text(label) }
                )
            }
        }

        ResourceStateBox(
            resource = mediaState,
            emptyMessage = "No rendered media assets found yet. Run Episode Production or Quick Generate first!",
            onRetry = { loadProjectMedia(activeProjectId) }
        ) { allItems ->
            val filtered = allItems.filter { selectedFilter == "ALL" || it.category == selectedFilter }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filtered, key = { it.id }) { item ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = if (item.category == "VIDEO") Icons.Default.Movie else Icons.Default.Image,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(item.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                        Text(item.subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                                StatusBadge(item.status)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        if (item.streamUrl.startsWith("http")) {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.streamUrl)))
                                        } else {
                                            Toast.makeText(context, "Stream URL not ready yet", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open Stream")
                                }

                                OutlinedButton(
                                    onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("Media URL", item.streamUrl))
                                        Toast.makeText(context, "Media link copied!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy")
                                }

                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            repository.submitStudioCommand(
                                                projectId = activeProjectId,
                                                intent = CommandIntent.RETRY_FAILED,
                                                payload = mapOf("asset_id" to item.id)
                                            )
                                            Toast.makeText(context, "Regenerate queued for ${item.title}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
