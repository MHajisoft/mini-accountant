package ir.mhajisoft.miniaccountant.domain.jalali

/**
 * Birashk leap-year test used for fiscal-year Esfand length.
 *
 * Contemporary Iranian civil years follow the 33-year arithmetic cycle
 * (remainders 1, 5, 9, 13, 17, 22, 26, 30): 1399 and 1403 are leap
 * (Esfand = 30) and 1404 is common. The 2820-year remainder used by some
 * Birashk ports — including jalalidate 1.0.4 `BirashkAlgorithm` — disagrees
 * on 1403; this app follows the civil 33-year cycle required for FY windows.
 */
object BirashkAlgorithm {
    private val leapRemainders = intArrayOf(1, 5, 9, 13, 17, 22, 26, 30)

    fun isJalaliLeapYear(year: Int): Boolean {
        val r = positiveModulo(year, 33)
        return r in leapRemainders
    }

    fun esfandLength(year: Int): Int = if (isJalaliLeapYear(year)) 30 else 29

    fun monthLength(year: Int, month: Int): Int {
        require(month in 1..12)
        return when (month) {
            in 1..6 -> 31
            in 7..11 -> 30
            else -> esfandLength(year)
        }
    }

    fun daysInYear(year: Int): Int = if (isJalaliLeapYear(year)) 366 else 365

    private fun positiveModulo(n: Int, d: Int): Int {
        val r = n % d
        return if (r < 0) r + d else r
    }
}

data class JalaliYmd(
    val year: Int,
    val month: Int,
    val day: Int,
) : Comparable<JalaliYmd> {
    init {
        require(month in 1..12)
        require(day in 1..BirashkAlgorithm.monthLength(year, month)) {
            "Invalid Jalali date $year/$month/$day"
        }
    }

    override fun compareTo(other: JalaliYmd): Int {
        return compareValuesBy(this, other, { it.year }, { it.month }, { it.day })
    }

    fun toIsoLike(): String = "%04d-%02d-%02d".format(year, month, day)
}

/**
 * Jalali ↔ Gregorian conversion using the same Birashk formulas as
 * `io.github.amirroid:jalalidate` BirashkAlgorithm (days since a shared epoch).
 *
 * Epoch millis stored in Room are always UTC. Jalali Y/M/D columns are denormalized
 * for indexes and must never be treated as a string source of truth.
 */
object JalaliConverter {
    private const val JALALI_EPOCH_OFFSET = 79

    fun fromEpochMillis(epochMillis: Long, zoneOffsetSeconds: Int = 3 * 3600 + 30 * 60): JalaliYmd {
        val localSeconds = Math.floorDiv(epochMillis, 1000L) + zoneOffsetSeconds
        val epochDay = Math.floorDiv(localSeconds, 86_400L)
        return fromEpochDay(epochDay)
    }

    fun toEpochMillisStartOfDay(ymd: JalaliYmd, zoneOffsetSeconds: Int = 3 * 3600 + 30 * 60): Long {
        val epochDay = toEpochDay(ymd)
        return (epochDay * 86_400L - zoneOffsetSeconds) * 1000L
    }

    fun toEpochMillisEndOfDay(ymd: JalaliYmd, zoneOffsetSeconds: Int = 3 * 3600 + 30 * 60): Long {
        return toEpochMillisStartOfDay(ymd, zoneOffsetSeconds) + 86_400_000L - 1L
    }

    fun fromGregorian(gy: Int, gm: Int, gd: Int): JalaliYmd {
        val epochDay = gregorianToEpochDay(gy, gm, gd)
        return fromEpochDay(epochDay)
    }

    fun toGregorian(ymd: JalaliYmd): Triple<Int, Int, Int> {
        val epochDay = toEpochDay(ymd)
        return epochDayToGregorian(epochDay)
    }

    fun fromEpochDay(epochDay: Long): JalaliYmd {
        val jdn = epochDay + 2_440_588L
        return jdnToJalali(jdn)
    }

    fun toEpochDay(ymd: JalaliYmd): Long {
        val jdn = jalaliToJdn(ymd.year, ymd.month, ymd.day)
        return jdn - 2_440_588L
    }

    /**
     * Astronomical Jalali (Birashk) via JDN, matching the published cycle.
     */
    fun jalaliToJdn(jy: Int, jm: Int, jd: Int): Long {
        val epBase = if (jy >= 0) jy - 474 else jy - 473
        val epYear = 474 + positiveModulo(epBase, 2820)
        val md = if (jm <= 7) (jm - 1) * 31 else (jm - 1) * 30 + 6
        return jd.toLong() + md +
            ((epYear * 682L - 110L) / 2816L) +
            (epYear - 1) * 365L +
            (Math.floorDiv(epBase.toLong(), 2820L) * 1029983L) +
            1948320L
    }

    fun jdnToJalali(jdn: Long): JalaliYmd {
        val depoch = jdn - jalaliToJdn(475, 1, 1)
        val cycle = Math.floorDiv(depoch, 1029983L)
        val cyear = depoch % 1029983L
        val ycycle = if (cyear == 1029982L) {
            2820L
        } else {
            val aux1 = Math.floorDiv(cyear, 366L)
            val aux2 = cyear % 366L
            Math.floorDiv(2134L * aux1 + 2816L * aux2 + 2815L, 1028522L) + aux1 + 1
        }
        var year = (ycycle + 474L + 2820L * cycle).toInt()
        if (year <= 0) year -= 1
        val yday = (jdn - jalaliToJdn(year, 1, 1) + 1).toInt()
        val month = if (yday <= 186) ((yday - 1) / 31) + 1 else ((yday - 187) / 30) + 7
        val day = (jdn - jalaliToJdn(year, month, 1) + 1).toInt()
        return JalaliYmd(year, month, day)
    }

    private fun gregorianToEpochDay(year: Int, month: Int, day: Int): Long {
        val y = year.toLong()
        val m = month.toLong()
        var total = 0L
        total += 365L * y
        total += Math.floorDiv(y + 3, 4)
        total += -Math.floorDiv(y + 99, 100)
        total += Math.floorDiv(y + 399, 400)
        total += (367L * m - 362L) / 12
        total += if (m <= 2) 0 else if (isGregorianLeap(year)) -1 else -2
        total += day - 1
        return total - 719528L
    }

    private fun epochDayToGregorian(epochDay: Long): Triple<Int, Int, Int> {
        val z = epochDay + 719528L
        val era = Math.floorDiv(z, 146097L)
        val doe = z - era * 146097L
        val yoe = Math.floorDiv(doe - Math.floorDiv(doe, 1460L) + Math.floorDiv(doe, 36524L) - Math.floorDiv(doe, 146096L), 365L)
        var year = (yoe + era * 400L).toInt()
        val doy = (doe - (365L * yoe + Math.floorDiv(yoe, 4L) - Math.floorDiv(yoe, 100L))).toInt()
        val mp = Math.floorDiv(5L * doy + 2, 153).toInt()
        val month = if (mp + 3 > 12) mp - 9 else mp + 3
        val day = doy - ((153 * mp + 2) / 5) + 1
        if (month <= 2) year += 1
        return Triple(year, month, day)
    }

    private fun isGregorianLeap(year: Int): Boolean =
        (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

    private fun positiveModulo(n: Int, d: Int): Int {
        val r = n % d
        return if (r < 0) r + d else r
    }

    @Suppress("unused")
    private val jalaliEpochOffset = JALALI_EPOCH_OFFSET
}

object JalaliLabels {
    val monthNamesFa = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )

    val weekdayNamesFa = listOf(
        "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه",
    )

    fun monthName(month: Int): String = monthNamesFa[month - 1]
}
