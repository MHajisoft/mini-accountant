package ir.mhajisoft.miniaccountant.data.local.db

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_1_2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
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
    }
}
