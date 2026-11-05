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
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.designsystem.*

private data class StudioMediaItem(
    val id: String,
    val category: String,
    val title: String,
    val status: String,
    val mediaUrl: String
)

@Composable
fun MediaLibraryScreen() {
    val context = LocalContext.current
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()

    var filterTab by remember { mutableStateOf("ALL") }
    var mediaState by remember { mutableStateOf<Resource<JsonMap>>(Resource.Loading) }

    fun loadMedia() {
        val pid = repository.currentProjectId()
        if (pid.isBlank()) {
            mediaState = Resource.Empty
            return
        }
        scope.launch {
            mediaState = Resource.Loading
            mediaState = repository.fetchProjectMedia(pid)
        }
    }

    LaunchedEffect(Unit) {
        loadMedia()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProjectSelectorBar(
            onProjectChanged = { loadMedia() },
            onRefreshClick = { loadMedia() }
        )

        ScreenHeader(
            title = "Project Media Library",
            subtitle = "Rendered video clips, scene keyframes & verified character reference assets"
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ALL", "CLIPS", "CHARACTERS", "SCENES").forEach { tab ->
                FilterChip(
                    selected = filterTab == tab,
                    onClick = { filterTab = tab },
                    label = { Text(tab) }
                )
            }
        }

        org.dpdns.bnai.anime.ui.components.ResourceStateBox(
            resource = mediaState,
            emptyMessage = "No media assets generated for this project yet.",
            onRetry = { loadMedia() }
        ) { payload ->
            val parsedItems = remember(payload, filterTab) {
                val list = mutableListOf<StudioMediaItem>()
                val clipsList = (payload["clips"] as? List<*>).orEmpty()
                clipsList.forEachIndexed { idx, raw ->
                    val m = raw as? Map<*, *> ?: return@forEachIndexed
                    val url = (m["clip_url"] ?: m["media_url"] ?: "").toString()
                    list.add(
                        StudioMediaItem(
                            id = (m["id"] ?: "clip_$idx").toString(),
                            category = "CLIPS",
                            title = "Scene #${m["scene_order"] ?: (idx + 1)} Video Clip",
                            status = (m["status"] ?: "READY").toString(),
                            mediaUrl = url
                        )
                    )
                }

                val charList = (payload["character_refs"] as? List<*>).orEmpty()
                charList.forEachIndexed { idx, raw ->
                    val m = raw as? Map<*, *> ?: return@forEachIndexed
                    val url = (m["image_url"] ?: "").toString()
                    list.add(
                        StudioMediaItem(
                            id = (m["id"] ?: "char_$idx").toString(),
                            category = "CHARACTERS",
                            title = "${m["name"] ?: m["character_code"] ?: "Character"} Reference",
                            status = "VERIFIED",
                            mediaUrl = url
                        )
                    )
                }

                val scenesList = (payload["scenes"] as? List<*>).orEmpty()
                scenesList.forEachIndexed { idx, raw ->
                    val m = raw as? Map<*, *> ?: return@forEachIndexed
                    list.add(
                        StudioMediaItem(
                            id = (m["id"] ?: "scene_$idx").toString(),
                            category = "SCENES",
                            title = "Scene #${m["scene_order"] ?: (idx + 1)}: ${(m["description"] ?: "").toString().take(40)}",
                            status = (m["status"] ?: "PENDING").toString(),
                            mediaUrl = ""
                        )
                    )
                }

                if (filterTab == "ALL") list else list.filter { it.category == filterTab }
            }

            if (parsedItems.isEmpty()) {
                StandardCard {
                    Text("No assets in category '$filterTab'.", fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(parsedItems, key = { it.id }) { item ->
                        StandardCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                StatusChip(label = item.status)
                            }
                            if (item.mediaUrl.startsWith("http")) {
                                AsyncMediaPreview(
                                    mediaUrl = item.mediaUrl,
                                    contentDescription = item.title,
                                    height = 140.dp
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SecondaryButton(
                                        text = "Copy URL",
                                        onClick = {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            cm?.setPrimaryClip(ClipData.newPlainText("Media URL", item.mediaUrl))
                                            Toast.makeText(context, "URL copied", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    PrimaryButton(
                                        text = "Open Stream",
                                        onClick = {
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.mediaUrl)))
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
