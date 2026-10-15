package ir.mhajisoft.hesabres.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.mhajisoft.hesabres.domain.jalali.JalaliCalendar
import ir.mhajisoft.hesabres.domain.jalali.JalaliLabels
import ir.mhajisoft.hesabres.domain.jalali.JalaliYmd
import ir.mhajisoft.hesabres.domain.money.AmountKeypad
import ir.mhajisoft.hesabres.domain.money.Money
import ir.mhajisoft.hesabres.domain.money.PersianDigits
import ir.mhajisoft.hesabres.domain.planner.ListOrder
import ir.mhajisoft.hesabres.domain.report.MonthBar
import ir.mhajisoft.hesabres.ui.theme.PoolakiBarMuted
import ir.mhajisoft.hesabres.ui.theme.PoolakiChip
import ir.mhajisoft.hesabres.ui.theme.PoolakiIndigo
import ir.mhajisoft.hesabres.ui.theme.PoolakiKeypad
import ir.mhajisoft.hesabres.ui.theme.PoolakiMuted
import ir.mhajisoft.hesabres.ui.theme.PoolakiNegative
import ir.mhajisoft.hesabres.ui.theme.PoolakiPeach
import ir.mhajisoft.hesabres.ui.theme.SymbolIcons

fun formatSignedAmount(rials: Long, toman: Boolean): String {
    val negative = rials < 0
    val grouped = PersianDigits.formatGrouped(Money.toDisplayUnit(kotlin.math.abs(rials), toman))
        .removePrefix("−")
    val number = if (negative) "—$grouped" else grouped
    val unit = if (toman) "تومان" else "ریال"
    return "$number $unit"
}

@Composable
fun <T> SlideSwap(
    key: T,
    forward: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = key,
        modifier = modifier,
        transitionSpec = {
            val dir = if (forward) 1 else -1
            (slideInHorizontally(tween(280)) { it / 3 * dir } + fadeIn(tween(220))) togetherWith
                (slideOutHorizontally(tween(220)) { -it / 4 * dir } + fadeOut(tween(160)))
        },
        label = "slide",
    ) { target -> content(target) }
}

@Composable
fun Appear(visible: Boolean = true, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(320)) + slideInHorizontally(tween(320)) { it / 8 },
        exit = fadeOut(tween(180)),
    ) { content() }
}

@Composable
fun PeachToggle(leftSelected: Boolean, left: String, right: String, onLeft: () -> Unit, onRight: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, PoolakiBarMuted, RoundedCornerShape(18.dp))
            .padding(4.dp),
    ) {
        ToggleHalf(left, leftSelected, onLeft, Modifier.weight(1f))
        ToggleHalf(right, !leftSelected, onRight, Modifier.weight(1f))
    }
}

@Composable
private fun ToggleHalf(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val bg by animateColorAsState(if (selected) PoolakiPeach else Color.Transparent, tween(220), label = "seg")
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) Color(0xFF5C4632) else PoolakiMuted,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@Composable
fun MonthBars(bars: List<MonthBar>) {
    val max = (bars.maxOfOrNull { it.amount } ?: 1L).coerceAtLeast(1L)
    Row(
        Modifier.fillMaxWidth().height(148.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEach { bar ->
            val target = (bar.amount.toFloat() / max.toFloat()).coerceIn(0.04f, 1f)
            val fraction by animateFloatAsState(if (bar.amount == 0L) 0.04f else target, tween(480), label = "bar")
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    Modifier
                        .width(64.dp)
                        .fillMaxHeight(fraction)
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                        .background(if (bar.current) PoolakiPeach else PoolakiBarMuted),
                )
                Text(
                    bar.label,
                    modifier = Modifier.padding(top = 8.dp),
                    color = PoolakiMuted,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
fun ShareTrack(fraction: Float) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(420), label = "share")
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(PoolakiBarMuted),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .background(PoolakiIndigo),
        )
    }
}

@Composable
fun AmountKeypadPad(onToken: (String) -> Unit) {
    val view = LocalView.current
    val rows = listOf(
        listOf("1", "2", "3", "+"),
        listOf("4", "5", "6", "-"),
        listOf("7", "8", "9", "×"),
        listOf(".", AmountKeypad.BACKSPACE, "0", "÷"),
    )
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { token ->
                    val op = token in listOf("+", "-", "×", "÷")
                    Box(
                        Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (op) PoolakiKeypad else Color.White)
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onToken(token)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (token[0].isDigit() || token == ".") PersianDigits.toPersian(token) else token,
                            color = if (op) Color.White else Color(0xFF222222),
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JalaliDateSheet(
    value: JalaliYmd,
    today: JalaliYmd,
    onSelect: (JalaliYmd) -> Unit,
    onDismiss: () -> Unit,
) {
    var year by remember(value) { mutableIntStateOf(value.year) }
    var month by remember(value) { mutableIntStateOf(value.month) }
    var forward by remember { mutableStateOf(true) }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)).clickable { onDismiss() }) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color.White)
                .clickable {}
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(42.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(PoolakiBarMuted),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(PoolakiChip)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(PersianDigits.toPersian(year.toString()), fontWeight = FontWeight.Bold, color = PoolakiIndigo)
                }
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    DayArrow("‹") { forward = false; val shifted = JalaliCalendar.shiftMonth(year, month, -1); year = shifted.first; month = shifted.second }
                    Text(
                        JalaliLabels.monthName(month),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    DayArrow("›") { forward = true; val shifted = JalaliCalendar.shiftMonth(year, month, 1); year = shifted.first; month = shifted.second }
                }
            }
            SlideSwap(key = year to month, forward = forward) { shown ->
                val shownYear = shown.first
                val shownMonth = shown.second
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        listOf("ش", "ی", "د", "س", "چ", "پ", "ج").forEach { label ->
                            Text(
                                label,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                color = PoolakiMuted,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                    val cells = JalaliCalendar.monthGrid(shownYear, shownMonth)
                    cells.chunked(7).forEach { week ->
                        Row(Modifier.fillMaxWidth()) {
                            week.forEach { cell ->
                                val day = cell.day
                                Box(Modifier.weight(1f).height(42.dp), contentAlignment = Alignment.Center) {
                                    if (day != null) {
                                        val ymd = JalaliYmd(shownYear, shownMonth, day)
                                        val selected = ymd == value
                                        val future = JalaliCalendar.isAfter(ymd, today)
                                        Box(
                                            Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (selected) PoolakiIndigo else Color.Transparent)
                                                .clickable(enabled = !future) { onSelect(ymd) },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                PersianDigits.toPersian(day.toString()),
                                                color = when {
                                                    selected -> Color.White
                                                    future -> PoolakiMuted.copy(alpha = 0.45f)
                                                    else -> Color(0xFF222222)
                                                },
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArrowButton(onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(PoolakiChip)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(SymbolIcons.Chevron, null, tint = PoolakiIndigo, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun DayArrow(symbol: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = PoolakiMuted, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
fun LavenderPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(PoolakiChip)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text, color = PoolakiIndigo, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun RequiredDot() {
    Box(Modifier.size(8.dp).clip(CircleShape).background(PoolakiNegative))
}

@Composable
fun <T> ReorderColumn(
    items: List<T>,
    key: (T) -> String,
    onCommit: (List<T>) -> Unit,
    row: @Composable (item: T, handleModifier: Modifier) -> Unit,
) {
    val identity = items.joinToString("|", transform = key)
    var local by remember(identity) { mutableStateOf(items) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        local.forEach { item ->
            val itemKey = key(item)
            val handle = Modifier.pointerInput(itemKey) {
                var accum = 0f
                detectVerticalDragGestures(
                    onDragStart = { accum = 0f },
                    onDragEnd = { onCommit(local) },
                    onDragCancel = { onCommit(local) },
                    onVerticalDrag = { _, dy ->
                        accum += dy
                        val from = local.indexOfFirst { key(it) == itemKey }
                        if (from < 0) return@detectVerticalDragGestures
                        val step = when {
                            accum > 48f -> 1
                            accum < -48f -> -1
                            else -> 0
                        }
                        if (step != 0) {
                            val to = (from + step).coerceIn(0, local.lastIndex)
                            if (to != from) local = ListOrder.move(local, from, to)
                            accum = 0f
                        }
                    },
                )
            }
            row(item, handle)
        }
    }
}

@Composable
fun HorizontalChipRow(content: @Composable () -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = { content() },
    )
}

fun dayTitle(ymd: JalaliYmd, today: JalaliYmd): String {
    val date = "${JalaliCalendar.weekdayName(ymd)}، ${PersianDigits.toPersian(ymd.day.toString())} ${JalaliLabels.monthName(ymd.month)}"
    return if (ymd == today) "(امروز) $date" else date
}
