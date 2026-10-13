package ir.mhajisoft.hesabres.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ir.mhajisoft.hesabres.R
import ir.mhajisoft.hesabres.diag.DiagnosticLog
import ir.mhajisoft.hesabres.ui.components.FinanceCard
import ir.mhajisoft.hesabres.ui.components.PrimaryWideButton
import ir.mhajisoft.hesabres.ui.components.ScreenHeader
import ir.mhajisoft.hesabres.ui.components.SecondaryWideButton
import ir.mhajisoft.hesabres.ui.components.SectionLabel
import kotlinx.coroutines.launch

@Composable
fun StartupDiagnosticsScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(stringResource(R.string.crash_reports))
        Text(
            stringResource(R.string.startup_failed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        CrashReportSection()
    }
}

@Composable
fun DiagnosticsScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(stringResource(R.string.crash_reports), stringResource(R.string.crash_reports_body))
        CrashReportSection()
    }
}

@Composable
fun CrashReportSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val needsLegacyPermission = Build.VERSION.SDK_INT <= 28
    var storageGranted by remember {
        mutableStateOf(
            !needsLegacyPermission ||
                context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        storageGranted = granted
        if (granted) DiagnosticLog.publishActiveBestEffort()
    }
    val shareLabel = stringResource(R.string.share_logs)
    val clearedLabel = stringResource(R.string.logs_cleared)
    val emptyLabel = stringResource(R.string.logs_empty)
    val failedLabel = stringResource(R.string.logs_share_failed)
    FinanceCard {
        SectionLabel(stringResource(R.string.crash_reports))
        Text(
            stringResource(R.string.crash_reports_path),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (needsLegacyPermission && !storageGranted) {
            SecondaryWideButton(stringResource(R.string.allow_log_folder)) {
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        PrimaryWideButton(shareLabel, onClick = {
            scope.launch {
                val intent = DiagnosticLog.shareIntent(context)
                if (intent == null) {
                    status = emptyLabel
                    return@launch
                }
                DiagnosticLog.grantShareTargets(context, intent)
                val chooser = Intent.createChooser(intent, shareLabel).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val host = context.findMainActivity() ?: context
                runCatching { host.startActivity(chooser) }
                    .onFailure {
                        DiagnosticLog.recordCaught("share", it)
                        status = failedLabel
                    }
            }
        })
        SecondaryWideButton(stringResource(R.string.clear_logs)) { confirmClear = true }
        status?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.crash_reports)) },
            text = { Text(stringResource(R.string.clear_logs_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    scope.launch {
                        DiagnosticLog.clearLogs()
                        status = clearedLabel
                    }
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
