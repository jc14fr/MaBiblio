package fr.exemple.biblio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max

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

// Réduit la photo (côté max 800 px, JPEG qualité 80) : quelques centaines de Ko au lieu de plusieurs Mo
fun compresserPhoto(fichier: File, dimensionMax: Int = 800, qualite: Int = 80) {
    val bornes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(fichier.absolutePath, bornes)
    if (bornes.outWidth <= 0) return

    // Décodage à taille réduite pour éviter de charger la photo pleine résolution
    var echantillon = 1
    while (max(bornes.outWidth, bornes.outHeight) / (echantillon * 2) >= dimensionMax) {
        echantillon *= 2
    }
    val original = BitmapFactory.decodeFile(
        fichier.absolutePath,
        BitmapFactory.Options().apply { inSampleSize = echantillon }
    ) ?: return

    val facteur = dimensionMax.toFloat() / max(original.width, original.height)
    val reduite = if (facteur < 1f) {
        Bitmap.createScaledBitmap(
            original,
            (original.width * facteur).toInt(),
            (original.height * facteur).toInt(),
            true
        )
    } else {
        original
    }

    // On écrase le fichier photo par sa version réduite
    fichier.outputStream().use { reduite.compress(Bitmap.CompressFormat.JPEG, qualite, it) }

    if (reduite !== original) reduite.recycle()
    original.recycle()
}