package ia.ankherth.grease.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Clase de base de datos Room para almacenar el historial de PDFs
 * Proporciona acceso a los DAOs y maneja la creación y migración de la base de datos
 */
@Database(entities = [PdfHistoryEntity::class, ChangelogEntry::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pdfHistoryDao(): PdfHistoryDao
    abstract fun changelogDao(): ChangelogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migración de versión 1 a 2: Añade scrollOffset y isFavorite
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Añadir columna scrollOffset con valor por defecto 0
                db.execSQL("ALTER TABLE pdf_history ADD COLUMN scrollOffset REAL NOT NULL DEFAULT 0.0")
                // Añadir columna isFavorite con valor por defecto false (0)
                db.execSQL("ALTER TABLE pdf_history ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pdftoon_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
