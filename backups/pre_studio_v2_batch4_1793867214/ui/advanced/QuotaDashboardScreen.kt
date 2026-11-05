package org.dpdns.bnai.anime.ui.advanced

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.components.formatInt

@Composable
fun QuotaDashboardScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()

    var activeProjectId by remember { mutableStateOf(repository.currentProjectId()) }
    var quotaState by remember { mutableStateOf<Resource<JsonMap>>(Resource.Loading) }

    // Fix GAP #4.4: Direct call to /quota without hiding real errors
    fun loadQuota(pid: String) {
        if (pid.isBlank()) return
        scope.launch {
            quotaState = Resource.Loading
            quotaState = repository.fetchQuota(pid)
        }
    }

    LaunchedEffect(activeProjectId) {
        loadQuota(activeProjectId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Quota & Daily Usage", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        ProjectSelectorBar(
            onProjectChanged = { newPid ->
                activeProjectId = newPid
                loadQuota(newPid)
            },
            onRefreshClick = { loadQuota(activeProjectId) }
        )

        ResourceStateBox(
            resource = quotaState,
            emptyMessage = "No quota usage recorded today.",
            onRetry = { loadQuota(activeProjectId) }
        ) { data ->
            val dailyLimit = (data["daily_limit"] as? Number)?.toInt() ?: 5000
            val latest = data["latest"] as? Map<*, *>
            val usedTokens = (latest?.get("tokens_used") as? Number)?.toInt()
                ?: (latest?.get("credits_used") as? Number)?.toInt()
                ?: (data["records_count"] as? Number)?.toInt()?.times(120) ?: 0
            val usageDate = latest?.get("usage_date")?.toString() ?: data["since_date"]?.toString() ?: "Today"
            val ratio = (usedTokens.toFloat() / dailyLimit.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Fix #395: Red Banner when quota is near/exceeded
                if (ratio >= 0.9f) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Quota Exceeded / Near Limit", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("Upgrade your plan or configure your own BYOD API keys in Settings for unlimited throughput.", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }

                // Fix #391, #392, #393, #394: Clean Dashboard Card + Circular Progress Bar + Int Formatting + Usage Date
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Daily Governor Quota", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("Usage Date: $usageDate", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                            Text(
                                text = "${formatInt(usedTokens)} / ${formatInt(dailyLimit)} Credits",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("Active Records: ${formatInt(data["records_count"] ?: 0)}", style = MaterialTheme.typography.bodyLarge)
                        }
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(88.dp)) {
                            CircularProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 8.dp
                            )
                            Text("${(ratio * 100).toInt()}%", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
