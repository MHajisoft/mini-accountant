package ir.mhajisoft.miniaccountant.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.mhajisoft.miniaccountant.domain.jalali.BirashkAlgorithm
import ir.mhajisoft.miniaccountant.domain.jalali.JalaliConverter
import ir.mhajisoft.miniaccountant.domain.jalali.JalaliLabels
import ir.mhajisoft.miniaccountant.domain.jalali.JalaliYmd
import ir.mhajisoft.miniaccountant.domain.money.PersianDigits

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
            TextButton(onClick = { onConfirm(JalaliYmd(year, month, day)) }) { Text("تأیید") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        },
        title = { Text("انتخاب تاریخ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { year -= 1 }) { Text("−") }
                    Text(PersianDigits.toPersian(year.toString()), modifier = Modifier.padding(top = 12.dp))
                    TextButton(onClick = { year += 1 }) { Text("+") }
                }
                LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxWidth()) {
                    items(12) { i ->
                        val m = i + 1
                        FilterChip(
                            selected = month == m,
                            onClick = { month = m },
                            label = { Text(JalaliLabels.monthName(m)) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(maxDay.coerceAtMost(10)) { }
                }
                LazyVerticalGrid(columns = GridCells.Adaptive(40.dp)) {
                    items(maxDay) { i ->
                        val d = i + 1
                        FilterChip(
                            selected = day == d,
                            onClick = { day = d },
                            label = { Text(PersianDigits.toPersian(d.toString())) },
                        )
                    }
                }
            }
        },
    )
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
