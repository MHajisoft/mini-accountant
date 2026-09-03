package ir.mhajisoft.miniaccountant.data.cloud

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.mhajisoft.miniaccountant.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveBackupClient @Inject constructor(
    @ApplicationContext private val context: Context,
) : CloudBackupClient {
    override val kind: CloudKind = CloudKind.DRIVE

    override fun availability(): CloudAvailability {
        val gms = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context)
        val hasGms = gms == ConnectionResult.SUCCESS
        val hasClient = BuildConfig.DRIVE_SERVER_CLIENT_ID.isNotBlank()
        return when {
            !hasGms -> CloudAvailability(
                kind,
                available = false,
                reasonFa = "خدمات گوگل روی این دستگاه نصب نیست. پشتیبان‌گیری ابری گوگل‌درایو غیرفعال است. از نسخهٔ کافه‌بازار یا پشتیبان محلی (SAF) استفاده کنید.",
            )
            !hasClient -> CloudAvailability(
                kind,
                available = false,
                reasonFa = "شناسهٔ OAuth درایو تنظیم نشده. DRIVE_SERVER_CLIENT_ID را در local.properties قرار دهید. پشتیبان محلی همیشه کار می‌کند.",
            )
            else -> CloudAvailability(
                kind,
                available = false,
                reasonFa = "آپلود گوگل‌درایو پس از تکمیل OAuth (Identity AuthorizationClient، scope drive.appdata) فعال می‌شود. پشتیبان محلی کار می‌کند.",
            )
        }
    }

    override suspend fun upload(fileName: String, bytes: ByteArray): Result<CloudSnapshot> {
        val avail = availability()
        if (!avail.available) {
            return Result.failure(IllegalStateException(avail.reasonFa))
        }
        return Result.failure(
            IllegalStateException("آپلود درایو فقط پس از تکمیل OAuth در local.properties فعال می‌شود."),
        )
    }

    override suspend fun download(remoteId: String): Result<ByteArray> =
        Result.failure(IllegalStateException("دانلود درایو پیکربندی نشده است."))

    override suspend fun listRecent(): Result<List<CloudSnapshot>> = Result.success(emptyList())

    override suspend fun delete(remoteId: String): Result<Unit> = Result.success(Unit)
}

@Singleton
class OneDriveBackupClient @Inject constructor() : CloudBackupClient {
    override val kind: CloudKind = CloudKind.ONEDRIVE

    override fun availability(): CloudAvailability {
        val hasClient = BuildConfig.ONEDRIVE_CLIENT_ID.isNotBlank()
        return CloudAvailability(
            kind,
            available = false,
            reasonFa = if (!hasClient) {
                "ورود به وان‌درایو نیاز به ONEDRIVE_CLIENT_ID در local.properties دارد. پشتیبان محلی همیشه در دسترس است."
            } else {
                "آپلود وان‌درایو پس از ثبت برنامه در Azure (MSAL + Graph approot) فعال می‌شود. پشتیبان محلی کار می‌کند."
            },
        )
    }

    override suspend fun upload(fileName: String, bytes: ByteArray): Result<CloudSnapshot> {
        val avail = availability()
        if (!avail.available) return Result.failure(IllegalStateException(avail.reasonFa))
        return Result.failure(
            IllegalStateException("آپلود وان‌درایو فقط پس از ثبت برنامه در Azure فعال می‌شود."),
        )
    }

    override suspend fun download(remoteId: String): Result<ByteArray> =
        Result.failure(IllegalStateException("دانلود وان‌درایو پیکربندی نشده است."))

    override suspend fun listRecent(): Result<List<CloudSnapshot>> = Result.success(emptyList())

    override suspend fun delete(remoteId: String): Result<Unit> = Result.success(Unit)
}
