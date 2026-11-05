package org.dpdns.bnai.anime.ui.owner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.formatRelativeTime
import org.dpdns.bnai.anime.ui.designsystem.*
import java.time.Instant
import java.time.temporal.ChronoUnit

@Composable
fun WorkerManagerScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()

    var workersState by remember { mutableStateOf<Resource<List<JsonMap>>>(Resource.Loading) }

    fun loadWorkers() {
        scope.launch {
            workersState = Resource.Loading
            workersState = repository.fetchAdminWorkers()
        }
    }

    LaunchedEffect(Unit) {
        loadWorkers()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(
            title = "Compute Worker Registry",
            subtitle = "Real worker_registry state & heartbeat expiry verification",
            trailingContent = {
                SecondaryButton(text = "Refresh", onClick = { loadWorkers() })
            }
        )

        org.dpdns.bnai.anime.ui.components.ResourceStateBox(
            resource = workersState,
            emptyMessage = "No compute workers registered in worker_registry.",
            onRetry = { loadWorkers() }
        ) { workers ->
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(workers) { w ->
                    val workerId = (w["worker_id"] ?: w["id"] ?: "worker").toString()
                    val provider = (w["provider"] ?: w["worker_type"] ?: "UNKNOWN_PROVIDER").toString()
                    val currentModel = (w["current_model"] ?: w["model"] ?: "Not reported").toString()
                    val capacity = (w["capacity"] ?: w["vram"] ?: "Capacity not reported").toString()
                    val heartbeatIso = (w["last_heartbeat"] ?: w["updated_at"])?.toString()
                    val rawStatus = (w["status"] ?: "UNKNOWN").toString().uppercase()

                    val isHeartbeatStale = try {
                        if (heartbeatIso.isNullOrBlank()) true
                        else ChronoUnit.MINUTES.between(Instant.parse(heartbeatIso), Instant.now()) > 5
                    } catch (_: Exception) {
                        false
                    }
                    val effectiveStatus = if (isHeartbeatStale) "OFFLINE (STALE HEARTBEAT)" else rawStatus

                    StandardCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("$provider (#${workerId.take(8)})", fontWeight = FontWeight.Bold)
                            StatusChip(label = effectiveStatus)
                        }
                        Text("Active Model: $currentModel", fontSize = 13.sp)
                        Text("Reported Capacity: $capacity", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "Last Heartbeat: ${if (heartbeatIso.isNullOrBlank()) "Never" else formatRelativeTime(heartbeatIso)}",
                            fontSize = 12.sp,
                            color = if (isHeartbeatStale) StudioTokens.Error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
