package ir.mhajisoft.hesabres.domain.jalali

/**
 * Saturday-first civil calendar used by the date sheet and day/month arrows.
 * Weekday index 0 is شنبه. Unix epoch day 0 is Thursday, which is پنجشنبه (index 5).
 */
object JalaliCalendar {
    const val SATURDAY = 0

    fun weekdayIndex(ymd: JalaliYmd): Int {
        val epochDay = JalaliConverter.toEpochDay(ymd)
        return Math.floorMod(epochDay + 5L, 7L).toInt()
    }

    fun weekdayName(ymd: JalaliYmd): String = JalaliLabels.weekdayNamesFa[weekdayIndex(ymd)]

    fun shiftDay(ymd: JalaliYmd, delta: Int): JalaliYmd {
        if (delta == 0) return ymd
        return JalaliConverter.fromEpochDay(JalaliConverter.toEpochDay(ymd) + delta)
    }

    fun shiftMonth(year: Int, month: Int, delta: Int): Pair<Int, Int> {
        val index = year * 12 + (month - 1) + delta
        val y = Math.floorDiv(index, 12)
        val m = Math.floorMod(index, 12) + 1
        return y to m
    }

    fun clampDay(year: Int, month: Int, day: Int): JalaliYmd {
        val length = BirashkAlgorithm.monthLength(year, month)
        return JalaliYmd(year, month, day.coerceIn(1, length))
    }

    data class Cell(val day: Int?)

    /** Six rows are not required; trailing empty weeks are omitted. Leading blanks pad to Saturday. */
    fun monthGrid(year: Int, month: Int): List<Cell> {
        val length = BirashkAlgorithm.monthLength(year, month)
        val lead = weekdayIndex(JalaliYmd(year, month, 1))
        val cells = ArrayList<Cell>(lead + length)
        repeat(lead) { cells += Cell(null) }
        for (day in 1..length) cells += Cell(day)
        while (cells.size % 7 != 0) cells += Cell(null)
        return cells
    }

    fun isAfter(ymd: JalaliYmd, today: JalaliYmd): Boolean = ymd > today
}
