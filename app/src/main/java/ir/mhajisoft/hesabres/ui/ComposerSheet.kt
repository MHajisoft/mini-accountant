package ir.mhajisoft.hesabres.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.mhajisoft.hesabres.R
import ir.mhajisoft.hesabres.domain.jalali.JalaliCalendar
import ir.mhajisoft.hesabres.domain.jalali.JalaliConverter
import ir.mhajisoft.hesabres.domain.ledger.ComposerRules
import ir.mhajisoft.hesabres.domain.ledger.SystemCategories
import ir.mhajisoft.hesabres.domain.model.CategoryKind
import ir.mhajisoft.hesabres.domain.money.AmountKeypad
import ir.mhajisoft.hesabres.domain.money.PersianDigits
import ir.mhajisoft.hesabres.ui.components.AmountKeypadPad
import ir.mhajisoft.hesabres.ui.components.ChoiceChip
import ir.mhajisoft.hesabres.ui.components.DayArrow
import ir.mhajisoft.hesabres.ui.components.FinanceTextField
import ir.mhajisoft.hesabres.ui.components.JalaliDateSheet
import ir.mhajisoft.hesabres.ui.components.PeachToggle
import ir.mhajisoft.hesabres.ui.components.PrimaryWideButton
import ir.mhajisoft.hesabres.ui.components.RequiredDot
import ir.mhajisoft.hesabres.ui.components.dayTitle
import ir.mhajisoft.hesabres.ui.theme.PoolakiNegative
import ir.mhajisoft.hesabres.ui.theme.PoolakiPeach
import ir.mhajisoft.hesabres.ui.theme.SymbolIcons

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ComposerSheet(
    state: AppUiState,
    initialMode: Int,
    onDismiss: () -> Unit,
    onError: (String) -> Unit,
    onSaveExpense: (String, String, String, String, Long, Boolean) -> Unit,
    onSaveTransfer: (String, String, String, String, String, Long) -> Unit,
) {
    var mode by remember { mutableIntStateOf(initialMode.coerceIn(0, 2)) }
    var expression by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var fee by remember { mutableStateOf("") }
    val today = remember { JalaliConverter.fromEpochMillis(System.currentTimeMillis()) }
    var ymd by remember { mutableStateOf(today) }
    var pick by remember { mutableStateOf(false) }
    val ledgerAccounts = remember(state.accounts) { ComposerRules.liveAccounts(state.accounts) }
    val spendAccounts = remember(ledgerAccounts) { ComposerRules.spendAccounts(ledgerAccounts) }
    val lastAcc = ComposerRules.resolveSpendAccountId(
        state.settings.lastAccountId ?: state.settings.defaultAccountId.orEmpty(),
        ledgerAccounts,
    )
    var accountId by remember { mutableStateOf(lastAcc.orEmpty()) }
    val catIds = remember(mode, state.categories) {
        state.categories.filter {
            when (mode) {
                1 -> it.kind == CategoryKind.INCOME
                else -> it.kind == CategoryKind.EXPENSE && it.id != SystemCategories.FEE_ID
            }
        }.map { it.id }
    }
    val cats = state.categories.filter { it.id in catIds }
    val lastCat = if (mode == 1) state.settings.lastIncomeCategoryId else state.settings.lastExpenseCategoryId
    var catId by remember(mode, catIds) {
        mutableStateOf(lastCat?.takeIf { it in catIds } ?: catIds.firstOrNull().orEmpty())
    }
    var toId by remember {
        mutableStateOf(ledgerAccounts.firstOrNull { it.id != accountId }?.id.orEmpty())
    }
    val amountText = AmountKeypad.amountText(expression).orEmpty()
    val shown = AmountKeypad.evaluate(expression)?.let { PersianDigits.formatGrouped(it) }
        ?: PersianDigits.toPersian(expression.ifBlank { "0" })
    val at = JalaliConverter.toEpochMillisStartOfDay(ymd)
    val recent = cats.take(3)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize()) {
                    Text(
                        stringResource(R.string.new_txn),
                        modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(16.dp))
                                .padding(4.dp),
                        ) {
                            listOf(
                                0 to stringResource(R.string.outflow),
                                1 to stringResource(R.string.inflow),
                                2 to stringResource(R.string.transfer),
                            ).forEach { (index, label) ->
                                val selected = mode == index
                                Text(
                                    label,
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (selected) PoolakiPeach else Color.Transparent, RoundedCornerShape(12.dp))
                                        .clickable { mode = index }
                                        .padding(vertical = 10.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) PoolakiNegative else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RequiredDot()
                            Text(
                                shown,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(stringResource(if (state.settings.displayToman) R.string.toman else R.string.rial), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (mode < 2) {
                                spendAccounts.forEach { acc ->
                                    ChoiceChip(accountId == acc.id, { accountId = acc.id }, acc.name, SymbolIcons.Wallet)
                                }
                            } else {
                                Text(stringResource(R.string.from_account), modifier = Modifier.fillMaxWidth())
                                ledgerAccounts.forEach { acc ->
                                    ChoiceChip(accountId == acc.id, { accountId = acc.id }, acc.name)
                                }
                                Text(stringResource(R.string.to_account), modifier = Modifier.fillMaxWidth())
                                ledgerAccounts.forEach { acc ->
                                    ChoiceChip(toId == acc.id, { toId = acc.id }, acc.name)
                                }
                            }
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(16.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DayArrow("‹") { ymd = JalaliCalendar.shiftDay(ymd, -1) }
                            Text(
                                dayTitle(ymd, today),
                                modifier = Modifier.weight(1f).clickable { pick = true }.padding(horizontal = 8.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            DayArrow("›") { ymd = JalaliCalendar.shiftDay(ymd, 1) }
                            Text("📅", modifier = Modifier.padding(end = 6.dp))
                        }
                        FinanceTextField(note, { note = it }, label = stringResource(R.string.description))
                        if (mode == 2) {
                            FinanceTextField(fee, { fee = it }, label = stringResource(R.string.fee_optional))
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                RequiredDot()
                                Text(stringResource(R.string.category), fontWeight = FontWeight.Bold)
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                recent.forEach { c ->
                                    ChoiceChip(catId == c.id, { catId = c.id }, c.name, SymbolIcons.byKey(c.iconKey))
                                }
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                cats.forEach { c ->
                                    ChoiceChip(catId == c.id, { catId = c.id }, c.name, SymbolIcons.byKey(c.iconKey))
                                }
                            }
                        }
                        val enabled = if (mode < 2) {
                            ComposerRules.validate(
                                ComposerRules.Draft(
                                    accountId = ComposerRules.resolveSpendAccountId(accountId, ledgerAccounts).orEmpty(),
                                    categoryId = catId,
                                    amountDisplay = amountText,
                                    toman = state.settings.displayToman,
                                    accounts = ledgerAccounts,
                                    categories = cats,
                                    fiscalYear = state.fy,
                                ),
                            ) == null
                        } else {
                            ComposerRules.validate(
                                ComposerRules.Draft(
                                    accountId = accountId,
                                    categoryId = "",
                                    amountDisplay = amountText,
                                    toman = state.settings.displayToman,
                                    accounts = ledgerAccounts,
                                    categories = state.categories,
                                    fiscalYear = state.fy,
                                    toAccountId = toId,
                                    transfer = true,
                                ),
                            ) == null
                        }
                        PrimaryWideButton(stringResource(R.string.save_txn), enabled = enabled, onClick = {
                            if (mode < 2) {
                                val err = ComposerRules.validate(
                                    ComposerRules.Draft(
                                        accountId = ComposerRules.resolveSpendAccountId(accountId, ledgerAccounts).orEmpty(),
                                        categoryId = catId,
                                        amountDisplay = amountText,
                                        toman = state.settings.displayToman,
                                        accounts = ledgerAccounts,
                                        categories = cats,
                                        fiscalYear = state.fy,
                                    ),
                                )
                                if (err != null) onError(err) else {
                                    onSaveExpense(
                                        ComposerRules.resolveSpendAccountId(accountId, ledgerAccounts).orEmpty(),
                                        catId,
                                        amountText,
                                        note,
                                        at,
                                        mode == 1,
                                    )
                                }
                            } else {
                                val err = ComposerRules.validate(
                                    ComposerRules.Draft(
                                        accountId = accountId,
                                        categoryId = "",
                                        amountDisplay = amountText,
                                        toman = state.settings.displayToman,
                                        accounts = ledgerAccounts,
                                        categories = state.categories,
                                        fiscalYear = state.fy,
                                        toAccountId = toId,
                                        transfer = true,
                                    ),
                                )
                                if (err != null) onError(err) else onSaveTransfer(accountId, toId, amountText, fee, note, at)
                            }
                        })
                    }
                    AmountKeypadPad { token -> expression = AmountKeypad.press(expression, token) }
                }
                if (pick) {
                    JalaliDateSheet(ymd, today, onSelect = { ymd = it; pick = false }, onDismiss = { pick = false })
                }
            }
        }
    }
}
