package fr.exemple.biblio

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Titre de section avec un compteur facultatif
@Composable
fun TitreSection(texte: String, nombre: Int? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    ) {
        Text(
            texte,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        if (nombre != null) {
            Spacer(Modifier.width(8.dp))
            Badge { Text(nombre.toString()) }
        }
    }
}

// Tuile d'action : toutes de même taille, texte sur une ligne
@Composable
fun TuileAction(texte: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                texte,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

// Ligne en carte, avec pastille facultative et appui long facultatif
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LigneCarte(
    titre: String,
    sousTitre: String? = null,
    pastille: Int? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(titre, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                if (sousTitre != null) {
                    Text(sousTitre, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (pastille != null) {
                Spacer(Modifier.width(8.dp))
                Badge { Text(pastille.toString()) }
            }
        }
    }
}