package org.dpdns.bnai.anime.ui.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class NotificationSeverity {
    ERROR,
    WARNING,
    INFO
}

data class StudioNotification(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: String,
    val severity: NotificationSeverity
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen() {
    val notifications = remember {
        listOf(
            StudioNotification(
                id = "notif_1",
                title = "Lipsync Check Failed (WF-11)",
                message = "Scene 2 in Episode 5 failed MuseTalk lip-sync alignment. Queued for WF-13 stage retry.",
                timestamp = "10:15 AM",
                severity = NotificationSeverity.ERROR
            ),
            StudioNotification(
                id = "notif_2",
                title = "Quota Threshold Warning (WF-12)",
                message = "Project 'Shadows of Aethelgard' has consumed 82% of its daily GPU compute reservation.",
                timestamp = "09:40 AM",
                severity = NotificationSeverity.WARNING
            ),
            StudioNotification(
                id = "notif_3",
                title = "Approval Waiting in Inbox",
                message = "R12 Voice Config update for Lord Kaalraj is awaiting owner approval.",
                timestamp = "09:12 AM",
                severity = NotificationSeverity.WARNING
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Notifications & Incidents (S17)",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(notifications, key = { it.id }) { item ->
                val iconColor = when (item.severity) {
                    NotificationSeverity.ERROR -> Color(0xFFE53935)
                    NotificationSeverity.WARNING -> Color(0xFFFBC02D)
                    NotificationSeverity.INFO -> Color(0xFF1E88E5)
                }
                val iconVector = when (item.severity) {
                    NotificationSeverity.ERROR -> Icons.Default.Warning
                    NotificationSeverity.WARNING -> Icons.Default.Warning
                    NotificationSeverity.INFO -> Icons.Default.Info
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = item.severity.name,
                            tint = iconColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = item.timestamp,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }
    }
}
