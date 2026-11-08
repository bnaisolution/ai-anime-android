package org.dpdns.bnai.anime.ui.notifications
import androidx.compose.ui.platform.testTag
import org.dpdns.bnai.anime.ui.testing.TestTags

import org.dpdns.bnai.anime.ui.LocalAppContainer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.*

@Composable
fun NotificationScreen() {
    val repository = LocalAppContainer.current.repository
    val scope = rememberCoroutineScope()

    var activeProjectId by remember { mutableStateOf(repository.currentProjectId()) }
    var notificationsState by remember { mutableStateOf<Resource<List<JsonMap>>>(Resource.Loading) }
    val readIds = remember { mutableStateMapOf<String, Boolean>() }

    fun loadNotifications(pid: String) {
        if (pid.isBlank()) return
        scope.launch {
            notificationsState = Resource.Loading
            notificationsState = repository.fetchNotifications(pid)
        }
    }

    LaunchedEffect(activeProjectId) {
        loadNotifications(activeProjectId)
    }

    Column(
        modifier = Modifier.testTag(TestTags.Screen.NOTIFICATIONS)
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Notifications", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = {
                val list = (notificationsState as? Resource.Success)?.data.orEmpty()
                list.forEachIndexed { idx, map ->
                    readIds[(map["id"] ?: idx).toString()] = true
                }
            }) {
                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Mark All Read")
            }
        }

        ProjectSelectorBar(
            onProjectChanged = { newPid ->
                activeProjectId = newPid
                loadNotifications(newPid)
            },
            onRefreshClick = { loadNotifications(activeProjectId) }
        )

        ResourceStateBox(
            resource = notificationsState,
            emptyMessage = "All caught up! No new notifications for this project.",
            onRetry = { loadNotifications(activeProjectId) }
        ) { itemsList ->
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(itemsList) { item ->
                    val itemId = (item["id"] ?: item.hashCode()).toString()
                    val isRead = readIds[itemId] == true
                    val rawAction = (item["action"] ?: item["event_type"] ?: "STUDIO_UPDATE").toString()
                    val humanTitle = rawAction.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                    val detailText = (item["new_value"] ?: item["summary"] ?: item["resource_type"] ?: "Completed studio action").toString()
                    val createdAt = item["created_at"]?.toString()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { readIds[itemId] = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Fix #314, #315, #316: Human readable title & parsed detail without raw UUID
                                Text(cleanDisplayTitle(humanTitle), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                Text(detailText.take(120), style = MaterialTheme.typography.bodyLarge)
                                Text(formatRelativeTime(createdAt), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                            }
                            // Fix #322: Blue unread indicator dot
                            if (!isRead) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF3B82F6))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
