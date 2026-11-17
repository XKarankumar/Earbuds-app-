package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Button
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.desicalc.security.configureAppCheck
import com.example.desicalc.ui.components.AnimatedSplashOverlay
import com.example.desicalc.ui.components.DesiModalDialog
import com.example.desicalc.ui.screens.AiAssistantScreen
import com.example.desicalc.ui.screens.CalculatorScreen
import com.example.desicalc.ui.screens.CustomersAndLedgerScreen
import com.example.desicalc.ui.screens.DashboardScreen
import com.example.desicalc.ui.screens.ProfileAndSettingsScreen
import com.example.desicalc.ui.screens.SmsAndReportsScreen
import com.example.desicalc.ui.strings.localizedStrings
import com.example.desicalc.ui.theme.DesiCalculatorTheme
import com.example.desicalc.ui.theme.LocalDesiColors
import com.example.desicalc.ui.viewmodel.DesiViewModel
import kotlinx.coroutines.flow.emptyFlow

enum class DesiDestination(val route: String, val icon: ImageVector) {
    DASHBOARD("dashboard", Icons.Rounded.Dashboard),
    CALCULATOR("calculator", Icons.Rounded.Calculate),
    CUSTOMERS("customers", Icons.Rounded.Groups),
    AI_ASSISTANT("ai_assistant", Icons.Rounded.AutoAwesome),
    SMS_REPORTS("sms_reports", Icons.Rounded.Sms),
    PROFILE("profile", Icons.Rounded.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check before UI, ViewModels, or Firebase AI calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            val viewModel: DesiViewModel = viewModel()
            DesiCalculatorAppRoot(viewModel = viewModel)
        }
    }
}

@Composable
fun DesiCalculatorAppRoot(viewModel: DesiViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val allCustomers by viewModel.customerSummaries.collectAsStateWithLifecycle()
    val filteredCustomers by viewModel.filteredCustomers.collectAsStateWithLifecycle()
    val selectedCustomerId by viewModel.selectedCustomerId.collectAsStateWithLifecycle()
    val selectedCustomer = remember(allCustomers, selectedCustomerId) {
        allCustomers.firstOrNull { it.id == selectedCustomerId }
    }
    val selectedCustomerTx by remember(selectedCustomerId) {
        if (selectedCustomerId != null) {
            viewModel.repository.observeTransactionsForCustomer(selectedCustomerId!!)
        } else {
            emptyFlow()
        }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val calcHistory by viewModel.calculationHistory.collectAsStateWithLifecycle()
    val smsLogs by viewModel.smsLogs.collectAsStateWithLifecycle()
    val aiMessages by viewModel.aiChatMessages.collectAsStateWithLifecycle()
    val aiUiState by viewModel.aiUiState.collectAsStateWithLifecycle()
    val activeReport by viewModel.activePeriodReport.collectAsStateWithLifecycle()
    val reportPeriod by viewModel.reportPeriod.collectAsStateWithLifecycle()

    val searchQuery by viewModel.customerSearch.collectAsStateWithLifecycle()
    val customerFilter by viewModel.customerFilter.collectAsStateWithLifecycle()
    val calcMode by viewModel.calcMode.collectAsStateWithLifecycle()
    val calcExpression by viewModel.calcExpression.collectAsStateWithLifecycle()
    val calcResult by viewModel.calcResult.collectAsStateWithLifecycle()
    val calcError by viewModel.calcError.collectAsStateWithLifecycle()
    val duplicateWarning by viewModel.duplicateWarningPrompt.collectAsStateWithLifecycle()
    val lastBackupMessage by viewModel.lastBackupMessage.collectAsStateWithLifecycle()

    val strings = remember(settings.language) { localizedStrings(settings.language) }
    val snackbarHostState = remember { SnackbarHostState() }

    var currentDest by rememberSaveable { mutableStateOf(DesiDestination.DASHBOARD) }
    var showSplash by rememberSaveable { mutableStateOf(true) }

    val jsonBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::exportBackupToUri) }

    val csvExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let(viewModel::exportCsvToUri) }

    val jsonRestoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::importBackupOrLegacyFromUri) }

    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    DesiCalculatorTheme(settings = settings) {
        val ext = LocalDesiColors.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(ext.gradientTop, ext.gradientBottom)))
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useRail = maxWidth >= 600.dp

                Scaffold(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        if (!useRail) {
                            NavigationBar(
                                modifier = Modifier.navigationBarsPadding(),
                                containerColor = ext.cardElevated
                            ) {
                                DesiDestination.entries.forEach { dest ->
                                    val label = destLabel(dest, strings)
                                    NavigationBarItem(
                                        selected = currentDest == dest,
                                        onClick = { currentDest = dest },
                                        icon = { Icon(dest.icon, contentDescription = label) },
                                        label = {
                                            Text(
                                                text = label,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        modifier = Modifier.testTag("nav_${dest.route}")
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
                            NavigationRail(containerColor = ext.cardElevated) {
                                DesiDestination.entries.forEach { dest ->
                                    val label = destLabel(dest, strings)
                                    NavigationRailItem(
                                        selected = currentDest == dest,
                                        onClick = { currentDest = dest },
                                        icon = { Icon(dest.icon, contentDescription = label) },
                                        label = { Text(label, maxLines = 1) },
                                        modifier = Modifier.testTag("rail_${dest.route}")
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Crossfade(
                                targetState = currentDest,
                                animationSpec = tween(220),
                                modifier = Modifier.widthIn(max = 840.dp),
                                label = "mainScreenTransition"
                            ) { dest ->
                                when (dest) {
                                    DesiDestination.DASHBOARD -> DashboardScreen(
                                        metrics = metrics,
                                        customers = allCustomers,
                                        recentTransactions = allTransactions,
                                        settings = settings,
                                        strings = strings,
                                        onOpenCalculator = { currentDest = DesiDestination.CALCULATOR },
                                        onOpenCustomers = { currentDest = DesiDestination.CUSTOMERS },
                                        onSelectCustomer = { cId ->
                                            viewModel.selectCustomer(cId)
                                            currentDest = DesiDestination.CUSTOMERS
                                        },
                                        onOpenAiWithPrompt = { prompt ->
                                            currentDest = DesiDestination.AI_ASSISTANT
                                            if (prompt.isNotBlank()) viewModel.sendAiMessage(prompt)
                                        },
                                        onOpenSmsReports = { currentDest = DesiDestination.SMS_REPORTS }
                                    )

                                    DesiDestination.CALCULATOR -> CalculatorScreen(
                                        activeMode = calcMode,
                                        expression = calcExpression,
                                        result = calcResult,
                                        error = calcError,
                                        history = calcHistory,
                                        language = settings.language,
                                        onSelectMode = viewModel::setCalculatorMode,
                                        onAppendToken = viewModel::appendCalcToken,
                                        onBackspace = viewModel::backspaceCalc,
                                        onClear = viewModel::clearCalc,
                                        onEvaluate = viewModel::evaluateAndSaveCalc,
                                        onSaveSpecialized = viewModel::saveSpecializedCalculation,
                                        onReuseHistory = viewModel::reuseHistoryExpression,
                                        onClearHistory = viewModel::clearCalculationHistory,
                                        onAskAiCommand = { cmd ->
                                            currentDest = DesiDestination.AI_ASSISTANT
                                            viewModel.sendAiMessage(cmd)
                                        }
                                    )

                                    DesiDestination.CUSTOMERS -> CustomersAndLedgerScreen(
                                        customers = filteredCustomers,
                                        selectedCustomer = selectedCustomer,
                                        selectedCustomerTransactions = selectedCustomerTx,
                                        searchQuery = searchQuery,
                                        activeFilter = customerFilter,
                                        language = settings.language,
                                        strings = strings,
                                        hasSmsPermission = viewModel.smsManager.hasSendSmsPermission(),
                                        buildSmsPreview = viewModel::buildSmsPreview,
                                        onSearchChange = viewModel::setCustomerSearch,
                                        onFilterChange = viewModel::setCustomerFilter,
                                        onSelectCustomer = viewModel::selectCustomer,
                                        onAddCustomer = viewModel::addCustomer,
                                        onUpdateCustomer = viewModel::updateCustomer,
                                        onDeleteCustomer = viewModel::deleteCustomer,
                                        onAddTransaction = { cId, type, amt, desc, partial ->
                                            viewModel.requestAddTransaction(cId, type, amt, desc, partial)
                                        },
                                        onUpdateTransaction = viewModel::updateTransaction,
                                        onDeleteTransaction = viewModel::deleteTransaction,
                                        onOpenManualSms = viewModel::openManualSmsComposer,
                                        onSendDirectSms = viewModel::sendDirectSms
                                    )

                                    DesiDestination.AI_ASSISTANT -> AiAssistantScreen(
                                        messages = aiMessages,
                                        aiUiState = aiUiState,
                                        engineMode = settings.aiEngineMode,
                                        strings = strings,
                                        decodePendingAction = viewModel::decodePendingAction,
                                        onSelectEngineMode = viewModel::setAiEngineMode,
                                        onSendMessage = viewModel::sendAiMessage,
                                        onConfirmPendingTransaction = viewModel::confirmAiPendingTransaction,
                                        onClearChat = viewModel::clearAiChat
                                    )

                                    DesiDestination.SMS_REPORTS -> SmsAndReportsScreen(
                                        customers = allCustomers,
                                        smsLogs = smsLogs,
                                        report = activeReport,
                                        selectedPeriod = reportPeriod,
                                        settings = settings,
                                        hasSmsPermission = viewModel.smsManager.hasSendSmsPermission(),
                                        buildSmsPreview = viewModel::buildSmsPreview,
                                        onSelectPeriod = viewModel::setReportPeriod,
                                        onOpenManualSms = viewModel::openManualSmsComposer,
                                        onSendDirectSms = viewModel::sendDirectSms,
                                        onSaveTemplates = viewModel::updateSmsTemplates,
                                        onTriggerScheduledReminders = viewModel::triggerScheduledRemindersNow,
                                        onExportCsv = { csvExportLauncher.launch("desi_calculator_ledger.csv") },
                                        onExportJsonBackup = { jsonBackupLauncher.launch("desi_calculator_backup_v2.json") }
                                    )

                                    DesiDestination.PROFILE -> ProfileAndSettingsScreen(
                                        settings = settings,
                                        hasNotificationPermission = viewModel.notificationManager.hasNotificationPermission(),
                                        lastBackupMessage = lastBackupMessage,
                                        onSelectTheme = viewModel::setThemePreset,
                                        onSelectCustomAccent = viewModel::setCustomAccentColor,
                                        onSelectCustomBg = viewModel::setCustomBackgroundStyle,
                                        onSelectLanguage = viewModel::setLanguage,
                                        onUpdateProfile = viewModel::updateProfile,
                                        onUpdateProfilePhoto = viewModel::updateProfilePhoto,
                                        onUpdateNotifications = viewModel::updateNotificationSettings,
                                        onExportJsonBackup = { jsonBackupLauncher.launch("desi_calculator_backup_v2.json") },
                                        onExportCsv = { csvExportLauncher.launch("desi_calculator_ledger.csv") },
                                        onImportJsonFile = { jsonRestoreLauncher.launch(arrayOf("application/json", "*/*")) },
                                        onImportRawJsonText = viewModel::importBackupOrLegacyRawJson
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Duplicate Transaction Safety Dialog
            duplicateWarning?.let { dup ->
                DesiModalDialog(
                    onDismissRequest = viewModel::dismissDuplicatePrompt,
                    title = "Possible Duplicate Entry",
                    confirmButton = {
                        Button(onClick = viewModel::confirmDuplicatePromptSave) { Text("Save Anyway") }
                    },
                    dismissButton = {
                        TextButton(onClick = viewModel::dismissDuplicatePrompt) { Text("Cancel") }
                    }
                ) {
                    Text(dup.duplicateReason ?: "A matching transaction was recorded recently. Save anyway?")
                }
            }

            AnimatedSplashOverlay(
                visible = showSplash,
                onFinished = { showSplash = false }
            )
        }
    }
}

private fun destLabel(dest: DesiDestination, strings: com.example.desicalc.ui.strings.DesiStrings): String {
    return when (dest) {
        DesiDestination.DASHBOARD -> strings.navHome
        DesiDestination.CALCULATOR -> strings.navCalc
        DesiDestination.CUSTOMERS -> strings.navKhata
        DesiDestination.AI_ASSISTANT -> strings.navAi
        DesiDestination.SMS_REPORTS -> strings.navSmsReports
        DesiDestination.PROFILE -> strings.navProfile
    }
}
