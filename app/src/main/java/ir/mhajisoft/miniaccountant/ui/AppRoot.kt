@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package ir.mhajisoft.miniaccountant.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.columnSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.pie.PieChartHost
import com.patrykandpatrick.vico.compose.pie.data.PieChartModelProducer
import com.patrykandpatrick.vico.compose.pie.data.pieSeries
import com.patrykandpatrick.vico.compose.pie.rememberPieChart
import ir.mhajisoft.miniaccountant.MainActivity
import ir.mhajisoft.miniaccountant.R
import ir.mhajisoft.miniaccountant.data.repository.AccountBalance
import ir.mhajisoft.miniaccountant.domain.bank.BankMatch
import ir.mhajisoft.miniaccountant.domain.bank.CardMath
import ir.mhajisoft.miniaccountant.domain.bank.IbanMath
import ir.mhajisoft.miniaccountant.domain.jalali.JalaliConverter
import ir.mhajisoft.miniaccountant.domain.jalali.JalaliLabels
import ir.mhajisoft.miniaccountant.domain.jalali.JalaliYmd
import ir.mhajisoft.miniaccountant.domain.ledger.SystemCategories
import ir.mhajisoft.miniaccountant.domain.model.Account
import ir.mhajisoft.miniaccountant.domain.model.AccountType
import ir.mhajisoft.miniaccountant.domain.model.AppLockSettings
import ir.mhajisoft.miniaccountant.domain.model.Category
import ir.mhajisoft.miniaccountant.domain.model.CategoryKind
import ir.mhajisoft.miniaccountant.domain.model.Direction
import ir.mhajisoft.miniaccountant.domain.model.LedgerTransaction
import ir.mhajisoft.miniaccountant.domain.money.Money
import ir.mhajisoft.miniaccountant.domain.money.PersianDigits
import ir.mhajisoft.miniaccountant.ui.components.ChoiceChip
import ir.mhajisoft.miniaccountant.ui.components.FinanceCard
import ir.mhajisoft.miniaccountant.ui.components.FinanceEmptyState
import ir.mhajisoft.miniaccountant.ui.components.FinanceTextField
import ir.mhajisoft.miniaccountant.ui.components.MoneyToneText
import ir.mhajisoft.miniaccountant.ui.components.PrimaryWideButton
import ir.mhajisoft.miniaccountant.ui.components.SectionLabel
import ir.mhajisoft.miniaccountant.ui.components.TonalCard
import ir.mhajisoft.miniaccountant.ui.components.formatJalali
import ir.mhajisoft.miniaccountant.ui.components.formatMoney
import ir.mhajisoft.miniaccountant.ui.theme.MiniAccountantTheme
import ir.mhajisoft.miniaccountant.ui.theme.SymbolIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable data object RouteHome : NavKey
@Serializable data object RouteTxns : NavKey
@Serializable data object RouteReports : NavKey
@Serializable data object RouteMore : NavKey
@Serializable data object RouteAccounts : NavKey
@Serializable data object RoutePeople : NavKey
@Serializable data class RoutePerson(val personId: String) : NavKey
@Serializable data object RouteCategories : NavKey
@Serializable data object RouteVault : NavKey
@Serializable data object RouteCardForm : NavKey
@Serializable data object RouteFiscal : NavKey
@Serializable data object RouteArchive : NavKey
@Serializable data object RouteBackup : NavKey
@Serializable data object RouteLock : NavKey
@Serializable data object RouteSettings : NavKey
@Serializable data class RouteFiltered(val categoryId: String? = null, val accountId: String? = null) : NavKey
@Serializable data class RouteArchiveViewer(val fileName: String) : NavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    onSecureWindow: (Boolean) -> Unit,
    vm: AppViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    if (!state.ready) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
        return
    }
    if (!state.settings.onboarded) {
        OnboardingScreen(vm)
        return
    }
    val backStack = rememberNavBackStack(RouteHome)
    var showComposer by remember { mutableStateOf(false) }
    var composerMode by remember { mutableIntStateOf(0) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    fun notify(msg: String) { scope.launch { snack.showSnackbar(msg) } }
    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            val current = backStack.lastOrNull()
            NavigationBar {
                NavigationBarItem(
                    selected = current is RouteHome,
                    onClick = { rewind(backStack, RouteHome) },
                    icon = { Icon(SymbolIcons.Home, null) },
                    label = { Text(stringResource(R.string.tab_home)) },
                )
                NavigationBarItem(
                    selected = current is RouteTxns,
                    onClick = { rewind(backStack, RouteTxns) },
                    icon = { Icon(SymbolIcons.Receipt, null) },
                    label = { Text(stringResource(R.string.tab_txns)) },
                )
                NavigationBarItem(
                    selected = current is RouteReports,
                    onClick = { rewind(backStack, RouteReports) },
                    icon = { Icon(SymbolIcons.Chart, null) },
                    label = { Text(stringResource(R.string.tab_reports)) },
                )
                NavigationBarItem(
                    selected = current is RouteMore,
                    onClick = { rewind(backStack, RouteMore) },
                    icon = { Icon(SymbolIcons.More, null) },
                    label = { Text(stringResource(R.string.tab_more)) },
                )
            }
        },
        floatingActionButton = {
            val current = backStack.lastOrNull()
            if (current is RouteHome || current is RouteTxns) {
                Surface(
                    shape = FloatingActionButtonDefaults.shape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.size(56.dp).combinedClickable(
                        onClick = { composerMode = 0; showComposer = true },
                        onLongClick = { composerMode = 2; showComposer = true },
                    ),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(SymbolIcons.Add, stringResource(R.string.add_expense))
                    }
                }
            }
        },
    ) { padding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(padding),
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<RouteHome> {
                    HomeScreen(
                        state,
                        onChip = { id -> backStack.add(RouteFiltered(accountId = id)) },
                        onDelete = vm::deleteTxnOrTransfer,
                        onEdit = vm::updateTxn,
                    )
                }
                entry<RouteTxns> {
                    TxnListScreen(
                        state,
                        onDelete = vm::deleteTxnOrTransfer,
                        onEdit = vm::updateTxn,
                    )
                }
                entry<RouteReports> {
                    ReportScreen(state, vm) { catId -> backStack.add(RouteFiltered(catId)) }
                }
                entry<RouteMore> { MoreScreen { backStack.add(it) } }
                entry<RouteAccounts> { AccountsScreen(state, vm) }
                entry<RoutePeople> { PeopleScreen(state, vm, onOpen = { backStack.add(RoutePerson(it)) }) }
                entry<RoutePerson> { key ->
                    PersonDetailScreen(state, vm, key.personId, onBack = { backStack.removeLastOrNull() })
                }
                entry<RouteCategories> { CategoriesScreen(state, vm) { notify(it) } }
                entry<RouteVault> {
                    VaultScreen(vm, onSecureWindow, onAddCard = { backStack.add(RouteCardForm) })
                }
                entry<RouteCardForm> { CardFormScreen(state, vm, onSecureWindow) }
                entry<RouteFiscal> { FiscalScreen(state, vm) }
                entry<RouteArchive> { ArchiveScreen(state, vm) { backStack.add(RouteArchiveViewer(it)) } }
                entry<RouteArchiveViewer> { key -> ArchiveViewerScreen(vm, key.fileName) }
                entry<RouteBackup> { BackupScreen(vm) }
                entry<RouteLock> { LockSettingsScreen(state, vm) }
                entry<RouteSettings> { SettingsScreen(state, vm) }
                entry<RouteFiltered> { key ->
                    TxnListScreen(
                        state.copy(
                            txns = state.txns.filter {
                                (key.categoryId == null || it.categoryId == key.categoryId) &&
                                    (key.accountId == null || it.accountId == key.accountId)
                            },
                        ),
                        onDelete = vm::deleteTxnOrTransfer,
                        onEdit = vm::updateTxn,
                    )
                }
            },
        )
    }
    if (showComposer) {
        ComposerSheet(
            state = state,
            initialMode = composerMode,
            onDismiss = { showComposer = false },
            onError = { notify(it) },
            onSaveExpense = { acc, cat, amt, note, at, income ->
                vm.saveExpense(acc, cat, amt, note, at, income, onError = { notify(it) })
                showComposer = false
            },
            onSaveTransfer = { from, to, amt, fee, note, at ->
                vm.saveTransfer(from, to, amt, fee, note, at, onError = { notify(it) })
                showComposer = false
            },
        )
    }
}

private fun rewind(stack: NavBackStack<NavKey>, tab: NavKey) {
    stack.clear()
    stack.add(tab)
}

@Composable
fun OnboardingScreen(vm: AppViewModel) {
    var step by remember { mutableIntStateOf(0) }
    val today = vm.jalaliToday
    var month by remember { mutableIntStateOf(1) }
    var day by remember { mutableIntStateOf(1) }
    var cashName by remember { mutableStateOf("") }
    var opening by remember { mutableStateOf("") }
    var lock by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        when (step) {
            0 -> {
                FinanceCard {
                    Text(stringResource(R.string.onboarding_fy_title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.onboarding_fy_body))
                    Text(stringResource(R.string.year) + " " + PersianDigits.toPersian(today.year.toString()))
                    FinanceTextField(
                        PersianDigits.toPersian(month.toString()),
                        { month = PersianDigits.toAscii(it).toIntOrNull() ?: 1 },
                        label = stringResource(R.string.fy_start_month),
                        keyboardType = KeyboardType.Number,
                    )
                    FinanceTextField(
                        PersianDigits.toPersian(day.toString()),
                        { day = PersianDigits.toAscii(it).toIntOrNull() ?: 1 },
                        label = stringResource(R.string.fy_start_day),
                        keyboardType = KeyboardType.Number,
                    )
                    PrimaryWideButton(stringResource(R.string.next), onClick = { step = 1 })
                }
            }
            1 -> {
                FinanceCard {
                    Text(stringResource(R.string.onboarding_account_title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.onboarding_account_body))
                    FinanceTextField(cashName, { cashName = it }, label = stringResource(R.string.cash_account_name))
                    FinanceTextField(
                        opening,
                        { opening = it },
                        label = stringResource(R.string.opening_balance),
                        keyboardType = KeyboardType.Number,
                    )
                    PrimaryWideButton(stringResource(R.string.next), onClick = { step = 2 })
                }
            }
            else -> {
                FinanceCard {
                    Text(stringResource(R.string.onboarding_lock_title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.onboarding_lock_body))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(lock, { lock = it })
                        Text(stringResource(R.string.enable_lock))
                    }
                    PrimaryWideButton(
                        stringResource(R.string.start),
                        onClick = {
                            val open = Money.parseDisplayAmount(opening, false) ?: 0L
                            vm.completeOnboarding(month, day, cashName, open, lock)
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    state: AppUiState,
    onChip: (String) -> Unit,
    onDelete: (LedgerTransaction) -> Unit,
    onEdit: (LedgerTransaction) -> Unit,
) {
    val toman = state.settings.displayToman
    var editing by remember { mutableStateOf<LedgerTransaction?>(null) }
    var pendingDelete by remember { mutableStateOf<LedgerTransaction?>(null) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TonalCard {
                Text(stringResource(R.string.total_balance), style = MaterialTheme.typography.labelLarge)
                Text(formatMoney(state.total, toman), style = MaterialTheme.typography.headlineMedium)
            }
        }
        item {
            FinanceCard {
                SectionLabel(stringResource(R.string.accounts))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.balances.filter { it.account.type != AccountType.PERSON }.forEach { ab ->
                        ChoiceChip(
                            selected = false,
                            onClick = { onChip(ab.account.id) },
                            label = "${ab.account.name} ${formatMoney(ab.balanceSigned, toman)}",
                        )
                    }
                }
            }
        }
        item { SectionLabel(stringResource(R.string.recent_txns)) }
        if (state.recent.isEmpty()) {
            item {
                FinanceEmptyState(
                    SymbolIcons.Receipt,
                    stringResource(R.string.empty_home_title),
                    stringResource(R.string.empty_home),
                )
            }
        } else {
            items(state.recent, key = { it.id }) { txn ->
                TxnRow(txn, state, onClick = {
                    if (txn.transferId == null) editing = txn
                }, onLongClick = { pendingDelete = txn })
            }
        }
    }
    editing?.let { txn ->
        EditTxnDialog(
            txn = txn,
            toman = state.settings.displayToman,
            onDismiss = { editing = null },
            onSave = { updated ->
                onEdit(updated)
                editing = null
            },
        )
    }
    pendingDelete?.let { txn ->
        ConfirmDeleteDialog(
            txn = txn,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                onDelete(txn)
                pendingDelete = null
            },
        )
    }
}

@Composable
fun TxnRow(txn: LedgerTransaction, state: AppUiState, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    val cat = state.categories.firstOrNull { it.id == txn.categoryId }
    val acc = state.accounts.firstOrNull { it.id == txn.accountId }
    val toman = state.settings.displayToman
    val sign = if (txn.direction == Direction.IN) "+" else "−"
    FinanceCard(Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(SymbolIcons.byKey(cat?.iconKey ?: "swap_horiz"), null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text(cat?.name ?: stringResource(R.string.transfer), style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${acc?.name.orEmpty()} · ${formatJalali(txn.occurredAt)} ${txn.note}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            MoneyToneText(sign + formatMoney(txn.amount, toman), inbound = txn.direction == Direction.IN)
        }
    }
}

@Composable
fun TxnListScreen(
    state: AppUiState,
    onDelete: (LedgerTransaction) -> Unit,
    onEdit: (LedgerTransaction) -> Unit = {},
) {
    var q by remember { mutableStateOf("") }
    var accountFilter by remember { mutableStateOf<String?>(null) }
    var categoryFilter by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<LedgerTransaction?>(null) }
    var pendingDelete by remember { mutableStateOf<LedgerTransaction?>(null) }
    val needle = PersianDigits.toAscii(q).lowercase()
    val grouped = state.txns
        .filter { txn ->
            if (needle.isBlank()) return@filter true
            val cat = state.categories.firstOrNull { it.id == txn.categoryId }?.name.orEmpty()
            val acc = state.accounts.firstOrNull { it.id == txn.accountId }?.name.orEmpty()
            txn.note.contains(q) ||
                cat.contains(q) ||
                acc.contains(q) ||
                txn.id.contains(needle) ||
                PersianDigits.toAscii(txn.note).contains(needle)
        }
        .filter { accountFilter == null || it.accountId == accountFilter }
        .filter { categoryFilter == null || it.categoryId == categoryFilter }
        .groupBy { Triple(it.jalaliYear, it.jalaliMonth, it.jalaliDay) }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FinanceTextField(q, { q = it }, label = stringResource(R.string.search))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip(accountFilter == null, { accountFilter = null }, stringResource(R.string.all))
                state.accounts.filter { it.type != AccountType.PERSON && !it.archived }.take(6).forEach { acc ->
                    ChoiceChip(accountFilter == acc.id, { accountFilter = acc.id }, acc.name)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip(categoryFilter == null, { categoryFilter = null }, stringResource(R.string.filter))
                state.categories.filter { !it.isSystem || it.kind != CategoryKind.TRANSFER }.take(6).forEach { cat ->
                    ChoiceChip(categoryFilter == cat.id, { categoryFilter = cat.id }, cat.name)
                }
            }
        }
        if (grouped.isEmpty()) {
            Box(Modifier.padding(16.dp)) {
                FinanceEmptyState(
                    SymbolIcons.Receipt,
                    stringResource(R.string.empty_txns_title),
                    stringResource(R.string.empty_txns),
                )
            }
        } else {
            LazyColumn {
                grouped.toSortedMap(compareByDescending<Triple<Int, Int, Int>> { it.first }.thenByDescending { it.second }.thenByDescending { it.third }).forEach { (day, rows) ->
                    item {
                        Text(
                            PersianDigits.toPersian("${day.first}/${day.second}/${day.third}"),
                            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    items(rows, key = { it.id }) { txn ->
                        TxnRow(txn, state, onClick = {
                            if (txn.transferId == null) editing = txn
                        }, onLongClick = { pendingDelete = txn })
                    }
                }
            }
        }
    }
    editing?.let { txn ->
        EditTxnDialog(
            txn = txn,
            toman = state.settings.displayToman,
            onDismiss = { editing = null },
            onSave = { updated ->
                onEdit(updated)
                editing = null
            },
        )
    }
    pendingDelete?.let { txn ->
        ConfirmDeleteDialog(
            txn = txn,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                onDelete(txn)
                pendingDelete = null
            },
        )
    }
}

@Composable
private fun ConfirmDeleteDialog(
    txn: LedgerTransaction,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete)) },
        text = {
            Text(
                if (txn.transferId != null) {
                    stringResource(R.string.confirm_delete_transfer)
                } else {
                    stringResource(R.string.confirm_delete)
                },
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun EditTxnDialog(
    txn: LedgerTransaction,
    toman: Boolean,
    onDismiss: () -> Unit,
    onSave: (LedgerTransaction) -> Unit,
) {
    val initial = Money.toDisplayUnit(txn.amount, toman).toString()
    var amount by remember { mutableStateOf(PersianDigits.toPersian(initial)) }
    var note by remember { mutableStateOf(txn.note) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FinanceTextField(
                    amount,
                    { amount = it },
                    label = stringResource(R.string.amount),
                    keyboardType = KeyboardType.Number,
                )
                FinanceTextField(note, { note = it }, label = stringResource(R.string.note))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = Money.parseDisplayAmount(amount, toman) ?: return@TextButton
                onSave(txn.copy(amount = parsed, note = note))
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
fun ReportScreen(state: AppUiState, vm: AppViewModel, onSlice: (String) -> Unit) {
    val fy = state.fy
    if (fy == null) {
        Box(Modifier.padding(16.dp)) {
            FinanceEmptyState(
                SymbolIcons.Chart,
                stringResource(R.string.empty_reports_title),
                stringResource(R.string.empty_reports_body),
            )
        }
        return
    }
    var month by remember { mutableIntStateOf(JalaliConverter.fromEpochMillis(System.currentTimeMillis()).month) }
    var yearMode by remember { mutableStateOf(false) }
    val rows = state.txns.filter {
        it.fiscalYearId == fy.id &&
            (yearMode || it.jalaliMonth == month) &&
            it.transferId == null &&
            it.categoryId != SystemCategories.OPENING_BALANCE_ID
    }
    val byCat = rows.groupBy { it.categoryId ?: "" }.mapValues { e ->
        e.value.sumOf { if (it.direction == Direction.OUT) it.amount else 0L }
    }.filter { it.value > 0L }
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FinanceCard {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                ChoiceChip(!yearMode, { yearMode = false }, stringResource(R.string.month_mode))
                ChoiceChip(yearMode, { yearMode = true }, stringResource(R.string.year_mode))
            }
            if (!yearMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { month = if (month == 1) 12 else month - 1 }) { Text("−") }
                    Text("${JalaliLabels.monthName(month)} ${PersianDigits.toPersian(fy.startJalaliYear.toString())}")
                    TextButton(onClick = { month = if (month == 12) 1 else month + 1 }) { Text("+") }
                }
            } else {
                Text(PersianDigits.toPersian(fy.label))
            }
        }
        if (byCat.isEmpty()) {
            FinanceEmptyState(
                SymbolIcons.Chart,
                stringResource(R.string.empty_reports_title),
                stringResource(R.string.empty_reports_body),
            )
        } else {
            FinanceCard {
                Text(stringResource(R.string.pie_by_category), style = MaterialTheme.typography.titleMedium)
                ReportCharts(byCat, rows, yearMode, onSlice)
            }
            byCat.forEach { (id, amt) ->
                val cat = state.categories.firstOrNull { it.id == id }
                FinanceCard(Modifier.clickable { onSlice(id) }) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(cat?.name ?: id, style = MaterialTheme.typography.titleSmall)
                        Text(formatMoney(amt, state.settings.displayToman), style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportCharts(
    byCat: Map<String, Long>,
    rows: List<LedgerTransaction>,
    yearMode: Boolean,
    onSlice: (String) -> Unit,
) {
    val pieProducer = remember { PieChartModelProducer() }
    val barProducer = remember { CartesianChartModelProducer() }
    val catKeys = remember(byCat) { byCat.keys.toList() }
    LaunchedEffect(byCat, rows, yearMode) {
        pieProducer.runTransaction {
            pieSeries {
                series(byCat.values.map { it })
            }
        }
        val grouped = if (yearMode) rows.groupBy { it.jalaliMonth } else rows.groupBy { it.jalaliDay }
        val daily = grouped.toSortedMap()
        barProducer.runTransaction {
            columnSeries {
                series(
                    daily.keys.map { it },
                    daily.values.map { v -> v.filter { it.direction == Direction.OUT }.sumOf { it.amount } },
                )
            }
        }
    }
    PieChartHost(
        chart = rememberPieChart(),
        modelProducer = pieProducer,
        modifier = Modifier.height(200.dp).fillMaxWidth().clickable {
            catKeys.firstOrNull()?.let(onSlice)
        },
    )
    Text(stringResource(R.string.daily_bars), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
    CartesianChartHost(
        rememberCartesianChart(
            rememberColumnCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(),
        ),
        barProducer,
        modifier = Modifier.height(180.dp),
    )
}

@Composable
fun MoreScreen(open: (NavKey) -> Unit) {
    val items = listOf(
        R.string.accounts to RouteAccounts,
        R.string.people to RoutePeople,
        R.string.categories to RouteCategories,
        R.string.vault to RouteVault,
        R.string.fiscal_year to RouteFiscal,
        R.string.archive to RouteArchive,
        R.string.backup to RouteBackup,
        R.string.lock to RouteLock,
        R.string.settings to RouteSettings,
    )
    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(items.size) { i ->
            val (res, route) = items[i]
            FinanceCard(Modifier.clickable { open(route) }) {
                Text(stringResource(res), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun AccountsScreen(state: AppUiState, vm: AppViewModel) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccountType.CASH) }
    var include by remember { mutableStateOf(true) }
    var opening by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Account?>(null) }
    val ledgerAccounts = state.accounts.filter { it.type != AccountType.PERSON }
    Column(
        Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FinanceCard {
            SectionLabel(stringResource(R.string.add_account))
            FinanceTextField(name, { name = it }, label = stringResource(R.string.add_account))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip(type == AccountType.CASH, { type = AccountType.CASH }, stringResource(R.string.type_cash))
                ChoiceChip(type == AccountType.BANK, { type = AccountType.BANK }, stringResource(R.string.type_bank))
                ChoiceChip(type == AccountType.CARD, { type = AccountType.CARD }, stringResource(R.string.type_card))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(include, { include = it })
                Text(stringResource(R.string.include_in_total))
            }
            FinanceTextField(
                opening,
                { opening = it },
                label = stringResource(R.string.opening_balance),
                keyboardType = KeyboardType.Number,
            )
            PrimaryWideButton(stringResource(R.string.save), onClick = {
                if (name.isNotBlank()) {
                    val open = Money.parseDisplayAmount(opening, state.settings.displayToman) ?: 0L
                    vm.addAccount(name, type, include, open)
                    name = ""
                    opening = ""
                }
            })
        }
        if (ledgerAccounts.isEmpty()) {
            FinanceEmptyState(
                SymbolIcons.Wallet,
                stringResource(R.string.empty_accounts),
                stringResource(R.string.empty_accounts_body),
            )
        }
        ledgerAccounts.forEach { acc ->
            val typeLabel = when (acc.type) {
                AccountType.CASH -> stringResource(R.string.type_cash)
                AccountType.BANK -> stringResource(R.string.type_bank)
                AccountType.CARD -> stringResource(R.string.type_card)
                AccountType.PERSON -> stringResource(R.string.type_person)
            }
            val bal = state.balances.firstOrNull { it.account.id == acc.id }?.balanceSigned
            FinanceCard(Modifier.clickable { editing = acc }) {
                Text(acc.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    buildString {
                        append(typeLabel)
                        if (acc.archived) {
                            append(" · ")
                            append(stringResource(R.string.archived))
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(formatMoney(bal ?: 0L, state.settings.displayToman), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
    editing?.let { acc ->
        var editName by remember(acc.id) { mutableStateOf(acc.name) }
        var editInclude by remember(acc.id) { mutableStateOf(acc.includeInTotal) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(stringResource(R.string.edit)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FinanceTextField(editName, { editName = it }, label = stringResource(R.string.name))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(editInclude, { editInclude = it })
                        Text(stringResource(R.string.include_in_total))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.updateAccount(acc.copy(name = editName, includeInTotal = editInclude))
                    editing = null
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    vm.archiveAccount(acc.id, !acc.archived)
                    editing = null
                }) {
                    Text(stringResource(if (acc.archived) R.string.unarchive_account else R.string.archive_account))
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoriesScreen(state: AppUiState, vm: AppViewModel, onError: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(CategoryKind.EXPENSE) }
    var iconKey by remember { mutableStateOf(SymbolIcons.customIconKeys.first()) }
    var color by remember { mutableStateOf(0xFF1565C0L) }
    val palette = listOf(0xFFE65100L, 0xFF1565C0L, 0xFF6A1B9AL, 0xFF2E7D32L, 0xFFC62828L, 0xFF00838FL, 0xFFAD1457L, 0xFF37474FL)
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FinanceCard {
                FinanceTextField(name, { name = it }, label = stringResource(R.string.add_category))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip(kind == CategoryKind.EXPENSE, { kind = CategoryKind.EXPENSE }, stringResource(R.string.expense))
                    ChoiceChip(kind == CategoryKind.INCOME, { kind = CategoryKind.INCOME }, stringResource(R.string.income))
                }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SymbolIcons.customIconKeys.forEach { key ->
                    FilterChip(
                        selected = iconKey == key,
                        onClick = { iconKey = key },
                        label = { Icon(SymbolIcons.byKey(key), null, Modifier.size(18.dp)) },
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                palette.forEach { c ->
                    FilterChip(
                        selected = color == c,
                        onClick = { color = c },
                        label = { Box(Modifier.size(16.dp)) },
                        leadingIcon = { Icon(SymbolIcons.Category, null, tint = Color(c or 0xFF000000)) },
                    )
                }
            }
            PrimaryWideButton(stringResource(R.string.save), onClick = {
                if (name.isNotBlank()) {
                    vm.addCustomCategory(name, iconKey, color, kind)
                    name = ""
                }
            })
            }
        }
        LazyColumn {
            items(state.categories, key = { it.id }) { cat ->
                ListItem(
                    headlineContent = { Text(cat.name) },
                    supportingContent = {
                        Text(
                            when {
                                cat.isSystem -> stringResource(R.string.system_category)
                                cat.kind == CategoryKind.EXPENSE -> stringResource(R.string.expense)
                                cat.kind == CategoryKind.INCOME -> stringResource(R.string.income)
                                else -> stringResource(R.string.transfer)
                            },
                        )
                    },
                    leadingContent = { Icon(SymbolIcons.byKey(cat.iconKey), null, tint = Color(cat.color or 0xFF000000)) },
                    trailingContent = {
                        if (!cat.isSystem) {
                            TextButton(onClick = {
                                vm.deleteCustomCategory(cat.id) { onError(it) }
                            }) { Text(stringResource(R.string.delete)) }
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun VaultScreen(vm: AppViewModel, onSecure: (Boolean) -> Unit, onAddCard: () -> Unit) {
    DisposableEffect(Unit) {
        onSecure(true)
        onDispose { onSecure(false) }
    }
    val cards by vm.vaultRepo.cardsFlow.collectAsStateWithLifecycle(emptyList())
    val ibans by vm.vaultRepo.bankAccountsFlow.collectAsStateWithLifecycle(emptyList())
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Button(onClick = onAddCard) { Text(stringResource(R.string.new_card)) }
        if (cards.isEmpty()) Text(stringResource(R.string.empty_cards))
        val activity = LocalContext.current as MainActivity
        val scope = rememberCoroutineScope()
        cards.forEach { c ->
            val bank = vm.vaultRepo.directory.findByBin(c.bin6)
            var revealed by remember(c.id) { mutableStateOf<String?>(null) }
            ListItem(
                headlineContent = { Text(revealed ?: CardMath.maskPan(c.last4.padStart(16, '*'))) },
                supportingContent = {
                    Text(bank?.nameFa ?: stringResource(R.string.unknown_bank))
                },
                leadingContent = { BankLogo(bank?.logoDrawable ?: "bank_unknown") },
                trailingContent = {
                    TextButton(onClick = {
                        val panId = c.panCipherId ?: return@TextButton
                        activity.lockBeforePan {
                            activity.lifecycleScopeLaunch {
                                revealed = vm.vaultRepo.decryptPan(panId)
                            }
                        }
                    }) { Text(stringResource(R.string.reveal_pan)) }
                },
            )
            if (c.rememberCvv && c.cvvCipherId != null) {
                TextButton(onClick = {
                    activity.lifecycleScopeLaunch {
                        val iv = vm.vaultRepo.secretVault.cvvIv(c.cvvCipherId!!) ?: return@lifecycleScopeLaunch
                        val cipher = vm.vaultRepo.secretVault.createCvvDecryptCipher(iv)
                        activity.promptUnlock(
                            crypto = BiometricPrompt.CryptoObject(cipher),
                            onSuccess = { result ->
                                activity.lifecycleScopeLaunch {
                                    val unlocked = result.cryptoObject?.cipher ?: cipher
                                    val cvv = vm.vaultRepo.secretVault.revealCvv(c.cvvCipherId!!, unlocked) ?: return@lifecycleScopeLaunch
                                    val cm = activity.getSystemService(ClipboardManager::class.java)
                                    cm.setPrimaryClip(ClipData.newPlainText(activity.getString(R.string.cvv), cvv))
                                    scope.launch {
                                        delay(30_000)
                                        cm.setPrimaryClip(ClipData.newPlainText("", ""))
                                    }
                                }
                            },
                            onCancel = {},
                        )
                    }
                }) { Text(stringResource(R.string.reveal_cvv)) }
            }
        }
        ibans.forEach { a ->
            ListItem(headlineContent = { Text(IbanMath.formatGrouped(a.iban)) }, supportingContent = { Text(a.bankName) })
        }
    }
}

@Composable
fun BankLogo(drawableName: String) {
    val ctx = LocalContext.current
    val id = ctx.resources.getIdentifier(drawableName, "drawable", ctx.packageName)
    if (id != 0) Image(painterResource(id), null, Modifier.size(40.dp))
}

@Composable
fun CardFormScreen(state: AppUiState, vm: AppViewModel, onSecure: (Boolean) -> Unit) {
    DisposableEffect(Unit) {
        onSecure(true)
        onDispose { onSecure(false) }
    }
    val activity = LocalContext.current as MainActivity
    var pan by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var rememberCvv by remember { mutableStateOf(false) }
    var expiryMonth by remember { mutableStateOf("1") }
    var expiryYear by remember { mutableStateOf("1408") }
    var iban by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val digits = CardMath.normalizeDigits(pan)
    val match = if (digits.length >= 6) vm.vaultRepo.directory.resolvePan(digits) else BankMatch.Unknown
    val bank = (match as? BankMatch.Known)?.bank
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.card_form), style = MaterialTheme.typography.titleLarge)
        FinanceTextField(
            pan,
            { pan = it },
            label = stringResource(R.string.card_number),
            keyboardType = KeyboardType.Number,
            leadingIcon = { BankLogo(bank?.logoDrawable ?: "bank_unknown") },
        )
        if (digits.length >= 6) {
            Text(bank?.let { it.nameFa + (it.formerNameFa?.let { f -> " — $f" } ?: "") } ?: stringResource(R.string.unknown_bank))
        }
        FinanceTextField(cvv, { cvv = it }, label = stringResource(R.string.cvv), keyboardType = KeyboardType.NumberPassword)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(rememberCvv, { rememberCvv = it })
            Text(stringResource(R.string.remember_cvv))
        }
        Text(stringResource(R.string.cvv_warning), style = MaterialTheme.typography.bodySmall)
        FinanceTextField(expiryMonth, { expiryMonth = it }, label = stringResource(R.string.expiry), keyboardType = KeyboardType.Number)
        FinanceTextField(expiryYear, { expiryYear = it }, label = stringResource(R.string.year), keyboardType = KeyboardType.Number)
        val acc = state.accounts.firstOrNull { it.type == AccountType.CARD } ?: state.accounts.firstOrNull()
        Button(onClick = {
            if (!CardMath.luhnValid(pan)) {
                error = activity.getString(R.string.invalid_luhn)
                return@Button
            }
            val accountId = acc?.id ?: return@Button
            if (rememberCvv && cvv.isNotBlank()) {
                val cipher = vm.vaultRepo.secretVault.createCvvEncryptCipher()
                activity.promptUnlock(
                    crypto = BiometricPrompt.CryptoObject(cipher),
                    onSuccess = { result ->
                        activity.lifecycleScopeLaunch {
                            val unlocked = result.cryptoObject?.cipher ?: cipher
                            vm.vaultRepo.saveCard(
                                accountId = accountId,
                                panAscii = pan,
                                expiryMonth = expiryMonth.toIntOrNull() ?: 1,
                                expiryYear = expiryYear.toIntOrNull() ?: 1408,
                                holderName = null,
                                rememberCvv = true,
                                cvvAscii = cvv,
                                encryptCvv = { ascii -> vm.vaultRepo.secretVault.persistCvv(unlocked, ascii) },
                            )
                        }
                    },
                    onCancel = {},
                )
            } else {
                activity.promptUnlock(
                    onSuccess = {
                        activity.lifecycleScopeLaunch {
                            vm.vaultRepo.saveCard(
                                accountId = accountId,
                                panAscii = pan,
                                expiryMonth = expiryMonth.toIntOrNull() ?: 1,
                                expiryYear = expiryYear.toIntOrNull() ?: 1408,
                                holderName = null,
                                rememberCvv = false,
                                cvvAscii = null,
                            )
                        }
                    },
                    onCancel = {},
                )
            }
        }) { Text(stringResource(R.string.save)) }
        Spacer(Modifier.height(16.dp))
        FinanceTextField(accountNumber, { accountNumber = it }, label = stringResource(R.string.account_number))
        FinanceTextField(
            IbanMath.formatGrouped(iban),
            { iban = it },
            label = stringResource(R.string.iban),
        )
        val ibanMatch = vm.vaultRepo.directory.resolveIban(iban)
        val ibanBank = (ibanMatch as? BankMatch.Known)?.bank
        if (IbanMath.normalize(iban).length >= 7) {
            Row {
                BankLogo(ibanBank?.logoDrawable ?: "bank_unknown")
                Text(ibanBank?.nameFa ?: stringResource(R.string.unknown_bank))
            }
        }
        Button(onClick = {
            if (!IbanMath.isValidIranIban(iban)) {
                error = activity.getString(R.string.invalid_iban)
            } else {
                val accountId = acc?.id
                if (accountId == null) return@Button
                activity.lifecycleScopeLaunch {
                    vm.vaultRepo.saveBankAccount(accountId, accountNumber, iban)
                }
            }
        }) { Text(stringResource(R.string.save_iban)) }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
fun FiscalScreen(state: AppUiState, vm: AppViewModel) {
    val fy = state.fy
    var confirm by remember { mutableStateOf(false) }
    val openings by vm.openings.collectAsStateWithLifecycle()
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.fiscal_year), style = MaterialTheme.typography.titleLarge)
        fy?.let {
            Text("${PersianDigits.toPersian(it.startJalaliYear.toString())}/${PersianDigits.toPersian(it.startJalaliMonth.toString().padStart(2,'0'))}/${PersianDigits.toPersian(it.startJalaliDay.toString().padStart(2,'0'))} — ${PersianDigits.toPersian(it.endJalaliYear.toString())}/${PersianDigits.toPersian(it.endJalaliMonth.toString().padStart(2,'0'))}/${PersianDigits.toPersian(it.endJalaliDay.toString().padStart(2,'0'))}")
        }
        Button(onClick = { confirm = true }) { Text(stringResource(R.string.fy_reset_farvardin)) }
        Text(stringResource(R.string.opening_balance), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        state.accounts.filter { it.type != AccountType.PERSON }.forEach { acc ->
            var text by remember(acc.id, openings[acc.id], state.settings.displayToman) {
                val raw = openings[acc.id] ?: 0L
                mutableStateOf(PersianDigits.toPersian(Money.toDisplayUnit(raw, state.settings.displayToman).toString()))
            }
            FinanceTextField(
                text,
                { text = it },
                label = acc.name,
                keyboardType = KeyboardType.Number,
                trailingIcon = {
                    TextButton(onClick = { vm.setOpening(acc.id, text) }) { Text(stringResource(R.string.save)) }
                },
            )
        }
        if (confirm) {
            AlertDialog(
                onDismissRequest = { confirm = false },
                confirmButton = { TextButton(onClick = { vm.setFyStart(1, 1, true); confirm = false }) { Text(stringResource(R.string.ok)) } },
                dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) } },
                text = { Text(stringResource(R.string.confirm_rebucket)) },
            )
        }
    }
}

@Composable
fun ArchiveScreen(state: AppUiState, vm: AppViewModel, open: (String) -> Unit) {
    val recs by vm.archiveRepo.records.collectAsStateWithLifecycle(emptyList())
    val archivedIds = recs.map { it.fiscalYearId }.toSet()
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        state.fy?.let {
            Button(onClick = { vm.closeCurrentYear() }) { Text(stringResource(R.string.close_year)) }
        }
        state.fiscalYears.filter { !it.isCurrent && it.id !in archivedIds }.forEach { fy ->
            ListItem(
                headlineContent = { Text(PersianDigits.toPersian(fy.label)) },
                trailingContent = {
                    Button(onClick = { vm.archiveYear(fy.id) }) { Text(stringResource(R.string.archive_year)) }
                },
            )
        }
        recs.forEach {
            ListItem(
                headlineContent = { Text(it.fileName) },
                supportingContent = { Text(stringResource(R.string.read_only)) },
                trailingContent = {
                    TextButton(onClick = { open(it.fileName) }) { Text(stringResource(R.string.open_archive)) }
                },
            )
        }
    }
}

@Composable
fun ArchiveViewerScreen(vm: AppViewModel, fileName: String) {
    var rows by remember { mutableStateOf(emptyList<ir.mhajisoft.miniaccountant.data.archive.ArchivedTxn>()) }
    LaunchedEffect(fileName) {
        rows = runCatching { vm.archiveRepo.readArchivedTransactions(fileName) }.getOrDefault(emptyList())
    }
    Column(Modifier.padding(16.dp)) {
        Text(fileName, style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.read_only), style = MaterialTheme.typography.bodySmall)
        LazyColumn {
            items(rows, key = { it.id }) { txn ->
                ListItem(
                    headlineContent = { Text(txn.note.ifBlank { stringResource(R.string.transfer) }) },
                    supportingContent = {
                        Text(
                            PersianDigits.toPersian("${txn.jalaliYear}/${txn.jalaliMonth}/${txn.jalaliDay}"),
                        )
                    },
                    trailingContent = { Text(formatMoney(txn.amount, false)) },
                )
            }
        }
    }
}

@Composable
fun BackupScreen(vm: AppViewModel) {
    var pass by remember { mutableStateOf("") }
    val ctx = LocalContext.current
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) {
            (ctx as MainActivity).lifecycleScopeLaunch {
                val bytes = vm.backupRepo.createEncryptedBackup(pass)
                vm.backupRepo.writeToSaf(uri, bytes)
                vm.backupRepo.recordLocal(uri.toString(), bytes)
            }
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            (ctx as MainActivity).lifecycleScopeLaunch {
                val bytes = vm.backupRepo.readFromSaf(uri)
                vm.backupRepo.restoreEncrypted(bytes, pass)
                vm.backupRepo.restartProcess(ctx)
            }
        }
    }
    val drive = vm.backupRepo.driveAvailability()
    val one = vm.backupRepo.oneDriveAvailability()
    var cloudMsg by remember { mutableStateOf<String?>(null) }
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        FinanceTextField(pass, { pass = it }, label = stringResource(R.string.passphrase))
        Button(onClick = { create.launch("mini-accountant.pfbak") }, enabled = pass.length >= 4) {
            Text(stringResource(R.string.backup_local))
        }
        OutlinedButton(onClick = { open.launch(arrayOf("application/octet-stream", "*/*")) }) {
            Text(stringResource(R.string.restore_local))
        }
        Text(stringResource(R.string.backup_drive), style = MaterialTheme.typography.titleMedium)
        if (!drive.available) {
            Text(drive.reasonFa)
        } else {
            Button(onClick = {
                (ctx as MainActivity).lifecycleScopeLaunch {
                    val bytes = vm.backupRepo.createEncryptedBackup(pass)
                    val result = vm.backupRepo.uploadCloud(ir.mhajisoft.miniaccountant.data.cloud.CloudKind.DRIVE, bytes)
                    cloudMsg = result.exceptionOrNull()?.message ?: ctx.getString(R.string.ok)
                }
            }, enabled = pass.length >= 4) { Text(stringResource(R.string.upload_drive)) }
        }
        Text(stringResource(R.string.backup_onedrive), style = MaterialTheme.typography.titleMedium)
        if (!one.available) {
            Text(one.reasonFa)
        } else {
            Button(onClick = {
                (ctx as MainActivity).lifecycleScopeLaunch {
                    val bytes = vm.backupRepo.createEncryptedBackup(pass)
                    val result = vm.backupRepo.uploadCloud(ir.mhajisoft.miniaccountant.data.cloud.CloudKind.ONEDRIVE, bytes)
                    cloudMsg = result.exceptionOrNull()?.message ?: ctx.getString(R.string.ok)
                }
            }, enabled = pass.length >= 4) { Text(stringResource(R.string.upload_onedrive)) }
        }
        cloudMsg?.let { Text(it) }
    }
}

@Composable
fun LockSettingsScreen(state: AppUiState, vm: AppViewModel) {
    val lock = state.settings.lock
    Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(lock.enabled, { vm.setLock(lock.copy(enabled = it)) })
            Text(stringResource(R.string.enable_lock))
        }
        Text(stringResource(R.string.lock_timeout))
        Slider(lock.timeoutSec.toFloat(), { vm.setLock(lock.copy(timeoutSec = it.toInt().coerceIn(0, 300))) }, valueRange = 0f..300f)
        Text(PersianDigits.toPersian(lock.timeoutSec.toString()))
    }
}

@Composable
fun SettingsScreen(state: AppUiState, vm: AppViewModel) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FinanceCard {
            SectionLabel(stringResource(R.string.display_unit))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip(state.settings.displayToman, { vm.setToman(true) }, stringResource(R.string.toman))
                ChoiceChip(!state.settings.displayToman, { vm.setToman(false) }, stringResource(R.string.rial))
            }
        }
        FinanceCard {
            SectionLabel(stringResource(R.string.default_account))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.accounts.forEach { acc ->
                    ChoiceChip(state.settings.defaultAccountId == acc.id, { vm.setDefaultAccount(acc.id) }, acc.name)
                }
            }
        }
    }
}

private fun MainActivity.lifecycleScopeLaunch(block: suspend () -> Unit) {
    kotlinx.coroutines.MainScope().launch { block() }
}

@Preview(showBackground = true, locale = "fa", name = "Home")
@Composable
fun PreviewHome() {
    val cash = Account("1", "نقد", AccountType.CASH, color = 0xFF0F766E, sortOrder = 0, createdAt = 0, updatedAt = 0)
    MiniAccountantTheme {
        HomeScreen(
            AppUiState(
                ready = true,
                accounts = listOf(cash),
                balances = listOf(AccountBalance(cash, 5_420_000)),
                recent = listOf(
                    LedgerTransaction("t", "1", "sys-food", null, 120000, Direction.OUT, "نان", 0, 1405, 6, 15, "fy", null, 0),
                ),
                categories = ir.mhajisoft.miniaccountant.domain.ledger.CategoryCatalog.systemCategories(),
            ),
            {},
            {},
            {},
        )
    }
}

@Preview(showBackground = true, locale = "fa", name = "Txn list")
@Composable
fun PreviewTxns() {
    MiniAccountantTheme {
        TxnListScreen(
            AppUiState(
                ready = true,
                txns = listOf(
                    LedgerTransaction("t", "1", "sys-food", null, 120000, Direction.OUT, "نان", 0, 1405, 6, 15, "fy", null, 0),
                ),
                categories = ir.mhajisoft.miniaccountant.domain.ledger.CategoryCatalog.systemCategories(),
            ),
            {},
        )
    }
}

@Preview(showBackground = true, locale = "fa", name = "Card form")
@Composable
fun PreviewCard() {
    MiniAccountantTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.card_form), style = MaterialTheme.typography.titleLarge)
            FinanceTextField("6104337812345678", {}, label = stringResource(R.string.card_number), keyboardType = KeyboardType.Number)
            Text(stringResource(R.string.bank_mellat_name))
        }
    }
}
