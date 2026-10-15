package ir.mhajisoft.hesabres.ui

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.mhajisoft.hesabres.R
import ir.mhajisoft.hesabres.domain.backup.BackupReminder
import ir.mhajisoft.hesabres.domain.jalali.JalaliCalendar
import ir.mhajisoft.hesabres.domain.jalali.JalaliConverter
import ir.mhajisoft.hesabres.domain.jalali.JalaliLabels
import ir.mhajisoft.hesabres.domain.jalali.JalaliYmd
import ir.mhajisoft.hesabres.domain.model.Account
import ir.mhajisoft.hesabres.domain.model.AccountType
import ir.mhajisoft.hesabres.domain.model.Direction
import ir.mhajisoft.hesabres.domain.model.LedgerTransaction
import ir.mhajisoft.hesabres.domain.model.Person
import ir.mhajisoft.hesabres.domain.money.Money
import ir.mhajisoft.hesabres.domain.money.PersianDigits
import ir.mhajisoft.hesabres.domain.planner.BudgetMath
import ir.mhajisoft.hesabres.domain.planner.Cheque
import ir.mhajisoft.hesabres.domain.planner.ChequeStatus
import ir.mhajisoft.hesabres.domain.planner.DueAgenda
import ir.mhajisoft.hesabres.domain.planner.DueKind
import ir.mhajisoft.hesabres.domain.planner.FlowSide
import ir.mhajisoft.hesabres.domain.planner.InstallmentPlan
import ir.mhajisoft.hesabres.domain.planner.MonthBudget
import ir.mhajisoft.hesabres.domain.planner.Note
import ir.mhajisoft.hesabres.domain.planner.PlannerRules
import ir.mhajisoft.hesabres.domain.report.ReportMath
import ir.mhajisoft.hesabres.ui.components.Appear
import ir.mhajisoft.hesabres.ui.components.ArrowButton
import ir.mhajisoft.hesabres.ui.components.DayArrow
import ir.mhajisoft.hesabres.ui.components.FinanceCard
import ir.mhajisoft.hesabres.ui.components.FinanceEmptyState
import ir.mhajisoft.hesabres.ui.components.FinanceTextField
import ir.mhajisoft.hesabres.ui.components.IconBadge
import ir.mhajisoft.hesabres.ui.components.LavenderPill
import ir.mhajisoft.hesabres.ui.components.MonthBars
import ir.mhajisoft.hesabres.ui.components.PeachToggle
import ir.mhajisoft.hesabres.ui.components.PrimaryWideButton
import ir.mhajisoft.hesabres.ui.components.ReorderColumn
import ir.mhajisoft.hesabres.ui.components.SecondaryWideButton
import ir.mhajisoft.hesabres.ui.components.ShareTrack
import ir.mhajisoft.hesabres.ui.components.SlideSwap
import ir.mhajisoft.hesabres.ui.components.dayTitle
import ir.mhajisoft.hesabres.ui.components.formatSignedAmount
import ir.mhajisoft.hesabres.ui.theme.PoolakiChip
import ir.mhajisoft.hesabres.ui.theme.PoolakiIndigo
import ir.mhajisoft.hesabres.ui.theme.PoolakiMuted
import ir.mhajisoft.hesabres.ui.theme.PoolakiNegative
import ir.mhajisoft.hesabres.ui.theme.SymbolIcons
import ir.mhajisoft.hesabres.ui.theme.argbColor
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun HomeDashboard(
    state: AppUiState,
    notes: List<Note>,
    cheques: List<Cheque>,
    installments: List<InstallmentPlan>,
    lastBackupAt: Long?,
    onAccount: (String) -> Unit,
    onOpenAll: () -> Unit,
    onSearch: () -> Unit,
    onLedgers: () -> Unit,
    onBackup: () -> Unit,
    onAddNote: (JalaliYmd) -> Unit,
    onAddCheque: (JalaliYmd) -> Unit,
    onAddInstallment: (JalaliYmd) -> Unit,
    onEdit: (LedgerTransaction) -> Unit,
) {
    val today = remember { JalaliConverter.fromEpochMillis(System.currentTimeMillis()) }
    var day by remember { mutableStateOf(today) }
    var forward by remember { mutableStateOf(true) }
    var info by remember { mutableStateOf(false) }
    val toman = state.settings.displayToman
    val ledgers = state.balances.filter { it.account.type != AccountType.PERSON && !it.account.archived }
    val recent = ReportMath.recent(state.txns, 6)
    val agenda = DueAgenda.forDay(day, today, notes, cheques, installments)
    val overdue = BackupReminder.isOverdue(lastBackupAt, System.currentTimeMillis())
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(PoolakiIndigo)
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeaderIconButton(SymbolIcons.Receipt, onLedgers)
                Text(
                    formatSignedAmount(state.total, toman),
                    modifier = Modifier.weight(1f),
                    color = if (state.total < 0) Color(0xFFFFD0D0) else Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                HeaderIconButton(SymbolIcons.byKey("search").let { SymbolIcons.Category }, onSearch)
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ledgers.forEach { row ->
                    LedgerMiniCard(row.account, row.balanceSigned, toman) { onAccount(row.account.id) }
                }
            }
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (overdue) {
                Appear {
                    FinanceCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                stringResource(R.string.backup_overdue_title),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text("⚠", color = PoolakiNegative)
                        }
                        Text(stringResource(R.string.backup_overdue_body), color = PoolakiMuted, style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            HeaderIconButton(SymbolIcons.Report, { info = true }, tinted = true)
                            LavenderPill(stringResource(R.string.backup_now), onBackup, Modifier.weight(1f))
                        }
                    }
                }
            }
            Appear {
                FinanceCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DayArrow("‹") { forward = false; day = JalaliCalendar.shiftDay(day, -1) }
                        DayArrow("›") { forward = true; day = JalaliCalendar.shiftDay(day, 1) }
                        Text(
                            stringResource(R.string.today),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Icon(SymbolIcons.Receipt, null, tint = PoolakiIndigo)
                    }
                    SlideSwap(day, forward) { shown ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(dayTitle(shown, today), color = PoolakiMuted, style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                LavenderPill(stringResource(R.string.quick_note), { onAddNote(shown) })
                                LavenderPill(stringResource(R.string.quick_cheque), { onAddCheque(shown) })
                                LavenderPill(stringResource(R.string.quick_installment), { onAddInstallment(shown) })
                            }
                            val overdueLabel = stringResource(R.string.overdue)
                            if (agenda.isEmpty()) {
                                Text(stringResource(R.string.no_due), color = PoolakiMuted, style = MaterialTheme.typography.bodyMedium)
                            } else {
                                agenda.forEach { item ->
                                    Text(
                                        buildString {
                                            if (item.overdue) append("$overdueLabel · ")
                                            append(item.title)
                                            if (item.amount != null) append(" · " + formatSignedAmount(-item.amount, toman))
                                        },
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Appear {
                FinanceCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LavenderPill(stringResource(R.string.see_all) + " ‹", onOpenAll)
                        Text(
                            stringResource(R.string.recent_card),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Icon(SymbolIcons.Receipt, null, tint = PoolakiIndigo, modifier = Modifier.padding(start = 6.dp))
                    }
                    if (recent.isEmpty()) {
                        Text(stringResource(R.string.empty_home), color = PoolakiMuted)
                    } else {
                        recent.forEach { txn ->
                            CompactTxn(txn, state, onClick = { if (txn.transferId == null) onEdit(txn) })
                        }
                    }
                }
            }
        }
    }
    if (info) {
        AlertDialog(
            onDismissRequest = { info = false },
            confirmButton = { TextButton(onClick = { info = false }) { Text(stringResource(R.string.ok)) } },
            text = { Text(stringResource(R.string.backup_info)) },
        )
    }
}

@Composable
private fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    tinted: Boolean = false,
) {
    Box(
        Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (tinted) PoolakiChip else Color.White.copy(alpha = 0.16f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (tinted) PoolakiIndigo else Color.White)
    }
}

@Composable
private fun LedgerMiniCard(account: Account, balance: Long, toman: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .width(168.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                if (account.type == AccountType.CASH) SymbolIcons.Wallet else SymbolIcons.Card,
                null,
                tint = PoolakiIndigo,
                modifier = Modifier.size(18.dp),
            )
            Text(account.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            formatSignedAmount(balance, toman),
            color = if (balance < 0) PoolakiNegative else Color(0xFF222222),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactTxn(txn: LedgerTransaction, state: AppUiState, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    val cat = state.categories.firstOrNull { it.id == txn.categoryId }
    val toman = state.settings.displayToman
    val tint = cat?.let { argbColor(it.color) } ?: PoolakiIndigo
    Row(
        Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                formatSignedAmount(if (txn.direction == Direction.IN) txn.amount else -txn.amount, toman),
                fontWeight = FontWeight.Bold,
                color = if (txn.direction == Direction.OUT) PoolakiNegative else Color(0xFF222222),
                maxLines = 1,
            )
            Text(
                PersianDigits.toPersian("${txn.jalaliDay} ${JalaliLabels.monthName(txn.jalaliMonth)}"),
                color = PoolakiMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
            Text(txn.note.ifBlank { cat?.name ?: stringResource(R.string.transfer) }, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(cat?.name.orEmpty(), color = PoolakiMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
        IconBadge(SymbolIcons.byKey(cat?.iconKey ?: "category"), tint)
    }
}

@Composable
fun ReportDashboard(
    state: AppUiState,
    onFiltered: (categoryId: String?, accountId: String?, year: Int, month: Int) -> Unit,
) {
    val today = remember { JalaliConverter.fromEpochMillis(System.currentTimeMillis()) }
    var year by remember { mutableIntStateOf(today.year) }
    var month by remember { mutableIntStateOf(today.month) }
    var expense by remember { mutableStateOf(true) }
    var compare by remember { mutableStateOf(true) }
    var pie by remember { mutableStateOf(false) }
    var accountId by remember { mutableStateOf<String?>(null) }
    var forward by remember { mutableStateOf(true) }
    var picking by remember { mutableStateOf(false) }
    val toman = state.settings.displayToman
    val ledgers = state.accounts.filter { it.type != AccountType.PERSON && !it.archived }
    val focus = if (expense) ReportMath.monthTotals(state.txns, year, month, accountId).expense
        else ReportMath.monthTotals(state.txns, year, month, accountId).income
    val totals = ReportMath.monthTotals(state.txns, year, month, accountId)
    val prev = JalaliCalendar.shiftMonth(year, month, -1)
    val prevTotals = ReportMath.monthTotals(state.txns, prev.first, prev.second, accountId)
    val currentSide = if (expense) totals.expense else totals.income
    val prevSide = if (expense) prevTotals.expense else prevTotals.income
    val delta = ReportMath.monthDelta(currentSide, prevSide)
    val bars = ReportMath.comparisonBars(state.txns, year, month, expense, accountId)
    val daily = ReportMath.dailyAmounts(state.txns, year, month, expense, accountId)
    val shares = ReportMath.categoryShares(state.txns, year, month, expense, accountId)
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 96.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(PoolakiIndigo)
                .padding(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderIconButton(SymbolIcons.Category, {})
                LavenderPill(stringResource(R.string.filter_action), { picking = true })
                LavenderPill(stringResource(R.string.export_report), {
                    val text = ReportMath.exportText(year, month, expense, totals, shares) { id ->
                        state.categories.firstOrNull { it.id == id }?.name ?: id
                    }
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    ctx.startActivity(Intent.createChooser(send, ctx.getString(R.string.export_report)))
                })
            }
        }
        Column(Modifier.padding(horizontal = 16.dp).padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val selected = ledgers.firstOrNull { it.id == accountId }
            if (selected != null) {
                FinanceCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.clear_filter), color = PoolakiNegative, modifier = Modifier.clickable { accountId = null })
                        Text(selected.name, modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
                    }
                }
            }
            FinanceCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    DayArrow("‹") {
                        forward = false
                        val shifted = JalaliCalendar.shiftMonth(year, month, -1)
                        year = shifted.first
                        month = shifted.second
                    }
                    Text(
                        "${JalaliLabels.monthName(month)} ${PersianDigits.toPersian(year.toString())} ▾",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(PoolakiChip)
                            .clickable { /* month arrows are the selector */ }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    DayArrow("›") {
                        forward = true
                        val shifted = JalaliCalendar.shiftMonth(year, month, 1)
                        year = shifted.first
                        month = shifted.second
                    }
                }
                PeachToggle(
                    leftSelected = expense,
                    left = stringResource(R.string.outflow),
                    right = stringResource(R.string.inflow),
                    onLeft = { expense = true },
                    onRight = { expense = false },
                )
                val span = (totals.income + totals.expense).coerceAtLeast(1L)
                ShareTrack((currentSide.toFloat() / span.toFloat()))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatSignedAmount(totals.net, toman),
                        color = if (totals.net < 0) PoolakiNegative else Color(0xFF222222),
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        stringResource(R.string.net_diff),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                        color = PoolakiMuted,
                    )
                }
                LavenderPill(
                    stringResource(R.string.txn_count, PersianDigits.toPersian(totals.count.toString())) + " ‹",
                    { onFiltered(null, accountId, year, month) },
                )
                SlideSwap(year to month, forward) { _ ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "${if (expense) stringResource(R.string.outflow) else stringResource(R.string.inflow)} ${JalaliLabels.monthName(month)} ${PersianDigits.toPersian(year.toString())}",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = PoolakiMuted,
                        )
                        Text(
                            formatSignedAmount(focus, toman),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        val arrow = if (delta > 0) "↗" else if (delta < 0) "↘" else "→"
                        Text(
                            "$arrow ${stringResource(R.string.vs_prev)} ${formatSignedAmount(kotlin.math.abs(delta), toman)}",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = PoolakiMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                PeachToggle(
                    leftSelected = !compare,
                    left = stringResource(R.string.daily),
                    right = stringResource(R.string.compare),
                    onLeft = { compare = false },
                    onRight = { compare = true },
                )
                if (compare) MonthBars(bars) else DailyBars(daily)
            }
            FinanceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LavenderPill(if (pie) stringResource(R.string.list_view) else stringResource(R.string.pie_view), { pie = !pie })
                    Text(
                        if (expense) stringResource(R.string.where_spent) else stringResource(R.string.where_earned),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (shares.isEmpty()) {
                    Text(stringResource(R.string.empty_reports), color = PoolakiMuted)
                } else if (pie) {
                    shares.forEach { share ->
                        val cat = state.categories.firstOrNull { it.id == share.categoryId }
                        Text("${cat?.name ?: stringResource(R.string.category)}  ${PersianDigits.toPersian(share.percent.toString())}٪")
                        ShareTrack(share.percent / 100f)
                    }
                } else {
                    shares.forEach { share ->
                        val cat = state.categories.firstOrNull { it.id == share.categoryId }
                        Row(
                            Modifier.fillMaxWidth().clickable { onFiltered(share.categoryId, accountId, year, month) }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("‹", color = PoolakiMuted)
                            Text(formatSignedAmount(share.amount, toman), modifier = Modifier.padding(horizontal = 8.dp))
                            Text(
                                PersianDigits.toPersian(share.percent.toString()) + "٪",
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(PoolakiChip)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                color = PoolakiIndigo,
                            )
                            Text(
                                cat?.name ?: stringResource(R.string.category),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            IconBadge(SymbolIcons.byKey(cat?.iconKey ?: "category"), cat?.let { argbColor(it.color) } ?: PoolakiIndigo)
                        }
                        ShareTrack(share.percent / 100f)
                    }
                }
            }
        }
    }
    if (picking) {
        AlertDialog(
            onDismissRequest = { picking = false },
            confirmButton = { TextButton(onClick = { picking = false }) { Text(stringResource(R.string.cancel)) } },
            title = { Text(stringResource(R.string.filter_action)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { accountId = null; picking = false }) { Text(stringResource(R.string.all)) }
                    ledgers.forEach { acc ->
                        TextButton(onClick = { accountId = acc.id; picking = false }) { Text(acc.name) }
                    }
                }
            },
        )
    }
}

@Composable
private fun DailyBars(days: List<Pair<Int, Long>>) {
    if (days.isEmpty()) {
        Text(stringResource(R.string.empty_reports), color = PoolakiMuted)
        return
    }
    val max = days.maxOf { it.second }.coerceAtLeast(1L)
    Row(
        Modifier.fillMaxWidth().height(120.dp).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEach { (day, amount) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .width(18.dp)
                        .height((72f * amount / max).dp.coerceAtLeast(4.dp))
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(PoolakiPeachSafe),
                )
                Text(PersianDigits.toPersian(day.toString()), style = MaterialTheme.typography.labelSmall, color = PoolakiMuted)
            }
        }
    }
}

private val PoolakiPeachSafe = Color(0xFFFBE3C4)

@Composable
fun GroupedTxnList(
    state: AppUiState,
    initialYear: Int?,
    initialMonth: Int?,
    initialAccountId: String?,
    initialCategoryId: String?,
    onEdit: (LedgerTransaction) -> Unit,
    onDelete: (LedgerTransaction) -> Unit,
) {
    val today = remember { JalaliConverter.fromEpochMillis(System.currentTimeMillis()) }
    var year by remember { mutableIntStateOf(initialYear ?: today.year) }
    var month by remember { mutableIntStateOf(initialMonth ?: today.month) }
    var accountId by remember { mutableStateOf(initialAccountId) }
    var categoryId by remember { mutableStateOf(initialCategoryId) }
    var query by remember { mutableStateOf("") }
    var forward by remember { mutableStateOf(true) }
    var editing by remember { mutableStateOf<LedgerTransaction?>(null) }
    var pending by remember { mutableStateOf<LedgerTransaction?>(null) }
    val named = state.txns.filter { txn ->
        val needle = PersianDigits.toAscii(query).trim()
        if (needle.isEmpty()) return@filter true
        val cat = state.categories.firstOrNull { it.id == txn.categoryId }?.name.orEmpty()
        val acc = state.accounts.firstOrNull { it.id == txn.accountId }?.name.orEmpty()
        PersianDigits.toAscii(txn.note + cat + acc).contains(needle, ignoreCase = true)
    }
    val groups = ReportMath.dayGroups(named, year, month, accountId, categoryId)
    val account = state.accounts.firstOrNull { it.id == accountId }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.all_txns), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            FinanceTextField(query, { query = it }, label = stringResource(R.string.search))
            if (account != null || categoryId != null) {
                FinanceCard {
                    Row {
                        Text(stringResource(R.string.clear_filter), color = PoolakiNegative, modifier = Modifier.clickable { accountId = null; categoryId = null })
                        Text(
                            account?.name ?: state.categories.firstOrNull { it.id == categoryId }?.name.orEmpty(),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                DayArrow("‹") { forward = false; val s = JalaliCalendar.shiftMonth(year, month, -1); year = s.first; month = s.second }
                Text("${JalaliLabels.monthName(month)} ${PersianDigits.toPersian(year.toString())} ▾", fontWeight = FontWeight.Bold)
                DayArrow("›") { forward = true; val s = JalaliCalendar.shiftMonth(year, month, 1); year = s.first; month = s.second }
            }
        }
        if (groups.isEmpty()) {
            Box(Modifier.padding(16.dp)) {
                FinanceEmptyState(SymbolIcons.Receipt, stringResource(R.string.empty_txns_title), stringResource(R.string.empty_txns))
            }
        } else {
            SlideSwap(year to month, forward, Modifier.weight(1f)) { _ ->
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    groups.forEach { group ->
                        item(key = "h-${group.day}-${group.month}-${group.year}") {
                            Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    formatSignedAmount(group.totalSigned, state.settings.displayToman),
                                    fontWeight = FontWeight.Bold,
                                    color = if (group.totalSigned < 0) PoolakiNegative else Color(0xFF222222),
                                )
                                Text(group.header(), modifier = Modifier.weight(1f), textAlign = TextAlign.End, color = PoolakiMuted)
                            }
                        }
                        items(group.transactions, key = { it.id }) { txn ->
                            CompactTxn(
                                txn,
                                state,
                                onClick = { if (txn.transferId == null) editing = txn },
                                onLongClick = { pending = txn },
                            )
                        }
                    }
                }
            }
        }
    }
    editing?.let { txn ->
        EditTxnDialog(txn, state.settings.displayToman, { editing = null }) {
            onEdit(it)
            editing = null
        }
    }
    pending?.let { txn ->
        AlertDialog(
            onDismissRequest = { pending = null },
            confirmButton = { TextButton(onClick = { onDelete(txn); pending = null }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { pending = null }) { Text(stringResource(R.string.cancel)) } },
            text = { Text(stringResource(R.string.confirm_delete)) },
        )
    }
}

@Composable
fun PlannerHub(
    state: AppUiState,
    notes: List<Note>,
    onOpenTxns: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenPeople: () -> Unit,
    onOpenLedgers: () -> Unit,
    onOpenCheques: () -> Unit,
    onOpenInstallments: () -> Unit,
    onSettings: () -> Unit,
    onSearch: () -> Unit,
    onAddNote: (JalaliYmd) -> Unit,
    onAddCheque: (JalaliYmd) -> Unit,
    onAddInstallment: (JalaliYmd) -> Unit,
    cheques: List<Cheque>,
    installments: List<InstallmentPlan>,
) {
    val today = remember { JalaliConverter.fromEpochMillis(System.currentTimeMillis()) }
    var day by remember { mutableStateOf(today) }
    val ledgerCount = state.accounts.count { it.type != AccountType.PERSON && !it.archived }
    val txnCount = state.txns.count { ReportMath.countsAsFlow(it) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(PoolakiIndigo)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HeaderIconButton(SymbolIcons.Category, onSearch)
                HeaderIconButton(SymbolIcons.Settings, onSettings)
                Text(
                    stringResource(R.string.tab_planner),
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Icon(SymbolIcons.byKey("menu_book"), null, tint = Color.White)
            }
            PlannerGrid(
                txnCount, notes.size, ledgerCount,
                onOpenTxns, onOpenNotes, onOpenPeople, onOpenLedgers, onOpenCheques, onOpenInstallments,
            )
        }
        Column(Modifier.padding(16.dp)) {
            FinanceCard {
                Text(dayTitle(day, today), fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayArrow("‹") { day = JalaliCalendar.shiftDay(day, -1) }
                    DayArrow("›") { day = JalaliCalendar.shiftDay(day, 1) }
                    LavenderPill(stringResource(R.string.quick_note), { onAddNote(day) })
                    LavenderPill(stringResource(R.string.quick_cheque), { onAddCheque(day) })
                    LavenderPill(stringResource(R.string.quick_installment), { onAddInstallment(day) })
                }
                val agenda = DueAgenda.forDay(day, today, notes, cheques, installments)
                if (agenda.isEmpty()) Text(stringResource(R.string.no_due), color = PoolakiMuted)
                agenda.forEach { Text(it.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}

@Composable
private fun PlannerGrid(
    txnCount: Int,
    noteCount: Int,
    ledgerCount: Int,
    onTxns: () -> Unit,
    onNotes: () -> Unit,
    onPeople: () -> Unit,
    onLedgers: () -> Unit,
    onCheques: () -> Unit,
    onInstallments: () -> Unit,
) {
    val tiles = listOf(
        Triple(stringResource(R.string.planner_txns), PersianDigits.toPersian(txnCount.toString()), onTxns),
        Triple(stringResource(R.string.quick_note), if (noteCount == 0) "" else PersianDigits.toPersian(noteCount.toString()), onNotes),
        Triple(stringResource(R.string.receivables), "", onPeople),
        Triple(stringResource(R.string.ledgers), PersianDigits.toPersian(ledgerCount.toString()), onLedgers),
        Triple(stringResource(R.string.quick_cheque), "", onCheques),
        Triple(stringResource(R.string.installments), "", onInstallments),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tiles.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (title, badge, click) ->
                    Column(
                        Modifier
                            .weight(1f)
                            .height(92.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                            .clickable(onClick = click)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(badge, color = PoolakiMuted, style = MaterialTheme.typography.labelMedium)
                        Text(title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetScreen(state: AppUiState, budgets: List<MonthBudget>, onSave: (Int, Int, Long) -> Unit) {
    val today = remember { JalaliConverter.fromEpochMillis(System.currentTimeMillis()) }
    var year by remember { mutableIntStateOf(today.year) }
    var month by remember { mutableIntStateOf(today.month) }
    val current = budgets.firstOrNull { it.year == year && it.month == month }
    var raw by remember(year, month, current?.limitRials) {
        mutableStateOf(current?.let { PersianDigits.toPersian((it.limitRials / if (state.settings.displayToman) 10 else 1).toString()) }.orEmpty())
    }
    val spent = ReportMath.monthTotals(state.txns, year, month).expense
    val limit = current?.limitRials ?: 0L
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.tab_budget), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            DayArrow("‹") { val s = JalaliCalendar.shiftMonth(year, month, -1); year = s.first; month = s.second }
            Text("${JalaliLabels.monthName(month)} ${PersianDigits.toPersian(year.toString())}", fontWeight = FontWeight.Bold)
            DayArrow("›") { val s = JalaliCalendar.shiftMonth(year, month, 1); year = s.first; month = s.second }
        }
        FinanceCard {
            Text(stringResource(R.string.spent_label) + " " + formatSignedAmount(spent, state.settings.displayToman), fontWeight = FontWeight.Bold)
            if (limit > 0L) {
                ShareTrack(BudgetMath.progress(limit, spent))
                val left = BudgetMath.remaining(limit, spent)
                Text(
                    if (left < 0) stringResource(R.string.budget_over) else stringResource(R.string.budget_left),
                    color = if (left < 0) PoolakiNegative else PoolakiMuted,
                )
                Text(formatSignedAmount(kotlin.math.abs(left), state.settings.displayToman), fontWeight = FontWeight.Bold)
            } else {
                Text(stringResource(R.string.budget_empty), color = PoolakiMuted)
            }
            FinanceTextField(raw, { raw = it }, label = stringResource(R.string.budget_limit), keyboardType = KeyboardType.Number)
            PrimaryWideButton(stringResource(R.string.save), onClick = {
                val amount = Money.parseDisplayAmount(raw, state.settings.displayToman) ?: return@PrimaryWideButton
                if (PlannerRules.validateBudget(amount) == null) onSave(year, month, amount)
            })
        }
    }
}

@Composable
fun LedgerBoard(
    state: AppUiState,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    onReorder: (List<String>) -> Unit,
) {
    val rows = state.balances.filter { it.account.type != AccountType.PERSON && !it.account.archived }
    val toman = state.settings.displayToman
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.tab_assets), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        FinanceCard {
            Text(formatSignedAmount(rows.sumOf { it.balanceSigned }, toman), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Text(stringResource(R.string.total_balance), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = PoolakiMuted)
        }
        ReorderColumn(rows, { it.account.id }, { ordered -> onReorder(ordered.map { it.account.id }) }) { row, handle ->
            FinanceCard(Modifier.clickable { onOpen(row.account.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⋮⋮", modifier = handle.padding(6.dp), color = PoolakiMuted)
                    Text(
                        formatSignedAmount(row.balanceSigned, toman),
                        color = if (row.balanceSigned < 0) PoolakiNegative else Color(0xFF222222),
                        fontWeight = FontWeight.Bold,
                    )
                    Text(row.account.name, modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
                }
            }
        }
        FinanceCard(Modifier.clickable(onClick = onAdd)) {
            Text(stringResource(R.string.add_ledger) + "  +", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CounterpartyBoard(state: AppUiState, onOpen: (String) -> Unit, onAdd: () -> Unit, onReorder: (List<String>) -> Unit) {
    val people = state.people.sortedBy { it.sortOrder }
    val toman = state.settings.displayToman
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.counterparties), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        val total = people.sumOf { person -> state.balances.firstOrNull { it.account.id == person.accountId }?.balanceSigned ?: 0L }
        FinanceCard {
            Text(formatSignedAmount(total, toman), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = if (total < 0) PoolakiNegative else Color(0xFF222222))
            Text(stringResource(R.string.no_balance), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = PoolakiMuted)
        }
        ReorderColumn(people, { it.id }, { ordered -> onReorder(ordered.map { it.id }) }) { person, handle ->
            val bal = state.balances.firstOrNull { it.account.id == person.accountId }?.balanceSigned ?: 0L
            FinanceCard(Modifier.clickable { onOpen(person.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⋮⋮", modifier = handle.padding(6.dp), color = PoolakiMuted)
                    Text(formatSignedAmount(bal, toman), color = if (bal < 0) PoolakiNegative else Color(0xFF222222), fontWeight = FontWeight.Bold)
                    Text(person.displayName, modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    PersonDot(person)
                }
            }
        }
        FinanceCard(Modifier.clickable(onClick = onAdd)) {
            Text(stringResource(R.string.add_counterparty) + "  +", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PersonDot(person: Person) {
    Box(
        Modifier.padding(start = 8.dp).size(36.dp).clip(CircleShape).background(argbColor(person.avatarColor)),
        contentAlignment = Alignment.Center,
    ) {
        Text(person.initials, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun NotesScreen(notes: List<Note>, onAdd: (JalaliYmd) -> Unit, onDelete: (String) -> Unit) {
    val today = remember { JalaliConverter.fromEpochMillis(System.currentTimeMillis()) }
    PlainList(
        title = stringResource(R.string.notes_title),
        empty = stringResource(R.string.notes_empty),
        onAdd = { onAdd(today) },
    ) {
        notes.forEach { note ->
            FinanceCard {
                Text(note.body, fontWeight = FontWeight.SemiBold)
                Text(dayTitle(note.ymd, today), color = PoolakiMuted, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { onDelete(note.id) }) { Text(stringResource(R.string.delete)) }
            }
        }
    }
}

@Composable
fun ChequesScreen(cheques: List<Cheque>, toman: Boolean, onAdd: () -> Unit, onSettle: (Cheque) -> Unit, onDelete: (String) -> Unit) {
    var paid by remember { mutableStateOf(true) }
    val side = if (paid) FlowSide.PAID else FlowSide.RECEIVED
    val rows = cheques.filter { it.side == side }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.cheques), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        PeachToggle(
            leftSelected = paid,
            left = stringResource(R.string.paid_side) + " (${PersianDigits.toPersian(cheques.count { it.side == FlowSide.PAID }.toString())})",
            right = stringResource(R.string.received_side) + " (${PersianDigits.toPersian(cheques.count { it.side == FlowSide.RECEIVED }.toString())})",
            onLeft = { paid = true },
            onRight = { paid = false },
        )
        if (rows.isEmpty()) {
            FinanceEmptyState(SymbolIcons.Receipt, stringResource(R.string.cheques), stringResource(R.string.cheque_empty))
        } else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { cheque ->
                    FinanceCard {
                        Text(cheque.counterparty, fontWeight = FontWeight.Bold)
                        Text(formatSignedAmount(cheque.amount, toman), color = PoolakiNegative, fontWeight = FontWeight.Bold)
                        Text(dayTitle(cheque.ymd, JalaliConverter.fromEpochMillis(System.currentTimeMillis())), color = PoolakiMuted)
                        if (cheque.status == ChequeStatus.OPEN) {
                            TextButton(onClick = { onSettle(cheque) }) { Text(stringResource(R.string.mark_settled)) }
                        }
                        TextButton(onClick = { onDelete(cheque.id) }) { Text(stringResource(R.string.delete)) }
                    }
                }
            }
        }
        if (rows.isEmpty()) Box(Modifier.weight(1f))
    }
}

@Composable
fun InstallmentsScreen(
    plans: List<InstallmentPlan>,
    toman: Boolean,
    onAdd: () -> Unit,
    onPay: (InstallmentPlan) -> Unit,
    onDelete: (String) -> Unit,
) {
    var paid by remember { mutableStateOf(true) }
    val side = if (paid) FlowSide.PAID else FlowSide.RECEIVED
    val rows = plans.filter { it.side == side }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.installments), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        PeachToggle(paid, stringResource(R.string.paid_side), stringResource(R.string.received_side), { paid = true }, { paid = false })
        if (rows.isEmpty()) {
            FinanceEmptyState(SymbolIcons.Card, stringResource(R.string.installments), stringResource(R.string.installment_empty))
        } else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { plan ->
                    FinanceCard {
                        Text(plan.title, fontWeight = FontWeight.Bold)
                        Text(plan.counterparty, color = PoolakiMuted)
                        Text(
                            "${PersianDigits.toPersian(plan.paidCount.toString())}/${PersianDigits.toPersian(plan.count.toString())} · ${formatSignedAmount(plan.installmentAmount, toman)}",
                        )
                        if (plan.open) TextButton(onClick = { onPay(plan) }) { Text(stringResource(R.string.pay_installment)) }
                        TextButton(onClick = { onDelete(plan.id) }) { Text(stringResource(R.string.delete)) }
                    }
                }
            }
        }
        if (rows.isEmpty()) Box(Modifier.weight(1f))
    }
}

@Composable
private fun PlainList(title: String, empty: String, onAdd: () -> Unit, body: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("+", modifier = Modifier.clickable(onClick = onAdd).padding(8.dp), color = PoolakiIndigo, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
            Text(title, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        }
        body()
    }
}

@Composable
fun SimpleEntryForm(
    title: String,
    error: String?,
    saveLabel: String,
    onSave: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        content()
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
        PrimaryWideButton(saveLabel, onSave)
    }
}

@Composable
fun NoteForm(initial: JalaliYmd, onSave: (String, JalaliYmd) -> Unit) {
    var body by remember { mutableStateOf("") }
    var day by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }
    SimpleEntryForm(stringResource(R.string.quick_note), error, stringResource(R.string.save_note), {
        val problem = PlannerRules.validateNote(body)
        if (problem != null) error = problem else onSave(body, day)
    }) {
        Text(dayTitle(day, initial), color = PoolakiMuted)
        Row {
            DayArrow("‹") { day = JalaliCalendar.shiftDay(day, -1) }
            DayArrow("›") { day = JalaliCalendar.shiftDay(day, 1) }
        }
        FinanceTextField(body, { body = it; error = null }, label = stringResource(R.string.note), singleLine = false)
    }
}

@Composable
fun ChequeForm(initial: JalaliYmd, toman: Boolean, onSave: (Cheque) -> Unit) {
    MoneyPlanForm(
        title = stringResource(R.string.quick_cheque),
        save = stringResource(R.string.save_cheque),
        initial = initial,
        toman = toman,
        withCount = false,
    ) { side, amount, who, ymd, _ , _ ->
        val problem = PlannerRules.validateCheque(amount, who)
        if (problem != null) problem else {
            onSave(
                Cheque(
                    id = java.util.UUID.randomUUID().toString(),
                    side = side,
                    amount = amount,
                    counterparty = who.trim(),
                    personId = null,
                    year = ymd.year,
                    month = ymd.month,
                    day = ymd.day,
                    dueAt = JalaliConverter.toEpochMillisStartOfDay(ymd),
                    status = ChequeStatus.OPEN,
                    note = "",
                    createdAt = System.currentTimeMillis(),
                ),
            )
            null
        }
    }
}

@Composable
fun InstallmentForm(initial: JalaliYmd, toman: Boolean, onSave: (InstallmentPlan) -> Unit) {
    MoneyPlanForm(
        title = stringResource(R.string.installments),
        save = stringResource(R.string.save_installment),
        initial = initial,
        toman = toman,
        withCount = true,
    ) { side, each, who, ymd, title, count ->
        val total = each * count
        val problem = PlannerRules.validateInstallment(title, total, each, count, who)
        if (problem != null) problem else {
            onSave(
                InstallmentPlan(
                    id = java.util.UUID.randomUUID().toString(),
                    side = side,
                    title = title.trim(),
                    totalAmount = total,
                    installmentAmount = each,
                    count = count,
                    paidCount = 0,
                    counterparty = who.trim(),
                    personId = null,
                    year = ymd.year,
                    month = ymd.month,
                    day = ymd.day,
                    nextDueAt = JalaliConverter.toEpochMillisStartOfDay(ymd),
                    note = "",
                    createdAt = System.currentTimeMillis(),
                ),
            )
            null
        }
    }
}

@Composable
private fun MoneyPlanForm(
    title: String,
    save: String,
    initial: JalaliYmd,
    toman: Boolean,
    withCount: Boolean,
    onSave: (FlowSide, Long, String, JalaliYmd, String, Int) -> String?,
) {
    var sidePaid by remember { mutableStateOf(true) }
    var amount by remember { mutableStateOf("") }
    var who by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var countText by remember { mutableStateOf(PersianDigits.toPersian("1")) }
    var day by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }
    SimpleEntryForm(title, error, save, {
        val parsed = Money.parseDisplayAmount(amount, toman)
        val count = PersianDigits.toAscii(countText).toIntOrNull() ?: 1
        if (parsed == null) {
            error = PlannerRules.ERR_AMOUNT
        } else {
            error = onSave(if (sidePaid) FlowSide.PAID else FlowSide.RECEIVED, parsed, who, day, label, count)
        }
    }) {
        PeachToggle(sidePaid, stringResource(R.string.paid_side), stringResource(R.string.received_side), { sidePaid = true }, { sidePaid = false })
        if (withCount) FinanceTextField(label, { label = it }, label = stringResource(R.string.title_field))
        FinanceTextField(amount, { amount = it }, label = stringResource(R.string.amount), keyboardType = KeyboardType.Number)
        if (withCount) FinanceTextField(countText, { countText = it }, label = stringResource(R.string.installment_count), keyboardType = KeyboardType.Number)
        FinanceTextField(who, { who = it }, label = stringResource(R.string.counterparty))
        Text(dayTitle(day, initial), color = PoolakiMuted)
        Row { DayArrow("‹") { day = JalaliCalendar.shiftDay(day, -1) }; DayArrow("›") { day = JalaliCalendar.shiftDay(day, 1) } }
    }
}

@Composable
fun SubScreenScaffold(title: String, onBack: () -> Unit, onAction: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onAction != null) {
                TextButton(onClick = onAction) { Text("+", style = MaterialTheme.typography.headlineSmall, color = PoolakiIndigo) }
            } else {
                Box(Modifier.width(48.dp))
            }
            Text(title, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            TextButton(onClick = onBack) { Icon(SymbolIcons.Back, stringResource(R.string.close)) }
        }
        Box(Modifier.weight(1f)) { content() }
    }
}
