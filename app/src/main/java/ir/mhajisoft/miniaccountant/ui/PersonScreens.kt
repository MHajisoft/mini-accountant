package ir.mhajisoft.miniaccountant.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.mhajisoft.miniaccountant.R
import ir.mhajisoft.miniaccountant.data.repository.AccountBalance
import ir.mhajisoft.miniaccountant.domain.bank.CardMath
import ir.mhajisoft.miniaccountant.domain.bank.IbanMath
import ir.mhajisoft.miniaccountant.domain.model.Account
import ir.mhajisoft.miniaccountant.domain.model.AccountType
import ir.mhajisoft.miniaccountant.domain.model.BankAccount
import ir.mhajisoft.miniaccountant.domain.model.BankCard
import ir.mhajisoft.miniaccountant.domain.model.Direction
import ir.mhajisoft.miniaccountant.domain.model.LedgerTransaction
import ir.mhajisoft.miniaccountant.domain.model.Person
import ir.mhajisoft.miniaccountant.ui.components.FinanceCard
import ir.mhajisoft.miniaccountant.ui.components.FinanceEmptyState
import ir.mhajisoft.miniaccountant.ui.components.FinanceTextField
import ir.mhajisoft.miniaccountant.ui.components.LabeledRow
import ir.mhajisoft.miniaccountant.ui.components.PrimaryWideButton
import ir.mhajisoft.miniaccountant.ui.components.SecondaryWideButton
import ir.mhajisoft.miniaccountant.ui.components.SectionLabel
import ir.mhajisoft.miniaccountant.ui.components.TonalCard
import ir.mhajisoft.miniaccountant.ui.components.formatJalali
import ir.mhajisoft.miniaccountant.ui.components.formatMoney
import ir.mhajisoft.miniaccountant.ui.theme.LocalLedgerTones
import ir.mhajisoft.miniaccountant.ui.theme.MiniAccountantTheme
import ir.mhajisoft.miniaccountant.ui.theme.SymbolIcons

@Composable
fun PeopleScreen(state: AppUiState, vm: AppViewModel, onOpen: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.people), style = MaterialTheme.typography.headlineSmall)
        FinanceCard {
            SectionLabel(stringResource(R.string.add_person))
            FinanceTextField(name, { name = it }, label = stringResource(R.string.name))
            FinanceTextField(phone, { phone = it }, label = stringResource(R.string.phone), keyboardType = KeyboardType.Phone)
            PrimaryWideButton(stringResource(R.string.save), onClick = {
                if (name.isNotBlank()) {
                    vm.addPerson(name, phone.ifBlank { null }, null)
                    name = ""
                    phone = ""
                }
            })
        }
        if (state.people.isEmpty()) {
            FinanceEmptyState(
                icon = SymbolIcons.People,
                title = stringResource(R.string.empty_people),
                body = stringResource(R.string.empty_people_body),
            )
        } else {
            state.people.forEach { p ->
                val bal = state.balances.firstOrNull { it.account.id == p.accountId }?.balanceSigned ?: 0L
                val tones = LocalLedgerTones.current
                val role = if (bal >= 0) stringResource(R.string.debtor) else stringResource(R.string.creditor)
                FinanceCard(Modifier.clickable { onOpen(p.id) }) {
                    Text(p.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        role,
                        color = if (bal >= 0) tones.debtor else tones.creditor,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    if (!p.phone.isNullOrBlank()) {
                        Text(p.phone, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(formatMoney(kotlin.math.abs(bal), state.settings.displayToman), style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

@Composable
fun PersonDetailScreen(state: AppUiState, vm: AppViewModel, personId: String, onBack: () -> Unit) {
    val person = state.people.firstOrNull { it.id == personId }
    val cards by vm.vaultRepo.cardsFlow.collectAsStateWithLifecycle(emptyList())
    val ibans by vm.vaultRepo.bankAccountsFlow.collectAsStateWithLifecycle(emptyList())
    var payError by remember { mutableStateOf<String?>(null) }
    PersonDetailContent(
        state = state,
        person = person,
        cards = cards.filter { it.accountId == person?.accountId },
        ibans = ibans.filter { it.accountId == person?.accountId },
        payError = payError,
        onBack = onBack,
        onSave = { vm.updatePerson(it) },
        onPay = { acc, amt, they ->
            payError = null
            vm.payPerson(acc, amt, they) { payError = it }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailContent(
    state: AppUiState,
    person: Person?,
    cards: List<BankCard>,
    ibans: List<BankAccount>,
    onBack: () -> Unit,
    onSave: (Person) -> Unit,
    onPay: (String, String, Boolean) -> Unit,
    payError: String? = null,
) {
    if (person == null) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FinanceEmptyState(SymbolIcons.People, stringResource(R.string.person_missing), stringResource(R.string.empty_people_body))
            SecondaryWideButton(stringResource(R.string.close), onBack)
        }
        return
    }
    val bal = state.balances.firstOrNull { it.account.id == person.accountId }?.balanceSigned ?: 0L
    val txns = state.txns.filter { it.accountId == person.accountId }.take(12)
    val tones = LocalLedgerTones.current
    var name by remember(person.id) { mutableStateOf(person.name) }
    var phone by remember(person.id) { mutableStateOf(person.phone.orEmpty()) }
    var note by remember(person.id) { mutableStateOf(person.note.orEmpty()) }
    var payAmount by remember { mutableStateOf("") }
    val role = if (bal >= 0) stringResource(R.string.debtor) else stringResource(R.string.creditor)
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.person_profile)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(SymbolIcons.Back, stringResource(R.string.close))
                }
            },
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TonalCard {
                Text(person.name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    role,
                    color = if (bal >= 0) tones.debtor else tones.creditor,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(formatMoney(kotlin.math.abs(bal), state.settings.displayToman), style = MaterialTheme.typography.headlineMedium)
                Text(
                    if (bal >= 0) stringResource(R.string.they_owe_you) else stringResource(R.string.you_owe_them),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            FinanceCard {
                SectionLabel(stringResource(R.string.person_info))
                FinanceTextField(name, { name = it }, label = stringResource(R.string.name))
                FinanceTextField(phone, { phone = it }, label = stringResource(R.string.phone), keyboardType = KeyboardType.Phone)
                FinanceTextField(note, { note = it }, label = stringResource(R.string.note), singleLine = false)
                PrimaryWideButton(stringResource(R.string.save), onClick = {
                    onSave(person.copy(name = name, phone = phone.ifBlank { null }, note = note.ifBlank { null }))
                })
            }
            FinanceCard {
                SectionLabel(stringResource(R.string.settle))
                FinanceTextField(payAmount, { payAmount = it }, label = stringResource(R.string.pay_amount), keyboardType = KeyboardType.Number)
                if (payError != null) {
                    Text(payError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        PrimaryWideButton(stringResource(R.string.pay_them), onClick = {
                            onPay(person.accountId, payAmount, false)
                        })
                    }
                    Box(Modifier.weight(1f)) {
                        SecondaryWideButton(stringResource(R.string.they_pay)) {
                            onPay(person.accountId, payAmount, true)
                        }
                    }
                }
            }
            FinanceCard {
                SectionLabel(stringResource(R.string.linked_cards))
                if (cards.isEmpty()) {
                    Text(stringResource(R.string.empty_linked_cards), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    cards.forEach { c ->
                        LabeledRow(CardMath.maskPan(c.last4.padStart(16, '*')), c.bankCode)
                    }
                }
            }
            FinanceCard {
                SectionLabel(stringResource(R.string.linked_iban))
                if (ibans.isEmpty()) {
                    Text(stringResource(R.string.empty_linked_iban), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    ibans.forEach { a ->
                        LabeledRow(IbanMath.formatGrouped(a.iban), a.bankName)
                    }
                }
            }
            FinanceCard {
                SectionLabel(stringResource(R.string.recent_txns))
                if (txns.isEmpty()) {
                    Text(stringResource(R.string.empty_person_txns), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    txns.forEach { txn ->
                        val cat = state.categories.firstOrNull { it.id == txn.categoryId }
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(cat?.name ?: stringResource(R.string.transfer), style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    formatJalali(txn.occurredAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                (if (txn.direction == Direction.IN) "+" else "−") +
                                    formatMoney(txn.amount, state.settings.displayToman),
                                color = if (txn.direction == Direction.IN) tones.income else tones.expense,
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, locale = "fa", name = "Person detail")
@Composable
fun PreviewPersonDetail() {
    val acc = Account("a1", "علی رضایی", AccountType.PERSON, color = 0xFF6A1B9A, sortOrder = 0, createdAt = 0, updatedAt = 0)
    MiniAccountantTheme {
        PersonDetailContent(
            state = AppUiState(
                ready = true,
                people = listOf(Person("p1", "a1", "علی رضایی", "۰۹۱۲۱۲۳۴۵۶۷", "همکار")),
                accounts = listOf(acc),
                balances = listOf(AccountBalance(acc, 2_500_000)),
                txns = listOf(
                    LedgerTransaction("t", "a1", "sys-food", "p1", 2500000, Direction.IN, "بدهی ناهار", 0, 1405, 6, 15, "fy", null, 0),
                ),
                categories = ir.mhajisoft.miniaccountant.domain.ledger.CategoryCatalog.systemCategories(),
            ),
            person = Person("p1", "a1", "علی رضایی", "۰۹۱۲۱۲۳۴۵۶۷", "همکار"),
            cards = listOf(
                BankCard("c1", "a1", "4331", "610433", "mellat", 12, 1408, null, null, null, false),
            ),
            ibans = listOf(
                BankAccount("b1", "a1", "123", "IR000000000000000000000000", "mellat", "بانک ملت"),
            ),
            onBack = {},
            onSave = {},
            onPay = { _, _, _ -> },
        )
    }
}
