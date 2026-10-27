package org.dpdns.bnai.anime.ui.notifications
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable
fun NotificationScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Notifications") }) }) { pad -> Text("Alerts Here", modifier = Modifier.padding(pad).padding(16.dp)) }
}
