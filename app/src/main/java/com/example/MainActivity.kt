package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PhoneInTalk
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.SearchHistoryRepository
import com.example.data.local.AppDatabase
import com.example.model.DashboardCardId
import com.example.security.AppPreferencesRepository
import com.example.security.LocalAppPreferences
import com.example.security.configureAppCheck
import com.example.ui.components.GlassAmbientBackground
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.NumberLookupScreen
import com.example.ui.screens.PhotoScannerScreen
import com.example.ui.screens.SearchHistoryScreen
import com.example.ui.screens.SecurityCenterScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SpamDetectorScreen
import com.example.ui.screens.SplashOverlay
import com.example.ui.theme.KaranFinderTheme
import com.example.ui.viewmodel.MainViewModel

private enum class TopNavTab(
    val cardId: DashboardCardId?,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(null, "Home", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    LOOKUP(DashboardCardId.NUMBER_LOOKUP, "Lookup", Icons.Filled.PhoneInTalk, Icons.Outlined.PhoneInTalk),
    SCAN(DashboardCardId.PHOTO_SCANNER, "Photo AI", Icons.Filled.DocumentScanner, Icons.Outlined.DocumentScanner),
    ASSISTANT(DashboardCardId.AI_ASSISTANT, "AI Chat", Icons.Filled.SmartToy, Icons.Outlined.SmartToy),
    HISTORY(DashboardCardId.SEARCH_HISTORY, "History", Icons.Filled.History, Icons.Outlined.History)
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory Step 1: Configure Firebase App Check with launch intent before Compose UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val prefsRepo = remember(context) { AppPreferencesRepository(context.applicationContext) }
            val localPrefs by prefsRepo.preferencesFlow.collectAsStateWithLifecycle(
                initialValue = LocalAppPreferences()
            )

            KaranFinderTheme(
                themeId = localPrefs.themeId,
                glassIntensity = localPrefs.glassIntensity
            ) {
                KaranFinderRoot(prefsRepo = prefsRepo)
            }
        }
    }
}

@Composable
private fun KaranFinderRoot(prefsRepo: AppPreferencesRepository) {
    val viewModel: MainViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                } as Application
                val db = AppDatabase.getInstance(app)
                MainViewModel(
                    application = app,
                    historyRepo = SearchHistoryRepository(db.searchHistoryDao()),
                    prefsRepo = prefsRepo
                )
            }
        }
    )

    val showSplash by viewModel.showSplash.collectAsStateWithLifecycle()
    val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
    val localPrefs by viewModel.localPrefs.collectAsStateWithLifecycle()
    val activeScreen by viewModel.activeScreen.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.toastMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLocked == true && localPrefs.pinLockEnabled) {
            AppLockScreen(
                prefs = localPrefs,
                onUnlocked = viewModel::unlockApp
            )
        } else {
            GlassAmbientBackground {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useRail = maxWidth >= 600.dp

                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets.safeDrawing,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            if (!useRail) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
                                ) {
                                    TopNavTab.entries.forEach { tab ->
                                        val selected = activeScreen == tab.cardId
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { viewModel.navigateTo(tab.cardId) },
                                            modifier = Modifier.testTag("nav_${tab.name.lowercase()}"),
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = tab.label
                                                )
                                            },
                                            label = { Text(tab.label) }
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (useRail) {
                                NavigationRail(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f)
                                ) {
                                    TopNavTab.entries.forEach { tab ->
                                        val selected = activeScreen == tab.cardId
                                        NavigationRailItem(
                                            selected = selected,
                                            onClick = { viewModel.navigateTo(tab.cardId) },
                                            modifier = Modifier.testTag("rail_${tab.name.lowercase()}"),
                                            icon = {
                                                Icon(
                                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = tab.label
                                                )
                                            },
                                            label = { Text(tab.label) }
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                Crossfade(targetState = activeScreen, label = "screen_nav") { screen ->
                                    when (screen) {
                                        null -> DashboardScreen(
                                            viewModel = viewModel,
                                            onOpenCard = viewModel::navigateTo
                                        )
                                        DashboardCardId.NUMBER_LOOKUP,
                                        DashboardCardId.BUSINESS_LOOKUP -> NumberLookupScreen(
                                            viewModel = viewModel,
                                            onBack = viewModel::navigateBackHome
                                        )
                                        DashboardCardId.PHOTO_SCANNER -> PhotoScannerScreen(
                                            viewModel = viewModel,
                                            onBack = viewModel::navigateBackHome
                                        )
                                        DashboardCardId.SPAM_DETECTOR -> SpamDetectorScreen(
                                            viewModel = viewModel,
                                            onBack = viewModel::navigateBackHome
                                        )
                                        DashboardCardId.AI_ASSISTANT -> AiAssistantScreen(
                                            viewModel = viewModel,
                                            onBack = viewModel::navigateBackHome
                                        )
                                        DashboardCardId.SEARCH_HISTORY -> SearchHistoryScreen(
                                            viewModel = viewModel,
                                            onBack = viewModel::navigateBackHome
                                        )
                                        DashboardCardId.SECURITY_CENTER -> SecurityCenterScreen(
                                            viewModel = viewModel,
                                            onBack = viewModel::navigateBackHome
                                        )
                                        DashboardCardId.SETTINGS -> SettingsScreen(
                                            viewModel = viewModel,
                                            onBack = viewModel::navigateBackHome
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showSplash,
            exit = fadeOut()
        ) {
            SplashOverlay(onSkip = viewModel::dismissSplash)
        }
    }
}
