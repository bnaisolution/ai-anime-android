package org.dpdns.bnai.anime.ui.brain

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dpdns.bnai.anime.core.network.DirectorReadinessDto

private val READINESS_ORDER = listOf(
    "database", "ai_text", "ai_vision", "gpu", "cpu", "storage", "telegram", "github", "youtube", "instagram"
)

private fun resourceLabel(key: String, entry: org.dpdns.bnai.anime.core.network.ReadinessResourceDto): String {
    val name = key.replace('_', ' ')
    val state = entry.mode ?: entry.status
    return "$name: $state"
}

/**
 * Compact BYOD readiness strip. Shows which user resources are ready, missing or degraded
 * before the user confirms a production action.
 */
@Composable
fun DirectorReadinessStrip(
    readiness: DirectorReadinessDto?,
    modifier: Modifier = Modifier
) {
    if (readiness == null) return
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Resources: ${readiness.overall.uppercase()}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                READINESS_ORDER.forEach { key ->
                    val entry = readiness.resources[key] ?: return@forEach
                    AssistChip(
                        onClick = {},
                        label = { Text(resourceLabel(key, entry), fontSize = 10.sp) }
                    )
                }
            }
            readiness.setup_hints.take(3).forEach { hint ->
                Text(
                    text = "Setup: ${hint.message}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (readiness.blocking.isNotEmpty()) {
                Text(
                    text = "Blocked actions: ${readiness.blocking.joinToString { it.capability.replace('_', ' ') }}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
