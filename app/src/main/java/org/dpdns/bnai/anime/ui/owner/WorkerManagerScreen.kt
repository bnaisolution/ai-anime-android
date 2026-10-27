package org.dpdns.bnai.anime.ui.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerManagerScreen() {
    var isSessionActive by remember { mutableStateOf(true) }
    var pendingDialogAction by remember { mutableStateOf<String?>(null) }

    pendingDialogAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingDialogAction = null },
            title = {
                Text(
                    text = if (action == "START") "Start Kaggle Session?" else "Stop Kaggle Session?",
                    style = MaterialTheme.typography.headlineMedium
                )
            },
            text = {
                Text(
                    text = if (action == "START") {
                        "This will boot the 'kaggle_story_engine' dual-GPU worker session via WF-04."
                    } else {
                        "Rule 10-Min Destructive Gate: Stopping the Kaggle session will interrupt active GPU renders. Confirm?"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSessionActive = (action == "START")
                        pendingDialogAction = null
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDialogAction = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Workers & Kaggle Compute (S21)",
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isSessionActive) "Kaggle Session: ACTIVE" else "Kaggle Session: INACTIVE",
                        style = MaterialTheme.typography.headlineLarge,
                        color = if (isSessionActive) Color(0xFF4CAF50) else Color(0xFFE53935)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "GPU_0: AnimateDiff | GPU_1: MuseTalk + LivePortrait",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            Button(
                onClick = { pendingDialogAction = "START" },
                enabled = !isSessionActive,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Start Kaggle Session",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Button(
                onClick = { pendingDialogAction = "STOP" },
                enabled = isSessionActive,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Stop Kaggle Session",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
