package fr.exemple.biblio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Les informations trouvées dans Open Library
data class InfosLivre(
    val titre: String,
    val auteur: String,
    val urlCouverture: String?
)

// Interroge Open Library avec un ISBN. Renvoie null si rien n'est trouvé ou en cas d'erreur.
suspend fun chercherParIsbn(isbn: String): InfosLivre? = withContext(Dispatchers.IO) {
    try {
        val isbnPropre = isbn.replace("-", "").trim()
        val adresse = "https://openlibrary.org/api/books" +
            "?bibkeys=ISBN:$isbnPropre&format=json&jscmd=data"

        val connexion = URL(adresse).openConnection() as HttpURLConnection
        connexion.connectTimeout = 8000
        connexion.readTimeout = 8000
        val texte = connexion.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        connexion.disconnect()

        val racine = JSONObject(texte)
        val cle = "ISBN:$isbnPropre"
        if (!racine.has(cle)) return@withContext null

        val livre = racine.getJSONObject(cle)
        val titre = livre.optString("title", "")
        if (titre.isBlank()) return@withContext null

        val auteurs = livre.optJSONArray("authors")
        val auteur = if (auteurs != null && auteurs.length() > 0) {
            auteurs.getJSONObject(0).optString("name", "")
        } else {
            ""
        }

        val couverture = livre.optJSONObject("cover")
        val urlCouverture = couverture?.optString("medium")?.ifBlank { null }

        InfosLivre(titre = titre, auteur = auteur, urlCouverture = urlCouverture)
    } catch (e: Exception) {
        null
    }
}