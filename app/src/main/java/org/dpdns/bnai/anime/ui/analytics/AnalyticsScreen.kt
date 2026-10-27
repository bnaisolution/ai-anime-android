package org.dpdns.bnai.anime.ui.analytics
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Analytics") }) }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) { Card(Modifier.weight(1f)) { Column(Modifier.padding(12.dp)) { Text("Views"); Text("148,920", style = MaterialTheme.typography.headlineMedium) } }; Card(Modifier.weight(1f)) { Column(Modifier.padding(12.dp)) { Text("Watch Time"); Text("64,310m", style = MaterialTheme.typography.headlineMedium) } } } }
            item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("Retention Trend"); Canvas(Modifier.fillMaxWidth().height(120.dp)) { val p = listOf(0.4f, 0.6f, 0.7f, 0.9f); val path = Path(); val step = size.width / (p.size - 1); p.forEachIndexed { i, v -> if (i == 0) path.moveTo(i * step, size.height - (v * size.height)) else path.lineTo(i * step, size.height - (v * size.height)) }; drawPath(path, color = androidx.compose.ui.graphics.Color.Green, style = Stroke(5f)) } } } }
        }
    }
}
