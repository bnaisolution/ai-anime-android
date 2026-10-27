package org.dpdns.bnai.anime.ui.review
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable
fun ReviewInboxScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Review Inbox") }) }) { pad -> Text("Approvals List here", modifier = Modifier.padding(pad).padding(16.dp)) }
}
