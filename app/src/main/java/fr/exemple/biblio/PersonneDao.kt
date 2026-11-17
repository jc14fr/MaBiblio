package fr.exemple.biblio

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonneDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun ajouter(personne: Personne): Long

    @Query("SELECT * FROM personnes ORDER BY nom")
    fun toutes(): Flow<List<Personne>>

    @Query("SELECT * FROM personnes")
    suspend fun lireTout(): List<Personne>

    @Query("DELETE FROM personnes")
    suspend fun effacerTout()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun ajouterTout(liste: List<Personne>)
}