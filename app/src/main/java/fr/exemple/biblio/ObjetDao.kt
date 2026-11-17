package fr.exemple.biblio

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ObjetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun ajouter(objet: Objet): Long

    @Update
    suspend fun modifier(objet: Objet)

    @Delete
    suspend fun supprimer(objet: Objet)

    @Query("SELECT * FROM objets WHERE categorieId = :categorieId ORDER BY titre")
    fun dansCategorie(categorieId: Long): Flow<List<Objet>>

    @Query("SELECT * FROM objets ORDER BY titre")
    fun tous(): Flow<List<Objet>>

    @Query("SELECT COUNT(*) FROM objets WHERE statut = 'POSSEDE'")
    fun nombre(): Flow<Int>

    @Query("SELECT * FROM objets")
    suspend fun lireTout(): List<Objet>

    @Query("DELETE FROM objets")
    suspend fun effacerTout()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun ajouterTout(liste: List<Objet>)

    @Query("SELECT * FROM objets WHERE statut = 'SOUHAITE' ORDER BY titre")
    fun souhaits(): Flow<List<Objet>>
}