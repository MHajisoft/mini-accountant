package ir.mhajisoft.hesabres.domain.money

/**
 * In-app amount keypad. The expression is stored in ASCII and shown with Persian digits.
 * × and ÷ bind tighter than + and −. Division truncates toward zero. A negative or
 * divide-by-zero result is not a payable amount.
 */
object AmountKeypad {
    const val BACKSPACE = "⌫"
    const val CLEAR = "C"

    fun press(expression: String, token: String): String {
        val current = expression.trim()
        return when (token) {
            BACKSPACE -> current.dropLast(1)
            CLEAR -> ""
            "+", "-", "×", "÷" -> appendOperator(current, token)
            "." -> appendDot(current)
            else -> {
                val digit = PersianDigits.toAscii(token)
                if (digit.length != 1 || digit[0] !in '0'..'9') current
                else appendDigit(current, digit[0])
            }
        }
    }

    fun evaluate(expression: String): Long? {
        val ascii = PersianDigits.toAscii(expression).replace(" ", "").replace("٬", "").replace(",", "")
        if (ascii.isEmpty()) return null
        if (ascii.last() in "+-×÷.") return null
        val tokens = tokenize(ascii) ?: return null
        if (tokens.isEmpty()) return null
        return eval(tokens)?.takeIf { it >= 0L }
    }

    /** Display string passed to [Money.parseDisplayAmount]. Null until the expression is a valid amount. */
    fun amountText(expression: String): String? = evaluate(expression)?.toString()

    private fun appendDigit(current: String, digit: Char): String {
        if (current.isEmpty()) return digit.toString()
        val last = current.last()
        if (last in "+-×÷") return current + digit
        val number = trailingNumber(current)
        if (number == "0" && !number.contains('.')) return current.dropLast(1) + digit
        if (number.length >= 15) return current
        return current + digit
    }

    private fun appendDot(current: String): String {
        if (current.isEmpty() || current.last() in "+-×÷") return current + "0."
        val number = trailingNumber(current)
        if (number.contains('.')) return current
        return current + "."
    }

    private fun appendOperator(current: String, op: String): String {
        if (current.isEmpty()) return current
        val last = current.last()
        if (last in "+-×÷") return current.dropLast(1) + op
        if (last == '.') return current.dropLast(1) + op
        return current + op
    }

    private fun trailingNumber(current: String): String {
        val cut = current.indexOfLast { it in "+-×÷" }
        return if (cut < 0) current else current.substring(cut + 1)
    }

    private sealed interface Tok {
        data class Num(val value: Long) : Tok
        data class Op(val symbol: Char) : Tok
    }

    private fun tokenize(ascii: String): List<Tok>? {
        val out = ArrayList<Tok>()
        var i = 0
        while (i < ascii.length) {
            val ch = ascii[i]
            if (ch in "+-×÷") {
                if (out.isEmpty() || out.last() is Tok.Op) return null
                out += Tok.Op(ch)
                i++
                continue
            }
            if (ch !in '0'..'9') return null
            val start = i
            while (i < ascii.length && ascii[i] in '0'..'9') i++
            if (i < ascii.length && ascii[i] == '.') {
                i++
                while (i < ascii.length && ascii[i] in '0'..'9') i++
            }
            val raw = ascii.substring(start, i)
            val whole = raw.substringBefore('.')
            out += Tok.Num(whole.toLongOrNull() ?: return null)
        }
        return out
    }

    private fun eval(tokens: List<Tok>): Long? {
        val collapsed = ArrayList<Tok>()
        var index = 0
        while (index < tokens.size) {
            val tok = tokens[index]
            if (tok is Tok.Op && (tok.symbol == '×' || tok.symbol == '÷')) {
                val left = (collapsed.removeLastOrNull() as? Tok.Num)?.value ?: return null
                val right = (tokens.getOrNull(index + 1) as? Tok.Num)?.value ?: return null
                val value = when (tok.symbol) {
                    '×' -> left * right
                    else -> if (right == 0L) return null else left / right
                }
                collapsed += Tok.Num(value)
                index += 2
            } else {
                collapsed += tok
                index++
            }
        }
        var acc = (collapsed.firstOrNull() as? Tok.Num)?.value ?: return null
        var i = 1
        while (i < collapsed.size) {
            val op = collapsed[i] as? Tok.Op ?: return null
            val right = (collapsed.getOrNull(i + 1) as? Tok.Num)?.value ?: return null
            acc = when (op.symbol) {
                '+' -> acc + right
                '-' -> acc - right
                else -> return null
            }
            i += 2
        }
        return acc
    }
}
