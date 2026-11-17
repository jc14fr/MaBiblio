package fr.exemple.biblio

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personnes")
data class Personne(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nom: String
)