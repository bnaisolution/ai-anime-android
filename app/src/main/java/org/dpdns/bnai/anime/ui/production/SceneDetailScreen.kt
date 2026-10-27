package org.dpdns.bnai.anime.ui.production

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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

data class SceneFlagItem(
    val sceneNumber: Int,
    val summary: String,
    val needs_image: Boolean,
    val needs_voice: Boolean,
    val needs_motion: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SceneDetailScreen() {
    val scenes = remember {
        listOf(
            SceneFlagItem(
                sceneNumber = 1,
                summary = "Opening establishing shot of Citadel Throne Room under crimson storm.",
                needs_image = true,
                needs_voice = true,
                needs_motion = true
            ),
            SceneFlagItem(
                sceneNumber = 2,
                summary = "Aarav confronts Commander Meera on the Astral Bridge (Lip-sync dialogue).",
                needs_image = true,
                needs_voice = true,
                needs_motion = false
            ),
            SceneFlagItem(
                sceneNumber = 3,
                summary = "High-speed blade clash as Rift portals open across the sky.",
                needs_image = true,
                needs_voice = false,
                needs_motion = false
            ),
            SceneFlagItem(
                sceneNumber = 4,
                summary = "Cliffhanger close-up: Lord Kaalraj awakens the ancient artifact.",
                needs_image = false,
                needs_voice = false,
                needs_motion = false
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Scene Inspector (S09)",
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
            items(scenes, key = { it.sceneNumber }) { scene ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Scene ${scene.sceneNumber}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = scene.summary,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            FlagStatusChip(label = "needs_image", isDone = scene.needs_image)
                            FlagStatusChip(label = "needs_voice", isDone = scene.needs_voice)
                            FlagStatusChip(label = "needs_motion", isDone = scene.needs_motion)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlagStatusChip(label: String, isDone: Boolean) {
    val statusColor = if (isDone) Color(0xFF4CAF50) else Color(0xFFE53935)
    val icon = if (isDone) Icons.Default.CheckCircle else Icons.Default.Warning

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = statusColor
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = statusColor
        )
    }
}
