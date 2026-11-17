package fr.exemple.biblio

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// Version lisible d'un prêt, avec le titre de l'objet et le nom de la personne
data class PretAvecDetails(
    val id: Long,
    val titre: String,
    val nom: String,
    val sens: Sens,
    val dateDebut: Long,
    val dateRetour: Long?
)

@Dao
interface PretDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun ajouter(pret: Pret): Long

    @Query("UPDATE prets SET dateRetour = :date WHERE id = :id")
    suspend fun marquerRendu(id: Long, date: Long)

    // Tous les prêts d'un sens donné (PRETE ou EMPRUNTE), les plus récents d'abord
    @Query("""
        SELECT p.id AS id, o.titre AS titre, pe.nom AS nom, p.sens AS sens,
               p.dateDebut AS dateDebut, p.dateRetour AS dateRetour
        FROM prets p
        INNER JOIN objets o ON o.id = p.objetId
        INNER JOIN personnes pe ON pe.id = p.personneId
        WHERE p.sens = :sens
        ORDER BY p.dateDebut DESC
    """)
    fun pretsAvecDetails(sens: Sens): Flow<List<PretAvecDetails>>

        // Nombre de prêts non rendus pour un sens donné
    @Query("SELECT COUNT(*) FROM prets WHERE sens = :sens AND dateRetour IS NULL")
    fun nombreEnCours(sens: Sens): Flow<Int>

    @Query("SELECT * FROM prets")
    suspend fun lireTout(): List<Pret>

    @Query("DELETE FROM prets")
    suspend fun effacerTout()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun ajouterTout(liste: List<Pret>)

    @Query("""
        SELECT p.id AS id, o.titre AS titre, pe.nom AS nom, p.sens AS sens,
               p.dateDebut AS dateDebut, p.dateRetour AS dateRetour
        FROM prets p
        INNER JOIN objets o ON o.id = p.objetId
        INNER JOIN personnes pe ON pe.id = p.personneId
        WHERE p.personneId = :personneId
        ORDER BY p.dateDebut DESC
    """)
    fun pretsDePersonne(personneId: Long): Flow<List<PretAvecDetails>>
    
}