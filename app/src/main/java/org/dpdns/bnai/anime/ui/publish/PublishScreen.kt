package org.dpdns.bnai.anime.ui.publish
import androidx.compose.ui.platform.testTag
import org.dpdns.bnai.anime.ui.testing.TestTags

import org.dpdns.bnai.anime.ui.LocalAppContainer

import android.content.Intent
import android.net.Uri
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
import org.dpdns.bnai.anime.core.network.PublishChannelDto
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

    var channels by remember { mutableStateOf<List<PublishChannelDto>>(emptyList()) }
    var configured by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var isLoadingChannels by remember { mutableStateOf(true) }
    var busyChannel by remember { mutableStateOf<String?>(null) }
    var channelMessage by remember { mutableStateOf<String?>(null) }

    var selectYt by remember { mutableStateOf(false) }
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

    fun refreshChannels() {
        scope.launch {
            isLoadingChannels = true
            when (val res = repository.loadPublishChannels()) {
                is Resource.Success -> {
                    channels = res.data.channels.filter { it.status == "active" }
                    configured = res.data.configured
                }
                is Resource.Error -> channelMessage = res.message
                else -> Unit
            }
            isLoadingChannels = false
        }
    }

    fun connect(platform: String) {
        if (busyChannel != null) return
        scope.launch {
            busyChannel = platform
            val projectId = repository.currentProjectId().ifBlank { null }
            when (val res = repository.startPublishOAuth(platform, projectId)) {
                is Resource.Success -> {
                    val url = res.data.authorization_url
                    if (!res.data.configured || url.isNullOrBlank()) {
                        channelMessage = "Server OAuth for $platform is not configured yet. Ask the owner to set client credentials."
                    } else {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (e: Exception) {
                            channelMessage = e.localizedMessage ?: "Could not open the browser."
                        }
                    }
                }
                is Resource.Error -> channelMessage = res.message
                else -> Unit
            }
            busyChannel = null
        }
    }

    fun disconnect(channelId: String) {
        if (busyChannel != null) return
        scope.launch {
            busyChannel = channelId
            when (val res = repository.disconnectPublishChannel(channelId)) {
                is Resource.Success -> {
                    channelMessage = "Channel disconnected."
                    refreshChannels()
                }
                is Resource.Error -> channelMessage = res.message
                else -> Unit
            }
            busyChannel = null
        }
    }

    fun test(channelId: String) {
        if (busyChannel != null) return
        scope.launch {
            busyChannel = channelId
            when (val res = repository.testPublishChannel(channelId)) {
                is Resource.Success -> {
                    channelMessage = if (res.data.ok) {
                        "Channel verified${res.data.channel?.channel_name?.let { ": $it" } ?: "."}"
                    } else {
                        "Channel check failed: ${res.data.reason ?: "unknown error"}"
                    }
                }
                is Resource.Error -> channelMessage = res.message
                else -> Unit
            }
            busyChannel = null
        }
    }

    LaunchedEffect(Unit) {
        loadViralMetadata()
        refreshChannels()
    }

    LaunchedEffect(channels) {
        selectYt = channels.any { it.platform == "youtube" }
        selectInsta = channels.any { it.platform == "instagram" }
    }

    val youtubeChannel = channels.firstOrNull { it.platform == "youtube" }
    val instagramChannel = channels.firstOrNull { it.platform == "instagram" }
    val canPublish = (youtubeChannel != null && selectYt) || (instagramChannel != null && selectInsta)

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Connected Distribution Channels", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    if (isLoadingChannels) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        TextButton(onClick = { refreshChannels() }) { Text("Refresh") }
                    }
                }

                PublishChannelRow(
                    label = "YouTube / Shorts",
                    channel = youtubeChannel,
                    serverConfigured = configured["youtube"] == true,
                    selected = selectYt,
                    onSelectedChange = { selectYt = it },
                    busy = busyChannel == "youtube" || (youtubeChannel != null && busyChannel == youtubeChannel.id),
                    onConnect = { connect("youtube") },
                    onTest = { youtubeChannel?.let { test(it.id) } },
                    onDisconnect = { youtubeChannel?.let { disconnect(it.id) } }
                )

                PublishChannelRow(
                    label = "Instagram Reels",
                    channel = instagramChannel,
                    serverConfigured = configured["instagram"] == true,
                    selected = selectInsta,
                    onSelectedChange = { selectInsta = it },
                    busy = busyChannel == "instagram" || (instagramChannel != null && busyChannel == instagramChannel.id),
                    onConnect = { connect("instagram") },
                    onTest = { instagramChannel?.let { test(it.id) } },
                    onDisconnect = { instagramChannel?.let { disconnect(it.id) } }
                )

                channelMessage?.let { message ->
                    Text(
                        message,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                    if (youtubeChannel != null && selectYt) add("youtube")
                    if (instagramChannel != null && selectInsta) add("instagram")
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
                    publishStatus = if (res is Resource.Success) "PUBLISH QUEUED" else "PUBLISH FAILED"
                    val toast = if (res is Resource.Success) {
                        "Dispatched to ${platforms.joinToString(", ")}"
                    } else {
                        (res as? Resource.Error)?.message ?: "Publish failed"
                    }
                    Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
                }
            },
            enabled = canPublish,
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

@Composable
private fun PublishChannelRow(
    label: String,
    channel: PublishChannelDto?,
    serverConfigured: Boolean,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    busy: Boolean,
    onConnect: () -> Unit,
    onTest: () -> Unit,
    onDisconnect: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (channel != null) {
                    Checkbox(checked = selected, onCheckedChange = onSelectedChange)
                }
                Column {
                    Text(label, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = when {
                            channel != null -> channel.channel_name ?: "Connected"
                            serverConfigured -> "Not connected"
                            else -> "Server OAuth not configured"
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            OutlinedButton(onClick = onConnect, enabled = !busy) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (channel != null) "Reconnect" else "Connect")
            }
        }
        if (channel != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onTest, enabled = !busy) { Text("Check", fontSize = 11.sp) }
                TextButton(onClick = onDisconnect, enabled = !busy) { Text("Disconnect", fontSize = 11.sp) }
            }
        }
    }
}
