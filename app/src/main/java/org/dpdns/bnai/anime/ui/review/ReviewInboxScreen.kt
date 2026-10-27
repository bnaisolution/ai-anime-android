package org.dpdns.bnai.anime.ui.review

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class ApprovalRequestItem(
    val id: String,
    val type: String,
    val summary: String,
    val diffPreview: String,
    val decisionStatus: String = "PENDING"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewInboxScreen() {
    val approvals = remember {
        mutableStateListOf(
            ApprovalRequestItem(
                id = "APR_101",
                type = "Canon Change",
                summary = "Update Character Status: Commander Meera unlocked Astral Resonance Tier 2.",
                diffPreview = "Before: Tier 1 | After: Tier 2 (Effective Episode 5)"
            ),
            ApprovalRequestItem(
                id = "APR_102",
                type = "Voice Change",
                summary = "Switch Lord Kaalraj TTS voice profile to deeper sovereign timbre.",
                diffPreview = "Before: supertonic_v1 | After: supertonic_hi_male_villain_02"
            ),
            ApprovalRequestItem(
                id = "APR_103",
                type = "Publish Request",
                summary = "Release Episode 5: The Crimson Eclipse to connected YouTube channel.",
                diffPreview = "QC Gate: PASSED (142.4 MB MP4 verified)"
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Review & Approval Inbox (S11)",
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
            items(approvals, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.type,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = item.decisionStatus,
                                style = MaterialTheme.typography.labelLarge,
                                color = when (item.decisionStatus) {
                                    "APPROVED" -> Color(0xFF4CAF50)
                                    "REJECTED" -> Color(0xFFE53935)
                                    else -> MaterialTheme.colorScheme.secondary
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = item.summary,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = item.diffPreview,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    val idx = approvals.indexOfFirst { it.id == item.id }
                                    if (idx != -1) {
                                        approvals[idx] = approvals[idx].copy(decisionStatus = "APPROVED")
                                    }
                                },
                                enabled = item.decisionStatus == "PENDING",
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Approve")
                            }

                            OutlinedButton(
                                onClick = {
                                    val idx = approvals.indexOfFirst { it.id == item.id }
                                    if (idx != -1) {
                                        approvals[idx] = approvals[idx].copy(decisionStatus = "REJECTED")
                                    }
                                },
                                enabled = item.decisionStatus == "PENDING",
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Reject",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
