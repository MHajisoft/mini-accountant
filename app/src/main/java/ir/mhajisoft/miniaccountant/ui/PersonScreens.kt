package ir.mhajisoft.miniaccountant.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import ir.mhajisoft.miniaccountant.ui.components.ColorPackPicker
import ir.mhajisoft.miniaccountant.ui.components.FinanceCard
import ir.mhajisoft.miniaccountant.ui.components.FinanceEmptyState
import ir.mhajisoft.miniaccountant.ui.components.FinanceTextField
import ir.mhajisoft.miniaccountant.ui.components.LabeledRow
import ir.mhajisoft.miniaccountant.ui.components.PersonAvatar
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
fun PeopleScreen(state: AppUiState, onOpen: (String) -> Unit, onAdd: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Text(
                stringResource(R.string.people),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp),
            )
            if (state.people.isEmpty()) {
                Box(Modifier.padding(16.dp)) {
                    FinanceEmptyState(
                        SymbolIcons.People,
                        stringResource(R.string.empty_people),
                        stringResource(R.string.empty_people_body),
                    )
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.people, key = { it.id }) { p ->
                        val bal = state.balances.firstOrNull { it.account.id == p.accountId }?.balanceSigned ?: 0L
                        val tones = LocalLedgerTones.current
                        FinanceCard(Modifier.clickable { onOpen(p.id) }) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                PersonAvatar(p.initials, p.avatarColor)
                                Column(Modifier.weight(1f)) {
                                    Text(p.displayName, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        p.email ?: p.phone ?: stringResource(if (bal >= 0) R.string.debtor else R.string.creditor),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    formatMoney(kotlin.math.abs(bal), state.settings.displayToman),
                                    color = if (bal >= 0) tones.debtor else tones.creditor,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onAdd,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(SymbolIcons.Add, stringResource(R.string.add_person))
        }
    }
}

@Composable
fun PersonEditScreen(state: AppUiState, vm: AppViewModel, personId: String?, onBack: () -> Unit) {
    val existing = state.people.firstOrNull { it.id == personId }
    PersonEditForm(
        existing = existing,
        onBack = onBack,
        onCreate = { first, last, phone, email, ig, tg, wa, note, color ->
            vm.addPerson(first, last, phone, email, ig, tg, wa, note, color)
            onBack()
        },
        onUpdate = {
            vm.updatePerson(it)
            onBack()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonEditForm(
    existing: Person?,
    onBack: () -> Unit,
    onCreate: (String, String, String?, String?, String?, String?, String?, String?, Long) -> Unit,
    onUpdate: (Person) -> Unit,
) {
    var first by remember(existing?.id) { mutableStateOf(existing?.firstName?.ifBlank { existing.name } ?: "") }
    var last by remember(existing?.id) { mutableStateOf(existing?.lastName.orEmpty()) }
    var phone by remember(existing?.id) { mutableStateOf(existing?.phone.orEmpty()) }
    var email by remember(existing?.id) { mutableStateOf(existing?.email.orEmpty()) }
    var ig by remember(existing?.id) { mutableStateOf(existing?.instagram.orEmpty()) }
    var tg by remember(existing?.id) { mutableStateOf(existing?.telegram.orEmpty()) }
    var wa by remember(existing?.id) { mutableStateOf(existing?.whatsapp.orEmpty()) }
    var note by remember(existing?.id) { mutableStateOf(existing?.note.orEmpty()) }
    var color by remember(existing?.id) { mutableLongStateOf(existing?.avatarColor ?: 0xFF0B6E4F) }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(if (existing == null) R.string.add_person else R.string.person_info)) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(SymbolIcons.Back, stringResource(R.string.close)) }
            },
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val preview = Person(
                    id = existing?.id ?: "new",
                    accountId = existing?.accountId.orEmpty(),
                    name = "",
                    phone = null,
                    note = null,
                    firstName = first,
                    lastName = last,
                    avatarColor = color,
                )
                PersonAvatar(preview.initials, color, size = 64)
                Text(stringResource(R.string.avatar), style = MaterialTheme.typography.titleSmall)
            }
            ColorPackPicker(color) { color = it }
            FinanceCard {
                FinanceTextField(first, { first = it }, label = stringResource(R.string.first_name))
                FinanceTextField(last, { last = it }, label = stringResource(R.string.last_name))
                FinanceTextField(phone, { phone = it }, label = stringResource(R.string.phone), keyboardType = KeyboardType.Phone)
                FinanceTextField(email, { email = it }, label = stringResource(R.string.email), keyboardType = KeyboardType.Email)
                FinanceTextField(ig, { ig = it }, label = stringResource(R.string.instagram))
                FinanceTextField(tg, { tg = it }, label = stringResource(R.string.telegram))
                FinanceTextField(wa, { wa = it }, label = stringResource(R.string.whatsapp), keyboardType = KeyboardType.Phone)
                FinanceTextField(note, { note = it }, label = stringResource(R.string.note), singleLine = false)
            }
            PrimaryWideButton(stringResource(R.string.save), onClick = {
                if (first.isBlank() && last.isBlank()) return@PrimaryWideButton
                if (existing == null) {
                    onCreate(
                        first.trim(), last.trim(),
                        phone.ifBlank { null }, email.ifBlank { null },
                        ig.ifBlank { null }, tg.ifBlank { null }, wa.ifBlank { null },
                        note.ifBlank { null }, color,
                    )
                } else {
                    onUpdate(
                        existing.copy(
                            firstName = first.trim(),
                            lastName = last.trim(),
                            phone = phone.ifBlank { null },
                            email = email.ifBlank { null },
                            instagram = ig.ifBlank { null },
                            telegram = tg.ifBlank { null },
                            whatsapp = wa.ifBlank { null },
                            note = note.ifBlank { null },
                            avatarColor = color,
                        ),
                    )
                }
            })
        }
    }
}

@Composable
fun PersonDetailScreen(state: AppUiState, vm: AppViewModel, personId: String, onBack: () -> Unit, onEdit: () -> Unit) {
    val person = state.people.firstOrNull { it.id == personId }
    val cards by vm.vaultRepo.cardsFlow.collectAsStateWithLifecycle(emptyList())
    val ibans by vm.vaultRepo.bankAccountsFlow.collectAsStateWithLifecycle(emptyList())
    var payError by remember { mutableStateOf<String?>(null) }
    PersonDetailContent(
        state = state,
        person = person,
        cards = cards.filter { it.personId == personId || it.accountId == person?.accountId },
        ibans = ibans.filter { it.personId == personId || it.accountId == person?.accountId },
        payError = payError,
        onBack = onBack,
        onEdit = onEdit,
        onPay = { acc, amt, they ->
            payError = null
            vm.payPerson(acc, amt, they) { payError = it }
        },
        logoOf = { bin, code -> vm.vaultRepo.directory.logoOf(bin, code) },
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
    onPay: (String, String, Boolean) -> Unit,
    onEdit: () -> Unit = {},
    payError: String? = null,
    logoOf: (String, String) -> String = { _, _ -> "bank_unknown" },
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
    var payAmount by remember { mutableStateOf("") }
    val role = if (bal >= 0) stringResource(R.string.debtor) else stringResource(R.string.creditor)
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.person_profile)) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(SymbolIcons.Back, stringResource(R.string.close)) }
            },
            actions = {
                IconButton(onClick = onEdit) { Icon(SymbolIcons.Category, stringResource(R.string.edit)) }
            },
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TonalCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PersonAvatar(person.initials, person.avatarColor, size = 64)
                    Column {
                        Text(person.displayName, style = MaterialTheme.typography.headlineSmall)
                        Text(role, color = if (bal >= 0) tones.debtor else tones.creditor, style = MaterialTheme.typography.titleSmall)
                    }
                }
                Text(formatMoney(kotlin.math.abs(bal), state.settings.displayToman), style = MaterialTheme.typography.headlineMedium)
                Text(
                    if (bal >= 0) stringResource(R.string.they_owe_you) else stringResource(R.string.you_owe_them),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            FinanceCard {
                SectionLabel(stringResource(R.string.person_info))
                if (!person.phone.isNullOrBlank()) LabeledRow(stringResource(R.string.phone), person.phone)
                if (!person.email.isNullOrBlank()) LabeledRow(stringResource(R.string.email), person.email)
                if (!person.instagram.isNullOrBlank()) LabeledRow(stringResource(R.string.instagram), person.instagram)
                if (!person.telegram.isNullOrBlank()) LabeledRow(stringResource(R.string.telegram), person.telegram)
                if (!person.whatsapp.isNullOrBlank()) LabeledRow(stringResource(R.string.whatsapp), person.whatsapp)
                if (!person.note.isNullOrBlank()) Text(person.note, style = MaterialTheme.typography.bodyMedium)
            }
            FinanceCard {
                SectionLabel(stringResource(R.string.settle))
                FinanceTextField(payAmount, { payAmount = it }, label = stringResource(R.string.pay_amount), keyboardType = KeyboardType.Number)
                if (payError != null) {
                    Text(payError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        PrimaryWideButton(stringResource(R.string.pay_them), onClick = { onPay(person.accountId, payAmount, false) })
                    }
                    Box(Modifier.weight(1f)) {
                        SecondaryWideButton(stringResource(R.string.they_pay)) { onPay(person.accountId, payAmount, true) }
                    }
                }
            }
            FinanceCard {
                SectionLabel(stringResource(R.string.linked_cards))
                if (cards.isEmpty()) {
                    Text(stringResource(R.string.empty_linked_cards), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    cards.forEach { c ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BankLogo(logoOf(c.bin6, c.bankCode))
                            Column(Modifier.weight(1f)) {
                                Text(CardMath.maskPan(c.last4.padStart(16, '*')), style = MaterialTheme.typography.titleSmall)
                                Text(c.bankCode, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
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
                                Text(formatJalali(txn.occurredAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                (if (txn.direction == Direction.IN) "+" else "−") + formatMoney(txn.amount, state.settings.displayToman),
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
    val acc = Account("a1", "علی رضایی", AccountType.PERSON, color = 0xFF0B6E4F, sortOrder = 0, createdAt = 0, updatedAt = 0)
    MiniAccountantTheme {
        PersonDetailContent(
            state = AppUiState(
                ready = true,
                people = listOf(Person("p1", "a1", "علی رضایی", "۰۹۱۲۱۲۳۴۵۶۷", "همکار", firstName = "علی", lastName = "رضایی", email = "ali@mail.com")),
                accounts = listOf(acc),
                balances = listOf(AccountBalance(acc, 2_500_000)),
                txns = listOf(
                    LedgerTransaction("t", "a1", "sys-food", "p1", 2500000, Direction.IN, "بدهی ناهار", 0, 1405, 6, 15, "fy", null, 0),
                ),
                categories = ir.mhajisoft.miniaccountant.domain.ledger.CategoryCatalog.systemCategories(),
            ),
            person = Person("p1", "a1", "علی رضایی", "۰۹۱۲۱۲۳۴۵۶۷", "همکار", firstName = "علی", lastName = "رضایی", email = "ali@mail.com"),
            cards = listOf(BankCard("c1", "a1", "4331", "610433", "mellat", 12, 1408, null, null, null, false, "p1")),
            ibans = listOf(BankAccount("b1", "a1", "123", "IR000000000000000000000000", "mellat", "بانک ملت", "p1")),
            onBack = {},
            onPay = { _, _, _ -> },
        )
    }
}
