package org.dpdns.bnai.anime.ui.navigation

import org.dpdns.bnai.anime.ui.LocalAppContainer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.model.Character
import org.dpdns.bnai.anime.ui.advanced.*
import org.dpdns.bnai.anime.ui.analytics.AnalyticsScreen
import org.dpdns.bnai.anime.ui.auth.AuthScreen
import org.dpdns.bnai.anime.ui.brain.AIBrainScreen
import org.dpdns.bnai.anime.ui.character.CharacterDetailScreen
import org.dpdns.bnai.anime.ui.character.CharacterListScreen
import org.dpdns.bnai.anime.ui.components.GlobalAiDirectorBottomSheet
import org.dpdns.bnai.anime.ui.episode.EpisodePlannerScreen
import org.dpdns.bnai.anime.ui.library.MediaLibraryScreen
import org.dpdns.bnai.anime.ui.notifications.NotificationScreen
import org.dpdns.bnai.anime.ui.owner.*
import org.dpdns.bnai.anime.ui.production.ProductionScreen
import org.dpdns.bnai.anime.ui.production.SceneDetailScreen
import org.dpdns.bnai.anime.ui.project.BibleScreen
import org.dpdns.bnai.anime.ui.project.ProjectDetailScreen
import org.dpdns.bnai.anime.ui.publish.PublishScreen
import org.dpdns.bnai.anime.ui.qc.QCScreen
import org.dpdns.bnai.anime.ui.review.ReviewInboxScreen
import org.dpdns.bnai.anime.ui.settings.SettingsScreen
import org.dpdns.bnai.anime.ui.studio.PromptStudioScreen
import org.dpdns.bnai.anime.ui.studio.StudioDashboardScreen
import org.dpdns.bnai.anime.ui.tools.QuickToolsScreen
import org.dpdns.bnai.anime.ui.testing.TestTags

data class NavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun AppNavGraph(navController: NavHostController) {
    val tokenManager = LocalAppContainer.current.tokenManager
    val repository = LocalAppContainer.current.repository
    val startDest = if (!tokenManager.getToken().isNullOrBlank()) "studio" else "auth"
    val expiredMessage by tokenManager.sessionExpiredMessage.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: startDest
    var selectedCharacter by remember { mutableStateOf<Character?>(null) }

    var isBackendHealthy by remember { mutableStateOf(true) }
    var activeProviderBadge by remember { mutableStateOf(tokenManager.getConfiguredProviderLabel()) }
    var showGlobalAiSheet by remember { mutableStateOf(false) }
    var fabOffsetX by remember { mutableStateOf(tokenManager.getFabOffsetX()) }
    var fabOffsetY by remember { mutableStateOf(tokenManager.getFabOffsetY()) }

    LaunchedEffect(currentRoute) {
        if (currentRoute != "auth") {
            isBackendHealthy = repository.checkBackendHealth()
            activeProviderBadge = repository.resolveConfiguredAiProviderBadge()
            tokenManager.saveConfiguredProviderLabel(activeProviderBadge)
        }
    }

    LaunchedEffect(expiredMessage) {
        if (!expiredMessage.isNullOrBlank() && currentRoute != "auth") {
            navController.navigate("auth") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val bottomDestinations = listOf(
        NavItem("studio", "Home", Icons.Default.Home),
        NavItem("project_detail", "Projects", Icons.Default.Folder),
        NavItem("ai_brain", "AI Director", Icons.Default.AutoAwesome),
        NavItem("production", "Pipeline", Icons.Default.Movie),
        NavItem("episodes", "Episodes", Icons.Default.VideoLibrary)
    )

    val drawerSections = remember(tokenManager.isOwner()) {
        buildList {
            add("WORKSPACE" to listOf(
                "studio" to "Home Dashboard",
                "project_detail" to "Active Project",
                "ai_brain" to "AI Director",
                "quick_tools" to "Quick Generate",
                "prompt_studio" to "Prompt Studio"
            ))
            add("STORY" to listOf(
                "bible" to "Story Bible",
                "characters" to "Characters",
                "character_deep_dive" to "Character Deep Dive",
                "lore_explorer" to "World Wiki",
                "episodes" to "Episodes",
                "continuity" to "Continuity Checker",
                "canon" to "Canon & Facts",
                "vision_analysis" to "Vision DNA"
            ))
            add("PRODUCTION" to listOf(
                "production" to "Pipeline Monitor",
                "scenes" to "Scene Inspector",
                "jobs_monitor" to "Pipeline Jobs",
                "media_library" to "Media Library",
                "qc" to "Quality Check",
                "review_inbox" to "Review & Approvals",
                "publish" to "Publish Studio"
            ))
            add("INSIGHTS" to listOf(
                "analytics" to "Project Analytics",
                "quota_dashboard" to "Quota & Usage",
                "budget_ledger" to "Resource Usage (Credits)",
                "notifications" to "Notifications",
                "audit_log" to "Audit Activity"
            ))
            if (tokenManager.isOwner()) {
                add("OWNER CONTROL" to listOf(
                    "owner_overview" to "Owner Overview",
                    "workflows" to "Workflow Engine Status",
                    "workers" to "GPU & Worker Fleet",
                    "admin_queues" to "Pipeline Queues",
                    "database" to "Database Connectivity"
                ))
            }
        }
    }

    if (currentRoute == "auth") {
        NavHost(navController = navController, startDestination = startDest) {
            composable("auth") { AuthScreen(navController = navController) }
            composable("studio") { StudioDashboardScreen() }
        }
        return
    }

    if (showGlobalAiSheet) {
        GlobalAiDirectorBottomSheet(
            currentScreen = currentRoute,
            currentScreenName = currentRoute,
            onDismiss = { showGlobalAiSheet = false },
            onNavigateToRoute = { target -> navController.navigate(target) },
            onOpenFullAiBrain = { navController.navigate("ai_brain") { launchSingleTop = true } }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentRoute != "auth",
        modifier = Modifier.testTag(TestTags.Navigation.DRAWER),
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.fillMaxHeight().padding(12.dp)) {
                    val userEmail = tokenManager.getUserEmail().ifBlank { "studio@bnai.dpdns.org" }
                    val userRole = tokenManager.getUserRole().uppercase()
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = userEmail.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(userEmail.substringBefore("@"), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                    Text(userEmail, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                AssistChip(onClick = {}, label = { Text(userRole, fontSize = 10.sp) })
                            }
                            OutlinedButton(
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    navController.navigate("ai_brain") { launchSingleTop = true }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AI Director & Chat History")
                            }
                        }
                    }

                    HorizontalDivider()

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 6.dp)
                    ) {
                        drawerSections.forEach { (sectionTitle, routes) ->
                            // Always-visible sections: earlier collapsible headers did not respond to taps on device,
                            // which blocked navigation to Project Detail / Story Bible / Owner Control.
                            Text(
                                text = sectionTitle,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 4.dp)
                            )
                            routes.forEach { (route, title) ->
                                NavigationDrawerItem(
                                    label = { Text(title) },
                                    selected = currentRoute == route,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        navController.navigate(route) { launchSingleTop = true }
                                    },
                                    modifier = Modifier
                                        .padding(vertical = 2.dp)
                                        .testTag(TestTags.item(TestTags.Navigation.DRAWER_ITEM_PREFIX, route))
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigate("settings") { launchSingleTop = true }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Settings")
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    repository.logout()
                                    navController.navigate("auth") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f).testTag(TestTags.Settings.LOGOUT)
                        ) {
                            Text("Logout")
                        }
                    }
                }
            }
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isTablet = maxWidth >= 600.dp
            Scaffold(
                topBar = {
                    if (currentRoute != "auth") TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.clickable {
                                    navController.navigate("studio") { launchSingleTop = true }
                                }
                            ) {
                                Text("AI Studio", fontWeight = FontWeight.Bold)
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isBackendHealthy) Color(0xFF10B981) else Color(0xFFEF4444))
                                )
                                Text(
                                    text = if (isBackendHealthy) "Online" else "Offline",
                                    fontSize = 10.sp,
                                    color = if (isBackendHealthy) Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            }
                        },
                        navigationIcon = {
                            // Only Home shows the drawer hamburger. Every other screen shows an explicit
                            // back arrow so users never get stuck without navigation (B-10).
                            if (currentRoute == "studio") {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Open Navigation Drawer")
                                }
                            } else {
                                IconButton(onClick = {
                                    if (!navController.popBackStack()) {
                                        navController.navigate("studio") {
                                            popUpTo("studio") { inclusive = true }
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Navigate Back")
                                }
                            }
                        },
                        actions = {
                            // Provider badge is a Home-only indicator (B-15): it is contextual, not a global control.
                            if (currentRoute == "studio") {
                                AssistChip(
                                    onClick = { navController.navigate("settings") { launchSingleTop = true } },
                                    label = { Text(activeProviderBadge, fontSize = 11.sp) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                    )
                },
                floatingActionButton = {
                    if (isGlobalFabVisible(currentRoute)) {
                        val density = LocalDensity.current
                        val maxLeftDp = (maxWidth.value - 72f).coerceAtLeast(0f)
                        val maxUpDp = (maxHeight.value - 140f).coerceAtLeast(0f)
                        // Home already has its own "Create Project" FAB in the bottom-end slot,
                        // so the AI FAB floats above it instead of overlapping it.
                        val baseLiftDp = if (currentRoute == "studio") -88f else 0f
                        FloatingActionButton(
                            onClick = { showGlobalAiSheet = true },
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        with(density) { fabOffsetX.dp.roundToPx() },
                                        with(density) { (fabOffsetY + baseLiftDp).dp.roundToPx() }
                                    )
                                }
                                .pointerInput(maxLeftDp, maxUpDp) {
                                    detectDragGestures(
                                        onDragEnd = {
                                            val bounded = clampFabOffset(fabOffsetX, fabOffsetY, maxLeftDp, maxUpDp)
                                            fabOffsetX = bounded.xDp
                                            fabOffsetY = bounded.yDp
                                            tokenManager.saveFabOffset(fabOffsetX, fabOffsetY)
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val bounded = clampFabOffset(
                                                fabOffsetX + with(density) { dragAmount.x.toDp().value },
                                                fabOffsetY + with(density) { dragAmount.y.toDp().value },
                                                maxLeftDp,
                                                maxUpDp
                                            )
                                            fabOffsetX = bounded.xDp
                                            fabOffsetY = bounded.yDp
                                        }
                                    )
                                }
                                .testTag(TestTags.Navigation.AI_DIRECTOR_FAB),
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Ask AI Director", tint = Color.White)
                        }
                    }
                },
                bottomBar = {
                    if (!isTablet && currentRoute != "auth") {
                        NavigationBar {
                            bottomDestinations.forEach { item ->
                                NavigationBarItem(
                                    selected = currentRoute == item.route,
                                    onClick = { navController.navigate(item.route) { launchSingleTop = true } },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = {
                                        Text(
                                            text = item.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 11.sp
                                        )
                                    },
                                    modifier = Modifier.testTag(
                                        TestTags.item(TestTags.Navigation.BOTTOM_ITEM_PREFIX, item.route)
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    if (isTablet) {
                        NavigationRail {
                            bottomDestinations.forEach { item ->
                                NavigationRailItem(
                                    selected = currentRoute == item.route,
                                    onClick = { navController.navigate(item.route) { launchSingleTop = true } },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label, maxLines = 1) },
                                    modifier = Modifier.testTag(
                                        TestTags.item(TestTags.Navigation.BOTTOM_ITEM_PREFIX, item.route)
                                    )
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        NavHost(navController = navController, startDestination = startDest) {
                            composable("auth") { AuthScreen(navController = navController) }
                            composable("studio") {
                                StudioDashboardScreen(
                                    onProjectClick = { navController.navigate("project_detail") },
                                    onOpenReviews = { navController.navigate("review_inbox") },
                                    onOpenEpisodes = { navController.navigate("episodes") }
                                )
                            }
                            composable("project_detail") {
                                ProjectDetailScreen(
                                    onOpenBible = { navController.navigate("bible") },
                                    onOpenCharacters = { navController.navigate("characters") },
                                    onOpenEpisodes = { navController.navigate("episodes") },
                                    onOpenReviews = { navController.navigate("review_inbox") },
                                    onOpenReviewInbox = { navController.navigate("review_inbox") },
                                    onOpenAnalytics = { navController.navigate("analytics") }
                                )
                            }
                            composable("bible") { BibleScreen() }
                            composable("characters") {
                                CharacterListScreen(
                                    onCharacterClick = { ch ->
                                        selectedCharacter = ch
                                        navController.navigate("character_detail")
                                    },
                                    onOpenDeepDive = { id ->
                                        selectedCharacter = Character(id, id, "", "", "")
                                        navController.navigate("character_deep_dive")
                                    }
                                )
                            }
                            composable("character_detail") {
                                CharacterDetailScreen(
                                    characterId = selectedCharacter?.id.orEmpty(),
                                    initialCharacter = selectedCharacter,
                                    onOpenDeepDive = { navController.navigate("character_deep_dive") }
                                )
                            }
                            composable("episodes") {
                                EpisodePlannerScreen(
                                    onStartProduction = { cmdId -> navController.navigate("production/$cmdId") },
                                    onOpenScenes = { epNo -> navController.navigate("scenes/$epNo") }
                                )
                            }
                            composable("production") {
                                ProductionScreen(
                                    onViewVideoClick = { navController.navigate("qc") },
                                    onOpenSceneInspector = { navController.navigate("scenes") },
                                    onOpenMediaLibrary = { navController.navigate("media_library") }
                                )
                            }
                            composable("production/{cmdId}") { entry ->
                                ProductionScreen(
                                    episodeId = entry.arguments?.getString("cmdId").orEmpty(),
                                    onViewVideoClick = { navController.navigate("qc") },
                                    onOpenSceneInspector = { navController.navigate("scenes") },
                                    onOpenMediaLibrary = { navController.navigate("media_library") }
                                )
                            }
                            composable("scenes") { SceneDetailScreen(1) }
                            composable("scenes/{epNo}") { entry ->
                                val ep = entry.arguments?.getString("epNo")?.toIntOrNull() ?: 1
                                SceneDetailScreen(ep)
                            }
                            composable("qc") { QCScreen(onProceedToPublish = { navController.navigate("publish") }) }
                            composable("publish") { PublishScreen() }
                            composable("review_inbox") { ReviewInboxScreen() }
                            composable("ai_brain") { AIBrainScreen() }
                            composable("quick_tools") {
                                QuickToolsScreen(onOpenProductionPoll = { cmdId -> navController.navigate("production/$cmdId") })
                            }
                            composable("settings") { SettingsScreen() }
                            composable("prompt_studio") { PromptStudioScreen() }
                            composable("owner_overview") { OwnerOverviewScreen() }
                            composable("workflows") { WorkflowManagerScreen() }
                            composable("workers") { WorkerManagerScreen() }
                            composable("database") { DatabaseScreen() }
                            composable("audit_log") { AuditLogScreen() }
                            composable("analytics") { AnalyticsScreen() }
                            composable("media_library") { MediaLibraryScreen() }
                            composable("notifications") { NotificationScreen() }
                            composable("vision_analysis") { VisionAnalysisScreen() }
                            composable("jobs_monitor") { JobsMonitorScreen() }
                            composable("quota_dashboard") { QuotaDashboardScreen() }
                            composable("canon") { CanonViewerScreen() }
                            composable("continuity") { ContinuityCheckerScreen() }
                            composable("lore_explorer") { LoreExplorerScreen() }
                            composable("character_deep_dive") {
                                CharacterDeepDiveScreen(characterId = selectedCharacter?.id.orEmpty())
                            }
                            composable("admin_queues") { AdminQueuesScreen() }
                            composable("budget_ledger") { BudgetLedgerScreen() }
                        }
                    }
                }
            }
        }
    }
}
