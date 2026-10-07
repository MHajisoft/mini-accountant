package ir.mhajisoft.hesabres.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ir.mhajisoft.hesabres.R
import ir.mhajisoft.hesabres.domain.jalali.BirashkAlgorithm
import ir.mhajisoft.hesabres.domain.jalali.JalaliConverter
import ir.mhajisoft.hesabres.domain.jalali.JalaliLabels
import ir.mhajisoft.hesabres.domain.jalali.JalaliYmd
import ir.mhajisoft.hesabres.domain.money.PersianDigits

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JalaliDatePickerDialog(
    initial: JalaliYmd,
    onDismiss: () -> Unit,
    onConfirm: (JalaliYmd) -> Unit,
) {
    var year by remember { mutableIntStateOf(initial.year) }
    var month by remember { mutableIntStateOf(initial.month) }
    var day by remember { mutableIntStateOf(initial.day) }
    val maxDay = BirashkAlgorithm.monthLength(year, month)
    if (day > maxDay) day = maxDay
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(JalaliYmd(year, month, day.coerceAtMost(maxDay))) }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
        title = { Text(stringResource(R.string.pick_date)) },
        text = {
            Column(
                Modifier.height(360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { year -= 1 }) { Text("−") }
                    Text(PersianDigits.toPersian(year.toString()))
                    TextButton(onClick = { year += 1 }) { Text("+") }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..12).forEach { m ->
                        ChoiceChip(month == m, { month = m }, JalaliLabels.monthName(m))
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..maxDay).forEach { d ->
                        ChoiceChip(day == d, { day = d }, PersianDigits.toPersian(d.toString()))
                    }
                }
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpiryMonthYearPicker(
    month: Int,
    year: Int,
    onChange: (Int, Int) -> Unit,
) {
    val today = remember { JalaliConverter.fromEpochMillis(System.currentTimeMillis()) }
    val years = remember(today.year) { (today.year..(today.year + 10)).toList() }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(stringResource(R.string.expiry))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..12).forEach { m ->
                ChoiceChip(month == m, { onChange(m, year) }, JalaliLabels.monthName(m))
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            years.forEach { y ->
                ChoiceChip(year == y, { onChange(month, y) }, PersianDigits.toPersian(y.toString()))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JalaliYmdEditor(
    initial: JalaliYmd,
    onChange: (JalaliYmd) -> Unit,
) {
    var year by remember { mutableIntStateOf(initial.year) }
    var month by remember { mutableIntStateOf(initial.month.coerceIn(1, 12)) }
    var day by remember { mutableIntStateOf(initial.day) }
    val maxDay = BirashkAlgorithm.monthLength(year, month)
    val safeDay = day.coerceIn(1, maxDay)
    fun emit(y: Int, m: Int, d: Int) {
        val mm = m.coerceIn(1, 12)
        val capped = d.coerceIn(1, BirashkAlgorithm.monthLength(y, mm))
        year = y
        month = mm
        day = capped
        onChange(JalaliYmd(y, mm, capped))
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { emit(year - 1, month, safeDay) }) { Text("−") }
            Text(PersianDigits.toPersian(year.toString()))
            TextButton(onClick = { emit(year + 1, month, safeDay) }) { Text("+") }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..12).forEach { m ->
                ChoiceChip(month == m, { emit(year, m, safeDay) }, JalaliLabels.monthName(m))
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..maxDay).forEach { d ->
                ChoiceChip(safeDay == d, { emit(year, month, d) }, PersianDigits.toPersian(d.toString()))
            }
        }
    }
}

fun formatJalali(epoch: Long): String {
    val ymd = JalaliConverter.fromEpochMillis(epoch)
    return PersianDigits.toPersian(
        "${ymd.year}/${ymd.month.toString().padStart(2, '0')}/${ymd.day.toString().padStart(2, '0')}",
    )
}

fun formatMoney(rials: Long, toman: Boolean): String {
    val unit = if (toman) rials / 10 else rials
    val suffix = if (toman) "تومان" else "ریال"
    return PersianDigits.formatGrouped(unit) + " " + suffix
}
