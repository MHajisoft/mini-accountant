@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package ir.mhajisoft.hesabres.ui

import android.app.Activity
import android.content.res.Configuration
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
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
import ir.mhajisoft.hesabres.MainActivity
import ir.mhajisoft.hesabres.R
import ir.mhajisoft.hesabres.data.repository.AccountBalance
import ir.mhajisoft.hesabres.domain.bank.BankMatch
import ir.mhajisoft.hesabres.domain.bank.CardMath
import ir.mhajisoft.hesabres.domain.bank.IbanMath
import ir.mhajisoft.hesabres.domain.crash.TabStack
import ir.mhajisoft.hesabres.domain.crash.WriteFailures
import ir.mhajisoft.hesabres.domain.jalali.JalaliConverter
import ir.mhajisoft.hesabres.domain.jalali.JalaliLabels
import ir.mhajisoft.hesabres.domain.jalali.JalaliYmd
import ir.mhajisoft.hesabres.domain.ledger.SystemCategories
import ir.mhajisoft.hesabres.domain.model.Account
import ir.mhajisoft.hesabres.domain.model.AccountType
import ir.mhajisoft.hesabres.domain.model.FiscalYear
import ir.mhajisoft.hesabres.domain.model.AppLockSettings
import ir.mhajisoft.hesabres.domain.model.Category
import ir.mhajisoft.hesabres.domain.model.CategoryKind
import ir.mhajisoft.hesabres.domain.model.Direction
import ir.mhajisoft.hesabres.domain.model.LedgerTransaction
import ir.mhajisoft.hesabres.domain.money.Money
import ir.mhajisoft.hesabres.domain.money.PersianDigits
import ir.mhajisoft.hesabres.ui.components.BalanceHero
import ir.mhajisoft.hesabres.ui.components.CheckRow
import ir.mhajisoft.hesabres.ui.components.ChoiceChip
import ir.mhajisoft.hesabres.ui.components.DestinationRow
import ir.mhajisoft.hesabres.ui.components.ExpiryMonthYearPicker
import ir.mhajisoft.hesabres.ui.components.FinanceCard
import ir.mhajisoft.hesabres.ui.components.FinanceEmptyState
import ir.mhajisoft.hesabres.ui.components.FinanceTextField
import ir.mhajisoft.hesabres.ui.components.IconBadge
import ir.mhajisoft.hesabres.ui.components.MoneyToneText
import ir.mhajisoft.hesabres.ui.components.PrimaryWideButton
import ir.mhajisoft.hesabres.ui.components.ScreenHeader
import ir.mhajisoft.hesabres.ui.components.SecondaryWideButton
import ir.mhajisoft.hesabres.ui.components.SectionLabel
import ir.mhajisoft.hesabres.ui.components.ShareBar
import ir.mhajisoft.hesabres.ui.components.SoftDivider
import ir.mhajisoft.hesabres.ui.components.StepIndicator
import ir.mhajisoft.hesabres.ui.components.SwitchRow
import ir.mhajisoft.hesabres.ui.components.formatJalali
import ir.mhajisoft.hesabres.ui.components.formatMoney
import ir.mhajisoft.hesabres.ui.theme.HesabresTheme
import ir.mhajisoft.hesabres.ui.theme.LocalLedgerTones
import ir.mhajisoft.hesabres.ui.theme.SymbolIcons
import ir.mhajisoft.hesabres.ui.theme.argbColor
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
@Serializable data class RoutePersonEdit(val personId: String? = null) : NavKey
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
    LaunchedEffect(Unit) {
        vm.messages.collect { notify(it) }
    }
    val navColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val navStroke = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            val current = backStack.lastOrNull()
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                tonalElevation = 3.dp,
                modifier = Modifier.drawBehind {
                    drawLine(navStroke, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
                },
            ) {
                NavigationBarItem(
                    selected = current is RouteHome,
                    onClick = { rewind(backStack, RouteHome) },
                    icon = { Icon(SymbolIcons.Home, null) },
                    label = { Text(stringResource(R.string.tab_home), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = navColors,
                )
                NavigationBarItem(
                    selected = current is RouteTxns,
                    onClick = { rewind(backStack, RouteTxns) },
                    icon = { Icon(SymbolIcons.Receipt, null) },
                    label = { Text(stringResource(R.string.tab_txns), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = navColors,
                )
                NavigationBarItem(
                    selected = current is RouteReports,
                    onClick = { rewind(backStack, RouteReports) },
                    icon = { Icon(SymbolIcons.Chart, null) },
                    label = { Text(stringResource(R.string.tab_reports), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = navColors,
                )
                NavigationBarItem(
                    selected = current is RouteMore,
                    onClick = { rewind(backStack, RouteMore) },
                    icon = { Icon(SymbolIcons.More, null) },
                    label = { Text(stringResource(R.string.tab_more), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = navColors,
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
                    shadowElevation = 6.dp,
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
                    ReportScreen(state) { catId -> backStack.add(RouteFiltered(catId)) }
                }
                entry<RouteMore> { MoreScreen { backStack.add(it) } }
                entry<RouteAccounts> { AccountsScreen(state, vm) }
                entry<RoutePeople> {
                    PeopleScreen(
                        state,
                        onOpen = { backStack.add(RoutePerson(it)) },
                        onAdd = { backStack.add(RoutePersonEdit()) },
                    )
                }
                entry<RoutePerson> { key ->
                    PersonDetailScreen(
                        state,
                        vm,
                        key.personId,
                        onBack = { backStack.removeLastOrNull() },
                        onEdit = { backStack.add(RoutePersonEdit(key.personId)) },
                    )
                }
                entry<RoutePersonEdit> { key ->
                    PersonEditScreen(state, vm, key.personId, onBack = { backStack.removeLastOrNull() })
                }
                entry<RouteCategories> { CategoriesScreen(state, vm) { notify(it) } }
                entry<RouteVault> {
                    VaultScreen(state, vm, onSecureWindow, onAddCard = { backStack.add(RouteCardForm) })
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
                vm.saveExpense(acc, cat, amt, note, at, income, onError = { notify(it) }, onOk = { showComposer = false })
            },
            onSaveTransfer = { from, to, amt, fee, note, at ->
                vm.saveTransfer(from, to, amt, fee, note, at, onError = { notify(it) }, onOk = { showComposer = false })
            },
        )
    }
}

private fun rewind(stack: NavBackStack<NavKey>, tab: NavKey) {
    TabStack.switchTo(stack, tab)
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
    var error by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BrandOnboardingHeader()
        Text(
            stringResource(R.string.tagline),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        StepIndicator(step)
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
                    CheckRow(stringResource(R.string.enable_lock), lock) { lock = it }
                    PrimaryWideButton(
                        stringResource(R.string.start),
                        onClick = {
                            val open = Money.parseDisplayAmount(opening, false) ?: 0L
                            error = null
                            vm.completeOnboarding(month, day, cashName, open, lock) { error = it }
                        },
                    )
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
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
    val today = remember { JalaliConverter.fromEpochMillis(System.currentTimeMillis()) }
    val monthRows = state.txns.filter {
        it.jalaliYear == today.year &&
            it.jalaliMonth == today.month &&
            it.transferId == null &&
            it.categoryId != SystemCategories.OPENING_BALANCE_ID
    }
    val monthIncome = monthRows.filter { it.direction == Direction.IN }.sumOf { it.amount }
    val monthExpense = monthRows.filter { it.direction == Direction.OUT }.sumOf { it.amount }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { BrandTopBarLogo() }
        item {
            BalanceHero(
                label = stringResource(R.string.total_balance),
                amount = formatMoney(state.total, toman),
                incomeLabel = stringResource(R.string.income),
                income = formatMoney(monthIncome, toman),
                expenseLabel = stringResource(R.string.expense),
                expense = formatMoney(monthExpense, toman),
            )
        }
        item {
            FinanceCard {
                SectionLabel(stringResource(R.string.accounts))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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
    val tint = cat?.let { argbColor(it.color) } ?: MaterialTheme.colorScheme.primary
    val subtitle = listOf(acc?.name.orEmpty(), formatJalali(txn.occurredAt), txn.note)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
    FinanceCard(
        Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentPadding = 12.dp,
        contentSpacing = 0.dp,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(SymbolIcons.byKey(cat?.iconKey ?: "swap_horiz"), tint)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    cat?.name ?: stringResource(R.string.transfer),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            MoneyToneText(
                sign + formatMoney(txn.amount, toman),
                inbound = txn.direction == Direction.IN,
                modifier = Modifier.widthIn(max = 132.dp),
            )
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
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ScreenHeader(stringResource(R.string.tab_txns))
            FinanceTextField(q, { q = it }, label = stringResource(R.string.search))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChoiceChip(accountFilter == null, { accountFilter = null }, stringResource(R.string.all))
                state.accounts.filter { it.type != AccountType.PERSON && !it.archived }.take(6).forEach { acc ->
                    ChoiceChip(accountFilter == acc.id, { accountFilter = acc.id }, acc.name)
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChoiceChip(categoryFilter == null, { categoryFilter = null }, stringResource(R.string.filter))
                state.categories.filter { !it.isSystem || it.kind != CategoryKind.TRANSFER }.take(6).forEach { cat ->
                    ChoiceChip(categoryFilter == cat.id, { categoryFilter = cat.id }, cat.name)
                }
            }
        }
        if (grouped.isEmpty()) {
            Box(Modifier.padding(horizontal = 16.dp)) {
                FinanceEmptyState(
                    SymbolIcons.Receipt,
                    stringResource(R.string.empty_txns_title),
                    stringResource(R.string.empty_txns),
                )
            }
        } else {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                grouped.toSortedMap(compareByDescending<Triple<Int, Int, Int>> { it.first }.thenByDescending { it.second }.thenByDescending { it.third }).forEach { (day, rows) ->
                    item(key = "day-$day") {
                        Text(
                            PersianDigits.toPersian("${day.first}/${day.second.toString().padStart(2, '0')}/${day.third.toString().padStart(2, '0')}"),
                            Modifier.padding(top = 8.dp, bottom = 2.dp),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
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
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
            }
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
fun ReportScreen(state: AppUiState, onSlice: (String) -> Unit) {
    val fy = state.fy
    if (fy == null) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ScreenHeader(stringResource(R.string.tab_reports))
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
    val maxAmt = byCat.values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(stringResource(R.string.tab_reports), PersianDigits.toPersian(fy.label))
        FinanceCard {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChoiceChip(!yearMode, { yearMode = false }, stringResource(R.string.month_mode))
                ChoiceChip(yearMode, { yearMode = true }, stringResource(R.string.year_mode))
            }
            if (!yearMode) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = { month = if (month == 1) 12 else month - 1 }) { Text("−") }
                    Text(
                        "${JalaliLabels.monthName(month)} ${PersianDigits.toPersian(fy.startJalaliYear.toString())}",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { month = if (month == 12) 1 else month + 1 }) { Text("+") }
                }
            } else {
                Text(PersianDigits.toPersian(fy.label), style = MaterialTheme.typography.titleMedium)
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
                SectionLabel(stringResource(R.string.pie_by_category))
                ReportCharts(byCat, rows, yearMode, onSlice)
            }
            byCat.forEach { (id, amt) ->
                val cat = state.categories.firstOrNull { it.id == id }
                FinanceCard(Modifier.clickable { onSlice(id) }, contentSpacing = 6.dp) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconBadge(
                            SymbolIcons.byKey(cat?.iconKey ?: "category"),
                            cat?.let { argbColor(it.color) } ?: MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            cat?.name ?: id,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            formatMoney(amt, state.settings.displayToman),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = LocalLedgerTones.current.expense,
                            modifier = Modifier.widthIn(max = 140.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    ShareBar(amt.toFloat() / maxAmt.toFloat())
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
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(byCat, rows, yearMode) {
        val grouped = if (yearMode) rows.groupBy { it.jalaliMonth } else rows.groupBy { it.jalaliDay }
        val daily = grouped.toSortedMap().filter { it.value.isNotEmpty() }
        if (byCat.isEmpty() || daily.isEmpty()) {
            ready = false
            return@LaunchedEffect
        }
        val plotted = runCatching {
            pieProducer.runTransaction {
                pieSeries {
                    series(byCat.values.map { it.toDouble() })
                }
            }
            barProducer.runTransaction {
                columnSeries {
                    series(
                        daily.keys.map { it.toDouble() },
                        daily.values.map { v ->
                            v.filter { it.direction == Direction.OUT }.sumOf { it.amount }.toDouble()
                        },
                    )
                }
            }
        }
        ready = plotted.isSuccess
    }
    val pieChart = rememberPieChart()
    val columnLayer = rememberColumnCartesianLayer()
    val barChart = rememberCartesianChart(
        columnLayer,
        startAxis = VerticalAxis.rememberStart(),
        bottomAxis = HorizontalAxis.rememberBottom(),
    )
    if (!ready) return
    PieChartHost(
        chart = pieChart,
        modelProducer = pieProducer,
        modifier = Modifier.height(200.dp).fillMaxWidth().clickable {
            catKeys.firstOrNull()?.let(onSlice)
        },
    )
    Text(stringResource(R.string.daily_bars), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
    CartesianChartHost(
        barChart,
        barProducer,
        modifier = Modifier.height(180.dp),
    )
}

@Composable
fun MoreScreen(open: (NavKey) -> Unit) {
    val items = listOf(
        Triple(R.string.accounts, RouteAccounts, SymbolIcons.Wallet),
        Triple(R.string.people, RoutePeople, SymbolIcons.People),
        Triple(R.string.categories, RouteCategories, SymbolIcons.Category),
        Triple(R.string.vault, RouteVault, SymbolIcons.Card),
        Triple(R.string.fiscal_year, RouteFiscal, SymbolIcons.Chart),
        Triple(R.string.archive, RouteArchive, SymbolIcons.Archive),
        Triple(R.string.backup, RouteBackup, SymbolIcons.Receipt),
        Triple(R.string.lock, RouteLock, SymbolIcons.Lock),
        Triple(R.string.settings, RouteSettings, SymbolIcons.Settings),
    )
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { BrandLockup(showTagline = true) }
        item {
            FinanceCard(contentPadding = 8.dp, contentSpacing = 0.dp) {
                items.forEachIndexed { index, (res, route, icon) ->
                    DestinationRow(icon, stringResource(res)) { open(route) }
                    if (index != items.lastIndex) SoftDivider()
                }
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
        ScreenHeader(stringResource(R.string.accounts))
        FinanceCard {
            SectionLabel(stringResource(R.string.add_account))
            FinanceTextField(name, { name = it }, label = stringResource(R.string.add_account))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChoiceChip(type == AccountType.CASH, { type = AccountType.CASH }, stringResource(R.string.type_cash))
                ChoiceChip(type == AccountType.BANK, { type = AccountType.BANK }, stringResource(R.string.type_bank))
                ChoiceChip(type == AccountType.CARD, { type = AccountType.CARD }, stringResource(R.string.type_card))
            }
            CheckRow(stringResource(R.string.include_in_total), include) { include = it }
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
            val icon = when (acc.type) {
                AccountType.BANK -> SymbolIcons.byKey("account_balance")
                AccountType.CARD -> SymbolIcons.Card
                else -> SymbolIcons.Wallet
            }
            FinanceCard(Modifier.clickable { editing = acc }) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(icon, argbColor(acc.color))
                    Column(Modifier.weight(1f)) {
                        Text(acc.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(
                    formatMoney(bal ?: 0L, state.settings.displayToman),
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
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
                    CheckRow(stringResource(R.string.include_in_total), editInclude) { editInclude = it }
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
    var color by remember { mutableStateOf(0xFF0B6E4FL) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader(stringResource(R.string.categories)) }
        item {
            FinanceCard {
                SectionLabel(stringResource(R.string.add_category))
                FinanceTextField(name, { name = it }, label = stringResource(R.string.add_category))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ChoiceChip(kind == CategoryKind.EXPENSE, { kind = CategoryKind.EXPENSE }, stringResource(R.string.expense))
                    ChoiceChip(kind == CategoryKind.INCOME, { kind = CategoryKind.INCOME }, stringResource(R.string.income))
                }
                SectionLabel(stringResource(R.string.icon_pack))
                ir.mhajisoft.hesabres.ui.components.IconPackPicker(iconKey) { iconKey = it }
                SectionLabel(stringResource(R.string.color_pack))
                ir.mhajisoft.hesabres.ui.components.ColorPackPicker(color) { color = it }
                PrimaryWideButton(stringResource(R.string.save), onClick = {
                    if (name.isNotBlank()) {
                        vm.addCustomCategory(name, iconKey, color, kind)
                        name = ""
                    }
                })
            }
        }
        items(state.categories, key = { it.id }) { cat ->
            FinanceCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconBadge(SymbolIcons.byKey(cat.iconKey), argbColor(cat.color))
                    Column(Modifier.weight(1f)) {
                        Text(
                            cat.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            when {
                                cat.isSystem -> stringResource(R.string.system_category)
                                cat.kind == CategoryKind.EXPENSE -> stringResource(R.string.expense)
                                cat.kind == CategoryKind.INCOME -> stringResource(R.string.income)
                                else -> stringResource(R.string.transfer)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!cat.isSystem) {
                        TextButton(onClick = { vm.deleteCustomCategory(cat.id) { onError(it) } }) {
                            Text(stringResource(R.string.delete))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VaultScreen(state: AppUiState, vm: AppViewModel, onSecure: (Boolean) -> Unit, onAddCard: () -> Unit) {
    DisposableEffect(Unit) {
        onSecure(true)
        onDispose { onSecure(false) }
    }
    val cards by vm.vaultRepo.cardsFlow.collectAsStateWithLifecycle(emptyList())
    val ibans by vm.vaultRepo.bankAccountsFlow.collectAsStateWithLifecycle(emptyList())
    val ownerLabel = stringResource(R.string.owner_me)
    fun holderLabel(personId: String?): String {
        if (personId.isNullOrBlank()) return ownerLabel
        return state.people.firstOrNull { it.id == personId }?.displayName ?: ownerLabel
    }
    Column(
        Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(stringResource(R.string.vault), stringResource(R.string.bank_info))
        PrimaryWideButton(stringResource(R.string.new_card), onClick = onAddCard)
        if (cards.isEmpty() && ibans.isEmpty()) {
            FinanceEmptyState(SymbolIcons.Card, stringResource(R.string.empty_cards), stringResource(R.string.empty_cards_body))
        }
        val scope = rememberCoroutineScope()
        val activity = LocalContext.current.findMainActivity()
        if (activity == null) {
            Text(WriteFailures.ERR_NO_ACTIVITY, color = MaterialTheme.colorScheme.error)
            return@Column
        }
        cards.forEach { c ->
            val logo = vm.vaultRepo.directory.logoOf(c.bin6, c.bankCode)
            val bank = vm.vaultRepo.directory.findById(c.bankCode) ?: vm.vaultRepo.directory.findByBin(c.bin6)
            var revealed by remember(c.id) { mutableStateOf<String?>(null) }
            FinanceCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BankLogo(logo)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            revealed ?: CardMath.maskPan(c.last4.padStart(16, '*')),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            bank?.nameFa ?: stringResource(R.string.unknown_bank),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            holderLabel(c.personId),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (c.panCipherId != null) {
                    TextButton(onClick = {
                        val panId = c.panCipherId
                        activity.lockBeforePan {
                            activity.lifecycleScope.launch {
                                revealed = runCatching { vm.vaultRepo.decryptPan(panId) }.getOrNull()
                            }
                        }
                    }) { Text(stringResource(R.string.reveal_pan)) }
                }
                if (c.rememberCvv && c.cvvCipherId != null) {
                    val cvvId = c.cvvCipherId
                    TextButton(onClick = {
                        activity.lifecycleScope.launch {
                            val iv = vm.vaultRepo.secretVault.cvvIv(cvvId) ?: return@launch
                            val cipher = vm.vaultRepo.secretVault.createCvvDecryptCipher(iv) ?: return@launch
                            activity.promptUnlock(
                                crypto = BiometricPrompt.CryptoObject(cipher),
                                onSuccess = { result ->
                                    activity.lifecycleScope.launch {
                                        val unlocked = result.cryptoObject?.cipher ?: cipher
                                        val cvv = runCatching {
                                            vm.vaultRepo.secretVault.revealCvv(cvvId, unlocked)
                                        }.getOrNull() ?: return@launch
                                        val cm = activity.getSystemService(ClipboardManager::class.java)
                                        cm.setPrimaryClip(ClipData.newPlainText(activity.getString(R.string.cvv), cvv))
                                        scope.launch {
                                            delay(30_000)
                                            cm.setPrimaryClip(ClipData.newPlainText("", ""))
                                        }
                                    }
                                },
                                onCancel = {},
                                onError = {},
                            )
                        }
                    }) { Text(stringResource(R.string.reveal_cvv)) }
                }
            }
        }
        ibans.forEach { a ->
            FinanceCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BankLogo(vm.vaultRepo.directory.findById(a.bankCode)?.logoDrawable ?: "bank_unknown")
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            IbanMath.formatGrouped(a.iban),
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(a.bankName, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            holderLabel(a.personId),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BankLogo(drawableName: String) {
    val ctx = LocalContext.current
    val id = ctx.resources.getIdentifier(drawableName, "drawable", ctx.packageName)
    val fallback = ctx.resources.getIdentifier("bank_unknown", "drawable", ctx.packageName)
    val res = if (id != 0) id else fallback
    if (res != 0) Image(painterResource(res), null, Modifier.size(40.dp))
}

@Composable
fun CardFormScreen(state: AppUiState, vm: AppViewModel, onSecure: (Boolean) -> Unit) {
    DisposableEffect(Unit) {
        onSecure(true)
        onDispose { onSecure(false) }
    }
    val activity = LocalContext.current.findMainActivity()
    val today = remember { JalaliConverter.fromEpochMillis(System.currentTimeMillis()) }
    var pan by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var rememberCvv by remember { mutableStateOf(false) }
    var expiryMonth by remember { mutableIntStateOf(today.month) }
    var expiryYear by remember { mutableIntStateOf(today.year) }
    var iban by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var holderPersonId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val digits = CardMath.normalizeDigits(pan)
    val match = if (digits.length >= 6) vm.vaultRepo.directory.resolvePan(digits) else BankMatch.Unknown
    val bank = (match as? BankMatch.Known)?.bank
    val ownerAccount = state.settings.defaultAccountId
        ?: state.accounts.firstOrNull { it.type != AccountType.PERSON && !it.archived }?.id
    val person = state.people.firstOrNull { it.id == holderPersonId }
    val ledgerAccountId = person?.accountId ?: ownerAccount
    Column(
        Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(stringResource(R.string.bank_info))
        FinanceCard {
            SectionLabel(stringResource(R.string.assign_holder))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip(holderPersonId == null, { holderPersonId = null }, stringResource(R.string.owner_me))
                state.people.forEach { p ->
                    ChoiceChip(holderPersonId == p.id, { holderPersonId = p.id }, p.displayName)
                }
            }
        }
        FinanceTextField(
            pan,
            { pan = it },
            label = stringResource(R.string.card_number),
            keyboardType = KeyboardType.Number,
            leadingIcon = { BankLogo(bank?.logoDrawable ?: "bank_unknown") },
        )
        if (digits.length >= 6) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BankLogo(bank?.logoDrawable ?: "bank_unknown")
                Text(bank?.nameFa ?: stringResource(R.string.unknown_bank))
            }
        }
        ExpiryMonthYearPicker(expiryMonth, expiryYear) { m, y ->
            expiryMonth = m
            expiryYear = y
        }
        FinanceTextField(cvv, { cvv = it }, label = stringResource(R.string.cvv), keyboardType = KeyboardType.NumberPassword)
        SwitchRow(stringResource(R.string.remember_cvv), rememberCvv) { rememberCvv = it }
        Text(stringResource(R.string.cvv_warning), style = MaterialTheme.typography.bodySmall)
        PrimaryWideButton(stringResource(R.string.save), onClick = {
            val host = activity ?: run {
                error = WriteFailures.ERR_NO_ACTIVITY
                return@PrimaryWideButton
            }
            if (!CardMath.luhnValid(pan)) {
                error = host.getString(R.string.invalid_luhn)
                return@PrimaryWideButton
            }
            val accountId = ledgerAccountId ?: return@PrimaryWideButton
            val holderName = person?.displayName ?: host.getString(R.string.owner_me)
            if (rememberCvv && cvv.isNotBlank()) {
                val cipher = vm.vaultRepo.secretVault.createCvvEncryptCipher()
                if (cipher == null) {
                    error = host.getString(R.string.cvv_unavailable)
                    return@PrimaryWideButton
                }
                host.promptUnlock(
                    crypto = BiometricPrompt.CryptoObject(cipher),
                    onSuccess = { result ->
                        host.lifecycleScope.launch {
                            val unlocked = result.cryptoObject?.cipher ?: cipher
                            runCatching {
                                vm.vaultRepo.saveCard(
                                    accountId = accountId,
                                    panAscii = pan,
                                    expiryMonth = expiryMonth,
                                    expiryYear = expiryYear,
                                    holderName = holderName,
                                    rememberCvv = true,
                                    cvvAscii = cvv,
                                    encryptCvv = { ascii -> vm.vaultRepo.secretVault.persistCvv(unlocked, ascii) },
                                    personId = holderPersonId,
                                )
                            }.onFailure { error = WriteFailures.ERR_CVV }
                        }
                    },
                    onCancel = {},
                    onError = { error = WriteFailures.ERR_BIOMETRIC },
                )
            } else {
                host.promptUnlock(
                    onSuccess = {
                        host.lifecycleScope.launch {
                            runCatching {
                                vm.vaultRepo.saveCard(
                                    accountId = accountId,
                                    panAscii = pan,
                                    expiryMonth = expiryMonth,
                                    expiryYear = expiryYear,
                                    holderName = holderName,
                                    rememberCvv = false,
                                    cvvAscii = null,
                                    personId = holderPersonId,
                                )
                            }.onFailure { error = WriteFailures.ERR_GENERIC }
                        }
                    },
                    onCancel = {},
                    onError = { error = WriteFailures.ERR_BIOMETRIC },
                )
            }
        })
        FinanceTextField(accountNumber, { accountNumber = it }, label = stringResource(R.string.account_number))
        FinanceTextField(
            IbanMath.formatGrouped(iban),
            { iban = it },
            label = stringResource(R.string.iban),
        )
        val ibanMatch = vm.vaultRepo.directory.resolveIban(iban)
        val ibanBank = (ibanMatch as? BankMatch.Known)?.bank
        if (IbanMath.normalize(iban).length >= 7) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BankLogo(ibanBank?.logoDrawable ?: "bank_unknown")
                Text(ibanBank?.nameFa ?: stringResource(R.string.unknown_bank))
            }
        }
        PrimaryWideButton(stringResource(R.string.save_iban), onClick = {
            val host = activity ?: run {
                error = WriteFailures.ERR_NO_ACTIVITY
                return@PrimaryWideButton
            }
            if (!IbanMath.isValidIranIban(iban)) {
                error = host.getString(R.string.invalid_iban)
            } else {
                val accountId = ledgerAccountId ?: return@PrimaryWideButton
                host.lifecycleScope.launch {
                    runCatching {
                        vm.vaultRepo.saveBankAccount(accountId, accountNumber, iban, personId = holderPersonId)
                    }.onFailure { error = WriteFailures.ERR_GENERIC }
                }
            }
        })
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
fun FiscalScreen(state: AppUiState, vm: AppViewModel) {
    val fy = state.fy
    var confirm by remember { mutableStateOf(false) }
    val openings by vm.openings.collectAsStateWithLifecycle()
    Column(
        Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(stringResource(R.string.fiscal_year))
        FinanceCard {
            fy?.let {
                Text(
                    "${PersianDigits.toPersian(it.startJalaliYear.toString())}/${PersianDigits.toPersian(it.startJalaliMonth.toString().padStart(2,'0'))}/${PersianDigits.toPersian(it.startJalaliDay.toString().padStart(2,'0'))} — ${PersianDigits.toPersian(it.endJalaliYear.toString())}/${PersianDigits.toPersian(it.endJalaliMonth.toString().padStart(2,'0'))}/${PersianDigits.toPersian(it.endJalaliDay.toString().padStart(2,'0'))}",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            PrimaryWideButton(stringResource(R.string.fy_reset_farvardin), onClick = { confirm = true })
        }
        FinanceCard {
            SectionLabel(stringResource(R.string.opening_balance))
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
        }
        if (confirm) {
            AlertDialog(
                onDismissRequest = { confirm = false },
                confirmButton = { TextButton(onClick = { vm.setFyStart(1, 1, true); confirm = false }) { Text(stringResource(R.string.ok)) } },
                dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) } },
                text = { Text(stringResource(R.string.confirm_rebucket)) },
            )
        }
        FinanceCard {
            Text(stringResource(R.string.close_year_hint), style = MaterialTheme.typography.bodyMedium)
            PrimaryWideButton(stringResource(R.string.close_year), onClick = { vm.closeCurrentYear() })
        }
    }
}

@Composable
fun ArchiveScreen(state: AppUiState, vm: AppViewModel, open: (String) -> Unit) {
    val recs by vm.archiveRepo.records.collectAsStateWithLifecycle(emptyList())
    val archivedIds = recs.map { it.fiscalYearId }.toSet()
    Column(
        Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(stringResource(R.string.archive))
        state.fy?.let {
            PrimaryWideButton(stringResource(R.string.close_year), onClick = { vm.closeCurrentYear() })
        }
        state.fiscalYears.filter { !it.isCurrent && it.id !in archivedIds }.forEach { fy ->
            FinanceCard {
                Text(PersianDigits.toPersian(fy.label), style = MaterialTheme.typography.titleMedium)
                PrimaryWideButton(stringResource(R.string.archive_year), onClick = { vm.archiveYear(fy.id) })
            }
        }
        recs.forEach {
            FinanceCard(Modifier.clickable { open(it.fileName) }) {
                Text(it.fileName, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.read_only), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.open_archive), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun ArchiveViewerScreen(vm: AppViewModel, fileName: String) {
    var rows by remember { mutableStateOf(emptyList<ir.mhajisoft.hesabres.data.archive.ArchivedTxn>()) }
    LaunchedEffect(fileName) {
        rows = runCatching { vm.archiveRepo.readArchivedTransactions(fileName) }.getOrDefault(emptyList())
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScreenHeader(fileName, stringResource(R.string.read_only))
        if (rows.isEmpty()) {
            FinanceEmptyState(SymbolIcons.Receipt, stringResource(R.string.read_only), stringResource(R.string.empty_txns))
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(rows, key = { it.id }) { txn ->
                    FinanceCard(contentPadding = 12.dp, contentSpacing = 2.dp) {
                        Text(
                            txn.note.ifBlank { stringResource(R.string.transfer) },
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            PersianDigits.toPersian("${txn.jalaliYear}/${txn.jalaliMonth}/${txn.jalaliDay}"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(formatMoney(txn.amount, false), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BackupScreen(vm: AppViewModel) {
    var pass by remember { mutableStateOf("") }
    val ctx = LocalContext.current
    var backupError by remember { mutableStateOf<String?>(null) }
    fun activityOrNull(): MainActivity? = ctx.findMainActivity()
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val activity = activityOrNull() ?: run {
            backupError = WriteFailures.ERR_NO_ACTIVITY
            return@rememberLauncherForActivityResult
        }
        if (uri != null) {
            activity.lifecycleScope.launch {
                backupError = runCatching {
                    val bytes = vm.backupRepo.createEncryptedBackup(pass)
                    vm.backupRepo.writeToSaf(uri, bytes)
                    vm.backupRepo.recordLocal(uri.toString(), bytes)
                    null
                }.exceptionOrNull()?.let { WriteFailures.ERR_BACKUP }
            }
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val activity = activityOrNull() ?: run {
            backupError = WriteFailures.ERR_NO_ACTIVITY
            return@rememberLauncherForActivityResult
        }
        if (uri != null) {
            activity.lifecycleScope.launch {
                backupError = runCatching {
                    val bytes = vm.backupRepo.readFromSaf(uri)
                    vm.backupRepo.restoreEncrypted(bytes, pass)
                    vm.backupRepo.restartProcess(activity)
                    null
                }.exceptionOrNull()?.let { WriteFailures.ERR_RESTORE }
            }
        }
    }
    val drive = vm.backupRepo.driveAvailability()
    val one = vm.backupRepo.oneDriveAvailability()
    var cloudMsg by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(stringResource(R.string.backup))
        FinanceCard {
            FinanceTextField(pass, { pass = it }, label = stringResource(R.string.passphrase))
            PrimaryWideButton(
                stringResource(R.string.backup_local),
                enabled = pass.length >= 4,
                onClick = { create.launch("mini-accountant.pfbak") },
            )
            SecondaryWideButton(stringResource(R.string.restore_local)) {
                open.launch(arrayOf("application/octet-stream", "*/*"))
            }
        }
        FinanceCard {
            SectionLabel(stringResource(R.string.backup_drive))
            if (!drive.available) {
                Text(drive.reasonFa, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                PrimaryWideButton(stringResource(R.string.upload_drive), enabled = pass.length >= 4, onClick = {
                    val activity = activityOrNull() ?: run {
                        backupError = WriteFailures.ERR_NO_ACTIVITY
                        return@PrimaryWideButton
                    }
                    activity.lifecycleScope.launch {
                        runCatching {
                            val bytes = vm.backupRepo.createEncryptedBackup(pass)
                            val result = vm.backupRepo.uploadCloud(ir.mhajisoft.hesabres.data.cloud.CloudKind.DRIVE, bytes)
                            cloudMsg = result.exceptionOrNull()?.message ?: ctx.getString(R.string.ok)
                        }.onFailure { backupError = WriteFailures.ERR_BACKUP }
                    }
                })
            }
        }
        FinanceCard {
            SectionLabel(stringResource(R.string.backup_onedrive))
            if (!one.available) {
                Text(one.reasonFa, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                PrimaryWideButton(stringResource(R.string.upload_onedrive), enabled = pass.length >= 4, onClick = {
                    val activity = activityOrNull() ?: run {
                        backupError = WriteFailures.ERR_NO_ACTIVITY
                        return@PrimaryWideButton
                    }
                    activity.lifecycleScope.launch {
                        runCatching {
                            val bytes = vm.backupRepo.createEncryptedBackup(pass)
                            val result = vm.backupRepo.uploadCloud(ir.mhajisoft.hesabres.data.cloud.CloudKind.ONEDRIVE, bytes)
                            cloudMsg = result.exceptionOrNull()?.message ?: ctx.getString(R.string.ok)
                        }.onFailure { backupError = WriteFailures.ERR_BACKUP }
                    }
                })
            }
        }
        backupError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        cloudMsg?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
fun LockSettingsScreen(state: AppUiState, vm: AppViewModel) {
    val lock = state.settings.lock
    Column(
        Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(stringResource(R.string.lock))
        FinanceCard {
            SwitchRow(stringResource(R.string.enable_lock), lock.enabled) { vm.setLock(lock.copy(enabled = it)) }
            SectionLabel(stringResource(R.string.lock_timeout))
            Text(
                PersianDigits.toPersian(lock.timeoutSec.toString()),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Slider(
                lock.timeoutSec.toFloat(),
                { vm.setLock(lock.copy(timeoutSec = it.toInt().coerceIn(0, 300))) },
                valueRange = 0f..300f,
            )
        }
    }
}

@Composable
fun SettingsScreen(state: AppUiState, vm: AppViewModel) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScreenHeader(stringResource(R.string.settings))
        FinanceCard {
            SectionLabel(stringResource(R.string.display_unit))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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

private fun Context.findMainActivity(): MainActivity? {
    var current: Context? = this
    while (current is android.content.ContextWrapper) {
        if (current is MainActivity) return current
        current = current.baseContext
    }
    return null
}

private fun previewFinanceState(): AppUiState {
    val today = JalaliConverter.fromEpochMillis(System.currentTimeMillis())
    val cash = Account("1", "نقد", AccountType.CASH, color = 0xFF0F766E, sortOrder = 0, createdAt = 0, updatedAt = 0)
    val bank = Account("2", "بانک ملت", AccountType.BANK, color = 0xFF1D4ED8, sortOrder = 1, createdAt = 0, updatedAt = 0)
    val fy = FiscalYear(
        id = "fy",
        label = "${today.year}",
        startJalaliYear = today.year,
        startJalaliMonth = 1,
        startJalaliDay = 1,
        endJalaliYear = today.year,
        endJalaliMonth = 12,
        endJalaliDay = 29,
        startEpoch = 0,
        endEpoch = 0,
        isCurrent = true,
        closedAt = null,
    )
    val txns = listOf(
        LedgerTransaction("t1", "1", "sys-food", null, 1_850_000, Direction.OUT, "نان سنگک", 0, today.year, today.month, today.day, "fy", null, 0),
        LedgerTransaction("t2", "2", "sys-transport", null, 640_000, Direction.OUT, "مترو", 0, today.year, today.month, today.day, "fy", null, 0),
        LedgerTransaction("t3", "2", "sys-salary", null, 85_000_000, Direction.IN, "حقوق", 0, today.year, today.month, today.day, "fy", null, 0),
    )
    return AppUiState(
        ready = true,
        accounts = listOf(cash, bank),
        balances = listOf(AccountBalance(cash, 12_400_000), AccountBalance(bank, 86_200_000)),
        recent = txns,
        txns = txns,
        categories = ir.mhajisoft.hesabres.domain.ledger.CategoryCatalog.systemCategories(),
        fy = fy,
        fiscalYears = listOf(fy),
    )
}

@Preview(showBackground = true, locale = "fa", name = "Home", widthDp = 390, heightDp = 820)
@Preview(showBackground = true, locale = "fa", name = "Home dark", widthDp = 390, heightDp = 820, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewHome() {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    HesabresTheme(darkTheme = dark) {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeScreen(previewFinanceState(), {}, {}, {})
        }
    }
}

@Preview(showBackground = true, locale = "fa", name = "Txn list", widthDp = 390, heightDp = 820)
@Composable
fun PreviewTxns() {
    HesabresTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TxnListScreen(previewFinanceState(), {})
        }
    }
}

@Preview(showBackground = true, locale = "fa", name = "Reports", widthDp = 390, heightDp = 820)
@Preview(showBackground = true, locale = "fa", name = "Reports dark", widthDp = 390, heightDp = 820, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewReports() {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    HesabresTheme(darkTheme = dark) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ReportScreen(previewFinanceState()) {}
        }
    }
}

@Preview(showBackground = true, locale = "fa", name = "More", widthDp = 390, heightDp = 820)
@Preview(showBackground = true, locale = "fa", name = "More dark", widthDp = 390, heightDp = 820, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewMore() {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    HesabresTheme(darkTheme = dark) {
        Surface(color = MaterialTheme.colorScheme.background) {
            MoreScreen {}
        }
    }
}

@Preview(showBackground = true, locale = "fa", name = "Card form")
@Composable
fun PreviewCard() {
    HesabresTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.card_form), style = MaterialTheme.typography.titleLarge)
            FinanceTextField("6104337812345678", {}, label = stringResource(R.string.card_number), keyboardType = KeyboardType.Number)
            Text(stringResource(R.string.bank_mellat_name))
        }
    }
}
