package fr.exemple.biblio

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

const val EMAIL_CONTACT = "apk@jc14fr.ovh"

// Bouton « ⋮ » en haut à droite, avec le menu Version et Me contacter
@Composable
fun MenuTroisPoints() {
    val contexte = LocalContext.current
    val versionInstallee = remember {
        try {
            contexte.packageManager.getPackageInfo(contexte.packageName, 0).versionName
        } catch (e: Exception) {
            "inconnue"
        }
    }
    var menuOuvert by remember { mutableStateOf(false) }
    var versionAffichee by remember { mutableStateOf(false) }

    IconButton(onClick = { menuOuvert = true }) {
        Text("⋮", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
    }

    DropdownMenu(expanded = menuOuvert, onDismissRequest = { menuOuvert = false }) {
        DropdownMenuItem(
            text = { Text("Ma Biblio, version $versionInstallee") },
            onClick = {
                menuOuvert = false
                versionAffichee = true
            }
        )
        DropdownMenuItem(
            text = { Text("Me contacter") },
            onClick = {
                menuOuvert = false
                val intention = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:$EMAIL_CONTACT")
                    putExtra(Intent.EXTRA_SUBJECT, "Ma Biblio")
                }
                contexte.startActivity(intention)
            }
        )
    }

    if (versionAffichee) {
        AlertDialog(
            onDismissRequest = { versionAffichee = false },
            title = { Text("Version") },
            text = { Text("Ma Biblio, version $versionInstallee") },
            confirmButton = {
                TextButton(onClick = { versionAffichee = false }) { Text("OK") }
            }
        )
    }
}