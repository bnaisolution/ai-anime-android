package org.dpdns.bnai.anime.ui.owner
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable
fun OwnerOverviewScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Owner Console") }) }) { pad -> Text("System Health: VERIFIED", modifier = Modifier.padding(pad).padding(16.dp)) }
}
