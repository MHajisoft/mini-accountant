package ir.mhajisoft.hesabres.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.TextButton
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
import ir.mhajisoft.hesabres.R
import ir.mhajisoft.hesabres.data.repository.AccountBalance
import ir.mhajisoft.hesabres.domain.bank.CardMath
import ir.mhajisoft.hesabres.domain.bank.IbanMath
import ir.mhajisoft.hesabres.domain.model.Account
import ir.mhajisoft.hesabres.domain.model.AccountType
import ir.mhajisoft.hesabres.domain.model.BankAccount
import ir.mhajisoft.hesabres.domain.model.BankCard
import ir.mhajisoft.hesabres.domain.model.Direction
import ir.mhajisoft.hesabres.domain.model.LedgerTransaction
import ir.mhajisoft.hesabres.domain.model.Person
import ir.mhajisoft.hesabres.domain.model.SocialLink
import ir.mhajisoft.hesabres.domain.people.SocialLinkCatalog
import ir.mhajisoft.hesabres.ui.components.ChoiceChip
import ir.mhajisoft.hesabres.ui.components.ColorPackPicker
import ir.mhajisoft.hesabres.ui.components.FinanceCard
import ir.mhajisoft.hesabres.ui.components.FinanceEmptyState
import ir.mhajisoft.hesabres.ui.components.FinanceTextField
import ir.mhajisoft.hesabres.ui.components.LabeledRow
import ir.mhajisoft.hesabres.ui.components.PersonAvatar
import ir.mhajisoft.hesabres.ui.components.PrimaryWideButton
import ir.mhajisoft.hesabres.ui.components.SecondaryWideButton
import ir.mhajisoft.hesabres.ui.components.SectionLabel
import ir.mhajisoft.hesabres.ui.components.TonalCard
import ir.mhajisoft.hesabres.ui.components.formatJalali
import ir.mhajisoft.hesabres.ui.components.formatMoney
import ir.mhajisoft.hesabres.ui.theme.LocalLedgerTones
import ir.mhajisoft.hesabres.ui.theme.HesabresTheme
import ir.mhajisoft.hesabres.ui.theme.SymbolIcons

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
        onCreate = { first, last, phone, email, note, color, socials ->
            vm.addPerson(first, last, phone, email, note, color, socials)
            onBack()
        },
        onUpdate = {
            vm.updatePerson(it)
            onBack()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PersonEditForm(
    existing: Person?,
    onBack: () -> Unit,
    onCreate: (String, String, String?, String?, String?, Long, List<SocialLink>) -> Unit,
    onUpdate: (Person) -> Unit,
) {
    var first by remember(existing?.id) { mutableStateOf(existing?.firstName?.ifBlank { existing.name } ?: "") }
    var last by remember(existing?.id) { mutableStateOf(existing?.lastName.orEmpty()) }
    var phone by remember(existing?.id) { mutableStateOf(existing?.phone.orEmpty()) }
    var email by remember(existing?.id) { mutableStateOf(existing?.email.orEmpty()) }
    var note by remember(existing?.id) { mutableStateOf(existing?.note.orEmpty()) }
    var color by remember(existing?.id) { mutableLongStateOf(existing?.avatarColor ?: 0xFF0B6E4F) }
    var socials by remember(existing?.id) {
        mutableStateOf(
            existing?.socialLinks?.map { it.label to it.value }?.ifEmpty { null }
                ?: SocialLinkCatalog.fromLegacyColumns(
                    existing?.id.orEmpty(),
                    existing?.instagram,
                    existing?.telegram,
                    existing?.whatsapp,
                ).map { it.label to it.value },
        )
    }
    var customLabel by remember { mutableStateOf("") }
    var customValue by remember { mutableStateOf("") }
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
                FinanceTextField(note, { note = it }, label = stringResource(R.string.note), singleLine = false)
            }
            FinanceCard {
                SectionLabel(stringResource(R.string.social_links))
                Text(stringResource(R.string.social_links_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SocialLinkCatalog.suggestions.forEach { label ->
                        ChoiceChip(false, {
                            if (socials.none { it.first == label }) socials = socials + (label to "")
                        }, label)
                    }
                }
                socials.forEachIndexed { index, (label, value) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FinanceTextField(label, { newLabel ->
                                socials = socials.toMutableList().also { it[index] = newLabel to value }
                            }, label = stringResource(R.string.social_label))
                            FinanceTextField(value, { newVal ->
                                socials = socials.toMutableList().also { it[index] = label to newVal }
                            }, label = stringResource(R.string.social_value))
                        }
                        TextButton(onClick = { socials = socials.toMutableList().also { it.removeAt(index) } }) {
                            Text(stringResource(R.string.delete))
                        }
                    }
                }
                FinanceTextField(customLabel, { customLabel = it }, label = stringResource(R.string.social_custom_label))
                FinanceTextField(customValue, { customValue = it }, label = stringResource(R.string.social_value))
                SecondaryWideButton(stringResource(R.string.add_social_link)) {
                    val label = customLabel.trim().ifBlank { "سایر" }
                    val value = customValue.trim()
                    if (value.isNotEmpty()) {
                        socials = socials + (label to value)
                        customLabel = ""
                        customValue = ""
                    } else if (customLabel.isNotBlank()) {
                        socials = socials + (label to "")
                        customLabel = ""
                    }
                }
            }
            PrimaryWideButton(stringResource(R.string.save), onClick = {
                if (first.isBlank() && last.isBlank()) return@PrimaryWideButton
                val links = socials.mapIndexed { i, pair ->
                    SocialLink(
                        id = existing?.socialLinks?.getOrNull(i)?.id.orEmpty(),
                        personId = existing?.id.orEmpty(),
                        label = pair.first,
                        value = pair.second,
                        sortOrder = i,
                    )
                }
                if (existing == null) {
                    onCreate(
                        first.trim(), last.trim(),
                        phone.ifBlank { null }, email.ifBlank { null },
                        note.ifBlank { null }, color, links,
                    )
                } else {
                    onUpdate(
                        existing.copy(
                            firstName = first.trim(),
                            lastName = last.trim(),
                            phone = phone.ifBlank { null },
                            email = email.ifBlank { null },
                            instagram = null,
                            telegram = null,
                            whatsapp = null,
                            note = note.ifBlank { null },
                            avatarColor = color,
                            socialLinks = links,
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
                person.socialLinks.forEach { link ->
                    LabeledRow(link.label, link.value)
                }
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
    HesabresTheme {
        PersonDetailContent(
            state = AppUiState(
                ready = true,
                people = listOf(Person("p1", "a1", "علی رضایی", "۰۹۱۲۱۲۳۴۵۶۷", "همکار", firstName = "علی", lastName = "رضایی", email = "ali@mail.com")),
                accounts = listOf(acc),
                balances = listOf(AccountBalance(acc, 2_500_000)),
                txns = listOf(
                    LedgerTransaction("t", "a1", "sys-food", "p1", 2500000, Direction.IN, "بدهی ناهار", 0, 1405, 6, 15, "fy", null, 0),
                ),
                categories = ir.mhajisoft.hesabres.domain.ledger.CategoryCatalog.systemCategories(),
            ),
            person = Person("p1", "a1", "علی رضایی", "۰۹۱۲۱۲۳۴۵۶۷", "همکار", firstName = "علی", lastName = "رضایی", email = "ali@mail.com"),
            cards = listOf(BankCard("c1", "a1", "4331", "610433", "mellat", 12, 1408, null, null, null, false, "p1")),
            ibans = listOf(BankAccount("b1", "a1", "123", "IR000000000000000000000000", "mellat", "بانک ملت", "p1")),
            onBack = {},
            onPay = { _, _, _ -> },
        )
    }
}
