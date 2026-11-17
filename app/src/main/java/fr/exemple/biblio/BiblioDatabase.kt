package fr.exemple.biblio

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Ajoute la colonne « statut » aux objets existants, en gardant les données
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE objets ADD COLUMN statut TEXT NOT NULL DEFAULT 'POSSEDE'")
    }
}

@Database(
    entities = [Categorie::class, Objet::class, Personne::class, Pret::class],
    version = 2
)
@TypeConverters(Convertisseurs::class, ConvertisseurStatut::class)
abstract class BiblioDatabase : RoomDatabase() {
    abstract fun categorieDao(): CategorieDao
    abstract fun objetDao(): ObjetDao
    abstract fun personneDao(): PersonneDao
    abstract fun pretDao(): PretDao

    companion object {
        @Volatile
        private var instance: BiblioDatabase? = null

        fun get(context: Context): BiblioDatabase =
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                BiblioDatabase::class.java,
                "biblio.db"
            )
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
    }
}