package ir.mhajisoft.miniaccountant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ir.mhajisoft.miniaccountant.R
import ir.mhajisoft.miniaccountant.domain.jalali.JalaliConverter
import ir.mhajisoft.miniaccountant.domain.ledger.ComposerRules
import ir.mhajisoft.miniaccountant.domain.ledger.SystemCategories
import ir.mhajisoft.miniaccountant.domain.model.Account
import ir.mhajisoft.miniaccountant.domain.model.AccountType
import ir.mhajisoft.miniaccountant.domain.model.CategoryKind
import ir.mhajisoft.miniaccountant.ui.components.ChoiceChip
import ir.mhajisoft.miniaccountant.ui.components.FinanceCard
import ir.mhajisoft.miniaccountant.ui.components.FinanceTextField
import ir.mhajisoft.miniaccountant.ui.components.JalaliDatePickerDialog
import ir.mhajisoft.miniaccountant.ui.components.PrimaryWideButton
import ir.mhajisoft.miniaccountant.ui.components.SectionLabel
import ir.mhajisoft.miniaccountant.ui.components.formatJalali
import ir.mhajisoft.miniaccountant.ui.theme.MiniAccountantTheme
import ir.mhajisoft.miniaccountant.ui.theme.SymbolIcons
import kotlinx.coroutines.delay

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
    var mode by remember { mutableIntStateOf(initialMode) }
    val focus = remember { FocusRequester() }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val ledgerAccounts = state.accounts.filter { !it.archived }
    val spendAccounts = ledgerAccounts.filter { it.type != AccountType.PERSON }
    val lastAcc = ComposerRules.resolveLedgerAccountId(
        state.settings.lastAccountId ?: state.settings.defaultAccountId.orEmpty(),
        ledgerAccounts,
    )
    var accountId by remember { mutableStateOf(lastAcc.orEmpty()) }
    val cats = state.categories.filter {
        when (mode) {
            1 -> it.kind == CategoryKind.INCOME
            else -> it.kind == CategoryKind.EXPENSE && it.id != SystemCategories.FEE_ID
        }
    }
    val lastCat = if (mode == 1) state.settings.lastIncomeCategoryId else state.settings.lastExpenseCategoryId
    var catId by remember(mode, cats) {
        mutableStateOf(
            lastCat?.takeIf { id -> cats.any { it.id == id } } ?: cats.firstOrNull()?.id.orEmpty(),
        )
    }
    var toId by remember {
        mutableStateOf(ledgerAccounts.firstOrNull { it.id != accountId }?.id.orEmpty())
    }
    var fee by remember { mutableStateOf("") }
    var at by remember { mutableStateOf(System.currentTimeMillis()) }
    var pick by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 3.dp,
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(
                    when (mode) {
                        1 -> R.string.income
                        2 -> R.string.transfer
                        else -> R.string.add_expense
                    },
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            FinanceCard {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val labels = listOf(
                        stringResource(R.string.expense),
                        stringResource(R.string.income),
                        stringResource(R.string.transfer),
                    )
                    labels.forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = mode == index,
                            onClick = { mode = index },
                            shape = SegmentedButtonDefaults.itemShape(index, labels.size),
                            modifier = Modifier.weight(1f),
                            label = { Text(label) },
                            icon = {},
                        )
                    }
                }
            }
            FinanceTextField(
                value = amount,
                onValueChange = { amount = it },
                modifier = Modifier.focusRequester(focus),
                label = stringResource(R.string.amount),
                keyboardType = KeyboardType.Number,
            )
            ChoiceChip(false, { pick = true }, formatJalali(at), SymbolIcons.Receipt)
            if (mode < 2) {
                SectionLabel(stringResource(R.string.account))
                if (spendAccounts.isEmpty()) {
                    Text(
                        stringResource(R.string.composer_need_account),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        spendAccounts.forEach { acc ->
                            ChoiceChip(accountId == acc.id, { accountId = acc.id }, acc.name)
                        }
                    }
                }
                SectionLabel(stringResource(R.string.category))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cats.forEach { c ->
                        ChoiceChip(catId == c.id, { catId = c.id }, c.name, SymbolIcons.byKey(c.iconKey))
                    }
                }
                FinanceTextField(note, { note = it }, label = stringResource(R.string.note))
                PrimaryWideButton(stringResource(R.string.save), onClick = {
                    val err = ComposerRules.validate(
                        ComposerRules.Draft(
                            accountId = accountId,
                            categoryId = catId,
                            amountDisplay = amount,
                            toman = state.settings.displayToman,
                            accounts = ledgerAccounts,
                            categories = cats,
                            fiscalYear = state.fy,
                        ),
                    )
                    if (err != null) {
                        onError(err)
                    } else {
                        onSaveExpense(
                            ComposerRules.resolveLedgerAccountId(accountId, ledgerAccounts).orEmpty(),
                            catId,
                            amount,
                            note,
                            at,
                            mode == 1,
                        )
                    }
                })
            } else {
                SectionLabel(stringResource(R.string.from_account))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ledgerAccounts.forEach { acc ->
                        ChoiceChip(accountId == acc.id, { accountId = acc.id }, acc.name)
                    }
                }
                SectionLabel(stringResource(R.string.to_account))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ledgerAccounts.forEach { acc ->
                        ChoiceChip(toId == acc.id, { toId = acc.id }, acc.name)
                    }
                }
                FinanceTextField(fee, { fee = it }, label = stringResource(R.string.fee_optional), keyboardType = KeyboardType.Number)
                FinanceTextField(note, { note = it }, label = stringResource(R.string.note))
                PrimaryWideButton(stringResource(R.string.save), onClick = {
                    val err = ComposerRules.validate(
                        ComposerRules.Draft(
                            accountId = accountId,
                            categoryId = "",
                            amountDisplay = amount,
                            toman = state.settings.displayToman,
                            accounts = ledgerAccounts,
                            categories = state.categories,
                            fiscalYear = state.fy,
                            toAccountId = toId,
                            transfer = true,
                        ),
                    )
                    if (err != null) onError(err) else onSaveTransfer(accountId, toId, amount, fee, note, at)
                })
            }
            Spacer(Modifier.height(12.dp))
        }
        LaunchedEffect(Unit) {
            delay(120)
            runCatching { focus.requestFocus() }
        }
    }
    if (pick) {
        JalaliDatePickerDialog(JalaliConverter.fromEpochMillis(at), { pick = false }) {
            at = JalaliConverter.toEpochMillisStartOfDay(it) + (at % 86_400_000L)
            pick = false
        }
    }
}

@Preview(showBackground = true, locale = "fa", name = "Add sheet")
@Composable
fun PreviewComposerSheetBody() {
    MiniAccountantTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("ثبت هزینه", style = MaterialTheme.typography.titleLarge)
            FinanceCard {
                Text("هزینه · درآمد · جابه‌جایی", style = MaterialTheme.typography.labelLarge)
            }
            FinanceTextField("۱۲۰۰۰۰", {}, label = "مبلغ", keyboardType = KeyboardType.Number)
            ChoiceChip(true, {}, "نقد")
            ChoiceChip(false, {}, "خوراک", SymbolIcons.byKey("restaurant"))
            PrimaryWideButton("ذخیره", onClick = {})
        }
    }
}
