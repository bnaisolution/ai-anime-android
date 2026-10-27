package org.dpdns.bnai.anime.ui.production

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionScreen(
    episodeId: String = "ep_005",
    onViewVideoClick: () -> Unit = {},
    onOpenSceneInspector: () -> Unit = {},
    onOpenMediaLibrary: () -> Unit = {},
    viewModel: ProductionViewModel = viewModel()
) {
    val state by viewModel.productionState.collectAsState()

    LaunchedEffect(episodeId) {
        viewModel.startPolling(episodeId)
    }

    val timelineSteps = listOf(
        ProductionStage.Queued,
        ProductionStage.Scripting,
        ProductionStage.Rendering,
        ProductionStage.QC,
        ProductionStage.Completed
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Production Command Center",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.currentStage != ProductionStage.Completed &&
                state.currentStage != ProductionStage.Failed
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(72.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    strokeWidth = 6.dp
                )
            }

            Text(
                text = "Stage: ${state.currentStage.displayTitle}",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = state.currentStage.detailMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    timelineSteps.forEachIndexed { index, step ->
                        val currentIndex = timelineSteps.indexOf(state.currentStage)
                        val isDone = currentIndex > index || state.currentStage == ProductionStage.Completed
                        val isCurrent = state.currentStage == step

                        val indicatorColor = when {
                            isDone -> Color(0xFF4CAF50)
                            isCurrent -> MaterialTheme.colorScheme.primary
                            else -> Color.Gray
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(indicatorColor)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = step.displayTitle,
                                style = MaterialTheme.typography.headlineMedium,
                                color = if (isDone || isCurrent) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                }
                            )
                        }
                    }
                }
            }

            if (state.currentStage == ProductionStage.Completed) {
                Button(
                    onClick = onViewVideoClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = "View Video (Proceed to QC)",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            if (state.currentStage == ProductionStage.Failed) {
                Text(
                    text = state.errorMessage ?: "Production stage failed.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
                Button(
                    onClick = { viewModel.startPolling(episodeId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = "Retry",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenSceneInspector,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("S09 Scenes")
                }
                OutlinedButton(
                    onClick = onOpenMediaLibrary,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("S13 Library")
                }
            }
        }
    }
}
