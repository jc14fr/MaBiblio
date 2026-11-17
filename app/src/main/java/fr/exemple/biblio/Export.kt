package fr.exemple.biblio

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// Écrit une archive ZIP : biblio.json + le dossier pochettes/
suspend fun ecrireSauvegarde(contexte: Context, base: BiblioDatabase, destination: Uri): Boolean {
    val texte = construireSauvegarde(base)
    val pochettes = base.objetDao().lireTout()
        .mapNotNull { it.couvertureUrl }
        .distinct()
        .map { File(it) }
        .filter { it.exists() }

    contexte.contentResolver.openOutputStream(destination)?.use { flux ->
        ZipOutputStream(flux).use { zip ->
            zip.putNextEntry(ZipEntry("biblio.json"))
            zip.write(texte.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            pochettes.forEach { fichier ->
                zip.putNextEntry(ZipEntry("pochettes/${fichier.name}"))
                fichier.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    } ?: return false
    return true
}