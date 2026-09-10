package ir.mhajisoft.miniaccountant.di

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.mhajisoft.miniaccountant.data.local.db.MiniAccountantDatabase
import ir.mhajisoft.miniaccountant.data.local.db.MIGRATION_1_2
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MiniAccountantDatabase {
        val file = context.getDatabasePath(MiniAccountantDatabase.FILE_NAME)
        file.parentFile?.mkdirs()
        return Room.databaseBuilder(
            context,
            MiniAccountantDatabase::class.java,
            file.absolutePath,
        )
            .addMigrations(MIGRATION_1_2)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
}
