package ir.mhajisoft.hesabres.ui

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
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ir.mhajisoft.hesabres.R
import ir.mhajisoft.hesabres.domain.crash.SheetBounds
import ir.mhajisoft.hesabres.domain.jalali.JalaliConverter
import ir.mhajisoft.hesabres.domain.ledger.ComposerRules
import ir.mhajisoft.hesabres.domain.ledger.SystemCategories
import ir.mhajisoft.hesabres.domain.model.CategoryKind
import ir.mhajisoft.hesabres.ui.components.ChoiceChip
import ir.mhajisoft.hesabres.ui.components.FinanceTextField
import ir.mhajisoft.hesabres.ui.components.JalaliYmdEditor
import ir.mhajisoft.hesabres.ui.components.PrimaryWideButton
import ir.mhajisoft.hesabres.ui.components.SectionLabel
import ir.mhajisoft.hesabres.ui.components.formatJalali
import ir.mhajisoft.hesabres.ui.theme.HesabresTheme
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
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
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
    var fee by remember { mutableStateOf("") }
    var at by remember { mutableStateOf(System.currentTimeMillis()) }
    var pick by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxSheetHeight = SheetBounds.maxHeightDp(LocalConfiguration.current.screenHeightDp).dp
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.extraLarge,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline)
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .height(maxSheetHeight)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
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
                style = MaterialTheme.typography.headlineSmall,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChoiceChip(mode == 0, { mode = 0 }, stringResource(R.string.expense))
                ChoiceChip(mode == 1, { mode = 1 }, stringResource(R.string.income))
                ChoiceChip(mode == 2, { mode = 2 }, stringResource(R.string.transfer))
            }
            FinanceTextField(
                value = amount,
                onValueChange = { amount = it },
                label = stringResource(R.string.amount),
                keyboardType = KeyboardType.Number,
                textStyle = MaterialTheme.typography.headlineSmall,
            )
            ChoiceChip(pick, { pick = !pick }, formatJalali(at), SymbolIcons.Receipt)
            if (pick) {
                JalaliYmdEditor(JalaliConverter.fromEpochMillis(at)) { ymd ->
                    at = JalaliConverter.toEpochMillisStartOfDay(ymd) + (at % 86_400_000L)
                }
            }
            if (mode < 2) {
                SectionLabel(stringResource(R.string.account))
                if (spendAccounts.isEmpty()) {
                    Text(
                        stringResource(R.string.composer_need_account),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        spendAccounts.forEach { acc ->
                            ChoiceChip(accountId == acc.id, { accountId = acc.id }, acc.name)
                        }
                    }
                }
                SectionLabel(stringResource(R.string.category))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    cats.forEach { c ->
                        ChoiceChip(catId == c.id, { catId = c.id }, c.name, SymbolIcons.byKey(c.iconKey))
                    }
                }
                FinanceTextField(note, { note = it }, label = stringResource(R.string.note))
                PrimaryWideButton(
                    stringResource(R.string.save),
                    enabled = spendAccounts.isNotEmpty() && cats.isNotEmpty(),
                    onClick = {
                    val err = ComposerRules.validate(
                        ComposerRules.Draft(
                            accountId = ComposerRules.resolveSpendAccountId(accountId, ledgerAccounts).orEmpty(),
                            categoryId = catId,
                            amountDisplay = amount,
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
                            amount,
                            note,
                            at,
                            mode == 1,
                        )
                    }
                })
            } else {
                SectionLabel(stringResource(R.string.from_account))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ledgerAccounts.forEach { acc ->
                        ChoiceChip(accountId == acc.id, { accountId = acc.id }, acc.name)
                    }
                }
                SectionLabel(stringResource(R.string.to_account))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ledgerAccounts.forEach { acc ->
                        ChoiceChip(toId == acc.id, { toId = acc.id }, acc.name)
                    }
                }
                FinanceTextField(fee, { fee = it }, label = stringResource(R.string.fee_optional), keyboardType = KeyboardType.Number)
                FinanceTextField(note, { note = it }, label = stringResource(R.string.note))
                PrimaryWideButton(
                    stringResource(R.string.save),
                    enabled = ledgerAccounts.size >= 2,
                    onClick = {
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
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Preview(showBackground = true, locale = "fa", name = "Add sheet", widthDp = 390, heightDp = 760)
@Preview(showBackground = true, locale = "fa", name = "Add sheet dark", widthDp = 390, heightDp = 760, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewComposerSheetBody() {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    HesabresTheme(darkTheme = dark) {
        androidx.compose.material3.Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.add_expense), style = MaterialTheme.typography.headlineSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ChoiceChip(true, {}, stringResource(R.string.expense))
                    ChoiceChip(false, {}, stringResource(R.string.income))
                    ChoiceChip(false, {}, stringResource(R.string.transfer))
                }
                FinanceTextField(
                    "۱۲۰٬۰۰۰",
                    {},
                    label = stringResource(R.string.amount),
                    keyboardType = KeyboardType.Number,
                    textStyle = MaterialTheme.typography.headlineSmall,
                )
                SectionLabel(stringResource(R.string.account))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip(true, {}, "نقد")
                    ChoiceChip(false, {}, "بانک ملت")
                }
                SectionLabel(stringResource(R.string.category))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip(true, {}, "خوراک", SymbolIcons.byKey("restaurant"))
                    ChoiceChip(false, {}, "حمل‌ونقل", SymbolIcons.byKey("directions_car"))
                }
                FinanceTextField("", {}, label = stringResource(R.string.note))
                PrimaryWideButton(stringResource(R.string.save), onClick = {})
            }
        }
    }
}
