package fr.exemple.biblio

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

// Construit le ZIP dans le cache, le vérifie, puis le copie à l'emplacement choisi
suspend fun ecrireSauvegarde(contexte: Context, base: BiblioDatabase, destination: Uri): Boolean {
    return try {
        val texte = construireSauvegarde(base)
        val pochettes = base.objetDao().lireTout()
            .mapNotNull { it.couvertureUrl }
            .distinct()
            .map { File(it) }
            .filter { it.exists() }

        // 1. ZIP complet en local
        val tampon = File(contexte.cacheDir, "export.zip")
        ZipOutputStream(tampon.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("biblio.json"))
            zip.write(texte.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            pochettes.forEach { fichier ->
                zip.putNextEntry(ZipEntry("pochettes/${fichier.name}"))
                fichier.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }

        // 2. Vérification : le ZIP doit pouvoir être relu et contenir biblio.json
        ZipFile(tampon).use { zip ->
            zip.getEntry("biblio.json") ?: error("biblio.json absent du ZIP")
        }

        // 3. Copie vers l'emplacement choisi
        contexte.contentResolver.openOutputStream(destination, "wt")?.use { sortie ->
            tampon.inputStream().use { it.copyTo(sortie) }
        } ?: return false

        tampon.delete()
        true
    } catch (e: Exception) {
        false
    }
}