package fr.exemple.biblio

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.io.File

// Ligne simple
@Composable
fun LigneObjet(objet: Objet, onClick: () -> Unit) {
    val suffixe = if (objet.statut == Statut.SOUHAITE) " (souhait)" else ""
    Text(
        text = objet.titre + suffixe,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    )
}

// Case avec la pochette (ou un rectangle « Vue indisponible ») et le titre dessous.
// La zone image a toujours la même taille, avec ou sans pochette.
@Composable
fun CaseObjet(objet: Objet, onClick: () -> Unit) {
    val image = remember(objet.couvertureUrl) {
        objet.couvertureUrl?.let { chemin ->
            if (File(chemin).exists()) BitmapFactory.decodeFile(chemin)?.asImageBitmap() else null
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f),
            contentAlignment = Alignment.Center
        ) {
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = objet.titre,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Vue indisponible",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Text(
            text = objet.titre,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2
        )
    }
}

// Grille de cases, adaptée à la largeur de l'écran
@Composable
fun GrilleObjets(objets: List<Objet>, onOuvrir: (Objet) -> Unit) {
    LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 110.dp)) {
        items(objets) { objet ->
            CaseObjet(objet = objet, onClick = { onOuvrir(objet) })
        }
    }
}