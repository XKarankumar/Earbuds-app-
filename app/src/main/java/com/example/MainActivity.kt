package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AppRepository
import com.example.data.local.AppDatabase
import com.example.model.AppDestination
import com.example.security.AppPreferencesRepository
import com.example.security.LocalAppPreferences
import com.example.ui.components.OceanEmeraldBackground
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.CustomerProfileScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GstCalculatorsScreen
import com.example.ui.screens.HisabKitabScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SmsCenterScreen
import com.example.ui.screens.ToolsHubScreen
import com.example.ui.screens.UnitConverterScreen
import com.example.ui.theme.LocalOceanPalette
import com.example.ui.theme.OceanEmeraldColors
import com.example.ui.theme.OceanEmeraldTheme
import com.example.ui.viewmodel.MainViewModel

private enum class BottomNavTab(
    val destination: AppDestination,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    CALC(AppDestination.Calculator, "Calc", Icons.Filled.Calculate, Icons.Outlined.Calculate),
    HOME(AppDestination.Dashboard, "Home", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    LEDGER(AppDestination.HisabKitab, "Ledger", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    SMS(AppDestination.SmsCenter, "SMS", Icons.Filled.Sms, Icons.Outlined.Sms),
    TOOLS(AppDestination.ToolsHub, "Tools", Icons.Filled.GridView, Icons.Outlined.GridView)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val prefsRepo = remember(context) { AppPreferencesRepository(context.applicationContext) }
            val prefs by prefsRepo.preferencesFlow.collectAsStateWithLifecycle(
                initialValue = LocalAppPreferences()
            )

            OceanEmeraldTheme(
                mode = prefs.themeMode,
                animationPreset = prefs.animationPreset,
                glassIntensity = prefs.glassIntensity
            ) {
                SimpleSmsCalculatorRoot(prefsRepo = prefsRepo)
            }
        }
    }
}

@Composable
private fun SimpleSmsCalculatorRoot(prefsRepo: AppPreferencesRepository) {
    val viewModel: MainViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                } as Application
                val db = AppDatabase.getInstance(app)
                val repo = AppRepository(
                    customerDao = db.customerDao(),
                    ledgerDao = db.ledgerDao(),
                    calcHistoryDao = db.calcHistoryDao(),
                    smsLogDao = db.smsLogDao()
                )
                MainViewModel(
                    application = app,
                    repository = repo,
                    prefsRepo = prefsRepo
                )
            }
        }
    )

    val backStack by viewModel.backStack.collectAsStateWithLifecycle()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val activeDest = backStack.lastOrNull() ?: AppDestination.Calculator
    val snackbarHostState = remember { SnackbarHostState() }
    val palette = LocalOceanPalette.current

    LaunchedEffect(viewModel) {
        viewModel.toastMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val activeTab = when (activeDest) {
        AppDestination.Calculator -> BottomNavTab.CALC
        AppDestination.Dashboard -> BottomNavTab.HOME
        AppDestination.HisabKitab, is AppDestination.CustomerProfile -> BottomNavTab.LEDGER
        AppDestination.SmsCenter -> BottomNavTab.SMS
        else -> BottomNavTab.TOOLS
    }

    OceanEmeraldBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            containerColor = palette.navBarSurface,
                            tonalElevation = 0.dp
                        ) {
                            BottomNavTab.entries.forEach { tab ->
                                val selected = activeTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.navigateToRootTab(tab.destination) },
                                    modifier = Modifier.testTag("nav_${tab.name.lowercase()}"),
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = OceanEmeraldColors.MintGreen,
                                        selectedTextColor = palette.jamaAccent,
                                        indicatorColor = OceanEmeraldColors.EmeraldGreen.copy(alpha = 0.24f),
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
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
                            containerColor = palette.navBarSurface
                        ) {
                            BottomNavTab.entries.forEach { tab ->
                                val selected = activeTab == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { viewModel.navigateToRootTab(tab.destination) },
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
                        val animMs = (240 * prefs.animationPreset.durationScale).toInt().coerceAtLeast(1)
                        Crossfade(
                            targetState = activeDest,
                            animationSpec = tween(durationMillis = animMs),
                            label = "ocean_screen_nav"
                        ) { screen ->
                            when (screen) {
                                AppDestination.Calculator -> CalculatorScreen(
                                    viewModel = viewModel,
                                    onNavigate = viewModel::navigateTo
                                )
                                AppDestination.Dashboard -> DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigate = viewModel::navigateTo
                                )
                                AppDestination.HisabKitab -> HisabKitabScreen(
                                    viewModel = viewModel,
                                    onNavigate = viewModel::navigateTo
                                )
                                is AppDestination.CustomerProfile -> CustomerProfileScreen(
                                    customerId = screen.customerId,
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateBack() }
                                )
                                AppDestination.SmsCenter -> SmsCenterScreen(
                                    viewModel = viewModel
                                )
                                AppDestination.ToolsHub -> ToolsHubScreen(
                                    viewModel = viewModel,
                                    onNavigate = viewModel::navigateTo
                                )
                                AppDestination.History -> HistoryScreen(
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateBack() }
                                )
                                AppDestination.Reports -> ReportsScreen(
                                    viewModel = viewModel,
                                    onNavigate = viewModel::navigateTo,
                                    onBack = { viewModel.navigateBack() }
                                )
                                AppDestination.UnitConverter -> UnitConverterScreen(
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateBack() }
                                )
                                AppDestination.GstCalculators -> GstCalculatorsScreen(
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateBack() }
                                )
                                AppDestination.Settings -> SettingsScreen(
                                    viewModel = viewModel,
                                    onBack = { viewModel.navigateBack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
