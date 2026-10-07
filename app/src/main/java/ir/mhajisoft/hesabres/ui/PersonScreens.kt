@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package ir.mhajisoft.hesabres.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import ir.mhajisoft.hesabres.R
import ir.mhajisoft.hesabres.data.repository.AccountBalance
import ir.mhajisoft.hesabres.domain.bank.CardMath
import ir.mhajisoft.hesabres.domain.bank.IbanMath
import ir.mhajisoft.hesabres.domain.crash.WriteFailures
import ir.mhajisoft.hesabres.domain.model.Account
import ir.mhajisoft.hesabres.domain.model.AccountType
import ir.mhajisoft.hesabres.domain.model.BankAccount
import ir.mhajisoft.hesabres.domain.model.BankCard
import ir.mhajisoft.hesabres.domain.model.Direction
import ir.mhajisoft.hesabres.domain.model.LedgerTransaction
import ir.mhajisoft.hesabres.domain.model.Person
import ir.mhajisoft.hesabres.domain.model.SocialLink
import ir.mhajisoft.hesabres.domain.money.PersianDigits
import ir.mhajisoft.hesabres.domain.people.ContactLinks
import ir.mhajisoft.hesabres.domain.people.PersonFieldError
import ir.mhajisoft.hesabres.domain.people.PersonListFilter
import ir.mhajisoft.hesabres.domain.people.PersonProfileRules
import ir.mhajisoft.hesabres.domain.people.PersonRoster
import ir.mhajisoft.hesabres.domain.people.PersonVaultLinks
import ir.mhajisoft.hesabres.domain.people.SocialLinkCatalog
import ir.mhajisoft.hesabres.ui.components.ChoiceChip
import ir.mhajisoft.hesabres.ui.components.ColorPackPicker
import ir.mhajisoft.hesabres.ui.components.FinanceCard
import ir.mhajisoft.hesabres.ui.components.FinanceEmptyState
import ir.mhajisoft.hesabres.ui.components.FinanceTextField
import ir.mhajisoft.hesabres.ui.components.PersonAvatar
import ir.mhajisoft.hesabres.ui.components.PrimaryWideButton
import ir.mhajisoft.hesabres.ui.components.SecondaryWideButton
import ir.mhajisoft.hesabres.ui.components.SectionLabel
import ir.mhajisoft.hesabres.ui.components.SoftDivider
import ir.mhajisoft.hesabres.ui.components.TonalCard
import ir.mhajisoft.hesabres.ui.components.formatJalali
import ir.mhajisoft.hesabres.ui.components.formatMoney
import ir.mhajisoft.hesabres.ui.theme.HesabresTheme
import ir.mhajisoft.hesabres.ui.theme.LocalLedgerTones
import ir.mhajisoft.hesabres.ui.theme.SymbolIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PeopleScreen(state: AppUiState, onOpen: (String) -> Unit, onAdd: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(PersonListFilter.ALL) }
    val balanceOf: (Person) -> Long = { person ->
        state.balances.firstOrNull { it.account.id == person.accountId }?.balanceSigned ?: 0L
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Text(
                stringResource(R.string.people),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (state.people.isEmpty()) {
                FinanceEmptyState(
                    SymbolIcons.People,
                    stringResource(R.string.empty_people),
                    stringResource(R.string.empty_people_body),
                )
            } else {
                FinanceTextField(
                    query,
                    { query = it },
                    label = stringResource(R.string.search_people),
                )
                FlowRow(
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ChoiceChip(filter == PersonListFilter.ALL, { filter = PersonListFilter.ALL }, stringResource(R.string.all))
                    ChoiceChip(filter == PersonListFilter.DEBTOR, { filter = PersonListFilter.DEBTOR }, stringResource(R.string.debtor))
                    ChoiceChip(filter == PersonListFilter.CREDITOR, { filter = PersonListFilter.CREDITOR }, stringResource(R.string.creditor))
                    ChoiceChip(filter == PersonListFilter.SETTLED, { filter = PersonListFilter.SETTLED }, stringResource(R.string.settled))
                }
                val shown = PersonRoster.visible(state.people, balanceOf, query, filter)
                if (shown.isEmpty()) {
                    FinanceEmptyState(
                        SymbolIcons.People,
                        stringResource(R.string.empty_people_filter),
                        stringResource(R.string.search_people),
                    )
                } else {
                    LazyColumn(
                        Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(shown, key = { it.id }) { person ->
                            PersonListCard(person, balanceOf(person), state.settings.displayToman) { onOpen(person.id) }
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
private fun PersonListCard(person: Person, balance: Long, toman: Boolean, onOpen: () -> Unit) {
    val tones = LocalLedgerTones.current
    val role = PersonRoster.roleOf(balance)
    val roleColor = when (role) {
        PersonListFilter.DEBTOR -> tones.debtor
        PersonListFilter.CREDITOR -> tones.creditor
        PersonListFilter.SETTLED, PersonListFilter.ALL -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val roleLabel = when (role) {
        PersonListFilter.DEBTOR -> stringResource(R.string.debtor)
        PersonListFilter.CREDITOR -> stringResource(R.string.creditor)
        else -> stringResource(R.string.settled)
    }
    val caption = when (role) {
        PersonListFilter.DEBTOR -> stringResource(R.string.balance_you)
        PersonListFilter.CREDITOR -> stringResource(R.string.balance_them)
        else -> stringResource(R.string.balance_even)
    }
    val contact = PersonRoster.contactLine(person)
    FinanceCard(Modifier.clickable(onClick = onOpen)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PersonAvatar(person.initials, person.avatarColor)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    person.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    contact ?: stringResource(R.string.no_contact),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDirection = if (contact == null) TextDirection.Content else TextDirection.Ltr,
                    ),
                    textAlign = if (contact == null) TextAlign.Start else TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                Modifier.widthIn(max = 132.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    roleLabel,
                    color = roleColor,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    caption,
                    color = roleColor,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    formatMoney(kotlin.math.abs(balance), toman),
                    color = roleColor,
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
    var fieldError by remember { mutableStateOf<PersonFieldError?>(null) }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    stringResource(if (existing == null) R.string.add_person else R.string.person_info),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(SymbolIcons.Back, stringResource(R.string.close)) }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
            ),
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
            SectionLabel(stringResource(R.string.color_pack))
            ColorPackPicker(color) { color = it }
            FinanceCard {
                FinanceTextField(
                    first,
                    { first = it; fieldError = null },
                    label = stringResource(R.string.first_name),
                    isError = fieldError == PersonFieldError.NAME,
                    supportingText = if (fieldError == PersonFieldError.NAME) stringResource(R.string.invalid_name) else null,
                )
                FinanceTextField(
                    last,
                    { last = it; fieldError = null },
                    label = stringResource(R.string.last_name),
                    isError = fieldError == PersonFieldError.NAME,
                )
                FinanceTextField(
                    phone,
                    { phone = it; fieldError = null },
                    label = stringResource(R.string.phone),
                    keyboardType = KeyboardType.Phone,
                    isError = fieldError == PersonFieldError.PHONE,
                    supportingText = if (fieldError == PersonFieldError.PHONE) stringResource(R.string.invalid_phone) else null,
                )
                FinanceTextField(
                    email,
                    { email = it; fieldError = null },
                    label = stringResource(R.string.email),
                    keyboardType = KeyboardType.Email,
                    isError = fieldError == PersonFieldError.EMAIL,
                    supportingText = if (fieldError == PersonFieldError.EMAIL) stringResource(R.string.invalid_email) else null,
                )
                FinanceTextField(note, { note = it }, label = stringResource(R.string.note), singleLine = false)
            }
            FinanceCard {
                SectionLabel(stringResource(R.string.social_links))
                Text(
                    stringResource(R.string.social_links_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (socials.isEmpty()) {
                    Text(
                        stringResource(R.string.empty_social),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SocialLinkCatalog.suggestions.forEach { label ->
                        ChoiceChip(socials.any { it.first == label }, {
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
                val problem = PersonProfileRules.validate(first, last, phone, email)
                if (problem != null) {
                    fieldError = problem
                    return@PrimaryWideButton
                }
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
fun PersonDetailScreen(
    state: AppUiState,
    vm: AppViewModel,
    personId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddCard: () -> Unit,
    onEditCard: (String) -> Unit,
    onAddIban: () -> Unit,
    onEditIban: (String) -> Unit,
    onSecure: (Boolean) -> Unit,
) {
    val person = state.people.firstOrNull { it.id == personId }
    val cards by vm.vaultRepo.cardsFlow.collectAsStateWithLifecycle(emptyList())
    val ibans by vm.vaultRepo.bankAccountsFlow.collectAsStateWithLifecycle(emptyList())
    val context = LocalContext.current
    val activity = context.findMainActivity()
    val scope = rememberCoroutineScope()
    var revealed by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var notice by remember { mutableStateOf<String?>(null) }
    DisposableEffect(revealed.isNotEmpty()) {
        if (revealed.isNotEmpty()) onSecure(true)
        onDispose { if (revealed.isNotEmpty()) onSecure(false) }
    }
    val linkedCards = if (person == null) emptyList() else cards.filter {
        PersonVaultLinks.cardLinked(person.id, person.accountId, it)
    }
    val linkedIbans = if (person == null) emptyList() else ibans.filter {
        PersonVaultLinks.accountLinked(person.id, person.accountId, it)
    }
    fun copyPlain(label: String, value: String, clearSecret: Boolean) {
        val cm = context.getSystemService(ClipboardManager::class.java) ?: return
        cm.setPrimaryClip(ClipData.newPlainText(label, value))
        notice = if (clearSecret) context.getString(R.string.copied_clears) else context.getString(R.string.copied)
        if (clearSecret) {
            scope.launch {
                delay(30_000)
                cm.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }
    }
    PersonDetailContent(
        state = state,
        person = person,
        cards = linkedCards,
        ibans = linkedIbans,
        notice = notice,
        revealedPan = revealed,
        onBack = onBack,
        onEdit = onEdit,
        onPay = { acc, amt, they, done ->
            notice = null
            vm.payPerson(acc, amt, they, onError = { done(it) }, onDone = { done(null) })
        },
        onAddCard = onAddCard,
        onEditCard = { onEditCard(it.id) },
        onDeleteCard = { card ->
            scope.launch {
                runCatching { vm.vaultRepo.deleteCard(card.id) }
                    .onFailure { notice = WriteFailures.map(it) }
                revealed = revealed - card.id
            }
        },
        onAddIban = onAddIban,
        onEditIban = { onEditIban(it.id) },
        onDeleteIban = { row ->
            scope.launch {
                runCatching { vm.vaultRepo.deleteBankAccount(row.id) }
                    .onFailure { notice = WriteFailures.map(it) }
            }
        },
        onCopy = { label, value -> copyPlain(label, value, clearSecret = false) },
        onCopySecret = { label, value -> copyPlain(label, value, clearSecret = true) },
        onDial = { raw ->
            val uri = ContactLinks.dialUri(raw) ?: return@PersonDetailContent
            runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, uri.toUri())) }
        },
        onEmail = { raw ->
            val uri = ContactLinks.mailtoUri(raw) ?: return@PersonDetailContent
            runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, uri.toUri())) }
        },
        onOpen = { url ->
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
        },
        onRevealPan = { card ->
            val panId = card.panCipherId ?: return@PersonDetailContent
            val host = activity ?: run {
                notice = WriteFailures.ERR_NO_ACTIVITY
                return@PersonDetailContent
            }
            host.lockBeforePan {
                host.lifecycleScope.launch {
                    val plain = runCatching { vm.vaultRepo.decryptPan(panId) }.getOrNull()
                    if (plain != null) revealed = revealed + (card.id to plain)
                }
            }
        },
        onRevealCvv = { card ->
            val cvvId = card.cvvCipherId ?: return@PersonDetailContent
            val host = activity ?: run {
                notice = WriteFailures.ERR_NO_ACTIVITY
                return@PersonDetailContent
            }
            host.lifecycleScope.launch {
                val iv = vm.vaultRepo.secretVault.cvvIv(cvvId) ?: return@launch
                val cipher = vm.vaultRepo.secretVault.createCvvDecryptCipher(iv) ?: return@launch
                host.promptUnlock(
                    crypto = BiometricPrompt.CryptoObject(cipher),
                    onSuccess = { result ->
                        host.lifecycleScope.launch {
                            val unlocked = result.cryptoObject?.cipher ?: cipher
                            val cvv = runCatching { vm.vaultRepo.secretVault.revealCvv(cvvId, unlocked) }.getOrNull()
                                ?: return@launch
                            copyPlain(host.getString(R.string.cvv), cvv, clearSecret = true)
                        }
                    },
                    onCancel = {},
                    onError = { notice = WriteFailures.ERR_BIOMETRIC },
                )
            }
        },
        logoOf = { bin, code -> vm.vaultRepo.directory.logoOf(bin, code) },
        bankNameOf = { bin, code ->
            val bank = vm.vaultRepo.directory.findByBin(bin) ?: vm.vaultRepo.directory.findById(code)
            bank?.nameFa ?: context.getString(R.string.unknown_bank)
        },
    )
}

@Composable
fun PersonDetailContent(
    state: AppUiState,
    person: Person?,
    cards: List<BankCard>,
    ibans: List<BankAccount>,
    onBack: () -> Unit,
    onPay: (String, String, Boolean, (String?) -> Unit) -> Unit,
    onEdit: () -> Unit = {},
    notice: String? = null,
    revealedPan: Map<String, String> = emptyMap(),
    onAddCard: () -> Unit = {},
    onEditCard: (BankCard) -> Unit = {},
    onDeleteCard: (BankCard) -> Unit = {},
    onAddIban: () -> Unit = {},
    onEditIban: (BankAccount) -> Unit = {},
    onDeleteIban: (BankAccount) -> Unit = {},
    onCopy: (String, String) -> Unit = { _, _ -> },
    onCopySecret: (String, String) -> Unit = { _, _ -> },
    onDial: (String) -> Unit = {},
    onEmail: (String) -> Unit = {},
    onOpen: (String) -> Unit = {},
    onRevealPan: (BankCard) -> Unit = {},
    onRevealCvv: (BankCard) -> Unit = {},
    logoOf: (String, String) -> String = { _, _ -> "bank_unknown" },
    bankNameOf: (String, String) -> String = { _, code -> code },
) {
    if (person == null) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FinanceEmptyState(SymbolIcons.People, stringResource(R.string.person_missing), stringResource(R.string.empty_people_body))
            SecondaryWideButton(stringResource(R.string.close), onBack)
        }
        return
    }
    val bal = state.balances.firstOrNull { it.account.id == person.accountId }?.balanceSigned ?: 0L
    val txns = state.txns.filter { it.accountId == person.accountId }.sortedByDescending { it.occurredAt }.take(12)
    val tones = LocalLedgerTones.current
    var payAmount by remember { mutableStateOf("") }
    var payError by remember { mutableStateOf<String?>(null) }
    var payOk by remember { mutableStateOf(false) }
    var pendingCard by remember { mutableStateOf<BankCard?>(null) }
    var pendingIban by remember { mutableStateOf<BankAccount?>(null) }
    val role = when {
        bal > 0L -> stringResource(R.string.debtor)
        bal < 0L -> stringResource(R.string.creditor)
        else -> stringResource(R.string.settled)
    }
    val roleColor = when {
        bal > 0L -> tones.debtor
        bal < 0L -> tones.creditor
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    stringResource(R.string.person_profile),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(SymbolIcons.Back, stringResource(R.string.close)) }
            },
            actions = {
                TextButton(onClick = onEdit) { Text(stringResource(R.string.edit)) }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
            ),
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TonalCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PersonAvatar(person.initials, person.avatarColor, size = 64)
                    Column(Modifier.weight(1f)) {
                        Text(
                            person.displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(role, color = roleColor, style = MaterialTheme.typography.titleSmall)
                    }
                }
                Text(
                    formatMoney(kotlin.math.abs(bal), state.settings.displayToman),
                    style = MaterialTheme.typography.headlineMedium,
                    color = roleColor,
                )
                Text(
                    when {
                        bal > 0L -> stringResource(R.string.they_owe_you)
                        bal < 0L -> stringResource(R.string.you_owe_them)
                        else -> stringResource(R.string.balance_even)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (notice != null) {
                Text(notice, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            }
            val phoneLabel = stringResource(R.string.phone_short)
            val emailLabel = stringResource(R.string.email)
            val noteLabel = stringResource(R.string.note)
            val cardLabel = stringResource(R.string.card_number)
            val ibanLabel = stringResource(R.string.iban)
            FinanceCard {
                SectionHeaderAction(stringResource(R.string.person_info), stringResource(R.string.edit), onEdit)
                val structured = person.firstName.isNotBlank() || person.lastName.isNotBlank()
                if (!structured && person.name.isNotBlank()) {
                    ContactSlot(stringResource(R.string.name), person.name, stringResource(R.string.empty_name), onEdit)
                } else {
                    ContactSlot(
                        stringResource(R.string.first_name),
                        person.firstName.ifBlank { null },
                        stringResource(R.string.empty_name),
                        onEdit,
                    )
                    ContactSlot(
                        stringResource(R.string.last_name),
                        person.lastName.ifBlank { null },
                        stringResource(R.string.empty_name),
                        onEdit,
                    )
                }
                ContactSlot(
                    stringResource(R.string.phone_short),
                    person.phone,
                    stringResource(R.string.empty_phone),
                    onEdit,
                    ltr = true,
                    persianDigits = true,
                ) {
                    val phone = person.phone
                    if (!phone.isNullOrBlank()) {
                        TextButton(onClick = { onCopy(phoneLabel, phone) }) {
                            Text(stringResource(R.string.copy))
                        }
                        if (ContactLinks.dialUri(phone) != null) {
                            TextButton(onClick = { onDial(phone) }) { Text(stringResource(R.string.dial)) }
                        }
                    }
                }
                ContactSlot(
                    stringResource(R.string.email),
                    person.email,
                    stringResource(R.string.empty_email),
                    onEdit,
                    ltr = true,
                ) {
                    val email = person.email
                    if (!email.isNullOrBlank()) {
                        TextButton(onClick = { onCopy(emailLabel, email) }) {
                            Text(stringResource(R.string.copy))
                        }
                        if (ContactLinks.mailtoUri(email) != null) {
                            TextButton(onClick = { onEmail(email) }) { Text(stringResource(R.string.send_email)) }
                        }
                    }
                }
                if (person.socialLinks.isEmpty()) {
                    ContactSlot(
                        stringResource(R.string.social_links),
                        null,
                        stringResource(R.string.empty_social),
                        onEdit,
                    )
                } else {
                    person.socialLinks.forEach { link ->
                        val url = ContactLinks.openUri(link.label, link.value)
                        ContactSlot(link.label, link.value, stringResource(R.string.empty_social), onEdit, ltr = true) {
                            TextButton(onClick = { onCopy(link.label, link.value) }) { Text(stringResource(R.string.copy)) }
                            if (url != null) {
                                TextButton(onClick = { onOpen(url) }) { Text(stringResource(R.string.open_link)) }
                            }
                        }
                    }
                }
                ContactSlot(
                    stringResource(R.string.note),
                    person.note,
                    stringResource(R.string.empty_note),
                    onEdit,
                ) {
                    val note = person.note
                    if (!note.isNullOrBlank()) {
                        TextButton(onClick = { onCopy(noteLabel, note) }) {
                            Text(stringResource(R.string.copy))
                        }
                    }
                }
            }
            FinanceCard {
                SectionLabel(stringResource(R.string.settle))
                Text(
                    when {
                        bal > 0L -> stringResource(R.string.settle_hint_they_owe)
                        bal < 0L -> stringResource(R.string.settle_hint_you_owe)
                        else -> stringResource(R.string.settle_hint_even)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FinanceTextField(
                    payAmount,
                    {
                        payAmount = it
                        payError = null
                        payOk = false
                    },
                    label = stringResource(R.string.pay_amount),
                    keyboardType = KeyboardType.Number,
                    isError = payError != null,
                    supportingText = payError,
                )
                if (payOk) {
                    Text(
                        stringResource(R.string.pay_settled),
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                PrimaryWideButton(stringResource(R.string.pay_them), onClick = {
                    onPay(person.accountId, payAmount, false) { err ->
                        if (err == null) {
                            payAmount = ""
                            payError = null
                            payOk = true
                        } else {
                            payError = err
                            payOk = false
                        }
                    }
                })
                SecondaryWideButton(stringResource(R.string.they_pay)) {
                    onPay(person.accountId, payAmount, true) { err ->
                        if (err == null) {
                            payAmount = ""
                            payError = null
                            payOk = true
                        } else {
                            payError = err
                            payOk = false
                        }
                    }
                }
            }
            FinanceCard {
                SectionHeaderAction(stringResource(R.string.linked_cards), stringResource(R.string.add_card), onAddCard)
                if (cards.isEmpty()) {
                    Text(stringResource(R.string.empty_linked_cards), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SecondaryWideButton(stringResource(R.string.add_card), onAddCard)
                } else {
                    cards.forEachIndexed { index, card ->
                        if (index > 0) SoftDivider()
                        PersonCardRow(
                            card = card,
                            logo = logoOf(card.bin6, card.bankCode),
                            bankName = bankNameOf(card.bin6, card.bankCode),
                            revealed = revealedPan[card.id],
                            onEdit = { onEditCard(card) },
                            onDelete = { pendingCard = card },
                            onReveal = { onRevealPan(card) },
                            onRevealCvv = { onRevealCvv(card) },
                            onCopyPan = { pan -> onCopySecret(cardLabel, pan) },
                        )
                    }
                }
            }
            FinanceCard {
                SectionHeaderAction(stringResource(R.string.linked_iban), stringResource(R.string.add_iban), onAddIban)
                if (ibans.isEmpty()) {
                    Text(stringResource(R.string.empty_linked_iban), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SecondaryWideButton(stringResource(R.string.add_iban), onAddIban)
                } else {
                    ibans.forEachIndexed { index, row ->
                        if (index > 0) SoftDivider()
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BankLogo(logoOf("", row.bankCode))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        PersianDigits.toPersian(IbanMath.formatGrouped(row.iban)),
                                        style = MaterialTheme.typography.titleSmall.copy(textDirection = TextDirection.Ltr),
                                        textAlign = TextAlign.End,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        row.bankName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (row.accountNumber.isNotBlank()) {
                                        Text(
                                            PersianDigits.toPersian(row.accountNumber),
                                            style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr),
                                            textAlign = TextAlign.End,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { onCopy(ibanLabel, row.iban) }) {
                                    Text(stringResource(R.string.copy))
                                }
                                TextButton(onClick = { onEditIban(row) }) { Text(stringResource(R.string.edit)) }
                                TextButton(onClick = { pendingIban = row }) { Text(stringResource(R.string.delete)) }
                            }
                        }
                    }
                }
            }
            FinanceCard {
                SectionLabel(stringResource(R.string.recent_txns))
                if (txns.isEmpty()) {
                    Text(stringResource(R.string.empty_person_txns), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    txns.forEachIndexed { index, txn ->
                        if (index > 0) SoftDivider()
                        val cat = state.categories.firstOrNull { it.id == txn.categoryId }
                        val directionLabel = stringResource(
                            if (txn.direction == Direction.IN) R.string.txn_to_person else R.string.txn_from_person,
                        )
                        val title = when {
                            txn.note.isNotBlank() -> txn.note
                            cat != null && !cat.isSystem -> cat.name
                            else -> directionLabel
                        }
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${formatJalali(txn.occurredAt)} · $directionLabel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                (if (txn.direction == Direction.IN) "+" else "−") + formatMoney(txn.amount, state.settings.displayToman),
                                color = if (txn.direction == Direction.IN) tones.income else tones.expense,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.widthIn(max = 140.dp),
                                textAlign = TextAlign.End,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
    pendingCard?.let { card ->
        AlertDialog(
            onDismissRequest = { pendingCard = null },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCard(card)
                    pendingCard = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingCard = null }) { Text(stringResource(R.string.cancel)) }
            },
            text = { Text(stringResource(R.string.delete_card_confirm)) },
        )
    }
    pendingIban?.let { row ->
        AlertDialog(
            onDismissRequest = { pendingIban = null },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteIban(row)
                    pendingIban = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingIban = null }) { Text(stringResource(R.string.cancel)) }
            },
            text = { Text(stringResource(R.string.delete_iban_confirm)) },
        )
    }
}

@Composable
private fun PersonCardRow(
    card: BankCard,
    logo: String,
    bankName: String,
    revealed: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReveal: () -> Unit,
    onRevealCvv: () -> Unit,
    onCopyPan: (String) -> Unit,
) {
    val shown = if (revealed != null) {
        PersianDigits.toPersian(CardMath.normalizeDigits(revealed).chunked(4).joinToString(" "))
    } else {
        PersianDigits.toPersian(CardMath.maskPan(card.last4))
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BankLogo(logo)
            Column(Modifier.weight(1f)) {
                Text(
                    shown,
                    style = MaterialTheme.typography.titleSmall.copy(textDirection = TextDirection.Ltr),
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    bankName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val expiry = PersianDigits.toPersian(
                    "${card.expiryMonth.toString().padStart(2, '0')}/${card.expiryYear}",
                )
                val holder = card.holderName?.takeIf { it.isNotBlank() }
                Text(
                    if (holder == null) expiry else "$expiry · $holder",
                    style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr),
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onEdit) { Text(stringResource(R.string.edit)) }
            TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
            if (card.panCipherId != null && revealed == null) {
                TextButton(onClick = onReveal) { Text(stringResource(R.string.reveal_pan)) }
            }
            if (revealed != null) {
                TextButton(onClick = { onCopyPan(CardMath.normalizeDigits(revealed)) }) { Text(stringResource(R.string.copy)) }
            }
            if (card.rememberCvv && card.cvvCipherId != null) {
                TextButton(onClick = onRevealCvv) { Text(stringResource(R.string.reveal_cvv)) }
            }
        }
    }
}

@Composable
private fun SectionHeaderAction(title: String, action: String, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { SectionLabel(title) }
        TextButton(onClick = onAction) {
            Text(action, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ContactSlot(
    label: String,
    value: String?,
    emptyText: String,
    onAdd: () -> Unit,
    ltr: Boolean = false,
    persianDigits: Boolean = false,
    actions: @Composable () -> Unit = {},
) {
    if (value.isNullOrBlank()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                emptyText,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onAdd) { Text(stringResource(R.string.add_field)) }
        }
    } else {
        val shown = if (persianDigits) PersianDigits.toPersian(value) else value
        Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    shown,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall.copy(
                        textDirection = if (ltr) TextDirection.Ltr else TextDirection.Content,
                    ),
                    textAlign = if (ltr) TextAlign.End else TextAlign.Start,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                actions()
            }
        }
    }
}

@Preview(showBackground = true, locale = "fa", name = "People list", widthDp = 390, heightDp = 820)
@Composable
fun PreviewPeopleList() {
    val ali = Account("a1", "علی رضایی", AccountType.PERSON, color = 0xFF2563EB, sortOrder = 0, createdAt = 0, updatedAt = 0)
    val mina = Account("a2", "مینا", AccountType.PERSON, color = 0xFF1D4ED8, sortOrder = 1, createdAt = 0, updatedAt = 0)
    HesabresTheme {
        PeopleScreen(
            state = AppUiState(
                ready = true,
                people = listOf(
                    Person("p1", "a1", "علی رضایی", "09121234567", null, firstName = "علی", lastName = "رضایی", email = "ali@mail.com"),
                    Person("p2", "a2", "مینا", null, null, firstName = "مینا"),
                ),
                accounts = listOf(ali, mina),
                balances = listOf(AccountBalance(ali, 2_500_000), AccountBalance(mina, -800_000)),
            ),
            onOpen = {},
            onAdd = {},
        )
    }
}

@Preview(showBackground = true, locale = "fa", name = "Person edit", widthDp = 390, heightDp = 820)
@Composable
fun PreviewPersonEdit() {
    HesabresTheme {
        PersonEditForm(
            existing = Person(
                "p1", "a1", "علی رضایی", "09121234567", "همکار",
                firstName = "علی", lastName = "رضایی", email = "ali@mail.com",
                socialLinks = listOf(SocialLink("s1", "p1", "تلگرام", "ali_rezaei", 0)),
            ),
            onBack = {},
            onCreate = { _, _, _, _, _, _, _ -> },
            onUpdate = {},
        )
    }
}

@Preview(showBackground = true, locale = "fa", name = "Person detail", widthDp = 390, heightDp = 820)
@Composable
fun PreviewPersonDetail() {
    val acc = Account("a1", "علی رضایی", AccountType.PERSON, color = 0xFF2563EB, sortOrder = 0, createdAt = 0, updatedAt = 0)
    val person = Person(
        "p1", "a1", "علی رضایی", "09121234567", "همکار",
        firstName = "علی", lastName = "رضایی", email = "ali@mail.com", avatarColor = 0xFF2563EB,
        socialLinks = listOf(SocialLink("s1", "p1", "تلگرام", "ali_rezaei", 0)),
    )
    HesabresTheme {
        PersonDetailContent(
            state = AppUiState(
                ready = true,
                people = listOf(person),
                accounts = listOf(acc),
                balances = listOf(AccountBalance(acc, 2_500_000)),
                txns = listOf(
                    LedgerTransaction("t", "a1", "sys-food", "p1", 2500000, Direction.IN, "بدهی ناهار", 0, 1405, 6, 15, "fy", null, 0),
                ),
                categories = ir.mhajisoft.hesabres.domain.ledger.CategoryCatalog.systemCategories(),
            ),
            person = person,
            cards = listOf(BankCard("c1", "a1", "4331", "610433", "mellat", 12, 1408, "علی رضایی", null, null, false, "p1")),
            ibans = listOf(BankAccount("b1", "a1", "1234567890", "IR120170000000123456789012", "mellat", "بانک ملت", "p1")),
            onBack = {},
            onPay = { _, _, _, _ -> },
            bankNameOf = { _, _ -> "بانک ملت" },
            logoOf = { _, _ -> "bank_mellat" },
        )
    }
}
