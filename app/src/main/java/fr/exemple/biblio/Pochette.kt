package fr.exemple.biblio

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

// Doit correspondre à android:authorities dans le manifeste
const val AUTORITE_FICHIERS = "fr.exemple.biblio.fileprovider"

// Dossier privé de l'appli où sont rangées les pochettes et les photos
fun dossierPochettes(contexte: Context): File =
    File(contexte.filesDir, "pochettes").apply { mkdirs() }

// Télécharge une pochette et renvoie le chemin du fichier local, ou null en cas d'échec
suspend fun telechargerPochette(contexte: Context, url: String): String? =
    withContext(Dispatchers.IO) {
        try {
            val fichier = File(dossierPochettes(contexte), "pochette_${System.currentTimeMillis()}.jpg")
            val connexion = URL(url).openConnection() as HttpURLConnection
            connexion.connectTimeout = 8000
            connexion.readTimeout = 8000
            if (connexion.responseCode != 200) {
                connexion.disconnect()
                return@withContext null
            }
            connexion.inputStream.use { entree ->
                fichier.outputStream().use { sortie -> entree.copyTo(sortie) }
            }
            connexion.disconnect()
            fichier.absolutePath
        } catch (e: Exception) {
            null
        }
    }

// Prépare un fichier vide pour une photo prise avec l'appareil photo
fun preparerFichierPhoto(contexte: Context): Pair<File, Uri> {
    val fichier = File(dossierPochettes(contexte), "photo_${System.currentTimeMillis()}.jpg")
    val uri = FileProvider.getUriForFile(contexte, AUTORITE_FICHIERS, fichier)
    return fichier to uri
}