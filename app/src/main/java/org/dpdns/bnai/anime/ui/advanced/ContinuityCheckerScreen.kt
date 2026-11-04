package org.dpdns.bnai.anime.ui.advanced

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.*
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.StatusBadge
import org.dpdns.bnai.anime.ui.components.formatRelativeTime

@Composable
fun ContinuityCheckerScreen() {
    val context = LocalContext.current
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()

    var state by remember { mutableStateOf<Resource<List<JsonMap>>>(Resource.Loading) }
    var scopeSelection by remember { mutableStateOf("Entire Project") }
    var scopeExpanded by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") }
    var isScanning by remember { mutableStateOf(false) }

    val filters = listOf("All", "Critical", "Warnings", "Passed")

    fun loadContinuity() {
        val pid = repository.currentProjectId()
        scope.launch {
            state = Resource.Loading
            state = repository.fetchContinuity(pid)
        }
    }

    LaunchedEffect(Unit) { loadContinuity() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Fix #290: Header "Continuity Checker" + Project Selector
        ProjectSelectorBar(
            onProjectChanged = { loadContinuity() },
            onRefreshClick = { loadContinuity() }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Continuity Checker", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            // Fix #292: Scope Dropdown (Entire Project or Specific Episode)
            Box {
                OutlinedButton(onClick = { scopeExpanded = true }) {
                    Text(scopeSelection)
                }
                DropdownMenu(expanded = scopeExpanded, onDismissRequest = { scopeExpanded = false }) {
                    listOf("Entire Project", "Episode 1", "Episode 2", "Episode 3").forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                scopeSelection = item
                                scopeExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Fix #289 & #300: Clean full-width button + status polling
        Button(
            onClick = {
                isScanning = true
                scope.launch {
                    val res = repository.submitStudioCommand(
                        projectId = repository.currentProjectId(),
                        intent = CommandIntent.CHECK_CONTINUITY,
                        mode = "SAVE",
                        payload = mapOf("scope" to scopeSelection)
                    )
                    if (res is Resource.Success) {
                        val cmdId = res.data.command?.id ?: res.data.command_id ?: ""
                        if (cmdId.length == 36) {
                            delay(2500L)
                            repository.fetchCommandStatus(cmdId)
                        }
                    }
                    isScanning = false
                    loadContinuity()
                }
            },
            enabled = !isScanning,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (isScanning) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scanning Timeline & Character Continuity...")
            } else {
                Text("Run Continuity Scan ($scopeSelection)")
            }
        }

        // Fix #296: Filter Chips (All, Critical, Warnings, Passed)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters) { f ->
                FilterChip(
                    selected = selectedFilter == f,
                    onClick = { selectedFilter = f },
                    label = { Text(f) }
                )
            }
        }

        when (val res = state) {
            is Resource.Idle, is Resource.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is Resource.Empty, is Resource.Error -> {
                // Fix #291 & #297: Clean Empty State "No plot holes detected." + Guide Card
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(40.dp))
                            Text("No plot holes detected.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Continuity Checker verifies that dead characters don't speak, injuries persist across episodes, and power rules aren't violated.",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
            is Resource.Success -> {
                val filtered = res.data.filter { item ->
                    val status = (item["status"] ?: item["severity"] ?: "PASSED").toString().uppercase()
                    when (selectedFilter) {
                        "Critical" -> status in setOf("CRITICAL", "FAILED", "ERROR")
                        "Warnings" -> status in setOf("WARNING", "PENDING")
                        "Passed" -> status in setOf("PASSED", "OK", "RESOLVED")
                        else -> true
                    }
                }

                // Fix #293 & #294: Cards with ⚠️ / ✅ and "Fix with AI" button
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filtered) { item ->
                        val status = (item["status"] ?: item["severity"] ?: "PASSED").toString().uppercase()
                        val isIssue = status !in setOf("PASSED", "OK", "RESOLVED")
                        val summary = (item["issue"] ?: item["description"] ?: item["summary"] ?: "Timeline & Character State Check").toString()
                        val checkedAt = (item["checked_at"] ?: item["created_at"])?.toString()

                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = if (isIssue) Icons.Default.Warning else Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (isIssue) Color(0xFFF59E0B) else Color(0xFF10B981)
                                        )
                                        Text(summary, fontWeight = FontWeight.Bold)
                                    }
                                    StatusBadge(status)
                                }

                                if (!checkedAt.isNullOrBlank()) {
                                    Text("Checked ${formatRelativeTime(checkedAt)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                }

                                if (isIssue) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                repository.submitStudioCommand(
                                                    repository.currentProjectId(),
                                                    CommandIntent.SAVE_BIBLE,
                                                    "SAVE",
                                                    payload = mapOf("user_prompt" to "Fix continuity issue: $summary")
                                                )
                                                Toast.makeText(context, "✨ AI Director is resolving continuity conflict...", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Fix with AI")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
