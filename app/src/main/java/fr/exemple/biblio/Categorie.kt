package fr.exemple.biblio

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = Categorie::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("parentId")]
)
data class Categorie(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nom: String,
    val parentId: Long? = null   // null = catégorie de premier niveau
)