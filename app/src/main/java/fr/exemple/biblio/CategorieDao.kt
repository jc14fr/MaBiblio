package fr.exemple.biblio

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategorieDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun ajouter(categorie: Categorie): Long

    @Update
    suspend fun modifier(categorie: Categorie)

    @Delete
    suspend fun supprimer(categorie: Categorie)

    @Query("SELECT * FROM categories WHERE parentId IS NULL ORDER BY nom")
    fun premierNiveau(): Flow<List<Categorie>>

    @Query("SELECT * FROM categories WHERE parentId = :parentId ORDER BY nom")
    fun sousCategories(parentId: Long): Flow<List<Categorie>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun parId(id: Long): Categorie?

    @Query("SELECT * FROM categories")
    suspend fun lireTout(): List<Categorie>

    @Query("DELETE FROM categories")
    suspend fun effacerTout()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun ajouterTout(liste: List<Categorie>)

    @Query("""
        WITH RECURSIVE descendants(id) AS (
            SELECT id FROM categories WHERE id = :id
            UNION ALL
            SELECT c.id FROM categories c
            INNER JOIN descendants d ON c.parentId = d.id
        )
        SELECT COUNT(*) FROM objets
        WHERE categorieId IN (SELECT id FROM descendants)
    """)
    fun nombreObjetsRecursif(id: Long): Flow<Int>

}