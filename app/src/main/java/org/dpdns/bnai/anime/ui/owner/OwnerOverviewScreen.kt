package org.dpdns.bnai.anime.ui.owner

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerOverviewScreen(
    userRole: String = "owner",
    dbOnline: Boolean = true,
    n8nOnline: Boolean = true,
    onOpenWorkflows: () -> Unit = {},
    onOpenWorkers: () -> Unit = {},
    onOpenDatabase: () -> Unit = {},
    onOpenAuditLogs: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Owner Console — Overview (S19)",
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
        if (userRole.lowercase() != "owner") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "403 PROJECT_FORBIDDEN: Owner role required to access Owner Console.",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "System Health & Capability Registry",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            HealthIndicatorCard(
                serviceName = "DB Status (Anime Supabase PostgREST)",
                statusLabel = if (dbOnline) "VERIFIED / ONLINE" else "DOWN",
                isHealthy = dbOnline
            )

            HealthIndicatorCard(
                serviceName = "n8n Status (Orchestration Engine)",
                statusLabel = if (n8nOnline) "VERIFIED / ONLINE" else "DEGRADED",
                isHealthy = n8nOnline
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onOpenWorkflows,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("S22 n8n Workflow Manager (WF-00 to WF-13)", style = MaterialTheme.typography.labelLarge)
            }

            Button(
                onClick = onOpenWorkers,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("S21 Workers & Kaggle Compute", style = MaterialTheme.typography.labelLarge)
            }

            Button(
                onClick = onOpenDatabase,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("S23 Database & Storage Inventory", style = MaterialTheme.typography.labelLarge)
            }

            Button(
                onClick = onOpenAuditLogs,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("S25 Security & Audit Trail", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun HealthIndicatorCard(
    serviceName: String,
    statusLabel: String,
    isHealthy: Boolean
) {
    val dotColor = if (isHealthy) Color(0xFF4CAF50) else Color(0xFFE53935)

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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = serviceName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = dotColor
                )
            }
        }
    }
}
