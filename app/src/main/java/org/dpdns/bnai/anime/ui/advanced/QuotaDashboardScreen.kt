package org.dpdns.bnai.anime.ui.advanced
import androidx.compose.ui.platform.testTag
import org.dpdns.bnai.anime.ui.testing.TestTags

import org.dpdns.bnai.anime.ui.LocalAppContainer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.JsonMap
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.formatInt
import org.dpdns.bnai.anime.ui.designsystem.*

@Composable
fun QuotaDashboardScreen(
    onOpenSettings: () -> Unit = {}
) {
    val repository = LocalAppContainer.current.repository
    val scope = rememberCoroutineScope()

    var quotaState by remember { mutableStateOf<Resource<JsonMap>>(Resource.Loading) }

    fun loadQuota() {
        val pid = repository.currentProjectId()
        if (pid.isBlank()) {
            quotaState = Resource.Empty
            return
        }
        scope.launch {
            quotaState = Resource.Loading
            quotaState = repository.fetchProjectQuota(pid)
        }
    }

    LaunchedEffect(Unit) {
        loadQuota()
    }

    Column(
        modifier = Modifier.testTag(TestTags.Screen.QUOTA_DASHBOARD)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProjectSelectorBar(
            onProjectChanged = { loadQuota() },
            onRefreshClick = { loadQuota() }
        )

        ScreenHeader(
            title = "Quota Governor Dashboard (WF-12)",
            subtitle = "Real provider usage & quota limits from quota_usage (Zero fabricated token multipliers)"
        )

        org.dpdns.bnai.anime.ui.components.ResourceStateBox(
            resource = quotaState,
            emptyMessage = "No quota usage records logged for this project.",
            onRetry = { loadQuota() }
        ) { data ->
            val recordsCount = formatInt(data["records_count"] ?: 0)
            val requestsCount = formatInt(data["requests_count"] ?: recordsCount)
            val rawTokens = data["tokens_used"]
            val tokensDisplay = if (rawTokens != null && rawTokens.toString() != "null") formatInt(rawTokens) else "Not reported"
            val dailyLimit = data["daily_limit"]?.let { lim -> formatInt(lim) } ?: "UNKNOWN"
            val sinceDate = (data["since_date"] ?: "Last 24h").toString()

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard(label = "API Requests", value = requestsCount, subtitle = "Since $sinceDate", modifier = Modifier.weight(1f))
                MetricCard(label = "Verified Tokens", value = tokensDisplay, subtitle = "Actual DB sum", modifier = Modifier.weight(1f))
            }

            StandardCard {
                SectionHeader(title = "Configured Quota Ceiling", badgeText = "Limit: $dailyLimit")
                Text(
                    text = "WF-12 Quota Governor enforces fail-closed preflight checks before episode story or video generation.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
