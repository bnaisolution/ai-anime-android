package org.dpdns.bnai.anime.ui.owner

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.network.AnimeRepository
import org.dpdns.bnai.anime.core.network.Resource
import org.dpdns.bnai.anime.ui.components.ProjectSelectorBar
import org.dpdns.bnai.anime.ui.components.formatInt
import org.dpdns.bnai.anime.ui.designsystem.*

@Composable
fun BudgetLedgerScreen() {
    val context = LocalContext.current
    val repository = remember { AnimeRepository() }
    val scope = rememberCoroutineScope()

    var budgetCeiling by remember { mutableStateOf("1000") }
    var reservedAmount by remember { mutableStateOf("0") }
    var consumedAmount by remember { mutableStateOf("0") }
    var releasedAmount by remember { mutableStateOf("0") }
    var editCeilingInput by remember { mutableStateOf("1000") }
    var isLoading by remember { mutableStateOf(true) }
    var isSavingBudget by remember { mutableStateOf(false) }

    fun loadBudgetAndLedger() {
        val pid = repository.currentProjectId()
        if (pid.isBlank()) {
            isLoading = false
            return
        }
        scope.launch {
            isLoading = true
            val ovRes = repository.fetchProjectOverviewRaw(pid)
            val ledRes = repository.fetchAdminLedgerSummary()

            if (ovRes is Resource.Success) {
                val ceil = formatInt(ovRes.data["budget_ceiling"] ?: 1000)
                budgetCeiling = ceil
                editCeilingInput = ceil
            }
            if (ledRes is Resource.Success) {
                val d = ledRes.data
                reservedAmount = formatInt(d["total_reserved"] ?: d["reserved"] ?: 0)
                consumedAmount = formatInt(d["total_consumed"] ?: d["consumed"] ?: d["total_spent"] ?: 0)
                releasedAmount = formatInt(d["total_released"] ?: d["released"] ?: 0)
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadBudgetAndLedger() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProjectSelectorBar(
            onProjectChanged = { loadBudgetAndLedger() },
            onRefreshClick = { loadBudgetAndLedger() }
        )

        ScreenHeader(
            title = "Budget Ledger & Reservations",
            subtitle = "Reconciled resource reservations & project budget ceiling (Zero fake transactions)"
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(label = "Budget Ceiling", value = budgetCeiling, modifier = Modifier.weight(1f))
            MetricCard(label = "Consumed", value = consumedAmount, accentColor = StudioTokens.Warning, modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(label = "Reserved (Active)", value = reservedAmount, accentColor = StudioTokens.Secondary, modifier = Modifier.weight(1f))
            MetricCard(label = "Released", value = releasedAmount, accentColor = StudioTokens.Success, modifier = Modifier.weight(1f))
        }

        StandardCard {
            SectionHeader(title = "Update Project Budget Ceiling")
            OutlinedTextField(
                value = editCeilingInput,
                onValueChange = { editCeilingInput = it.filter { ch -> ch.isDigit() } },
                label = { Text("Budget Ceiling (Credits)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            PrimaryButton(
                text = "Save Budget Ceiling",
                isLoading = isSavingBudget,
                onClick = {
                    val pid = repository.currentProjectId()
                    val newLimit = editCeilingInput.toDoubleOrNull() ?: return@PrimaryButton
                    isSavingBudget = true
                    scope.launch {
                        val res = repository.updateBudgetLimit(pid, newLimit)
                        isSavingBudget = false
                        if (res is Resource.Success) {
                            loadBudgetAndLedger()
                            Toast.makeText(context, "Project budget ceiling persisted!", Toast.LENGTH_SHORT).show()
                        } else if (res is Resource.Error) {
                            Toast.makeText(context, res.message, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
