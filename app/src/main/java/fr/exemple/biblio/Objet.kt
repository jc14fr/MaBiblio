package fr.exemple.biblio

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class Statut { POSSEDE, SOUHAITE, EXTERIEUR }

@Entity(
    tableName = "objets",
    foreignKeys = [
        ForeignKey(
            entity = Categorie::class,
            parentColumns = ["id"],
            childColumns = ["categorieId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categorieId")]
)
data class Objet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titre: String,
    val auteur: String = "",
    val isbn: String? = null,
    val categorieId: Long? = null,
    val serie: String? = null,
    val numeroTome: Int? = null,
    @ColumnInfo(defaultValue = "POSSEDE") val statut: Statut = Statut.POSSEDE,
    // Chemin local de la pochette ou de la photo (vide si aucune)
    val couvertureUrl: String? = null
)

class ConvertisseurStatut {
    @TypeConverter
    fun statutVersTexte(statut: Statut): String = statut.name

    @TypeConverter
    fun texteVersStatut(texte: String): Statut = Statut.valueOf(texte)
}