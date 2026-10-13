package ir.mhajisoft.hesabres.diag

import java.io.PrintWriter
import java.io.StringWriter

object CrashText {
    fun format(
        timestamp: String,
        threadName: String,
        error: Throwable,
        breadcrumbs: List<String>,
    ): String = buildString {
        appendLine("----- crash $timestamp thread=$threadName -----")
        if (breadcrumbs.isNotEmpty()) {
            appendLine("breadcrumbs:")
            breadcrumbs.forEach { appendLine("  $it") }
        }
        appendLine(stackTrace(error))
        appendLine("----- end crash -----")
    }

    fun caught(timestamp: String, where: String, error: Throwable): String = buildString {
        appendLine("----- caught $timestamp where=$where -----")
        appendLine(stackTrace(error))
        appendLine("----- end caught -----")
    }

    fun stackTrace(error: Throwable): String {
        val writer = StringWriter()
        error.printStackTrace(PrintWriter(writer))
        return writer.toString()
    }
}

class BreadcrumbRing(private val capacity: Int = 200) {
    private val items = ArrayDeque<String>()

    fun add(line: String) {
        synchronized(items) {
            items.addLast(line)
            while (items.size > capacity) items.removeFirst()
        }
    }

    fun snapshot(): List<String> = synchronized(items) { items.toList() }

    fun clear() {
        synchronized(items) { items.clear() }
    }
}
