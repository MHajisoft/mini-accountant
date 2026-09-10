package ir.mhajisoft.hesabres.data.local.db

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

val MIGRATION_2_3 = object : Migration(2, 3) {
    override suspend fun migrate(connection: SQLiteConnection) {
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
    }
}
