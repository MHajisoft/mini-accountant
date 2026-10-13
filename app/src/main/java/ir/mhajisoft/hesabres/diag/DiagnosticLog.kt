package ir.mhajisoft.hesabres.diag

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.Process
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import ir.mhajisoft.hesabres.BuildConfig
import ir.mhajisoft.hesabres.domain.crash.SwallowedErrors
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * On-device diagnostic log. Installed before Hilt so a crash inside
 * Application.onCreate still hits the handler. File IO stays off the main
 * thread except the synchronous crash write.
 */
object DiagnosticLog {
    const val TAG = "HesabresDiag"
    const val PUBLIC_FOLDER = PublicLogMirror.FOLDER_LABEL

    private val installed = AtomicBoolean(false)
    private val started = AtomicBoolean(false)
    private val headerWritten = AtomicBoolean(false)
    private val crashing = AtomicBoolean(false)
    private val ring = BreadcrumbRing(200)
    private val worker = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "hesabres-diag").apply { isDaemon = true }
    }
    private val pending = StringBuilder()
    private val gate = Any()
    private val ioLock = Any()
    private val recentCaught = ConcurrentHashMap<String, Long>()
    private val generation = AtomicInteger(0)
    private val publishing = ThreadLocal<Boolean>()

    @Volatile private var appContext: Context? = null
    @Volatile private var store: LogStore? = null
    @Volatile private var mirror: PublicLogMirror? = null
    @Volatile private var previousHandler: Thread.UncaughtExceptionHandler? = null

    private val sessionId: String = UUID.randomUUID().toString().take(8)
    private val iso: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun install(app: Application) {
        if (!installed.compareAndSet(false, true)) return
        appContext = app
        previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            if (!crashing.compareAndSet(false, true)) {
                previousHandler?.uncaughtException(thread, error)
                return@setDefaultUncaughtExceptionHandler
            }
            try {
                writeCrashSync(thread, error)
            } catch (_: Throwable) {
            }
            previousHandler?.uncaughtException(thread, error)
        }
        SwallowedErrors.sink = { where, error -> recordCaught(where, error) }
        app.registerActivityLifecycleCallbacks(lifecycleCallbacks)
    }

    fun start(app: Application) {
        if (!started.compareAndSet(false, true)) return
        appContext = app.applicationContext
        breadcrumb("app", "start")
        enqueue {
            try {
                recordPreviousExits()
                appendLogcat("launch")
            } catch (_: Throwable) {
            }
        }
        runCatching {
            ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    breadcrumb("process", "foreground")
                }

                override fun onStop(owner: LifecycleOwner) {
                    breadcrumb("process", "background")
                }
            })
        }
    }

    fun coroutineHandler(scope: String): CoroutineExceptionHandler =
        CoroutineExceptionHandler { _, error -> recordCaught("coroutine.$scope", error) }

    fun breadcrumb(area: String, message: String) {
        try {
            val line = LogRedactor.redact("${nowIso()} $area ${message.trim()}")
            ring.add(line)
            runCatching { Log.i(TAG, line) }
            val onMain = runCatching { Looper.myLooper() == Looper.getMainLooper() }.getOrDefault(true)
            if (onMain) {
                enqueueAppend(line + "\n")
            } else {
                appendSync(line + "\n")
            }
        } catch (_: Throwable) {
        }
    }

    fun recordCaught(where: String, error: Throwable) {
        try {
            val key = "$where:${error.javaClass.name}:${error.message?.take(120)}"
            val now = System.currentTimeMillis()
            val previous = recentCaught.put(key, now)
            if (previous != null && now - previous < 60_000) return
            if (recentCaught.size > 200) {
                recentCaught.entries.removeIf { now - it.value > 10 * 60_000 }
            }
            val text = LogRedactor.redact(CrashText.caught(nowIso(), where, error))
            runCatching { Log.w(TAG, text) }
            val onMain = runCatching { Looper.myLooper() == Looper.getMainLooper() }.getOrDefault(true)
            if (onMain) enqueueAppend(text) else appendSync(text)
        } catch (_: Throwable) {
        }
    }

    suspend fun shareIntent(context: Context): Intent? = withContext(Dispatchers.IO) {
        try {
            drainPendingSync()
            val files = storeOrNull()?.files().orEmpty().filter { it.length() > 0L }
            if (files.isEmpty()) return@withContext null
            val zip = File(context.cacheDir, "log-share/hesabres-logs.zip")
            zip.parentFile?.mkdirs()
            ZipOutputStream(FileOutputStream(zip)).use { zipOut ->
                files.forEach { file ->
                    zipOut.putNextEntry(ZipEntry(file.name))
                    file.inputStream().use { input -> input.copyTo(zipOut) }
                    zipOut.closeEntry()
                }
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zip)
            Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Hesabres logs")
                clipData = ClipData.newRawUri("Hesabres logs", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (t: Throwable) {
            recordCaught("share", t)
            null
        }
    }

    fun grantShareTargets(context: Context, intent: Intent) {
        val uri = intent.clipData?.getItemAt(0)?.uri ?: return
        grant(context, intent, uri)
    }

    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        try {
            generation.incrementAndGet()
            synchronized(gate) { pending.setLength(0) }
            synchronized(ioLock) {
                headerWritten.set(false)
                ring.clear()
                storeOrNull()?.clear()
                runCatching { mirrorOrNull()?.clear() }
                recentCaught.clear()
                ensureHeaderSync("cleared")
                publishActive()
            }
        } catch (_: Throwable) {
        }
    }

    fun publishActiveBestEffort() {
        enqueue {
            try {
                synchronized(ioLock) { publishActive() }
            } catch (_: Throwable) {
            }
        }
    }

    private fun writeCrashSync(thread: Thread, error: Throwable) {
        val crumbs = ring.snapshot()
        val body = LogRedactor.redact(
            CrashText.format(
                timestamp = nowIso(),
                threadName = thread.name,
                error = error,
                breadcrumbs = crumbs,
            ),
        )
        appendSync(body, respectGeneration = false)
        val logcat = runCatching { readLogcat(1_500) }.getOrNull()
        if (!logcat.isNullOrBlank()) {
            appendSync(
                LogRedactor.redact("----- logcat crash -----\n$logcat\n----- end logcat -----\n"),
                respectGeneration = false,
            )
        }
    }

    private fun enqueueAppend(text: String) {
        synchronized(gate) { pending.append(text) }
        enqueue { drainPendingSync() }
    }

    private fun enqueue(block: () -> Unit) {
        runCatching {
            worker.execute {
                try {
                    block()
                } catch (_: Throwable) {
                }
            }
        }
    }

    private fun drainPendingSync() {
        val chunk = synchronized(gate) {
            if (pending.isEmpty()) return
            val text = pending.toString()
            pending.setLength(0)
            text
        }
        appendSync(chunk)
    }

    private fun appendSync(text: String, respectGeneration: Boolean = true) {
        if (text.isEmpty()) return
        val gen = generation.get()
        synchronized(ioLock) {
            if (respectGeneration && gen != generation.get()) return
            ensureHeaderSync(null)
            storeOrNull()?.append(text)
            publishActive()
        }
    }

    private fun ensureHeaderSync(note: String?) {
        if (headerWritten.get()) return
        val target = storeOrNull() ?: return
        target.append(LogRedactor.redact(sessionHeader(note)))
        headerWritten.set(true)
    }

    private fun publishActive() {
        if (publishing.get() == true) return
        val file = store?.activeFile() ?: return
        if (!file.exists() || file.length() == 0L) return
        publishing.set(true)
        try {
            runCatching { mirrorOrNull()?.publish(file) }
                .onFailure { error ->
                    runCatching { Log.w(TAG, "public log failed: ${error.javaClass.simpleName}") }
                }
        } finally {
            publishing.set(false)
        }
    }

    private fun storeOrNull(): LogStore? {
        store?.let { return it }
        val context = appContext ?: return null
        return try {
            val dir = privateLogDir(context)
            LogStore(dir).also { store = it }
        } catch (_: Throwable) {
            null
        }
    }

    private fun mirrorOrNull(): PublicLogMirror? {
        mirror?.let { return it }
        val context = appContext ?: return null
        return PublicLogMirror(context).also { mirror = it }
    }

    @RequiresApi(30)
    private fun recordPreviousExits() {
        if (Build.VERSION.SDK_INT < 30) return
        val context = appContext ?: return
        val activityManager = context.getSystemService(ActivityManager::class.java) ?: return
        val cursorFile = File(context.filesDir, "diag-exit-cursor.txt")
        val seen = cursorFile.takeIf { it.exists() }
            ?.readLines()
            ?.mapNotNull { it.toLongOrNull() }
            ?.toMutableSet()
            ?: mutableSetOf()
        val infos = activityManager.getHistoricalProcessExitReasons(context.packageName, 0, 16)
        val fresh = infos.filter { it.timestamp !in seen }.sortedBy { it.timestamp }
        if (fresh.isEmpty()) return
        val block = buildString {
            fresh.forEach { info ->
                appendLine(
                    "----- previous exit ${iso(info.timestamp)} reason=${reasonName(info.reason)} " +
                        "(${info.reason}) status=${info.status} importance=${info.importance} " +
                        "pss=${info.pss} rss=${info.rss} desc=${info.description.orEmpty()} -----",
                )
                val wantsTrace = info.reason == ApplicationExitInfo.REASON_ANR ||
                    info.reason == ApplicationExitInfo.REASON_CRASH_NATIVE ||
                    info.reason == ApplicationExitInfo.REASON_CRASH ||
                    info.reason == ApplicationExitInfo.REASON_LOW_MEMORY
                if (wantsTrace) {
                    runCatching {
                        info.traceInputStream?.use { input ->
                            appendLine(readCapped(input, 64 * 1024).toString(Charsets.UTF_8))
                        }
                    }
                }
                seen += info.timestamp
            }
        }
        appendSync(LogRedactor.redact(block))
        cursorFile.writeText(seen.toList().sorted().takeLast(40).joinToString("\n"))
    }

    private fun appendLogcat(label: String) {
        val text = readLogcat(3_000)
        if (text.isBlank()) return
        appendSync(LogRedactor.redact("----- logcat $label -----\n$text\n----- end logcat -----\n"))
    }

    private fun readLogcat(timeoutMs: Long): String {
        val pid = Process.myPid().toString()
        val process = ProcessBuilder(
            "logcat",
            "-d",
            "-t",
            "400",
            "--pid",
            pid,
            "-v",
            "time",
        ).redirectErrorStream(true).start()
        val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
        if (!finished) {
            process.destroy()
            return ""
        }
        return process.inputStream.bufferedReader().use { it.readText() }.take(80_000)
    }

    private fun sessionHeader(note: String?): String {
        val context = appContext
        val runtime = Runtime.getRuntime()
        return buildString {
            appendLine("===== session $sessionId ${nowIso()} =====")
            if (note != null) appendLine("marker: $note")
            appendLine("version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("build: ${BuildConfig.BUILD_TYPE}")
            appendLine("package: ${context?.packageName.orEmpty()}")
            appendLine("device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("android: ${Build.VERSION.RELEASE} (sdk ${Build.VERSION.SDK_INT})")
            appendLine("locale: ${Locale.getDefault().toLanguageTag()}")
            appendLine(
                "memory: free ${runtime.freeMemory() / (1024 * 1024)} MB, " +
                    "max ${runtime.maxMemory() / (1024 * 1024)} MB",
            )
            appendLine("folder: $PUBLIC_FOLDER")
        }
    }

    private fun nowIso(): String = iso.format(Instant.now().atZone(ZoneId.systemDefault()))

    private fun iso(epochMillis: Long): String =
        iso.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

    private val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            breadcrumb("activity", "${activity.javaClass.simpleName} created restored=${savedInstanceState != null}")
        }

        override fun onActivityStarted(activity: Activity) {
            breadcrumb("activity", "${activity.javaClass.simpleName} started")
        }

        override fun onActivityResumed(activity: Activity) {
            breadcrumb("activity", "${activity.javaClass.simpleName} resumed")
        }

        override fun onActivityPaused(activity: Activity) {
            breadcrumb("activity", "${activity.javaClass.simpleName} paused")
        }

        override fun onActivityStopped(activity: Activity) {
            breadcrumb("activity", "${activity.javaClass.simpleName} stopped")
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

        override fun onActivityDestroyed(activity: Activity) {
            breadcrumb("activity", "${activity.javaClass.simpleName} destroyed")
        }
    }

    @RequiresApi(30)
    private fun reasonName(reason: Int): String {
        if (Build.VERSION.SDK_INT < 30) return reason.toString()
        return when (reason) {
            ApplicationExitInfo.REASON_UNKNOWN -> "UNKNOWN"
            ApplicationExitInfo.REASON_EXIT_SELF -> "EXIT_SELF"
            ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED"
            ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY"
            ApplicationExitInfo.REASON_CRASH -> "CRASH"
            ApplicationExitInfo.REASON_CRASH_NATIVE -> "CRASH_NATIVE"
            ApplicationExitInfo.REASON_ANR -> "ANR"
            ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "INITIALIZATION_FAILURE"
            ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "PERMISSION_CHANGE"
            ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "EXCESSIVE_RESOURCE_USAGE"
            ApplicationExitInfo.REASON_USER_REQUESTED -> "USER_REQUESTED"
            ApplicationExitInfo.REASON_USER_STOPPED -> "USER_STOPPED"
            ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "DEPENDENCY_DIED"
            ApplicationExitInfo.REASON_OTHER -> "OTHER"
            else -> "REASON_$reason"
        }
    }

    private fun privateLogDir(context: Context): File {
        val external = runCatching { context.getExternalFilesDir(null) }.getOrNull()
        val base = external ?: runCatching { context.filesDir }.getOrNull() ?: context.cacheDir
        return File(base, "logs").apply { mkdirs() }
    }

    private fun readCapped(input: java.io.InputStream, max: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buf = ByteArray(4_096)
        var total = 0
        while (total < max) {
            val n = input.read(buf, 0, minOf(buf.size, max - total))
            if (n < 0) break
            out.write(buf, 0, n)
            total += n
        }
        return out.toByteArray()
    }

    private fun grant(context: Context, intent: Intent, uri: Uri) {
        val matches = context.packageManager.queryIntentActivities(intent, 0)
        for (resolve in matches) {
            context.grantUriPermission(
                resolve.activityInfo.packageName,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }
}
