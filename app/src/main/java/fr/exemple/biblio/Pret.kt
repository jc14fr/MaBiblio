package fr.exemple.biblio

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class Sens { PRETE, EMPRUNTE }

@Entity(
    tableName = "prets",
    foreignKeys = [
        ForeignKey(
            entity = Objet::class,
            parentColumns = ["id"],
            childColumns = ["objetId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Personne::class,
            parentColumns = ["id"],
            childColumns = ["personneId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("objetId"), Index("personneId")]
)
data class Pret(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val objetId: Long,
    val personneId: Long,
    val sens: Sens,
    val dateDebut: Long,          // date en millisecondes
    val dateRetour: Long? = null  // vide = pas encore rendu
)

class Convertisseurs {
    @TypeConverter
    fun sensVersTexte(sens: Sens): String = sens.name

    @TypeConverter
    fun texteVersSens(texte: String): Sens = Sens.valueOf(texte)
}