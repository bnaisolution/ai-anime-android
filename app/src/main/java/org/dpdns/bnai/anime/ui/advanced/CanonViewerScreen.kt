package org.dpdns.bnai.anime.ui.advanced
import androidx.compose.ui.platform.testTag
import org.dpdns.bnai.anime.ui.testing.TestTags

import org.dpdns.bnai.anime.ui.LocalAppContainer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.*
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.StatusBadge
import org.dpdns.bnai.anime.ui.components.cleanDisplayTitle

@Composable
fun CanonViewerScreen() {
    val context = LocalContext.current
    val repository = LocalAppContainer.current.repository
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) } // 0: Locked Facts, 1: Pending Proposals
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    var canonState by remember { mutableStateOf<Resource<List<JsonMap>>>(Resource.Loading) }
    var proposalsState by remember { mutableStateOf<Resource<List<JsonMap>>>(Resource.Loading) }

    var showAddFactDialog by remember { mutableStateOf(false) }
    var newFactText by remember { mutableStateOf("") }
    var lockFactImmediately by remember { mutableStateOf(true) }

    fun loadAll() {
        val pid = repository.currentProjectId()
        scope.launch {
            canonState = Resource.Loading
            canonState = repository.fetchCanon(pid)
            proposalsState = Resource.Loading
            proposalsState = repository.fetchCanonProposals(pid)
        }
    }

    LaunchedEffect(Unit) { loadAll() }

    // Fix #307: Add Canon Fact Dialog
    if (showAddFactDialog) {
        AlertDialog(
            onDismissRequest = { showAddFactDialog = false },
            title = { Text("➕ Add Canon Fact", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newFactText,
                        onValueChange = { newFactText = it },
                        label = { Text("Immutable Story / Character Fact") },
                        placeholder = { Text("e.g. Character A lost their right arm in Episode 3...") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Lock Fact Immediately (🔒 LOCKED)")
                        Switch(checked = lockFactImmediately, onCheckedChange = { lockFactImmediately = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.submitStudioCommand(
                                repository.currentProjectId(),
                                CommandIntent.CHECK_CANON,
                                "SAVE",
                                payload = mapOf("fact" to newFactText, "locked" to lockFactImmediately)
                            )
                            Toast.makeText(context, "Canon fact committed!", Toast.LENGTH_SHORT).show()
                            newFactText = ""
                            showAddFactDialog = false
                            loadAll()
                        }
                    },
                    enabled = newFactText.isNotBlank()
                ) { Text("Commit Fact") }
            },
            dismissButton = {
                TextButton(onClick = { showAddFactDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(modifier = Modifier.testTag(TestTags.Screen.CANON),
        // Fix #307: FAB (+) for "Add Canon Fact"
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddFactDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Canon Fact") },
                text = { Text("Add Canon Fact") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Fix #301: Header "Canon & Facts" + Project Selector
            ProjectSelectorBar(
                onProjectChanged = { loadAll() },
                onRefreshClick = { loadAll() }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Canon & Facts", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                // Fix #302: Clean "Commit Canon" button layout
                Button(onClick = {
                    scope.launch {
                        repository.submitStudioCommand(repository.currentProjectId(), CommandIntent.CHECK_CANON, "SAVE")
                        Toast.makeText(context, "Canon verification & commit triggered", Toast.LENGTH_SHORT).show()
                        loadAll()
                    }
                }) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Commit Canon")
                }
            }

            // Fix #306: 2 Tabs ("Locked Facts", "Pending Proposals")
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Locked Facts") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Pending Proposals") })
            }

            // Fix #308: Search and Filter Chips
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search canon facts or proposals...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (selectedTab == 0) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("All", "Locked", "Changeable")) { chip ->
                        FilterChip(
                            selected = selectedFilter == chip,
                            onClick = { selectedFilter = chip },
                            label = { Text(chip) }
                        )
                    }
                }

                when (val res = canonState) {
                    is Resource.Idle, is Resource.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is Resource.Empty, is Resource.Error -> {
                        // Fix #303 & #312: "No canon facts locked yet." + "How Canon Works" guide
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("No canon facts locked yet.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "How Canon Works: Locked facts act as immutable guardrails for all 14 StoryWF pipelines. Any AI script or visual prompt that contradicts a 🔒 LOCKED fact is automatically flagged.",
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    is Resource.Success -> {
                        val filtered = res.data.filter { map ->
                            val isLocked = map["locked"] != false
                            val matchesSearch = searchQuery.isBlank() || map.values.any { it?.toString()?.contains(searchQuery, ignoreCase = true) == true }
                            val matchesChip = when (selectedFilter) {
                                "Locked" -> isLocked
                                "Changeable" -> !isLocked
                                else -> true
                            }
                            matchesSearch && matchesChip
                        }

                        // Fix #304 & #305: Custom Cards with 🔒 LOCKED / 🔓 CHANGEABLE badges
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(filtered) { item ->
                                val isLocked = item["locked"] != false
                                val title = cleanDisplayTitle((item["fact"] ?: item["title"] ?: item["rule_code"] ?: "Canon Fact").toString())
                                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                            StatusBadge(if (isLocked) "🔒 LOCKED" else "🔓 CHANGEABLE")
                                        }
                                        item.entries.forEach { (k, v) ->
                                            if (k !in setOf("id", "project_id", "fact", "title", "locked", "created_at", "updated_at") && v != null) {
                                                Text("${k.replace("_", " ").replaceFirstChar { it.uppercase() }}: $v")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Pending Proposals Tab (Items #309, #310, #311)
                when (val res = proposalsState) {
                    is Resource.Idle, is Resource.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is Resource.Empty, is Resource.Error -> {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("No pending canon proposals.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                Text("When an episode script proposes a major lore or character change, it appears here with a Red/Green diff for approval.", color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                    is Resource.Success -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(res.data) { prop ->
                                val propId = prop["id"]?.toString().orEmpty()
                                val diff = prop["exact_diff"] as? Map<*, *> ?: emptyMap<String, Any>()
                                val oldVal = (diff["old_value"] ?: "Existing Locked Canon").toString()
                                val newVal = (diff["new_value"] ?: prop["decision_reason"] ?: "Proposed Canon Mutation").toString()

                                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Canon Change Proposal", fontWeight = FontWeight.Bold)
                                            StatusBadge((prop["status"] ?: "PENDING").toString())
                                        }

                                        // Fix #310: Old vs New (Red/Green diff)
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                                                .padding(10.dp)
                                        ) {
                                            Text("- Old Canon", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text(oldVal)
                                        }
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF10B981).copy(alpha = 0.12f))
                                                .padding(10.dp)
                                        ) {
                                            Text("+ Proposed Canon", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text(newVal)
                                        }

                                        // Fix #309: Approve / Reject Canon Change buttons
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Button(
                                                onClick = {
                                                    scope.launch {
                                                        repository.submitStudioCommand(
                                                            repository.currentProjectId(),
                                                            CommandIntent.APPROVE_CANON_CHANGE,
                                                            "SAVE",
                                                            payload = mapOf("proposal_id" to propId)
                                                        )
                                                        Toast.makeText(context, "Canon change approved!", Toast.LENGTH_SHORT).show()
                                                        loadAll()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("✅ Approve")
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    scope.launch {
                                                        repository.submitStudioCommand(
                                                            repository.currentProjectId(),
                                                            CommandIntent.REJECT_CANON_CHANGE,
                                                            "SAVE",
                                                            payload = mapOf("proposal_id" to propId)
                                                        )
                                                        Toast.makeText(context, "Canon change rejected.", Toast.LENGTH_SHORT).show()
                                                        loadAll()
                                                    }
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("❌ Reject")
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
    }
}
