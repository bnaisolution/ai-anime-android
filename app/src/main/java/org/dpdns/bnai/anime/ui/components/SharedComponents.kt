package org.dpdns.bnai.anime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource

@Composable
fun <T> ResourceStateBox(
    resource: Resource<T>,
    emptyMessage: String = "No records found in the Anime database for this project.",
    onRetry: (() -> Unit)? = null,
    content: @Composable (T) -> Unit
) {
    when (resource) {
        is Resource.Idle, is Resource.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Loading live data from Fastify backend...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        is Resource.Empty -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(emptyMessage, style = MaterialTheme.typography.bodyLarge)
                    if (onRetry != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = onRetry) { Text("Refresh") }
                    }
                }
            }
        }
        is Resource.Error -> {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val isForbidden = resource.code == 403
                    Text(
                        text = if (isForbidden)
                            "Access Denied (403): Insufficient permissions for this project/resource."
                        else resource.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (onRetry != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onRetry) { Text("Retry") }
                    }
                }
            }
        }
        is Resource.Success -> content(resource.data)
    }
}

@Composable
fun StatusBadge(label: String) {
    val upper = label.uppercase()
    val bgColor = when {
        upper in listOf(
            "SUCCEEDED", "ACTIVE", "COMMITTED", "PASSED", "APPROVED",
            "ONLINE", "COMPLETED", "HEALTHY", "UP"
        ) -> Color(0xFF10B981)
        upper in listOf(
            "QUEUED", "DISPATCHED", "ACKNOWLEDGED", "RECONCILING",
            "RUNNING", "DRAFT", "WARNING", "PENDING", "BUSY"
        ) -> Color(0xFFF59E0B)
        upper in listOf(
            "FAILED", "ERROR", "BLOCKED", "REJECTED", "OFFLINE", "DOWN"
        ) -> Color(0xFFEF4444)
        else -> Color(0xFF6B7280)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = upper,
            color = bgColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun KeyValueMapList(items: List<JsonMap>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(items) { map ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val title = (
                        map["title"] ?: map["name"] ?: map["wf_key"] ?:
                        map["worker_id"] ?: map["action"] ?: map["id"] ?: "Record"
                    ).toString()
                    val status = (
                        map["status"] ?: map["state"] ?: map["verification_status"]
                    )?.toString()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            title,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (!status.isNullOrBlank()) StatusBadge(status)
                    }
                    map.entries.forEach { (k, v) ->
                        if (k !in listOf("title", "name", "status", "state") && v != null) {
                            Text("$k: $v", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}
