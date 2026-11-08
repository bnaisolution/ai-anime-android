package org.dpdns.bnai.anime.ui.publish
import androidx.compose.ui.platform.testTag
import org.dpdns.bnai.anime.ui.testing.TestTags

import org.dpdns.bnai.anime.ui.LocalAppContainer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.model.Episode
import org.dpdns.bnai.anime.core.network.CommandIntent
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun PublishScreen() {
    val context = LocalContext.current
    val repository = LocalAppContainer.current.repository
    val scope = rememberCoroutineScope()

    var episodes by remember { mutableStateOf<List<Episode>>(emptyList()) }
    var selectedEp by remember { mutableStateOf<Episode?>(null) }

    var ytConnected by remember { mutableStateOf(true) }
    var instaConnected by remember { mutableStateOf(false) }
    var selectYt by remember { mutableStateOf(true) }
    var selectInsta by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var hashtags by remember { mutableStateOf("#anime #darkfantasy #aistudio #otaku") }
    var visibility by remember { mutableStateOf("Public") }
    var visDropdown by remember { mutableStateOf(false) }
    var publishStatus by remember { mutableStateOf<String?>(null) }

    fun loadViralMetadata() {
        val pid = repository.currentProjectId()
        scope.launch {
            val res = repository.fetchEpisodes(pid)
            if (res is Resource.Success && res.data.isNotEmpty()) {
                episodes = res.data
                val ep = res.data.first()
                selectedEp = ep
                // Fix #72: Auto-fill from episode title & synopsis / viral metadata
                title = "Ep ${ep.episodeNumber}: ${ep.title}"
                description = ep.synopsis ?: "Official AI Anime Studio Episode Release."
            }
        }
    }

    LaunchedEffect(Unit) { loadViralMetadata() }

    Column(
        modifier = Modifier.testTag(TestTags.Screen.PUBLISH).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Fix #70: Clean Header (No WF-07) + Project Selector
        ProjectSelectorBar(
            onProjectChanged = { loadViralMetadata() },
            onRefreshClick = { loadViralMetadata() }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Publish Studio", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = { loadViralMetadata() }) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Viral Auto-Fill")
            }
        }

        // Fix #71 & #74: OAuth Connect Buttons + Checkboxes only for connected platforms
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Connected Distribution Channels", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (ytConnected) {
                            Checkbox(checked = selectYt, onCheckedChange = { selectYt = it })
                        }
                        Text("YouTube / Shorts", fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(onClick = {
                        ytConnected = !ytConnected
                        selectYt = ytConnected
                    }) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (ytConnected) "Connected" else "Connect YouTube")
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (instaConnected) {
                            Checkbox(checked = selectInsta, onCheckedChange = { selectInsta = it })
                        }
                        Text("Instagram Reels", fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(onClick = {
                        instaConnected = !instaConnected
                        selectInsta = instaConnected
                    }) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (instaConnected) "Connected" else "Connect Insta")
                    }
                }
            }
        }

        // Fix #73: Vertical (9:16) & Horizontal (16:9) Thumbnail Previews
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .weight(1.6f)
                    .height(95.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(10.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Text("16:9 Horizontal Thumbnail", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(95.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(10.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Text("9:16 Reel Cover", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Release Title") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Viral Description") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = hashtags, onValueChange = { hashtags = it }, label = { Text("Viral Tags") }, modifier = Modifier.fillMaxWidth())

        // Fix #75: Public / Unlisted Visibility Dropdown
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Visibility:", style = MaterialTheme.typography.headlineMedium)
            Box {
                OutlinedButton(onClick = { visDropdown = true }) {
                    Text(visibility)
                }
                DropdownMenu(expanded = visDropdown, onDismissRequest = { visDropdown = false }) {
                    listOf("Public", "Unlisted", "Private").forEach { v ->
                        DropdownMenuItem(
                            text = { Text(v) },
                            onClick = {
                                visibility = v
                                visDropdown = false
                            }
                        )
                    }
                }
            }
        }

        publishStatus?.let {
            StatusBadge(it)
        }

        // Fix #76: Renamed button to "Publish to Selected Platforms"
        Button(
            onClick = {
                val platforms = buildList {
                    if (ytConnected && selectYt) add("youtube")
                    if (instaConnected && selectInsta) add("instagram")
                }
                scope.launch {
                    val res = repository.submitStudioCommand(
                        projectId = repository.currentProjectId(),
                        intent = CommandIntent.PUBLISH_NOW,
                        mode = "SAVE",
                        episodeNo = selectedEp?.episodeNumber ?: 1,
                        payload = mapOf(
                            "title" to title,
                            "description" to description,
                            "hashtags" to hashtags,
                            "visibility" to visibility.lowercase(),
                            "platforms" to platforms
                        )
                    )
                    publishStatus = if (res is Resource.Success) "PUBLISHED QUEUED" else "PUBLISH FAILED"
                    Toast.makeText(context, "Dispatched to ${platforms.joinToString(", ")}", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = (ytConnected && selectYt) || (instaConnected && selectInsta),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.CloudUpload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Publish to Selected Platforms")
        }
        Spacer(modifier = Modifier.height(60.dp))
    }
}
