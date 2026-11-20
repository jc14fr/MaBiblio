package fr.exemple.biblio

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background

const val EMAIL_CONTACT = "apk@jc14fr.ovh"
const val URL_CAFE = "https://revolut.me/jc14fr"

// Bouton « ⋮ » en haut à droite, avec un en-tête, puis contact et café
@Composable
fun MenuTroisPoints() {
    val contexte = LocalContext.current
    var menuOuvert by remember { mutableStateOf(false) }

    IconButton(onClick = { menuOuvert = true }) {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = "Menu"
        )
    }

    DropdownMenu(
        expanded = menuOuvert,
        onDismissRequest = { menuOuvert = false },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        // En-tête : nom de l'app et version
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "Ma Biblio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider()

        DropdownMenuItem(
            text = { Text("Me contacter") },
            leadingIcon = {
                Icon(Icons.Filled.Email, contentDescription = null)
            },
            onClick = {
                menuOuvert = false
                val intention = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:$EMAIL_CONTACT")
                    putExtra(Intent.EXTRA_SUBJECT, "Ma Biblio")
                }
                ouvrir(contexte, intention)
            }
        )

        DropdownMenuItem(
            text = { Text("M'offrir un café") },
            leadingIcon = {
                Icon(Icons.Filled.Favorite, contentDescription = null)
            },
            onClick = {
                menuOuvert = false
                ouvrir(contexte, Intent(Intent.ACTION_VIEW, Uri.parse(URL_CAFE)))
            }
        )
    }
}

// Lance une intention sans planter si aucune appli ne peut la traiter
private fun ouvrir(contexte: android.content.Context, intention: Intent) {
    try {
        contexte.startActivity(intention)
    } catch (e: ActivityNotFoundException) {
        // Aucune appli mail ou navigateur installée : rien à faire
    }
}