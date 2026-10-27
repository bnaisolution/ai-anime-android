package org.dpdns.bnai.anime.ui.owner

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class WorkflowRegistryItem(
    val code: String,
    val name: String,
    val status: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowManagerScreen() {
    val workflows = remember {
        listOf(
            WorkflowRegistryItem("WF-00", "Main Intent Router & Governance Gate", "Active"),
            WorkflowRegistryItem("WF-01", "Bible Architect & Voice Catalog Assignment", "Active"),
            WorkflowRegistryItem("WF-02", "Two-Stage Story Generator & Scene Router", "Active"),
            WorkflowRegistryItem("WF-03", "Distributed GPU/CPU Production Pipeline", "Active"),
            WorkflowRegistryItem("WF-04", "Admin Panel & R12 Versioned Voice Engine", "Active"),
            WorkflowRegistryItem("WF-05", "Multi-Modal Vision Pipeline (QUICK/SAVE)", "Active"),
            WorkflowRegistryItem("WF-06", "7-Source Continuity Engine", "Active"),
            WorkflowRegistryItem("WF-07", "Multi-Platform Publisher (YouTube/Instagram)", "Active"),
            WorkflowRegistryItem("WF-08", "Analytics & Learning Feedback Engine", "Active"),
            WorkflowRegistryItem("WF-09", "Canon Engine & Knowledge Lock Guard", "Active"),
            WorkflowRegistryItem("WF-10", "State Machine Manager & Zombie Sweeper", "Active"),
            WorkflowRegistryItem("WF-11", "Automated & Human QC Delivery Gate", "Active"),
            WorkflowRegistryItem("WF-12", "Quota Governor & Budget Admission", "Inactive"),
            WorkflowRegistryItem("WF-13", "Surgical Checkpoint Retry & Self-Healer", "Error")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Workflow Manager (S22)",
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(workflows, key = { it.code }) { wf ->
                val statusColor = when (wf.status) {
                    "Active" -> Color(0xFF4CAF50)
                    "Error" -> Color(0xFFE53935)
                    else -> Color(0xFF9E9E9E)
                }

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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = wf.code,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = wf.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            color = statusColor.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = wf.status,
                                style = MaterialTheme.typography.labelLarge,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
