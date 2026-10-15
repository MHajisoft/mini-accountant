package ir.mhajisoft.hesabres.data.local.db

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import ir.mhajisoft.hesabres.diag.DiagnosticLog

val MIGRATION_1_2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        DiagnosticLog.breadcrumb("db", "migration 1 to 2 start")
        try {
        connection.execSQL("ALTER TABLE people ADD COLUMN firstName TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE people ADD COLUMN lastName TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE people ADD COLUMN email TEXT")
        connection.execSQL("ALTER TABLE people ADD COLUMN instagram TEXT")
        connection.execSQL("ALTER TABLE people ADD COLUMN telegram TEXT")
        connection.execSQL("ALTER TABLE people ADD COLUMN whatsapp TEXT")
        connection.execSQL("ALTER TABLE people ADD COLUMN avatarColor INTEGER NOT NULL DEFAULT 4278228590")
        connection.execSQL("UPDATE people SET firstName = name WHERE firstName = ''")
        connection.execSQL("ALTER TABLE bank_cards ADD COLUMN personId TEXT")
        connection.execSQL("ALTER TABLE bank_accounts ADD COLUMN personId TEXT")
            DiagnosticLog.breadcrumb("db", "migration 1 to 2 done")
        } catch (t: Throwable) {
            DiagnosticLog.recordCaught("db.migration.1to2", t)
            throw t
        }
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override suspend fun migrate(connection: SQLiteConnection) {
        DiagnosticLog.breadcrumb("db", "migration 2 to 3 start")
        try {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS person_social_links (
                id TEXT NOT NULL PRIMARY KEY,
                personId TEXT NOT NULL,
                label TEXT NOT NULL,
                value TEXT NOT NULL,
                sortOrder INTEGER NOT NULL,
                FOREIGN KEY(personId) REFERENCES people(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_person_social_links_personId ON person_social_links(personId)")
        connection.execSQL(
            """
            INSERT INTO person_social_links (id, personId, label, value, sortOrder)
            SELECT 'ig-' || id, id, 'اینستاگرام', instagram, 0
            FROM people
            WHERE instagram IS NOT NULL AND trim(instagram) != ''
            """.trimIndent(),
        )
        connection.execSQL(
            """
            INSERT INTO person_social_links (id, personId, label, value, sortOrder)
            SELECT 'tg-' || id, id, 'تلگرام', telegram, 1
            FROM people
            WHERE telegram IS NOT NULL AND trim(telegram) != ''
            """.trimIndent(),
        )
        connection.execSQL(
            """
            INSERT INTO person_social_links (id, personId, label, value, sortOrder)
            SELECT 'wa-' || id, id, 'واتساپ', whatsapp, 2
            FROM people
            WHERE whatsapp IS NOT NULL AND trim(whatsapp) != ''
            """.trimIndent(),
        )
            DiagnosticLog.breadcrumb("db", "migration 2 to 3 done")
        } catch (t: Throwable) {
            DiagnosticLog.recordCaught("db.migration.2to3", t)
            throw t
        }
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override suspend fun migrate(connection: SQLiteConnection) {
        DiagnosticLog.breadcrumb("db", "migration 3 to 4 start")
        try {
            connection.execSQL("ALTER TABLE people ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS notes (
                    id TEXT NOT NULL PRIMARY KEY,
                    body TEXT NOT NULL,
                    jalaliYear INTEGER NOT NULL,
                    jalaliMonth INTEGER NOT NULL,
                    jalaliDay INTEGER NOT NULL,
                    occurredAt INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS cheques (
                    id TEXT NOT NULL PRIMARY KEY,
                    side TEXT NOT NULL,
                    amount INTEGER NOT NULL,
                    counterparty TEXT NOT NULL,
                    personId TEXT,
                    jalaliYear INTEGER NOT NULL,
                    jalaliMonth INTEGER NOT NULL,
                    jalaliDay INTEGER NOT NULL,
                    dueAt INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    note TEXT NOT NULL,
                    createdAt INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS installments (
                    id TEXT NOT NULL PRIMARY KEY,
                    side TEXT NOT NULL,
                    title TEXT NOT NULL,
                    totalAmount INTEGER NOT NULL,
                    installmentAmount INTEGER NOT NULL,
                    count INTEGER NOT NULL,
                    paidCount INTEGER NOT NULL,
                    counterparty TEXT NOT NULL,
                    personId TEXT,
                    jalaliYear INTEGER NOT NULL,
                    jalaliMonth INTEGER NOT NULL,
                    jalaliDay INTEGER NOT NULL,
                    nextDueAt INTEGER NOT NULL,
                    note TEXT NOT NULL,
                    createdAt INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS month_budgets (
                    jalaliYear INTEGER NOT NULL,
                    jalaliMonth INTEGER NOT NULL,
                    limitRials INTEGER NOT NULL,
                    PRIMARY KEY(jalaliYear, jalaliMonth)
                )
                """.trimIndent(),
            )
            DiagnosticLog.breadcrumb("db", "migration 3 to 4 done")
        } catch (t: Throwable) {
            DiagnosticLog.recordCaught("db.migration.3to4", t)
            throw t
        }
    }
}
