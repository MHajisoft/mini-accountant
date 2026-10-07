@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package ir.mhajisoft.hesabres.ui

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import ir.mhajisoft.hesabres.R
import ir.mhajisoft.hesabres.domain.bank.BankMatch
import ir.mhajisoft.hesabres.domain.bank.CardMath
import ir.mhajisoft.hesabres.domain.bank.IbanMath
import ir.mhajisoft.hesabres.domain.bank.VaultFieldError
import ir.mhajisoft.hesabres.domain.bank.VaultInput
import ir.mhajisoft.hesabres.domain.crash.WriteFailures
import ir.mhajisoft.hesabres.domain.jalali.JalaliConverter
import ir.mhajisoft.hesabres.domain.model.BankAccount
import ir.mhajisoft.hesabres.domain.model.BankCard
import ir.mhajisoft.hesabres.domain.money.PersianDigits
import ir.mhajisoft.hesabres.ui.components.ExpiryMonthYearPicker
import ir.mhajisoft.hesabres.ui.components.FinanceCard
import ir.mhajisoft.hesabres.ui.components.FinanceEmptyState
import ir.mhajisoft.hesabres.ui.components.FinanceTextField
import ir.mhajisoft.hesabres.ui.components.PrimaryWideButton
import ir.mhajisoft.hesabres.ui.components.SecondaryWideButton
import ir.mhajisoft.hesabres.ui.components.SwitchRow
import ir.mhajisoft.hesabres.ui.theme.HesabresTheme
import ir.mhajisoft.hesabres.ui.theme.SymbolIcons
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.ExperimentalLayoutApi

@Composable
fun vaultErrorText(error: VaultFieldError): String = when (error) {
    VaultFieldError.PAN_REQUIRED -> stringResource(R.string.invalid_pan_required)
    VaultFieldError.PAN -> stringResource(R.string.invalid_luhn)
    VaultFieldError.EXPIRY -> stringResource(R.string.invalid_expiry)
    VaultFieldError.CVV -> stringResource(R.string.invalid_cvv)
    VaultFieldError.CVV_REQUIRED -> stringResource(R.string.invalid_cvv_required)
    VaultFieldError.IBAN -> stringResource(R.string.invalid_iban)
    VaultFieldError.ACCOUNT -> stringResource(R.string.invalid_account_number)
}

@Composable
fun PersonCardScreen(
    state: AppUiState,
    vm: AppViewModel,
    personId: String,
    cardId: String?,
    onSecure: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    DisposableEffect(Unit) {
        onSecure(true)
        onDispose { onSecure(false) }
    }
    val person = state.people.firstOrNull { it.id == personId }
    var existing by remember { mutableStateOf<BankCard?>(null) }
    var resolved by remember(cardId) { mutableStateOf(cardId == null) }
    LaunchedEffect(cardId) {
        if (cardId == null) {
            existing = null
            resolved = true
            return@LaunchedEffect
        }
        val all = vm.vaultRepo.cardsFlow.first()
        existing = all.firstOrNull { it.id == cardId }
        resolved = true
    }
    if (person == null) {
        MissingRecord(stringResource(R.string.person_missing), onBack)
        return
    }
    if (!resolved) {
        LinearProgressIndicator(Modifier.padding(16.dp))
        return
    }
    val card = existing
    if (cardId != null && card == null) {
        MissingRecord(stringResource(R.string.card_missing), onBack)
        return
    }
    val today = remember { JalaliConverter.fromEpochMillis(System.currentTimeMillis()) }
    var pan by remember(card?.id) { mutableStateOf("") }
    var holder by remember(card?.id) { mutableStateOf(card?.holderName ?: person.displayName) }
    var cvv by remember(card?.id) { mutableStateOf("") }
    var rememberCvv by remember(card?.id) { mutableStateOf(card?.rememberCvv == true) }
    var expiryMonth by remember(card?.id) { mutableIntStateOf(card?.expiryMonth?.coerceIn(1, 12) ?: today.month) }
    var expiryYear by remember(card?.id) { mutableIntStateOf(card?.expiryYear?.takeIf { it in 1300..1600 } ?: today.year) }
    var fieldError by remember(card?.id) { mutableStateOf<VaultFieldError?>(null) }
    var errorText by remember(card?.id) { mutableStateOf<String?>(null) }
    val activity = LocalContext.current.findMainActivity()
    val digits = CardMath.normalizeDigits(pan)
    val match = if (digits.length >= 6) vm.vaultRepo.directory.resolvePan(digits) else null
    val typedBank = (match as? BankMatch.Known)?.bank
    val storedBank = card?.let { vm.vaultRepo.directory.findByBin(it.bin6) ?: vm.vaultRepo.directory.findById(it.bankCode) }
    val bank = typedBank ?: if (digits.isEmpty()) storedBank else null
    val shownError = fieldError?.let { vaultErrorText(it) } ?: errorText
    PersonCardForm(
        title = stringResource(if (card == null) R.string.add_card else R.string.edit_card),
        personName = person.displayName,
        pan = pan,
        onPanChange = { pan = it; fieldError = null; errorText = null },
        bankLabel = bank?.nameFa ?: if (digits.length >= 6) stringResource(R.string.unknown_bank) else null,
        logoName = bank?.logoDrawable ?: "bank_unknown",
        expiryMonth = expiryMonth,
        expiryYear = expiryYear,
        onExpiryChange = { month, year ->
            expiryMonth = month
            expiryYear = year
        },
        holder = holder,
        onHolderChange = { holder = it },
        cvv = cvv,
        onCvvChange = { cvv = it; fieldError = null },
        rememberCvv = rememberCvv,
        onRememberCvv = { rememberCvv = it; fieldError = null },
        editing = card != null,
        error = shownError,
        onBack = onBack,
        onSave = save@{
            val problem = VaultInput.validateCard(
                panRaw = pan,
                expiryMonth = expiryMonth,
                expiryYear = expiryYear,
                rememberCvv = rememberCvv,
                cvvRaw = cvv,
                editing = card != null,
                hasStoredCvv = card?.cvvCipherId != null,
            )
            if (problem != null) {
                fieldError = problem
                errorText = null
                return@save
            }
            val host = activity
            if (host == null) {
                fieldError = null
                errorText = WriteFailures.ERR_NO_ACTIVITY
                return@save
            }
            if (person.accountId.isBlank()) {
                fieldError = null
                errorText = WriteFailures.ERR_GENERIC
                return@save
            }
            fieldError = null
            errorText = null
            val cvvDigits = CardMath.normalizeDigits(cvv)
            val needsFreshCvv = rememberCvv && cvvDigits.isNotEmpty()
            if (needsFreshCvv) {
                val cipher = vm.vaultRepo.secretVault.createCvvEncryptCipher()
                if (cipher == null) {
                    errorText = host.getString(R.string.cvv_unavailable)
                    return@save
                }
                val scope = host.lifecycleScope
                host.promptUnlock(
                    crypto = BiometricPrompt.CryptoObject(cipher),
                    onSuccess = { result ->
                        val unlocked = result.cryptoObject?.cipher ?: cipher
                        scope.launch {
                            val saved = runCatching {
                                vm.vaultRepo.saveCard(
                                    accountId = person.accountId,
                                    panAscii = pan,
                                    expiryMonth = expiryMonth,
                                    expiryYear = expiryYear,
                                    holderName = holder,
                                    rememberCvv = true,
                                    cvvAscii = cvv,
                                    existingId = card?.id,
                                    encryptCvv = { ascii -> vm.vaultRepo.secretVault.persistCvv(unlocked, ascii) },
                                    personId = person.id,
                                )
                            }.getOrElse {
                                errorText = WriteFailures.ERR_CVV
                                return@launch
                            }
                            saved.onSuccess { onBack() }.onFailure { errorText = WriteFailures.ERR_CVV }
                        }
                    },
                    onCancel = {},
                    onError = { errorText = WriteFailures.ERR_BIOMETRIC },
                )
            } else {
                val scope = host.lifecycleScope
                host.promptUnlock(
                    onSuccess = {
                        scope.launch {
                            val saved = runCatching {
                                vm.vaultRepo.saveCard(
                                    accountId = person.accountId,
                                    panAscii = pan,
                                    expiryMonth = expiryMonth,
                                    expiryYear = expiryYear,
                                    holderName = holder,
                                    rememberCvv = rememberCvv && card?.cvvCipherId != null,
                                    cvvAscii = null,
                                    existingId = card?.id,
                                    personId = person.id,
                                )
                            }.getOrElse {
                                errorText = WriteFailures.map(it)
                                return@launch
                            }
                            saved.onSuccess { onBack() }.onFailure { errorText = WriteFailures.ERR_GENERIC }
                        }
                    },
                    onCancel = {},
                    onError = { errorText = WriteFailures.ERR_BIOMETRIC },
                )
            }
        },
    )
}

@Composable
fun PersonCardForm(
    title: String,
    personName: String,
    pan: String,
    onPanChange: (String) -> Unit,
    bankLabel: String?,
    logoName: String,
    expiryMonth: Int,
    expiryYear: Int,
    onExpiryChange: (Int, Int) -> Unit,
    holder: String,
    onHolderChange: (String) -> Unit,
    cvv: String,
    onCvvChange: (String) -> Unit,
    rememberCvv: Boolean,
    onRememberCvv: (Boolean) -> Unit,
    editing: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    val groupedPan = PersianDigits.toPersian(
        CardMath.normalizeDigits(pan).chunked(4).joinToString(" "),
    )
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            Text(
                stringResource(R.string.card_for_person, personName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FinanceCard {
                FinanceTextField(
                    groupedPan,
                    onPanChange,
                    label = stringResource(R.string.card_number),
                    keyboardType = KeyboardType.Number,
                    leadingIcon = { BankLogo(logoName) },
                    supportingText = if (editing) stringResource(R.string.pan_keep_hint) else null,
                )
                if (bankLabel != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BankLogo(logoName)
                        Text(bankLabel, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                ExpiryMonthYearPicker(expiryMonth, expiryYear, onExpiryChange)
                FinanceTextField(
                    holder,
                    onHolderChange,
                    label = stringResource(R.string.holder_optional),
                )
                FinanceTextField(
                    cvv,
                    onCvvChange,
                    label = stringResource(R.string.cvv),
                    keyboardType = KeyboardType.NumberPassword,
                    supportingText = if (editing && rememberCvv) stringResource(R.string.cvv_keep_hint) else null,
                )
                SwitchRow(stringResource(R.string.remember_cvv), rememberCvv, onRememberCvv)
                Text(stringResource(R.string.cvv_warning), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            PrimaryWideButton(stringResource(R.string.save), onClick = onSave)
        }
    }
}

@Composable
fun PersonIbanScreen(
    state: AppUiState,
    vm: AppViewModel,
    personId: String,
    bankAccountId: String?,
    onSecure: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    DisposableEffect(Unit) {
        onSecure(true)
        onDispose { onSecure(false) }
    }
    val person = state.people.firstOrNull { it.id == personId }
    var existing by remember { mutableStateOf<BankAccount?>(null) }
    var resolved by remember(bankAccountId) { mutableStateOf(bankAccountId == null) }
    LaunchedEffect(bankAccountId) {
        if (bankAccountId == null) {
            existing = null
            resolved = true
            return@LaunchedEffect
        }
        val all = vm.vaultRepo.bankAccountsFlow.first()
        existing = all.firstOrNull { it.id == bankAccountId }
        resolved = true
    }
    if (person == null) {
        MissingRecord(stringResource(R.string.person_missing), onBack)
        return
    }
    if (!resolved) {
        LinearProgressIndicator(Modifier.padding(16.dp))
        return
    }
    val row = existing
    if (bankAccountId != null && row == null) {
        MissingRecord(stringResource(R.string.iban_missing), onBack)
        return
    }
    var accountNumber by remember(row?.id) { mutableStateOf(row?.accountNumber.orEmpty()) }
    var iban by remember(row?.id) { mutableStateOf(row?.iban.orEmpty()) }
    var fieldError by remember(row?.id) { mutableStateOf<VaultFieldError?>(null) }
    var errorText by remember(row?.id) { mutableStateOf<String?>(null) }
    val activity = LocalContext.current.findMainActivity()
    val compact = IbanMath.normalize(iban)
    val match = if (compact.length >= 7) vm.vaultRepo.directory.resolveIban(iban) else null
    val bank = (match as? BankMatch.Known)?.bank
    val shownError = fieldError?.let { vaultErrorText(it) } ?: errorText
    PersonIbanForm(
        title = stringResource(if (row == null) R.string.add_iban else R.string.edit_iban),
        personName = person.displayName,
        accountNumber = accountNumber,
        onAccountNumber = { accountNumber = it; fieldError = null; errorText = null },
        iban = iban,
        onIban = { iban = it; fieldError = null; errorText = null },
        bankLabel = when {
            bank != null -> bank.nameFa
            compact.length >= 7 -> stringResource(R.string.unknown_bank)
            else -> null
        },
        logoName = bank?.logoDrawable ?: "bank_unknown",
        error = shownError,
        onBack = onBack,
        onSave = save@{
            val problem = VaultInput.validateBankAccount(accountNumber, iban)
            if (problem != null) {
                fieldError = problem
                errorText = null
                return@save
            }
            val host = activity ?: run {
                errorText = WriteFailures.ERR_NO_ACTIVITY
                return@save
            }
            if (person.accountId.isBlank()) {
                errorText = WriteFailures.ERR_GENERIC
                return@save
            }
            fieldError = null
            errorText = null
            host.lifecycleScope.launch {
                val saved = runCatching {
                    vm.vaultRepo.saveBankAccount(
                        accountId = person.accountId,
                        accountNumber = accountNumber,
                        ibanRaw = iban,
                        existing = row,
                        personId = person.id,
                    )
                }.getOrElse {
                    errorText = WriteFailures.map(it)
                    return@launch
                }
                saved.onSuccess { onBack() }.onFailure { errorText = WriteFailures.ERR_GENERIC }
            }
        },
    )
}

@Composable
fun PersonIbanForm(
    title: String,
    personName: String,
    accountNumber: String,
    onAccountNumber: (String) -> Unit,
    iban: String,
    onIban: (String) -> Unit,
    bankLabel: String?,
    logoName: String,
    error: String?,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
            Text(
                stringResource(R.string.iban_for_person, personName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FinanceCard {
                FinanceTextField(
                    accountNumber,
                    onAccountNumber,
                    label = stringResource(R.string.account_number),
                    keyboardType = KeyboardType.Number,
                    supportingText = stringResource(R.string.account_number_hint),
                )
                FinanceTextField(
                    PersianDigits.toPersian(IbanMath.formatGrouped(iban)),
                    onIban,
                    label = stringResource(R.string.iban),
                )
                if (bankLabel != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BankLogo(logoName)
                        Text(bankLabel, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            PrimaryWideButton(stringResource(R.string.save_iban), onClick = onSave)
        }
    }
}

@Composable
private fun MissingRecord(message: String, onBack: () -> Unit) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FinanceEmptyState(SymbolIcons.Card, message, stringResource(R.string.empty_people_body))
        SecondaryWideButton(stringResource(R.string.close), onBack)
    }
}

@Preview(showBackground = true, locale = "fa", name = "Person card form", widthDp = 390, heightDp = 820)
@Composable
fun PreviewPersonCardForm() {
    HesabresTheme {
        PersonCardForm(
            title = stringResource(R.string.add_card),
            personName = "علی رضایی",
            pan = "6104337812345678",
            onPanChange = {},
            bankLabel = "بانک ملت",
            logoName = "bank_mellat",
            expiryMonth = 6,
            expiryYear = 1408,
            onExpiryChange = { _, _ -> },
            holder = "علی رضایی",
            onHolderChange = {},
            cvv = "",
            onCvvChange = {},
            rememberCvv = false,
            onRememberCvv = {},
            editing = false,
            error = null,
            onBack = {},
            onSave = {},
        )
    }
}

@Preview(showBackground = true, locale = "fa", name = "Person IBAN form", widthDp = 390, heightDp = 820)
@Composable
fun PreviewPersonIbanForm() {
    HesabresTheme {
        PersonIbanForm(
            title = stringResource(R.string.add_iban),
            personName = "علی رضایی",
            accountNumber = "1234567890123",
            onAccountNumber = {},
            iban = "IR120170000000123456789012",
            onIban = {},
            bankLabel = "بانک ملی ایران",
            logoName = "bank_melli",
            error = null,
            onBack = {},
            onSave = {},
        )
    }
}
