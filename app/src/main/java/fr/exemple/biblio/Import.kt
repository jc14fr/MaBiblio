package fr.exemple.biblio

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipInputStream

private fun JSONObject.texteOuNull(cle: String): String? =
    if (isNull(cle)) null else getString(cle)

private fun JSONObject.longOuNull(cle: String): Long? =
    if (isNull(cle)) null else getLong(cle)

private fun JSONObject.intOuNull(cle: String): Int? =
    if (isNull(cle)) null else getInt(cle)

private fun <T> JSONArray.versListe(transformer: (JSONObject) -> T): List<T> =
    (0 until length()).map { transformer(getJSONObject(it)) }

// Une sous-catégorie ne peut être insérée qu'après son parent
private fun ordonnerCategories(categories: List<Categorie>): List<Categorie> {
    val restantes = categories.toMutableList()
    val ordonnees = mutableListOf<Categorie>()
    val dejaAjoutees = mutableSetOf<Long>()

    while (restantes.isNotEmpty()) {
        val prets = restantes.filter { it.parentId == null || it.parentId in dejaAjoutees }
        if (prets.isEmpty()) {
            ordonnees.addAll(restantes)
            break
        }
        ordonnees.addAll(prets)
        dejaAjoutees.addAll(prets.map { it.id })
        restantes.removeAll(prets.toSet())
    }
    return ordonnees
}

// Un ZIP commence par les deux lettres « PK »
private fun estUneArchiveZip(octets: ByteArray): Boolean =
    octets.size >= 2 && octets[0] == 'P'.code.toByte() && octets[1] == 'K'.code.toByte()

// Importe un fichier .zip (JSON + pochettes) ou un ancien .json seul
suspend fun importerSauvegarde(contexte: Context, base: BiblioDatabase, source: Uri): Boolean {
    return try {
        val octets = contexte.contentResolver.openInputStream(source)
            ?.use { it.readBytes() }
            ?: return false

        val dossierPochettes = dossierPochettes(contexte)

        val texte: String = if (estUneArchiveZip(octets)) {
            var texteJson: String? = null
            ZipInputStream(ByteArrayInputStream(octets)).use { zip ->
                var entree = zip.nextEntry
                while (entree != null) {
                    if (!entree.isDirectory) {
                        if (entree.name == "biblio.json") {
                            texteJson = zip.readBytes().toString(Charsets.UTF_8)
                        } else if (entree.name.startsWith("pochettes/")) {
                            File(dossierPochettes, File(entree.name).name)
                                .writeBytes(zip.readBytes())
                        }
                    }
                    zip.closeEntry()
                    entree = zip.nextEntry
                }
            }
            texteJson ?: return false
        } else {
            String(octets, Charsets.UTF_8)
        }

        val racine = JSONObject(texte)

        val categories = racine.getJSONArray("categories").versListe { o ->
            Categorie(
                id = o.getLong("id"),
                nom = o.getString("nom"),
                parentId = o.longOuNull("parentId")
            )
        }

        val objets = racine.getJSONArray("objets").versListe { o ->
            Objet(
                id = o.getLong("id"),
                titre = o.getString("titre"),
                auteur = o.optString("auteur", ""),
                isbn = o.texteOuNull("isbn"),
                categorieId = o.longOuNull("categorieId"),
                serie = o.texteOuNull("serie"),
                numeroTome = o.intOuNull("numeroTome"),
                statut = Statut.valueOf(o.optString("statut", "POSSEDE")),
                // Le JSON contient seulement le nom du fichier : on recrée le chemin local
                couvertureUrl = o.texteOuNull("couvertureUrl")
                    ?.let { File(dossierPochettes, it).absolutePath }
            )
        }

        val personnes = racine.getJSONArray("personnes").versListe { o ->
            Personne(id = o.getLong("id"), nom = o.getString("nom"))
        }

        val prets = racine.getJSONArray("prets").versListe { o ->
            Pret(
                id = o.getLong("id"),
                objetId = o.getLong("objetId"),
                personneId = o.getLong("personneId"),
                sens = Sens.valueOf(o.getString("sens")),
                dateDebut = o.getLong("dateDebut"),
                dateRetour = o.longOuNull("dateRetour")
            )
        }

        base.withTransaction {
            base.pretDao().effacerTout()
            base.personneDao().effacerTout()
            base.objetDao().effacerTout()
            base.categorieDao().effacerTout()

            base.categorieDao().ajouterTout(ordonnerCategories(categories))
            base.objetDao().ajouterTout(objets)
            base.personneDao().ajouterTout(personnes)
            base.pretDao().ajouterTout(prets)
        }
        true
    } catch (e: Exception) {
        false
    }
}