package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.data.LedgerRepository
import com.example.data.local.AppDatabase
import com.example.notifications.ReminderScheduler
import com.example.security.AppPreferencesRepository
import com.example.security.LocalAppPreferences
import com.example.ui.components.GlassAmbientBackground
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.HistoryAuditScreen
import com.example.ui.screens.LedgerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.SimpleSmsCalculatorTheme
import com.example.ui.viewmodel.MainViewModel

private enum class AppDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    CALCULATOR("calculator", "Calc", Icons.Filled.Calculate, Icons.Outlined.Calculate),
    LEDGER("ledger", "Hisab", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    TOOLS("tools", "Tools", Icons.Filled.Build, Icons.Outlined.Build),
    AUDIT("audit", "Audit", Icons.Filled.History, Icons.Outlined.History),
    SETTINGS("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ReminderScheduler.ensureChannel(this)

        setContent {
            val context = LocalContext.current
            val prefsRepo = remember(context) { AppPreferencesRepository(context.applicationContext) }
            val localPrefs by prefsRepo.preferencesFlow.collectAsStateWithLifecycle(
                initialValue = LocalAppPreferences()
            )

            SimpleSmsCalculatorTheme(
                themeId = localPrefs.themeId,
                glassIntensity = localPrefs.glassIntensity
            ) {
                OfflineWorkspace(prefsRepo = prefsRepo)
            }
        }
    }
}

@Composable
private fun OfflineWorkspace(prefsRepo: AppPreferencesRepository) {
    val viewModel: MainViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                } as Application
                val db = AppDatabase.getInstance(app)
                MainViewModel(
                    repository = LedgerRepository(db.ledgerDao()),
                    prefsRepo = prefsRepo
                )
            }
        }
    )

    val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
    val localPrefs by viewModel.localPrefs.collectAsStateWithLifecycle()

    if (isLocked == true && localPrefs.pinLockEnabled) {
        AppLockScreen(
            prefs = localPrefs,
            onUnlocked = viewModel::unlockApp
        )
        return
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var currentDest by remember { mutableStateOf(AppDestination.CALCULATOR) }

    LaunchedEffect(viewModel) {
        viewModel.toastMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    GlassAmbientBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!useNavigationRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
                        ) {
                            AppDestination.entries.forEach { dest ->
                                val selected = currentDest == dest
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentDest = dest },
                                    modifier = Modifier.testTag("nav_${dest.route}"),
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label) }
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
                    if (useNavigationRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                        ) {
                            AppDestination.entries.forEach { dest ->
                                val selected = currentDest == dest
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { currentDest = dest },
                                    modifier = Modifier.testTag("rail_${dest.route}"),
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label) }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        Crossfade(targetState = currentDest, label = "main_nav") { dest ->
                            when (dest) {
                                AppDestination.CALCULATOR -> CalculatorScreen(
                                    viewModel = viewModel,
                                    onNavigateToLedger = { currentDest = AppDestination.LEDGER }
                                )
                                AppDestination.LEDGER -> LedgerScreen(viewModel = viewModel)
                                AppDestination.TOOLS -> ToolsScreen(
                                    viewModel = viewModel,
                                    onSendToCalculator = { currentDest = AppDestination.CALCULATOR }
                                )
                                AppDestination.AUDIT -> HistoryAuditScreen(viewModel = viewModel)
                                AppDestination.SETTINGS -> SettingsScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
