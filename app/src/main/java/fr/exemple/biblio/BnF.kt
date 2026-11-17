package fr.exemple.biblio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.xml.parsers.DocumentBuilderFactory

private const val NS_DC = "http://purl.org/dc/elements/1.1/"

// Convertit un ISBN-13 commençant par 978 en ISBN-10. Renvoie null si ce n'est pas possible.
fun isbn13VersIsbn10(isbn13: String): String? {
    if (isbn13.length != 13 || !isbn13.startsWith("978")) return null
    val base = isbn13.substring(3, 12)
    if (base.any { !it.isDigit() }) return null
    var somme = 0
    for (i in base.indices) somme += (base[i] - '0') * (10 - i)
    val reste = (11 - somme % 11) % 11
    val controle = if (reste == 10) "X" else reste.toString()
    return base + controle
}

// Interroge le catalogue de la BnF avec un ISBN. Renvoie null si rien n'est trouvé ou en cas d'erreur.
suspend fun chercherBnf(isbn: String): InfosLivre? = withContext(Dispatchers.IO) {
    try {
        val propre = isbn.replace("-", "").trim()
        val requete = "bib.isbn all \"$propre\""
        val adresse = "https://catalogue.bnf.fr/api/SRU?version=1.2&operation=searchRetrieve" +
            "&recordSchema=dublincore&maximumRecords=1&query=" +
            URLEncoder.encode(requete, "UTF-8").replace("+", "%20")

        val connexion = URL(adresse).openConnection() as HttpURLConnection
        connexion.connectTimeout = 8000
        connexion.readTimeout = 8000

        val document = connexion.inputStream.use { flux ->
            DocumentBuilderFactory.newInstance()
                .apply { isNamespaceAware = true }
                .newDocumentBuilder()
                .parse(flux)
        }
        connexion.disconnect()

        val titre = document.getElementsByTagNameNS(NS_DC, "title")
            .item(0)?.textContent?.trim().orEmpty()
        if (titre.isBlank()) return@withContext null

        val auteur = document.getElementsByTagNameNS(NS_DC, "creator")
            .item(0)?.textContent?.trim().orEmpty()

        InfosLivre(titre = titre, auteur = auteur, urlCouverture = null)
    } catch (e: Exception) {
        null
    }
}