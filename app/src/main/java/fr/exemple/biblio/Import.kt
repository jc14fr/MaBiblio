package fr.exemple.biblio

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipFile

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
private fun estUneArchiveZip(fichier: File): Boolean =
    fichier.inputStream().use { flux ->
        val debut = ByteArray(2)
        flux.read(debut) == 2 && debut[0] == 'P'.code.toByte() && debut[1] == 'K'.code.toByte()
    }

// Lit biblio.json dans le ZIP et copie les pochettes dans le dossier de l'appli
private fun lireZip(fichier: File, dossierPochettes: File): String? =
    ZipFile(fichier).use { zip ->
        val entreeJson = zip.getEntry("biblio.json") ?: return null
        val texte = zip.getInputStream(entreeJson).use { it.readBytes().toString(Charsets.UTF_8) }

        zip.entries().asSequence()
            .filter { !it.isDirectory && it.name.startsWith("pochettes/") }
            .forEach { entree ->
                File(dossierPochettes, File(entree.name).name).outputStream().use { sortie ->
                    zip.getInputStream(entree).use { it.copyTo(sortie) }
                }
            }
        texte
    }

// Importe un .zip (JSON + pochettes) ou un ancien .json seul
suspend fun importerSauvegarde(contexte: Context, base: BiblioDatabase, source: Uri): Boolean {
    val tampon = File(contexte.cacheDir, "import.tmp")
    return try {
        // 1. Copie locale du fichier choisi
        contexte.contentResolver.openInputStream(source)?.use { entree ->
            tampon.outputStream().use { entree.copyTo(it) }
        } ?: return false

        // 2. Lecture
        val dossierPochettes = dossierPochettes(contexte)
        val texte: String = if (estUneArchiveZip(tampon)) {
            lireZip(tampon, dossierPochettes) ?: return false
        } else {
            tampon.readText(Charsets.UTF_8)
        }

        // 3. Analyse du JSON
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

        // 4. Remplacement dans une transaction : tout ou rien
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
    } finally {
        tampon.delete()
    }
}