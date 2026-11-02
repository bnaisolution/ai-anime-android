package org.dpdns.bnai.anime.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.launch
import org.dpdns.bnai.anime.core.auth.TokenManager
import org.dpdns.bnai.anime.core.model.Character
import org.dpdns.bnai.anime.ui.advanced.AdminQueuesScreen
import org.dpdns.bnai.anime.ui.advanced.BudgetLedgerScreen
import org.dpdns.bnai.anime.ui.advanced.CanonViewerScreen
import org.dpdns.bnai.anime.ui.advanced.CharacterDeepDiveScreen
import org.dpdns.bnai.anime.ui.advanced.ContinuityCheckerScreen
import org.dpdns.bnai.anime.ui.advanced.JobsMonitorScreen
import org.dpdns.bnai.anime.ui.advanced.LoreExplorerScreen
import org.dpdns.bnai.anime.ui.advanced.QuotaDashboardScreen
import org.dpdns.bnai.anime.ui.advanced.VisionAnalysisScreen
import org.dpdns.bnai.anime.ui.analytics.AnalyticsScreen
import org.dpdns.bnai.anime.ui.auth.AuthScreen
import org.dpdns.bnai.anime.ui.brain.AIBrainScreen
import org.dpdns.bnai.anime.ui.character.CharacterDetailScreen
import org.dpdns.bnai.anime.ui.character.CharacterListScreen
import org.dpdns.bnai.anime.ui.episode.EpisodePlannerScreen
import org.dpdns.bnai.anime.ui.library.MediaLibraryScreen
import org.dpdns.bnai.anime.ui.notifications.NotificationScreen
import org.dpdns.bnai.anime.ui.owner.AuditLogScreen
import org.dpdns.bnai.anime.ui.owner.DatabaseScreen
import org.dpdns.bnai.anime.ui.owner.OwnerOverviewScreen
import org.dpdns.bnai.anime.ui.owner.WorkerManagerScreen
import org.dpdns.bnai.anime.ui.owner.WorkflowManagerScreen
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

data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun AppNavGraph(navController: NavHostController) {
    val tokenManager = remember { TokenManager.getInstance() }
    val startDest = if (!tokenManager?.getToken().isNullOrBlank()) "studio" else "auth"
    val expiredMessage by (
        tokenManager?.sessionExpiredMessage?.collectAsState()
            ?: remember { mutableStateOf(null) }
    )

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: startDest
    var selectedCharacter by remember { mutableStateOf<Character?>(null) }

    // Patch 2: Auto-redirect to Auth on session expiry
    LaunchedEffect(expiredMessage) {
        if (!expiredMessage.isNullOrBlank() && currentRoute != "auth") {
            navController.navigate("auth") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Part 10.2: Bottom navigation destinations
    val bottomDestinations = listOf(
        NavItem("studio", "Home", Icons.Default.Home),
        NavItem("project_detail", "Projects", Icons.Default.Folder),
        NavItem("ai_brain", "AI Brain", Icons.Default.AutoAwesome),
        NavItem("production", "Production", Icons.Default.Movie),
        NavItem("episodes", "Episodes", Icons.Default.VideoLibrary)
    )

    // Part 10.4: Sectioned drawer
    val isOwner = tokenManager?.isOwner() == true
    val drawerSections: List<Pair<String, List<Pair<String, String>>>> = remember(isOwner) {
        buildList {
            add(
                "WORKSPACE" to listOf(
                    "studio" to "Home",
                    "project_detail" to "Projects",
                    "ai_brain" to "AI Brain",
                    "quick_tools" to "Quick Tools"
                )
            )
            add(
                "STORY" to listOf(
                    "bible" to "Story Bible",
                    "characters" to "Characters",
                    "character_deep_dive" to "Character Deep Dive",
                    "lore_explorer" to "Lore Explorer",
                    "episodes" to "Episodes",
                    "continuity" to "Continuity",
                    "canon" to "Canon",
                    "vision_analysis" to "Vision Analysis"
                )
            )
            add(
                "PRODUCTION" to listOf(
                    "production" to "Production",
                    "scenes" to "Scene Inspector",
                    "jobs_monitor" to "Jobs",
                    "media_library" to "Media Library",
                    "qc" to "Quality Check",
                    "review_inbox" to "Review Inbox",
                    "publish" to "Publish"
                )
            )
            add(
                "INSIGHTS" to listOf(
                    "analytics" to "Analytics",
                    "notifications" to "Notifications",
                    "audit_log" to "Audit Activity"
                )
            )
            if (isOwner) {
                add(
                    "OWNER CONTROL" to listOf(
                        "owner_overview" to "Owner Overview",
                        "workflows" to "Workflows",
                        "workers" to "Workers",
                        "admin_queues" to "Queues",
                        "quota_dashboard" to "Quota",
                        "budget_ledger" to "Budget Ledger",
                        "database" to "Database",
                        "settings" to "Settings",
                        "prompt_studio" to "Prompt Studio"
                    )
                )
            }
        }
    }

    // Auth screen: standalone NavHost (no drawer, no bottom nav)
    if (currentRoute == "auth") {
        NavHost(navController = navController, startDestination = startDest) {
            composable("auth") { AuthScreen(navController = navController) }
            composable("studio") { StudioDashboardScreen() }
        }
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    Text(
                        "AI Cinematic Anime Engine",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                    HorizontalDivider()
                    drawerSections.forEach { (sectionTitle, routes) ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            sectionTitle,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                        routes.forEach { (route, title) ->
                            NavigationDrawerItem(
                                label = { Text(title) },
                                selected = currentRoute == route,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    navController.navigate(route) {
                                        launchSingleTop = true
                                    }
                                }
                            )
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
                    TopAppBar(
                        title = { Text("AI Anime Engine") },
                        navigationIcon = {
                            IconButton(onClick = {
                                scope.launch { drawerState.open() }
                            }) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = "Open Navigation Drawer"
                                )
                            }
                        },
                        actions = {
                            // Patch 2: Logout — clear JWT + redirect to Auth
                            TextButton(onClick = {
                                tokenManager?.clearToken()
                                navController.navigate("auth") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }) {
                                Text("Logout", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                },
                bottomBar = {
                    if (!isTablet) {
                        NavigationBar {
                            bottomDestinations.forEach { item ->
                                NavigationBarItem(
                                    selected = currentRoute == item.route,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            launchSingleTop = true
                                        }
                                    },
                                    icon = {
                                        Icon(item.icon, contentDescription = item.label)
                                    },
                                    label = { Text(item.label) }
                                )
                            }
                            NavigationBarItem(
                                selected = false,
                                onClick = { scope.launch { drawerState.open() } },
                                icon = {
                                    Icon(Icons.Default.MoreHoriz, contentDescription = "More")
                                },
                                label = { Text("More") }
                            )
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
                                    onClick = {
                                        navController.navigate(item.route) {
                                            launchSingleTop = true
                                        }
                                    },
                                    icon = {
                                        Icon(item.icon, contentDescription = item.label)
                                    },
                                    label = { Text(item.label) }
                                )
                            }
                            NavigationRailItem(
                                selected = false,
                                onClick = { scope.launch { drawerState.open() } },
                                icon = {
                                    Icon(Icons.Default.MoreHoriz, contentDescription = "More")
                                },
                                label = { Text("All") }
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        NavHost(navController = navController, startDestination = startDest) {
                            composable("auth") { AuthScreen(navController = navController) }
                            composable("studio") {
                                StudioDashboardScreen(
                                    onProjectClick = {
                                        navController.navigate("project_detail")
                                    },
                                    onOpenReviews = {
                                        navController.navigate("review_inbox")
                                    },
                                    onOpenEpisodes = {
                                        navController.navigate("episodes")
                                    }
                                )
                            }
                            composable("project_detail") {
                                ProjectDetailScreen(
                                    onOpenBible = { navController.navigate("bible") },
                                    onOpenCharacters = { navController.navigate("characters") },
                                    onOpenEpisodes = { navController.navigate("episodes") },
                                    onOpenReviews = { navController.navigate("review_inbox") },
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
                                    onOpenDeepDive = {
                                        navController.navigate("character_deep_dive")
                                    }
                                )
                            }
                            composable("episodes") {
                                EpisodePlannerScreen(
                                    onStartProduction = { cmdId ->
                                        navController.navigate("production/$cmdId")
                                    },
                                    onOpenScenes = { epNo ->
                                        navController.navigate("scenes/$epNo")
                                    }
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
                            composable("qc") {
                                QCScreen(onProceedToPublish = {
                                    navController.navigate("publish")
                                })
                            }
                            composable("publish") { PublishScreen() }
                            composable("review_inbox") { ReviewInboxScreen() }
                            composable("ai_brain") { AIBrainScreen() }
                            composable("quick_tools") {
                                QuickToolsScreen(
                                    onOpenProductionPoll = { cmdId ->
                                        navController.navigate("production/$cmdId")
                                    }
                                )
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
                                CharacterDeepDiveScreen(
                                    characterId = selectedCharacter?.id.orEmpty()
                                )
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
