package org.dpdns.bnai.anime.ui.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.HealthResponseDto
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ResourceStateBox
import org.dpdns.bnai.anime.ui.components.StatusBadge

@Composable
fun DatabaseScreen() {
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()
    var healthState by remember {
        mutableStateOf<Resource<HealthResponseDto>>(Resource.Loading)
    }

    fun load() {
        scope.launch {
            healthState = Resource.Loading
            healthState = repository.fetchHealthDetail()
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Database Connectivity Status", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Android is strictly isolated from direct Supabase access and queries health exclusively through Fastify (/v1/anime/health)."
        )
        ResourceStateBox(resource = healthState, onRetry = { load() }) { health ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Anime PostgreSQL Probe",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        StatusBadge(health.db ?: if (health.ok) "UP" else "DOWN")
                    }
                    Text("Backend Service Version: ${health.version ?: "N/A"}")
                }
            }
        }
    }
}
