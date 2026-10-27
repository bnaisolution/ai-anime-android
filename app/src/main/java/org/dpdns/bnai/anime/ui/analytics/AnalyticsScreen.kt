package org.dpdns.bnai.anime.ui.analytics
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable
fun AnalyticsScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Analytics") }) }) { pad -> Text("Views: 148,920", modifier = Modifier.padding(pad).padding(16.dp)) }
}
